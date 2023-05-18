package com.example.v2raytest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import dev.dev7.lib.v2ray.V2rayController
import dev.dev7.lib.v2ray.utils.AppConfigs

class MainActivity : AppCompatActivity() {

    private lateinit var connection: Button
    private lateinit var connection_speed: TextView
    private lateinit var connection_traffic: TextView
    private lateinit var connection_time: TextView
    private lateinit var server_delay: TextView
    private lateinit var connected_server_delay: TextView
    private lateinit var connection_mode: TextView
    private lateinit var core_version: TextView
    private lateinit var v2ray_json_config: EditText

    var v2rayBroadCastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            connection_time.text = "connection time : " + intent!!.extras!!.getString("DURATION")
            connection_speed.text =
                "connection speed : " + intent.extras!!.getString("UPLOAD_SPEED") + " | " + intent.extras!!
                    .getString("DOWNLOAD_SPEED")
            connection_traffic.text =
                "connection traffic : " + intent.extras!!.getString("UPLOAD_TRAFFIC") + " | " + intent.extras!!
                    .getString("DOWNLOAD_TRAFFIC")
            when (intent.extras!!.getSerializable("STATE").toString()) {
                "V2RAY_CONNECTED" -> connection.text =
                    "CONNECTED"

                "V2RAY_DISCONNECTED" -> connection.text = "DISCONNECTED"
                "V2RAY_CONNECTING" -> connection.text = "CONNECTING"
                else -> {}
            }
        }

    }

    var activityResultLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode != RESULT_OK) {
            Toast.makeText(this, "Permission not granted.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connection = findViewById(R.id.btn_connection)
        connection_speed = findViewById(R.id.connection_speed)
        connection_time = findViewById(R.id.connection_duration)
        server_delay = findViewById(R.id.server_delay)
        connection_mode = findViewById(R.id.connection_mode)
        connection_traffic = findViewById(R.id.connection_traffic)
        connected_server_delay = findViewById(R.id.connected_server_delay)
        v2ray_json_config = findViewById(R.id.v2ray_json_config)
        core_version = findViewById(R.id.core_version)

        // Checking the previous state value each time the activity is opened
        when (V2rayController.getConnectionState().toString()) {
            "V2RAY_CONNECTED" -> connection.text = "CONNECTED"
            "V2RAY_DISCONNECTED" -> connection.text = "DISCONNECTED"
            "V2RAY_CONNECTING" -> connection.text = "CONNECTING"
            else -> {}
        }

        connection_mode.text =
            "connection mode : " + V2rayController.getConnectionMode() + " (tap to toggle)"
        v2ray_json_config.setText(getConfigContent())
        core_version.text = "v" + ", " + V2rayController.getCoreVersion()

        // Checking for access to tunneling the entire device network
        // Checking for access to tunneling the entire device network
        val intent = VpnService.prepare(applicationContext)
        if (intent != null) {
            // we have not permission so taking it :)
            activityResultLauncher.launch(intent)
        }

        connection.setOnClickListener { view: View? ->
            if (V2rayController.getConnectionState() == AppConfigs.V2RAY_STATES.V2RAY_DISCONNECTED) {
                // in StartV2ray function we can set remark to show that on notification.
                // StartV2ray function steel need json config of v2ray. Unfortunately, it does not accept URI or base64 type at the moment.
                V2rayController.StartV2ray(
                    applicationContext,
                    "Default",
                    v2ray_json_config.text.toString(),
                    null
                )
                connected_server_delay.text = "connected server delay : measuring..."
                //getConnectedV2rayServerDelay function need a text view for now
                Handler().postDelayed({
                    V2rayController.getConnectedV2rayServerDelay(
                        applicationContext,
                        connected_server_delay
                    )
                }, 1000)
            } else {
                connected_server_delay.text = "connected server delay : wait for connection"
                V2rayController.StopV2ray(applicationContext)
            }
        }

        // Another way to check the connection delay of a config without connecting to it.

        // Another way to check the connection delay of a config without connecting to it.
        server_delay.setOnClickListener { view: View? ->
            server_delay.text = "server delay : measuring..."
            Handler().post {
                server_delay.text = "server delay : " + V2rayController.getV2rayServerDelay(
                    v2ray_json_config.text.toString()
                )
            }
        }

        // The connection mode determines whether the entire phone is tunneled or whether an internal proxy (http , socks) is run

        // The connection mode determines whether the entire phone is tunneled or whether an internal proxy (http , socks) is run
        connection_mode.setOnClickListener { view: View? ->
            // Oh,Sorry you can`t change connection mode when you have active connection. I can`t solve that for now. ):
            if (V2rayController.getConnectionState() != AppConfigs.V2RAY_STATES.V2RAY_DISCONNECTED) {
                Toast.makeText(
                    this,
                    "You can change the connection type only when you do not have an active connection.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (V2rayController.getConnectionMode() == AppConfigs.V2RAY_CONNECTION_MODES.PROXY_ONLY) {
                V2rayController.changeConnectionMode(AppConfigs.V2RAY_CONNECTION_MODES.VPN_TUN)
            } else {
                V2rayController.changeConnectionMode(AppConfigs.V2RAY_CONNECTION_MODES.PROXY_ONLY)
            }
            connection_mode.text =
                "connection mode : " + V2rayController.getConnectionMode() + " (tap to toggle)"
        }

        registerReceiver(v2rayBroadCastReceiver, IntentFilter("V2RAY_CONNECTION_INFO"))
    }


    private fun getConfigContent(): String { return "" }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(v2rayBroadCastReceiver)
    }
}
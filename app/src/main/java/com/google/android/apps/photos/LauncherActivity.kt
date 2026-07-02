package com.google.android.apps.photos

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class LauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        val grantButton = findViewById<Button>(R.id.grant_button)
        val explanationText = findViewById<TextView>(R.id.explanation_text)

        grantButton.setOnClickListener {
            requestSpecialPermission()
        }
    }

    override fun onStart() {
        super.onStart()
        checkPermissionStatus()
    }

    private fun checkPermissionStatus() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }

        val grantButton = findViewById<Button>(R.id.grant_button)
        if (hasPermission) {
            grantButton.text = getString(R.string.permission_granted)
            grantButton.isEnabled = false
        } else {
            grantButton.text = getString(R.string.grant_permission)
            grantButton.isEnabled = true
        }
    }

    private fun requestSpecialPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            Toast.makeText(this, "Special permission not required for this Android version", Toast.LENGTH_SHORT).show()
        }
    }
}

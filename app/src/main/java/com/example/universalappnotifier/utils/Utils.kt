package com.example.universalappnotifier.utils

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object Utils {

    val staticList = mutableListOf<String>()

    fun isInternetAvailable(context: Context): Boolean {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).run {
            return this.getNetworkCapabilities(this.activeNetwork)?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) ?: false
        }
    }

    fun singleOptionAlertDialog(
        context: Context,
        title: String,
        msg: String,
        optionName: String,
        isCancelable: Boolean,
        positiveMethod: () -> Unit = {}
    ) {
        val dialogBuilder = MaterialAlertDialogBuilder(context)

        // set message of alert dialog
        dialogBuilder.setMessage(msg)

            // if the dialog is cancelable
            .setCancelable(isCancelable)
            // positive button text and action
            .setPositiveButton(optionName) { dialog, id ->
                dialog.cancel()
                positiveMethod()
            }

        // create dialog box
        val alert = dialogBuilder.create()
        // set title for alert dialog box
        alert.setTitle(title)
        // show alert dialog
        alert.show()
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

    fun formatTimeFromTimestamp(timestamp: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        // Parse the timestamp string
        val date = inputFormat.parse(timestamp)

        // Format the parsed date to the desired time format
        return outputFormat.format(date)
    }

    fun convertUTCToIndianTime(utcTime: String): String {
        // Parse the UTC time string
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS")
        val utcDateTime = LocalDateTime.parse(utcTime, formatter)

        // Convert UTC time to Indian time (IST)
        val utcZone = ZoneId.of("UTC")
        val indianZone = ZoneId.of("Asia/Kolkata")
        val utcZonedDateTime = ZonedDateTime.of(utcDateTime, utcZone)
        val indianZonedDateTime = utcZonedDateTime.withZoneSameInstant(indianZone)

        // Format the Indian time
        val indianFormatter = DateTimeFormatter.ofPattern("hh:mm a")
        return indianZonedDateTime.format(indianFormatter)
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

    fun dpToPx(dp: Int, context: Context): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    fun createFadedVersionOfColor(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).roundToInt()
        val red: Int = Color.red(color)
        val green: Int = Color.green(color)
        val blue: Int = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    fun formatDate(unformattedDate: Date): String {
        //formats the date in "27 Mar 2024 (Wed)" format
        return SimpleDateFormat("dd MMM yyyy (EEE)", Locale.getDefault())
            .format(unformattedDate)
    }

}
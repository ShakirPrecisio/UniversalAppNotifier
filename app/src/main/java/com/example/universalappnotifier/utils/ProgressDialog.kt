package com.example.universalappnotifier.utils

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.TextView
import com.example.universalappnotifier.R

object ProgressDialog {

    private var dialog: Dialog? = null
    private var isVisible = false

    fun initialize(context: Context) {
        dialog = Dialog(context)
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.setContentView(R.layout.custom_progress_dialog)
        dialog?.setCancelable(false)
    }

    fun show(message: String) {
        dialog?.findViewById<TextView>(R.id.tv_message)?.text = message
        dialog?.show()
        isVisible = true
    }

    fun dismiss() {
        if (isVisible && dialog != null) {
            dialog?.dismiss()
            dialog = null
            isVisible = false
        }
    }
}
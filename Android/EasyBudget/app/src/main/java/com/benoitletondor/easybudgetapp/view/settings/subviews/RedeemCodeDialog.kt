package com.benoitletondor.easybudgetapp.view.settings.subviews

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import com.benoitletondor.easybudgetapp.R
import com.benoitletondor.easybudgetapp.helper.Logger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.URLEncoder

fun Context.openRedeemCodeDialog() {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_redeem_voucher, null)
    val voucherEditText = dialogView.findViewById<View>(R.id.voucher) as EditText

    val builder = MaterialAlertDialogBuilder(this)
        .setTitle(R.string.voucher_redeem_dialog_title)
        .setMessage(R.string.voucher_redeem_dialog_message)
        .setView(dialogView)
        .setPositiveButton(R.string.voucher_redeem_dialog_cta) { dialog, _ ->
            dialog.dismiss()

            val promocode = voucherEditText.text.toString()
            if (promocode.trim { it <= ' ' }.isEmpty()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.voucher_redeem_error_dialog_title)
                    .setMessage(R.string.voucher_redeem_error_code_invalid_dialog_message)
                    .setPositiveButton(R.string.ok) { dialog12, _ -> dialog12.dismiss() }
                    .show()

                return@setPositiveButton
            }

            try {
                val url = "https://play.google.com/redeem?code=" + URLEncoder.encode(promocode, "UTF-8")
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Logger.error("Error while redeeming promocode", e)
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.iab_purchase_error_title)
                    .setMessage(resources.getString(R.string.iab_purchase_error_message, "Error redeeming promo code"))
                    .setPositiveButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                    .show()
            }
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }

    val dialog = builder.show()

    // Directly show keyboard when the dialog pops
    voucherEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        // Check if the device doesn't have a physical keyboard
        if (hasFocus) {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }
    voucherEditText.requestFocus()
}
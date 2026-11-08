package com.logie.wowforever

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

/**
 * The loading screen over Winlator's game screen until the first window shows (patches/winlator/0004 puts it in
 * place of Winlator's own): the logo, a spinner and what is happening.
 */
class GameLoading(private val activity: Activity) {
    private var dialog: Dialog? = null
    private var label: TextView? = null

    @Synchronized
    fun show(textResId: Int) {
        if (activity.isFinishing || activity.isDestroyed) return
        val text = activity.getString(textResId)
        label?.text = text
        if (dialog?.isShowing == true) return
        val created = dialog ?: Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen).also { d ->
            val density = activity.resources.displayMetrics.density
            val box = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(Color.rgb(7, 11, 17))
                addView(ImageView(activity).apply {
                    setImageResource(R.drawable.forever_logo)
                }, LinearLayout.LayoutParams((120 * density).toInt(), (120 * density).toInt()))
                addView(ProgressBar(activity).apply { isIndeterminate = true }, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = (24 * density).toInt() })
                label = TextView(activity).apply {
                    this.text = text
                    setTextColor(Color.rgb(147, 161, 180))
                    textSize = 15f
                    gravity = Gravity.CENTER
                }
                addView(label, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (16 * density).toInt()
                })
            }
            d.setContentView(box)
            d.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
            d.setCancelable(false)
            dialog = d
        }
        runCatching { created.show() }
    }

    fun showOnUiThread(textResId: Int) = activity.runOnUiThread { show(textResId) }

    @Synchronized
    fun close() {
        runCatching { dialog?.dismiss() }
    }

    fun closeOnUiThread() = activity.runOnUiThread { close() }

    fun isShowing(): Boolean = dialog?.isShowing == true
}

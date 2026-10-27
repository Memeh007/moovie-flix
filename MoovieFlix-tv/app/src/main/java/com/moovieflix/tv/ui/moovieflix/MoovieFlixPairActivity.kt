package com.moovieflix.tv.ui.moovieflix

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.moovieflix.tv.core.auth.MoovieFlixPairing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * First-run screen: finds the Moovie Flix PC on the network and pairs with the code shown on it.
 * Plain Android views so it works with any remote / on-screen keyboard.
 */
class MoovieFlixPairActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var status: TextView
    private lateinit var address: EditText
    private lateinit var code: EditText
    private lateinit var pairButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (32 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#0D0D0D")); setPadding(pad, pad, pad, pad)
        }
        fun text(s: String, size: Float, color: String = "#FFFFFF") = TextView(this).apply { text = s; textSize = size; setTextColor(Color.parseColor(color)); gravity = Gravity.CENTER }
        root.addView(text("Moovie Flix", 34f, "#E50914"))
        root.addView(text("Pair this TV with Moovie Flix on your PC", 20f))
        root.addView(text("On the PC open My Movies \u2192 \"Pair a TV\" to see the code.", 15f, "#AAAAAA"))
        status = text("Looking for your PC on the network\u2026", 16f, "#FFD54F")
        root.addView(status)
        val w = (420 * resources.displayMetrics.density).toInt()
        code = EditText(this).apply {
            hint = "6-digit code"; inputType = InputType.TYPE_CLASS_NUMBER; textSize = 26f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); layoutParams = LinearLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        root.addView(code)
        pairButton = Button(this).apply { text = "Pair"; setOnClickListener { doPair() } }
        root.addView(pairButton)
        root.addView(text("PC address (filled in automatically; type it only if it was not found)", 13f, "#AAAAAA"))
        address = EditText(this).apply {
            hint = "e.g. 192.168.0.20"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI; textSize = 16f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); layoutParams = LinearLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        root.addView(address)
        root.addView(Button(this).apply { text = "Search again"; setOnClickListener { search() } })
        setContentView(root)
        code.requestFocus()
        search()
    }

    private fun search() {
        status.text = "Looking for your PC on the network\u2026"
        scope.launch {
            val found = MoovieFlixPairing.discover(this@MoovieFlixPairActivity)
            if (found.isEmpty()) {
                status.text = "PC not found. Make sure Moovie Flix is open on the PC and both are on the same Wi-Fi, or type the PC address below."
            } else {
                address.setText(found.first().url)
                status.text = "Found: ${found.first().name}. Enter the code shown on the PC."
            }
        }
    }

    private fun doPair() {
        val addr = address.text.toString().trim(); val c = code.text.toString().trim()
        if (addr.isBlank()) { status.text = "PC address is missing. Tap \"Search again\" or type it."; return }
        if (c.length < 6) { status.text = "Enter the 6-digit code shown on the PC."; return }
        pairButton.isEnabled = false; status.text = "Pairing\u2026"
        scope.launch {
            MoovieFlixPairing.pair(this@MoovieFlixPairActivity, addr, c).fold(
                onSuccess = { status.text = "Paired! Starting Moovie Flix\u2026"; MoovieFlixPairing.restartApp(this@MoovieFlixPairActivity) },
                onFailure = { status.text = "Pairing failed: ${it.message}"; pairButton.isEnabled = true },
            )
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}

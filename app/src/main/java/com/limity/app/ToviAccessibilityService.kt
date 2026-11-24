package com.limity.app

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ToviAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var overlay: View? = null
    private var activePackage: String? = null
    private var lastIntervenedPackage: String? = null
    private var allowedUntil = 0L
    private var selectedPackages: Set<String> = emptySet()
    private val prefs by lazy { getSharedPreferences("tovi_settings", MODE_PRIVATE) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        selectedPackages = prefs.getStringSet("selected_apps", emptySet())?.toSet() ?: emptySet()
        resetAllowanceIfNewDay()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return
        selectedPackages = prefs.getStringSet("selected_apps", emptySet())?.toSet() ?: emptySet()
        activePackage = pkg
        if (pkg !in selectedPackages) {
            lastIntervenedPackage = null
            allowedUntil = 0L
            removeIntervention()
            return
        }
        if (SystemClock.elapsedRealtime() < allowedUntil) return
        if (overlay == null && lastIntervenedPackage != pkg) {
            lastIntervenedPackage = pkg
            showIntervention(pkg)
        }
    }

    override fun onInterrupt() {
        removeIntervention()
    }

    override fun onDestroy() {
        removeIntervention()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun resetAllowanceIfNewDay() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        if (prefs.getString("last_reset_day", "") != today) {
            val daily = prefs.getInt("daily_tokens", 120)
            prefs.edit().putString("last_reset_day", today).putInt("tokens_remaining", daily).apply()
        }
    }

    private fun showIntervention(pkg: String) {
        if (overlay != null) return
        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) { "this app" }
        val remaining = prefs.getInt("tokens_remaining", prefs.getInt("daily_tokens", 120))
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            setBackgroundColor(Color.argb(242, 13, 19, 15))
            isClickable = true
            isFocusable = true
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(25, 27, 25, 25)
            background = rounded(Color.rgb(29, 42, 34), 30)
        }
        val eyebrow = label("A MOMENT FOR YOU", 11f, Color.rgb(170, 190, 174), true)
        eyebrow.letterSpacing = .14f
        card.addView(eyebrow)
        card.addView(space(17))
        card.addView(label("You opened $appName.", 27f, Color.rgb(244, 246, 240), true))
        card.addView(space(12))
        card.addView(label("Was this what you meant to do?", 17f, Color.rgb(220, 228, 219), false))
        card.addView(space(14))
        card.addView(label("You chose to be more intentional with your time. Take a breath and decide whether you want to stay here.", 14f, Color.rgb(177, 192, 180), false))
        card.addView(space(22))
        val balance = if (remaining > 0) "$remaining tokens left today" else "Your daily allowance is used up"
        card.addView(label(balance, 13f, Color.rgb(131, 201, 157), true))
        card.addView(space(22))
        val back = actionButton("Go back", Color.rgb(131, 201, 157), Color.rgb(16, 25, 19))
        card.addView(back, matchWidth())
        card.addView(space(10))
        val continueButton = actionButton("Continue for 5 minutes", Color.rgb(54, 70, 59), Color.WHITE)
        card.addView(continueButton, matchWidth())
        card.addView(space(12))
        card.addView(label("No judgement. Just a choice.", 11f, Color.rgb(145, 160, 148), false).apply { gravity = Gravity.CENTER })
        root.addView(card, LinearLayout.LayoutParams(-1, -2))
        back.setOnClickListener {
            removeIntervention()
            lastIntervenedPackage = null
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
        continueButton.setOnClickListener {
            removeIntervention()
            allowedUntil = SystemClock.elapsedRealtime() + 5 * 60 * 1000L
            startAllowanceTimer(pkg)
            handler.postDelayed({
                if (activePackage == pkg && overlay == null) {
                    allowedUntil = 0L
                    lastIntervenedPackage = null
                    showIntervention(pkg)
                }
            }, 5 * 60 * 1000L)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        try {
            windowManager?.addView(root, params)
            overlay = root
        } catch (_: Exception) {
            overlay = null
        }
    }

    private fun startAllowanceTimer(pkg: String) {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (activePackage != pkg || overlay != null) return
                val remaining = prefs.getInt("tokens_remaining", prefs.getInt("daily_tokens", 120)) - 1
                prefs.edit().putInt("tokens_remaining", remaining.coerceAtLeast(0)).apply()
                if (remaining <= 0) {
                    allowedUntil = 0L
                    lastIntervenedPackage = null
                    showIntervention(pkg)
                } else if (SystemClock.elapsedRealtime() < allowedUntil) {
                    handler.postDelayed(this, 60_000L)
                }
            }
        }, 60_000L)
    }

    private fun removeIntervention() {
        val current = overlay ?: return
        try { windowManager?.removeView(current) } catch (_: Exception) { }
        overlay = null
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
        setLineSpacing(5f, 1f)
    }

    private fun actionButton(text: String, backgroundColor: Int, textColor: Int) = Button(this).apply {
        this.text = text
        isAllCaps = false
        textSize = 15f
        setTextColor(textColor)
        background = rounded(backgroundColor, 16)
        minHeight = dp(52)
    }

    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun matchWidth() = LinearLayout.LayoutParams(-1, dp(54))
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
}

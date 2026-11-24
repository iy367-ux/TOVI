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

class ToviAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var overlay: View? = null
    private var activePackage: String? = null
    private var lastIntervenedPackage: String? = null
    private var allowedUntil = 0L
    private var selectedPackages: Set<String> = emptySet()
    private var sessionStartedAt = 0L
    private var sessionPackage: String? = null
    private var sessionDay = ""
    private var lastPromptDay = ""
    private val prefs by lazy { getSharedPreferences("tovi_settings", MODE_PRIVATE) }

    private val usageTicker = object : Runnable {
        override fun run() {
            flushForegroundTime()
            handler.postDelayed(this, 15_000L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        selectedPackages = prefs.getStringSet("selected_apps", emptySet())?.toSet() ?: emptySet()
        UsageLedger.resetIfNewDay(this)
        handler.removeCallbacks(usageTicker)
        handler.postDelayed(usageTicker, 15_000L)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                    event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED)) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        flushForegroundTime()
        UsageLedger.resetIfNewDay(this)
        selectedPackages = prefs.getStringSet("selected_apps", emptySet())?.toSet() ?: emptySet()
        activePackage = pkg
        if (pkg !in selectedPackages) {
            sessionPackage = null
            sessionStartedAt = 0L
            lastIntervenedPackage = null
            allowedUntil = 0L
            removeIntervention()
            return
        }

        if (sessionPackage != pkg) {
            sessionPackage = pkg
            sessionStartedAt = SystemClock.elapsedRealtime()
        }
        if (lastIntervenedPackage != pkg && overlay == null) {
            lastIntervenedPackage = pkg
            showIntervention(pkg)
        }
    }

    override fun onInterrupt() {
        flushForegroundTime()
        removeIntervention()
    }

    override fun onDestroy() {
        flushForegroundTime()
        removeIntervention()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun flushForegroundTime() {
        val pkg = sessionPackage ?: return
        if (sessionStartedAt == 0L || pkg != activePackage || pkg !in selectedPackages) return
        val now = SystemClock.elapsedRealtime()
        val elapsed = ((now - sessionStartedAt) / 1000L).coerceAtLeast(0L)
        if (elapsed > 0L) {
            UsageLedger.addForegroundSeconds(this, pkg, elapsed)
            sessionStartedAt = now
            maybeShowMilestone()
            if (UsageLedger.remainingTokens(this) <= 0) {
                allowedUntil = 0L
                lastIntervenedPackage = null
                if (overlay == null) showIntervention(pkg)
            }
        }
    }

    private fun maybeShowMilestone() {
        val daily = prefs.getInt("daily_tokens", 120).coerceAtLeast(1)
        val used = (UsageLedger.totalSeconds(this) / 60L).toInt()
        val percent = (used * 100 / daily).coerceAtMost(100)
        val milestones = listOf(25, 50, 75, 90, 100)
        val hit = milestones.lastOrNull { percent >= it } ?: return
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
        val key = "milestone_${today}_${hit}"
        if (prefs.getBoolean(key, false)) return
        prefs.edit().putBoolean(key, true).apply()
        // Milestones are recorded locally; notifications are added after notification permission is granted.
    }

    private fun showIntervention(pkg: String) {
        if (overlay != null) return
        // Time spent reading the pause screen is not counted as app usage.
        sessionStartedAt = 0L
        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) { "this app" }
        val remaining = UsageLedger.remainingTokens(this)
        val exhausted = remaining <= 0
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(22), dp(22), dp(22))
            setBackgroundColor(Color.argb(246, 13, 19, 15))
            isClickable = true
            isFocusable = true
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(25), dp(27), dp(25), dp(25))
            background = rounded(Color.rgb(29, 42, 34), 28)
        }
        val eyebrow = label(if (exhausted) "THAT'S YOUR LIMIT FOR TODAY" else "A MOMENT FOR YOU", 11f, Color.rgb(170, 190, 174), true)
        eyebrow.letterSpacing = .14f
        card.addView(eyebrow)
        card.addView(space(17))
        card.addView(label(if (exhausted) "Your time is up." else "You opened $appName.", 27f, Color.rgb(244, 246, 240), true))
        card.addView(space(12))
        card.addView(label(
            if (exhausted) "You've used your daily TOVI allowance. Give yourself a chance to do something away from the screen."
            else "Was this what you meant to do? Take a breath and decide whether you want to stay here.",
            16f, Color.rgb(220, 228, 219), false
        ))
        card.addView(space(16))
        card.addView(label(if (exhausted) "Your allowance resets tomorrow." else "$remaining tokens left today", 13f, Color.rgb(131, 201, 157), true))
        card.addView(space(22))
        val back = actionButton(if (exhausted) "Back to my day" else "Go back", Color.rgb(131, 201, 157), Color.rgb(16, 25, 19))
        card.addView(back, matchWidth())
        if (!exhausted) {
            card.addView(space(10))
            val continueButton = actionButton("Continue for 5 minutes", Color.rgb(54, 70, 59), Color.WHITE)
            card.addView(continueButton, matchWidth())
            continueButton.setOnClickListener {
                removeIntervention()
                allowedUntil = SystemClock.elapsedRealtime() + 5 * 60 * 1000L
                sessionPackage = pkg
                sessionStartedAt = SystemClock.elapsedRealtime()
                handler.postDelayed({
                    flushForegroundTime()
                    if (activePackage == pkg && overlay == null) {
                        allowedUntil = 0L
                        lastIntervenedPackage = null
                        showIntervention(pkg)
                    }
                }, 5 * 60 * 1000L)
            }
        }
        card.addView(space(12))
        card.addView(label("No judgement. Just a choice.", 11f, Color.rgb(145, 160, 148), false).apply { gravity = Gravity.CENTER })
        root.addView(card, LinearLayout.LayoutParams(-1, -2))
        back.setOnClickListener {
            removeIntervention()
            allowedUntil = 0L
            lastIntervenedPackage = null
            performGlobalAction(GLOBAL_ACTION_HOME)
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
        setLineSpacing(dp(5).toFloat(), 1f)
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

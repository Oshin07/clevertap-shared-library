package com.clevertap.sampleapp.scenario

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.sharedeventlib.SharedEventTracker

/**
 * SCENARIO 2 — Host app has its OWN CleverTap integration (Account A).
 *
 * This activity demonstrates that two CleverTap instances coexist in the same process
 * without interference.
 *
 * Expected behavior:
 *   - Events pushed via CleverTapAPI.getDefaultInstance() go to Account A ONLY.
 *   - Events pushed via SharedEventTracker go to Common Account C ONLY.
 *   - Neither instance receives the other's events.
 *   - Neither instance's configuration affects the other.
 *
 * HOW TO VERIFY:
 *   1. Open Account A dashboard → Events tab → confirm only "App Event from Host" appears.
 *   2. Open Common Account C dashboard → Events tab → confirm only library events appear.
 *   3. Check logcat for both instances' log output. They have separate prefixes.
 *
 * Prerequisites:
 *   - APP_ACCOUNT_ID / APP_ACCOUNT_TOKEN set in AndroidManifest.xml
 *   - COMMON_ACCOUNT_ID / COMMON_ACCOUNT_TOKEN set in LibraryConfig.kt
 *   - Both must be different CleverTap accounts
 */
class WithCleverTapScenarioActivity : AppCompatActivity() {

    private lateinit var logView: TextView

    // The host app's default CleverTap instance (Account A).
    // Accessed via getDefaultInstance() — NOT via the library's internal instance.
    private val hostCleverTap: CleverTapAPI? by lazy {
        CleverTapAPI.getDefaultInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())

        log("=== Scenario 2: Host App has Its Own CleverTap ===")
        log("")
        log("Host App CleverTap (Account A):")
        log("  Instance present: ${hostCleverTap != null}")
        log("  Account: APP_ACCOUNT_ID (from AndroidManifest)")
        log("")
        log("Shared Library CleverTap (Account C):")
        log("  SharedEventTracker.isInitialized() = ${SharedEventTracker.isInitialized()}")
        log("  sourceApp = '${SharedEventTracker.getSourceApp()}'")
        log("  Account: COMMON_ACCOUNT_ID (baked into library)")
        log("")
        log("These are independent instances. Fire events below to verify separation.")
    }

    private fun buildLayout(): ScrollView {
        val scroll = ScrollView(this)
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        ll.addView(titleView("Scenario 2 — Host App Has CleverTap"))

        logView = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFF1B4F72.toInt())
            setBackgroundColor(0xFFEBF5FB.toInt())
            setPadding(24, 24, 24, 24)
        }
        ll.addView(logView)
        ll.addView(spacer())

        // ── HOST APP EVENTS (go to Account A) ────────────────────────────────
        ll.addView(sectionLabel("HOST APP EVENTS → Account A"))

        ll.addView(btn("Push 'App Event from Host' (Account A only)") {
            hostCleverTap?.pushEvent("App Event from Host", hashMapOf("origin" to "host_app"))
                ?: log("[WARN] Host CleverTap not initialized — check AndroidManifest.xml credentials")
            log("[SENT → Account A] 'App Event from Host'")
            log("  VERIFY: This event should NOT appear in Common Account C")
        })

        ll.addView(btn("Update host user profile (Account A only)") {
            hostCleverTap?.pushProfile(hashMapOf("Name" to "Test User", "Plan" to "Premium"))
                ?: log("[WARN] Host CleverTap not initialized")
            log("[SENT → Account A] Profile update")
        })

        ll.addView(spacer())

        // ── LIBRARY EVENTS (go to Common Account C) ──────────────────────────
        ll.addView(sectionLabel("SHARED LIBRARY EVENTS → Common Account C"))

        ll.addView(btn("trackHomeScreenViewed() → Account C only") {
            SharedEventTracker.trackHomeScreenViewed()
            log("[SENT → Account C] Home Screen Viewed")
            log("  VERIFY: This event should NOT appear in Account A")
        })

        ll.addView(btn("trackContentPlayed(\"c_001\", \"News\") → Account C only") {
            SharedEventTracker.trackContentPlayed("c_001", "News")
            log("[SENT → Account C] Content Played | source_app='${SharedEventTracker.getSourceApp()}'")
        })

        ll.addView(btn("trackOrderPlaced(\"o_001\", 59.99) → Account C only") {
            SharedEventTracker.trackOrderPlaced("o_001", 59.99)
            log("[SENT → Account C] Order Placed")
        })

        ll.addView(spacer())

        // ── SEPARATION VERIFICATION ───────────────────────────────────────────
        ll.addView(sectionLabel("SEPARATION VERIFICATION"))

        ll.addView(btn("Fire both instances simultaneously") {
            // Host app event
            hostCleverTap?.pushEvent("Simultaneous Host Event", hashMapOf("test" to "separation"))
            // Library event
            SharedEventTracker.trackFeatureDiscovered("Simultaneous Test")

            log("[SENT] Both instances fired simultaneously.")
            log("  Account A should have: 'Simultaneous Host Event'")
            log("  Account C should have: 'Feature Discovered' with source_app='${SharedEventTracker.getSourceApp()}'")
            log("  Neither should have the other's event.")
        })

        ll.addView(btn("Attempt to get library instance via reflection (should fail)") {
            // This simulates an attempt to bypass the encapsulation
            try {
                // The library uses 'internal' Kotlin visibility.
                // From a different module, internal classes are not accessible.
                // This try block demonstrates the attempt and documents why it fails.
                val clazz = Class.forName("com.clevertap.sharedeventlib.internal.CleverTapManager")
                log("[REFLECTION] CleverTapManager class found via reflection!")
                log("[WARN] Class is visible via reflection. Use ProGuard to obfuscate in production.")
                try {
                    val field = clazz.getDeclaredField("cleverTapInstance")
                    field.isAccessible = true
                    log("[REFLECTION] cleverTapInstance field found: ${field.get(null)}")
                } catch (e: NoSuchFieldException) {
                    log("[OK] cleverTapInstance field not accessible: ${e.message}")
                }
            } catch (e: ClassNotFoundException) {
                log("[OK] CleverTapManager not accessible from host app module: ${e.message}")
            }
        })

        ll.addView(btn("Clear log") { logView.text = "" })

        scroll.addView(ll)
        return scroll
    }

    private fun log(msg: String) { logView.append("$msg\n") }

    private fun titleView(text: String) = TextView(this).apply {
        this.text = text
        textSize = 18f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setPadding(0, 0, 0, 16)
    }

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text
        textSize = 11f
        letterSpacing = 0.1f
        setTextColor(0xFF888888.toInt())
        setPadding(4, 16, 0, 4)
    }

    private fun btn(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 11f
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.setMargins(0, 8, 0, 0) }
    }

    private fun spacer() = android.view.View(this).also {
        it.layoutParams = LinearLayout.LayoutParams(1, 24)
    }
}

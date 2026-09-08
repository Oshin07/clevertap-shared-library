package com.clevertap.sampleapp.scenario

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.clevertap.sharedeventlib.SharedEventTracker

/**
 * SCENARIO 1 — Host app has NO existing CleverTap integration.
 *
 * This activity demonstrates that the shared library works correctly when the host app
 * has not configured any CleverTap instance of its own.
 *
 * Expected behavior:
 *   - All events fired via SharedEventTracker appear in Common Account C only.
 *   - No host-app CleverTap events exist (because none are configured).
 *   - Library initializes without any manifest CleverTap credentials.
 *
 * To simulate this scenario properly:
 *   1. Comment out the "Host app's own CleverTap" block in SampleApplication.kt
 *   2. Comment out CLEVERTAP_ACCOUNT_ID / CLEVERTAP_TOKEN in AndroidManifest.xml
 *   3. Rebuild and run
 *   4. Verify events in Common Account C dashboard
 */
class NoCleverTapScenarioActivity : AppCompatActivity() {

    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = buildLayout()
        setContentView(layout)
        log("=== Scenario 1: No Host CleverTap ===")
        log("SharedEventTracker.isInitialized() = ${SharedEventTracker.isInitialized()}")
        log("SharedEventTracker.getSourceApp()   = '${SharedEventTracker.getSourceApp()}'")
        log("")
        log("Expected: all events below go to Common Account C only.")
        log("Verify: open CleverTap dashboard for Account C and check Events tab.")
    }

    private fun buildLayout(): ScrollView {
        val scroll = ScrollView(this)
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        ll.addView(title("Scenario 1 — No Host CleverTap"))

        logView = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFF1B4F72.toInt())
            setBackgroundColor(0xFFEBF5FB.toInt())
            setPadding(24, 24, 24, 24)
            text = ""
        }
        ll.addView(logView)
        ll.addView(spacer())

        // ── Test Buttons ──────────────────────────────────────────────────────

        ll.addView(btn("trackHomeScreenViewed()") {
            SharedEventTracker.trackHomeScreenViewed()
            log("[SENT] Home Screen Viewed → Common Account C")
            log("  source_app = '${SharedEventTracker.getSourceApp()}'")
        })

        ll.addView(btn("trackContentPlayed(\"c_001\", \"Sports\")") {
            SharedEventTracker.trackContentPlayed("c_001", "Sports")
            log("[SENT] Content Played → Common Account C")
            log("  Content ID = 'c_001', Category = 'Sports'")
            log("  source_app = '${SharedEventTracker.getSourceApp()}'")
        })

        ll.addView(btn("trackUserRegistered(\"u_001\", \"Email\")") {
            SharedEventTracker.trackUserRegistered("u_001", "Email")
            log("[SENT] User Registered → Common Account C")
        })

        ll.addView(btn("trackSearchPerformed(\"cricket\", 42)") {
            SharedEventTracker.trackSearchPerformed("cricket", 42)
            log("[SENT] Search Performed → Common Account C")
        })

        ll.addView(btn("trackSearchPerformed() with filter") {
            SharedEventTracker.trackSearchPerformed("football", 18, filterApplied = "Free Only")
            log("[SENT] Search Performed (with filter) → Common Account C")
        })

        ll.addView(btn("trackItemAddedToCart(\"i_001\", \"Shirt\", 29.99)") {
            SharedEventTracker.trackItemAddedToCart("i_001", "Shirt", 29.99)
            log("[SENT] Item Added to Cart → Common Account C")
        })

        ll.addView(btn("trackOrderPlaced(\"order_001\", 149.99)") {
            SharedEventTracker.trackOrderPlaced("order_001", 149.99)
            log("[SENT] Order Placed → Common Account C")
        })

        ll.addView(btn("trackFeatureDiscovered(\"Dark Mode\")") {
            SharedEventTracker.trackFeatureDiscovered("Dark Mode")
            log("[SENT] Feature Discovered → Common Account C")
        })

        ll.addView(btn("Fire all events once") {
            SharedEventTracker.trackHomeScreenViewed()
            SharedEventTracker.trackContentPlayed("c_all", "All")
            SharedEventTracker.trackUserRegistered("u_all", "Phone")
            SharedEventTracker.trackSearchPerformed("all events", 10)
            SharedEventTracker.trackItemAddedToCart("i_all", "Bundle", 99.0)
            SharedEventTracker.trackOrderPlaced("order_all", 99.0)
            SharedEventTracker.trackFeatureDiscovered("Batch Test")
            log("[SENT] All 7 approved events fired → Common Account C")
        })

        ll.addView(btn("Clear log") { logView.text = "" })

        scroll.addView(ll)
        return scroll
    }

    private fun log(msg: String) {
        logView.append("$msg\n")
    }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        textSize = 18f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setPadding(0, 0, 0, 24)
    }

    private fun btn(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 12f
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

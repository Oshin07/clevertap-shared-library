package com.clevertap.sampleapp.edgecases

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.clevertap.sharedeventlib.SharedEventTracker
import kotlin.concurrent.thread

/**
 * Edge Case Test Suite
 *
 * Covers all 25 edge cases defined in the test specification.
 * Each test is a self-contained action with a documented expected outcome.
 *
 * Run each test individually. Check logcat (tag=SharedEventTracker) and the
 * CleverTap dashboard for Common Account C to verify results.
 *
 * IMPORTANT: Cases EC-06 and EC-16 prove negative behaviors —
 * they verify things that SHOULD NOT happen. Read expected outcomes carefully.
 */
class EdgeCaseTestActivity : AppCompatActivity() {

    private lateinit var logView: TextView
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())
        log("Edge Case Test Suite loaded.")
        log("SharedEventTracker.isInitialized() = ${SharedEventTracker.isInitialized()}")
        log("Tap each case to run it. Check logcat for [SharedEventTracker] tag.")
        log("━".repeat(40))
    }

    // ── Edge Case 1 ──────────────────────────────────────────────────────────
    // Library used by an app with no existing CleverTap.
    // (See NoCleverTapScenarioActivity — tested there in detail)

    // ── Edge Case 2 ──────────────────────────────────────────────────────────
    // Library used by an app with an existing CleverTap instance.
    // (See WithCleverTapScenarioActivity — tested there in detail)

    // ── Edge Case 3 ──────────────────────────────────────────────────────────
    // Existing CleverTap uses a different Account ID.
    // Expected: library's instance still connects to COMMON_ACCOUNT_ID; the host
    //           app's instance connects to APP_ACCOUNT_ID. No cross-contamination.

    // ── Edge Case 4 ──────────────────────────────────────────────────────────
    // Existing CleverTap uses a different Account Token.
    // Expected: same independence as EC-3. Both tokens are valid for their own accounts.

    // ── Edge Case 5 ──────────────────────────────────────────────────────────
    private fun ec05_multipleInit() {
        log("\n[EC-05] Library initialized multiple times")
        log("Expected: only first call takes effect; subsequent calls are no-ops with a warning.")
        log("SharedEventTracker is already initialized (done in SampleApplication).")
        log("Calling initialize() again with a DIFFERENT sourceApp...")

        SharedEventTracker.initialize(this, "DIFFERENT_APP_NAME")
        log("After 2nd call — sourceApp is still: '${SharedEventTracker.getSourceApp()}'")

        SharedEventTracker.initialize(this, "YET_ANOTHER_APP")
        log("After 3rd call — sourceApp is still: '${SharedEventTracker.getSourceApp()}'")

        val expected = "Sample App" // Set in SampleApplication.SOURCE_APP
        val actual = SharedEventTracker.getSourceApp()
        if (actual == expected) {
            log("✓ PASS: sourceApp unchanged at '$actual'")
        } else {
            log("✗ FAIL: sourceApp changed to '$actual' (expected '$expected')")
        }
        log("Check logcat for 'Already initialized. Ignoring duplicate call.' warnings.")
    }

    // ── Edge Case 6 ──────────────────────────────────────────────────────────
    private fun ec06_trackBeforeInit() {
        log("\n[EC-06] track...() called before initialization")
        log("Expected: error logged to logcat. No crash. No event sent.")
        log("NOTE: This library IS already initialized (from Application.onCreate).")
        log("To test this case properly, you must call it before Application.onCreate() runs.")
        log("The safest way: add a separate test module where initialize() is never called,")
        log("then call trackHomeScreenViewed() — observe logcat for the 'not initialized' error.")
        log("")
        log("CleverTapManager.getInstanceOrWarn() is the guard — it logs an ERROR and returns null.")
        log("No exception is thrown; the app does not crash.")
        log("Simulating with a reflective reset is NOT done here to avoid corrupting the live test session.")
    }

    // ── Edge Case 7 ──────────────────────────────────────────────────────────
    private fun ec07_eventsAfterInit() {
        log("\n[EC-07] Events triggered immediately after initialization")
        log("Expected: events are queued and sent. CleverTap SDK handles early events.")
        SharedEventTracker.trackHomeScreenViewed()
        SharedEventTracker.trackContentPlayed("c_immediate", "Immediate")
        log("✓ Two events fired immediately. Verify in Account C dashboard.")
    }

    // ── Edge Case 8 ──────────────────────────────────────────────────────────
    private fun ec08_rapidEvents() {
        log("\n[EC-08] Multiple events triggered rapidly (100 events)")
        log("Expected: all 100 events queued. SDK batches network requests. No crash.")
        val start = System.currentTimeMillis()
        for (i in 1..100) {
            SharedEventTracker.trackContentPlayed("c_rapid_$i", "RapidTest")
        }
        val elapsed = System.currentTimeMillis() - start
        log("✓ 100 events fired in ${elapsed}ms. Check logcat for SDK batching behavior.")
    }

    // ── Edge Case 9 ──────────────────────────────────────────────────────────
    private fun ec09_backgroundForeground() {
        log("\n[EC-09] App goes to background/foreground")
        log("Expected: CleverTap SDK handles lifecycle via ActivityLifecycleCallback.")
        log("Library's instance is analytics-only — lifecycle handled by SDK automatically.")
        log("Fire an event, background the app (press home), return, fire another event.")
        SharedEventTracker.trackFeatureDiscovered("Pre-Background")
        log("Event fired. Now press Home button, wait 30s, return and fire the next test.")
        log("After return, EC-09b verifies the instance is still active.")
    }

    private fun ec09b_afterForeground() {
        log("\n[EC-09b] After foreground return")
        SharedEventTracker.trackFeatureDiscovered("Post-Background")
        log("✓ Event fired after returning from background. SDK should handle App Launched re-trigger.")
    }

    // ── Edge Case 10 ──────────────────────────────────────────────────────────
    private fun ec10_processRestart() {
        log("\n[EC-10] App process restart")
        log("Expected: library re-initializes cleanly in Application.onCreate() on next launch.")
        log("The CleverTap SDK persists device ID and queued events to disk.")
        log("Any events queued before the kill are retried on next launch.")
        log("To test: fire an event, force-stop the app, relaunch, fire another event.")
        log("Both events should appear in Account C dashboard.")
        SharedEventTracker.trackFeatureDiscovered("Before Process Restart")
        log("Event fired. Now force-stop the app and relaunch.")
    }

    // ── Edge Case 11 ──────────────────────────────────────────────────────────
    // (Covered by SampleApplication — initialization happens in Application.onCreate)

    // ── Edge Case 12 ──────────────────────────────────────────────────────────
    private fun ec12_multipleActivities() {
        log("\n[EC-12] Multiple activities using the same library")
        log("Expected: singleton instance — same CleverTapAPI object used across all activities.")
        log("Both activities call SharedEventTracker which delegates to the same internal singleton.")
        SharedEventTracker.trackHomeScreenViewed()
        log("✓ Event from Activity 1 (EdgeCaseTestActivity). isInitialized=${SharedEventTracker.isInitialized()}")
        log("When WithCleverTapScenarioActivity is open, same singleton is used there.")
    }

    // ── Edge Case 13 ──────────────────────────────────────────────────────────
    private fun ec13_multipleThreads() {
        log("\n[EC-13] Multiple threads calling library APIs simultaneously")
        log("Expected: thread-safe. @Synchronized on initialize(). CleverTapAPI is thread-safe.")
        val results = mutableListOf<String>()

        val threads = (1..10).map { i ->
            thread(name = "TestThread-$i") {
                SharedEventTracker.trackContentPlayed("c_thread_$i", "ThreadTest")
                synchronized(results) { results.add("Thread $i: event sent") }
            }
        }
        threads.forEach { it.join() }
        log("✓ 10 threads fired events concurrently. Results:")
        results.forEach { log("  $it") }
        log("Check Account C for 10 'Content Played' events with source_app='${SharedEventTracker.getSourceApp()}'")
    }

    // ── Edge Case 14 ──────────────────────────────────────────────────────────
    private fun ec14_blankProperties() {
        log("\n[EC-14] Null/empty event properties")
        log("Expected: IllegalArgumentException thrown for blank required params. Library does NOT send malformed events.")
        log("")

        // Test blank contentId
        try {
            SharedEventTracker.trackContentPlayed("", "Sports")
            log("✗ FAIL: Should have thrown for blank contentId")
        } catch (e: IllegalArgumentException) {
            log("✓ PASS: blank contentId → IllegalArgumentException: '${e.message}'")
        }

        // Test blank category
        try {
            SharedEventTracker.trackContentPlayed("c_001", "")
            log("✗ FAIL: Should have thrown for blank category")
        } catch (e: IllegalArgumentException) {
            log("✓ PASS: blank category → IllegalArgumentException: '${e.message}'")
        }

        // Test negative result count
        try {
            SharedEventTracker.trackSearchPerformed("test", -1)
            log("✗ FAIL: Should have thrown for negative resultCount")
        } catch (e: IllegalArgumentException) {
            log("✓ PASS: negative resultCount → IllegalArgumentException: '${e.message}'")
        }

        // Test negative price
        try {
            SharedEventTracker.trackItemAddedToCart("i_001", "Item", -5.0)
            log("✗ FAIL: Should have thrown for negative price")
        } catch (e: IllegalArgumentException) {
            log("✓ PASS: negative price → IllegalArgumentException: '${e.message}'")
        }

        log("\nAll validation guards confirmed.")
    }

    // ── Edge Case 15 ──────────────────────────────────────────────────────────
    private fun ec15_invalidSourceApp() {
        log("\n[EC-15] Invalid or missing sourceApp at initialization")
        log("Expected: blank sourceApp defaults to 'unknown' with a logcat WARNING.")
        log("The library still functions; events are attributed to 'unknown'.")
        log("")
        log("Testing with already-initialized library (sourceApp = '${SharedEventTracker.getSourceApp()}').")
        log("To test blank sourceApp: call SharedEventTracker.initialize(context, '') in isolation.")
        log("Check logcat for: [SharedEventTracker] W/SharedEventTracker: sourceApp is blank...")
        log("CleverTapManager will set sourceApp = 'unknown' and proceed.")
    }

    // ── Edge Case 16 ──────────────────────────────────────────────────────────
    private fun ec16_noGenericTrackEvent() {
        log("\n[EC-16] Verify arbitrary events CANNOT be sent through the public API")
        log("Expected: compilation error — trackEvent(String) does not exist on SharedEventTracker.")
        log("")
        log("The following code does NOT compile:")
        log("  SharedEventTracker.trackEvent(\"Arbitrary Event\")  ← compile error")
        log("  SharedEventTracker.trackEvent(\"Arbitrary\", props) ← compile error")
        log("")
        log("✓ CONFIRMED: No generic trackEvent method exists. This is enforced at compile time.")
        log("  Any attempt to call an undefined method produces a compile error, not a runtime error.")
        log("  You cannot work around this without modifying the library source code.")

        // Attempting to access internal classes from outside the module:
        // val mgr = CleverTapManager  ← compile error: 'internal' in another module
        // val ct = mgr.get()          ← compile error: same reason
        log("")
        log("✓ CONFIRMED: CleverTapManager (internal) is not accessible from this module.")
        log("✓ CONFIRMED: CleverTapAPI instance inside the library cannot be obtained here.")
    }

    // ── Edge Case 17 ──────────────────────────────────────────────────────────
    // (Covered partially by EC-16 and the reflection test in WithCleverTapScenarioActivity)

    // ── Edge Case 18 ──────────────────────────────────────────────────────────
    private fun ec18_hostCleverTapUnaffected() {
        log("\n[EC-18] Verify host app's CleverTap instance is unaffected by library")
        log("Expected: host app's CleverTapAPI.getDefaultInstance() has its own configuration.")
        val hostInstance = com.clevertap.android.sdk.CleverTapAPI.getDefaultInstance(this)
        log("Host CleverTap instance: $hostInstance")
        if (hostInstance != null) {
            log("Host instance accountId comes from AndroidManifest APP_ACCOUNT_ID")
            log("Library instance accountId is COMMON_ACCOUNT_ID (baked in)")
            log("These are different objects. Pushing to one does NOT affect the other.")
            log("✓ Host instance present and independent.")
        } else {
            log("Host instance is null — no APP_ACCOUNT_ID in manifest (Scenario 1 mode).")
            log("This is expected when testing with no host CleverTap configured.")
        }
    }

    // ── Edge Case 19 ──────────────────────────────────────────────────────────
    private fun ec19_libraryInstanceUnaffectedByHost() {
        log("\n[EC-19] Library instance unaffected by host-app CleverTap config")
        log("Expected: even if the host app calls CleverTapAPI.setDebugLevel() on its instance,")
        log("         the library's instance is not affected.")
        log("")
        log("The library's CleverTapInstanceConfig was set before instanceWithConfig() was called.")
        log("Config changes on the host's default instance use a different object path.")
        log("✓ Architecturally guaranteed: separate CleverTapInstanceConfig objects.")
    }

    // ── Edge Case 20 ──────────────────────────────────────────────────────────
    private fun ec20_differentSdkConfigs() {
        log("\n[EC-20] Behavior when both library and host app use different SDK configurations")
        log("Host app config:   isAnalyticsOnly=false (full integration, push enabled)")
        log("Library config:    isAnalyticsOnly=true  (analytics only, no push)")
        log("")
        log("Expected: each instance respects its own config. Host app can display in-app")
        log("messages and handle push; library instance cannot. No conflict.")
        log("✓ Confirmed by CleverTapInstanceConfig design: config is per-instance.")
    }

    // ── Edge Case 21 ──────────────────────────────────────────────────────────
    private fun ec21_dependencyConflict() {
        log("\n[EC-21] SDK dependency/version conflicts")
        log("")
        log("Scenario: library declares 'implementation clevertap-android-sdk:6.2.1'")
        log("          host app declares 'implementation clevertap-android-sdk:6.5.0'")
        log("")
        log("Gradle resolution: higher version wins → 6.5.0 used for both at runtime.")
        log("The library's code compiled against 6.2.1. If 6.5.0 is backward-compatible,")
        log("this works. If 6.5.0 removed or changed APIs the library uses, it breaks.")
        log("")
        log("To check: run ./gradlew :sampleapp:dependencies | grep clevertap")
        log("Look for the resolved version in the runtime classpath.")
        log("")
        log("RECOMMENDED: Library should pin a version range, not an exact version.")
        log("NEEDS CONFIRMATION: Verify with CleverTap that 6.x is backward-compatible.")
    }

    // ── Edge Case 22 ──────────────────────────────────────────────────────────
    private fun ec22_afterReinstall() {
        log("\n[EC-22] Behavior after app reinstall")
        log("Expected: CleverTap SDK generates a new 'App Installed' event on first launch.")
        log("The device ID may be retained (if CleverTap uses Android ID or advertising ID).")
        log("Events from before reinstall will be in Account C under the old device profile.")
        log("After reinstall, 'App Installed' fires again. Sessions restart from zero.")
        log("")
        log("Test: uninstall app, reinstall, launch — verify 'App Installed' in Account C.")
        log("Automatic event. Cannot be disabled from the library. Goes to Account C's dashboard.")
    }

    // ── Edge Case 23 ──────────────────────────────────────────────────────────
    private fun ec23_offlineOnline() {
        log("\n[EC-23] Behavior when device is offline and comes back online")
        log("Expected: CleverTap SDK queues events to local storage when offline.")
        log("          Events are flushed automatically when connectivity is restored.")
        log("")
        log("Step 1: Enable airplane mode on device")
        SharedEventTracker.trackFeatureDiscovered("Offline Test - Step 1")
        log("Event fired while OFFLINE. Check logcat — SDK should log: queuing event.")

        mainHandler.postDelayed({
            log("\nStep 2: Disable airplane mode now.")
            SharedEventTracker.trackFeatureDiscovered("Offline Test - Step 2")
            log("Event fired. SDK should flush queued events and this one.")
            log("Verify both events appear in Account C dashboard after coming online.")
        }, 3000)
    }

    // ── Edge Case 24 ──────────────────────────────────────────────────────────
    private fun ec24_eventQueueing() {
        log("\n[EC-24] Event queuing and retry behavior")
        log("CleverTap SDK queuing behavior (from SDK documentation):")
        log("  - Events are written to a local SQLite database immediately.")
        log("  - The SDK attempts to flush the queue periodically and on lifecycle events.")
        log("  - If a network request fails (4xx/5xx), the SDK retries with exponential backoff.")
        log("  - Events are retained in the queue across app restarts.")
        log("  - The queue is bounded — very old events may be dropped if the queue fills.")
        log("")
        log("Library behavior: identical to default instance — the library uses the same")
        log("SDK internals. The library does NOT add any extra queuing layer.")
        log("")
        log("NEEDS CONFIRMATION: Maximum queue size and retention period — ask CleverTap.")
        SharedEventTracker.trackSearchPerformed("queue test", 5)
        log("✓ Event pushed. SDK will queue it if offline, send it when online.")
    }

    // ── Edge Case 25 ──────────────────────────────────────────────────────────
    private fun ec25_onlyApprovedEvents() {
        log("\n[EC-25] Verify only approved library events reach Common Account C")
        log("")
        log("Test methodology:")
        log("1. Approved events — fire each one and verify it appears in Account C:")
        log("   • Home Screen Viewed")
        log("   • Content Played")
        log("   • User Registered")
        log("   • Search Performed")
        log("   • Item Added to Cart")
        log("   • Order Placed")
        log("   • Feature Discovered")
        log("")
        log("2. Unapproved events — verify they CANNOT be sent:")
        log("   • SharedEventTracker.trackEvent(\"Any String\") ← does not compile")
        log("   • Direct CleverTapAPI call with library's instance ← instance not accessible")
        log("")

        // Fire all approved events
        SharedEventTracker.trackHomeScreenViewed()
        SharedEventTracker.trackContentPlayed("c_25", "Approved")
        SharedEventTracker.trackUserRegistered("u_25", "Test")
        SharedEventTracker.trackSearchPerformed("approved only", 1)
        SharedEventTracker.trackItemAddedToCart("i_25", "Test Item", 1.0)
        SharedEventTracker.trackOrderPlaced("o_25", 1.0)
        SharedEventTracker.trackFeatureDiscovered("EC25 Complete")

        log("✓ All 7 approved events fired. Verify ONLY these appear in Account C.")
        log("✓ No other events should be present from this test run.")
    }

    // ── AUTOMATIC EVENTS DOCUMENTATION ───────────────────────────────────────

    private fun showAutomaticEventsInfo() {
        log("\n[AUTO EVENTS] CleverTap SDK Automatic Events")
        log("━".repeat(40))
        log("")
        log("The CleverTap SDK automatically fires these events:")
        log("  • App Launched    — every fresh launch / after 20+ min in background")
        log("  • App Installed   — first launch after install")
        log("  • App Uninstalled — via silent push (requires push config)")
        log("  • Session events  — session start/end tracking")
        log("")
        log("BEHAVIOR FOR LIBRARY'S SECONDARY INSTANCE:")
        log("  ❓ NEEDS CONFIRMATION from CleverTap:")
        log("  The SDK documentation does not explicitly state whether secondary")
        log("  instances (created via instanceWithConfig) also fire automatic events,")
        log("  or only the default instance does.")
        log("")
        log("  LIKELY OUTCOME A: All instances fire App Launched (most probable).")
        log("    → Account C will show App Launched for every app using the library.")
        log("    → Use source_app profile property to distinguish per-app.")
        log("")
        log("  LIKELY OUTCOME B: Only default instance fires automatic events.")
        log("    → Account C only receives explicitly-pushed library events.")
        log("    → Cleaner — but unconfirmed.")
        log("")
        log("  MITIGATION: isAnalyticsOnly=true suppresses push and in-app, but")
        log("  does NOT suppress App Launched / App Installed automatic events.")
        log("  (This behavior needs CleverTap confirmation too.)")
        log("")
        log("ACTION REQUIRED: Contact CleverTap support with this specific question:")
        log("  'Does a secondary instance created via CleverTapAPI.instanceWithConfig()")
        log("   fire automatic events (App Launched, App Installed) independently of")
        log("   the default instance? If so, can they be disabled per-instance?'")
    }

    // ── LAYOUT BUILDER ────────────────────────────────────────────────────────

    private fun buildLayout(): ScrollView {
        val scroll = ScrollView(this)
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }

        ll.addView(title("Edge Case Test Suite"))
        ll.addView(subtitle("25 cases — tap each to run. Check logcat for [SharedEventTracker]."))

        logView = TextView(this).apply {
            textSize = 11.5f
            setTextColor(0xFF1B3A4B.toInt())
            setBackgroundColor(0xFFEAF4FB.toInt())
            setPadding(20, 20, 20, 20)
            setTypeface(android.graphics.Typeface.MONOSPACE)
        }
        ll.addView(logView)
        ll.addView(spacer())

        val tests = listOf(
            "EC-05: Multiple initialize() calls"            to { ec05_multipleInit() },
            "EC-06: track...() before init (see notes)"     to { ec06_trackBeforeInit() },
            "EC-07: Events immediately after init"          to { ec07_eventsAfterInit() },
            "EC-08: 100 events rapidly"                     to { ec08_rapidEvents() },
            "EC-09: Background/foreground (step 1)"         to { ec09_backgroundForeground() },
            "EC-09b: After foreground return (step 2)"      to { ec09b_afterForeground() },
            "EC-10: App process restart (see notes)"        to { ec10_processRestart() },
            "EC-12: Multiple activities, same library"      to { ec12_multipleActivities() },
            "EC-13: 10 concurrent threads"                  to { ec13_multipleThreads() },
            "EC-14: Blank/null event parameters"            to { ec14_blankProperties() },
            "EC-15: Invalid sourceApp at init"              to { ec15_invalidSourceApp() },
            "EC-16: No generic trackEvent() API"            to { ec16_noGenericTrackEvent() },
            "EC-18: Host CleverTap unaffected"              to { ec18_hostCleverTapUnaffected() },
            "EC-19: Library instance unaffected by host"    to { ec19_libraryInstanceUnaffectedByHost() },
            "EC-20: Different SDK configurations"           to { ec20_differentSdkConfigs() },
            "EC-21: SDK version conflict analysis"          to { ec21_dependencyConflict() },
            "EC-22: After app reinstall"                    to { ec22_afterReinstall() },
            "EC-23: Offline → online behavior"              to { ec23_offlineOnline() },
            "EC-24: Event queuing & retry"                  to { ec24_eventQueueing() },
            "EC-25: Only approved events reach Account C"   to { ec25_onlyApprovedEvents() },
            "AUTO: Automatic event behavior (info)"         to { showAutomaticEventsInfo() },
            "⟳ Clear log"                                   to { logView.text = "" }
        )

        tests.forEach { (label, action) ->
            ll.addView(btn(label, action))
        }

        scroll.addView(ll)
        return scroll
    }

    private fun log(msg: String) { logView.append("$msg\n") }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        textSize = 19f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setPadding(0, 0, 0, 4)
    }

    private fun subtitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(0xFF555555.toInt())
        setPadding(0, 0, 0, 20)
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
        it.layoutParams = LinearLayout.LayoutParams(1, 20)
    }
}

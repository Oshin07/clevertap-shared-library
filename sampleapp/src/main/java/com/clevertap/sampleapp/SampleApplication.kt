package com.clevertap.sampleapp

import android.app.Application
import android.util.Log
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.sharedeventlib.SharedEventTracker

/**
 * Application class for the test app.
 *
 * Demonstrates BOTH integration scenarios in one app:
 *   - The shared library is always initialized (library's CleverTap → Common Account C)
 *   - The host app's own CleverTap is initialized separately (app's CleverTap → Account A)
 *
 * TO TEST SCENARIO 1 (no host-app CleverTap):
 *   Comment out the "Host app's own CleverTap" block below.
 *   Also comment out CLEVERTAP_ACCOUNT_ID / CLEVERTAP_TOKEN in AndroidManifest.xml.
 *
 * TO TEST SCENARIO 2 (host app has its own CleverTap):
 *   Keep both blocks. Verify Account A and Common Account C both receive events.
 */
class SampleApplication : Application() {

    companion object {
        private const val TAG = "SampleApplication"

        // The source_app identifier for THIS application.
        // Change this value per consuming app during integration testing.
        const val SOURCE_APP = "Sample App"
    }

    override fun onCreate() {
        super.onCreate()

        // ── 1. SHARED LIBRARY INITIALIZATION ─────────────────────────────────
        // This is the ONLY thing an app needs to do to use the shared library.
        // The library creates and manages its own CleverTap instance internally.
        // No Account ID or Token is configured here.
        SharedEventTracker.initialize(this, SOURCE_APP)
        Log.d(TAG, "SharedEventTracker initialized. isInitialized=${SharedEventTracker.isInitialized()}")
        Log.d(TAG, "sourceApp=${SharedEventTracker.getSourceApp()}")

        // ── 2. HOST APP'S OWN CLEVERTAP (Scenario 2 — remove for Scenario 1) ─
        // This initializes the host app's SEPARATE CleverTap instance for Account A.
        // It reads credentials from AndroidManifest.xml meta-data.
        // This instance is completely independent from the library's instance.
        //
        // ActivityLifecycleCallback.register() must be called before super.onCreate()
        // in CleverTap's standard integration — but for multi-instance via
        // CleverTapAPI.autoIntegrate() the call here is sufficient.
        CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.DEBUG)
        ActivityLifecycleCallback.register(this)
        CleverTapAPI.autoIntegrate(this)
        Log.d(TAG, "Host app CleverTap (Account A) initialized via autoIntegrate.")

        // At this point two independent CleverTap instances exist in this process:
        //   1. The shared library's instance   → Common Account C (credentials baked in library)
        //   2. The host app's default instance → Account A (credentials in manifest)
        // They do not share event queues, user profiles, or configuration.
    }
}

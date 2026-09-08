package com.clevertap.sharedeventlib.internal

import android.content.Context
import android.util.Log
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig

/**
 * Internal singleton that owns and manages the library's CleverTap instance.
 *
 * ACCESS CONTROL:
 *   - This entire class is `internal` — it does not exist outside this library module.
 *   - The [CleverTapAPI] field is private — not even other library classes can
 *     get a reference to it directly; they must go through this manager's methods.
 *   - The consuming application has no compile-time visibility into this class.
 *
 * THREADING:
 *   - [initialize] is guarded by @Synchronized and safe to call from any thread.
 *   - All [pushEvent] paths check initialization state before proceeding.
 *
 * INITIALIZATION:
 *   - ALL CleverTapInstanceConfig settings MUST be applied before [CleverTapAPI.instanceWithConfig]
 *     is called. The CleverTap SDK documents that changes after instance creation have no effect.
 */
internal object CleverTapManager {

    // The library's exclusive CleverTap instance.
    // 'private' — only this object can reference it.
    private var cleverTapInstance: CleverTapAPI? = null

    // The identifier for the host application, set at initialization.
    private var sourceApp: String = "unknown"

    // Guards against multiple initialization calls.
    @Volatile
    private var isInitialized = false

    /**
     * Initialises the library's CleverTap instance.
     *
     * Called from [com.clevertap.sharedeventlib.SharedEventTracker.initialize].
     * Subsequent calls are no-ops — the instance is created only once per process lifecycle.
     *
     * @param context   Must be Application context to avoid Activity memory leaks.
     * @param sourceApp A stable, agreed identifier for the host application.
     *                  Examples: "App A", BuildConfig.APPLICATION_ID, "com.company.appa"
     */
    @Synchronized
    internal fun initialize(context: Context, sourceApp: String) {
        if (isInitialized) {
            Log.d(LibraryConfig.LOG_TAG, "Already initialized. sourceApp='${this.sourceApp}'. Ignoring duplicate call.")
            return
        }

        if (sourceApp.isBlank()) {
            Log.w(LibraryConfig.LOG_TAG, "sourceApp is blank. Events will be attributed to 'unknown'. " +
                    "Pass a non-empty identifier to SharedEventTracker.initialize().")
        }

        this.sourceApp = sourceApp.ifBlank { "unknown" }

        // Build the configuration object.
        // !! All config MUST be set on this object BEFORE calling instanceWithConfig !!
        val config = CleverTapInstanceConfig.createInstance(
            context.applicationContext,
            LibraryConfig.ACCOUNT_ID,
            LibraryConfig.ACCOUNT_TOKEN,
            LibraryConfig.ACCOUNT_REGION
        )

        // Analytics-only mode: disables in-app notifications and push handling
        // for this instance. Recommended until push/in-app is explicitly designed.
        config.isAnalyticsOnly = LibraryConfig.ANALYTICS_ONLY

        // Disable CleverTap debug logging in production builds.
        // Change to CleverTapAPI.LogLevel.DEBUG for development.
        config.setDebugLevel(CleverTapAPI.LogLevel.INFO)

        // Create the named instance. After this line, config changes have no effect.
        cleverTapInstance = CleverTapAPI.instanceWithConfig(context.applicationContext, config)

        // Belt-and-suspenders: explicitly block in-app display on the library instance
        // so no campaigns from Account C ever appear in the host app.
        cleverTapInstance?.suspendInAppNotifications()

        isInitialized = true
        Log.d(LibraryConfig.LOG_TAG, "Initialized. sourceApp='${this.sourceApp}' | accountId='${LibraryConfig.ACCOUNT_ID}'")

        // Push source_app as a user profile property so CleverTap segments can
        // filter automatic events (App Launched etc.) by originating application.
        pushSourceAppProfile()
    }

    /**
     * Pushes an event to the library's CleverTap instance.
     *
     * @param eventName  The CleverTap event name string (from [EventConstants]).
     * @param properties Additional event properties. source_app is always injected.
     */
    internal fun pushEvent(eventName: String, properties: HashMap<String, Any> = hashMapOf()) {
        val instance = getInstanceOrWarn("pushEvent('$eventName')") ?: return
        // Always inject source_app — consuming app cannot override or omit it.
        properties[EventConstants.PROP_SOURCE_APP] = sourceApp
        instance.pushEvent(eventName, properties)
        Log.d(LibraryConfig.LOG_TAG, "pushEvent: $eventName | props=$properties")
    }

    /**
     * Pushes a user profile update to the library's CleverTap instance.
     * Used internally — not exposed through the public API.
     */
    internal fun pushProfile(profileData: HashMap<String, Any>) {
        val instance = getInstanceOrWarn("pushProfile") ?: return
        instance.pushProfile(profileData)
    }

    /**
     * Returns the initialization state. Exposed only for diagnostics in the test app.
     * Not part of the public [com.clevertap.sharedeventlib.SharedEventTracker] API.
     */
    internal fun isReady(): Boolean = isInitialized

    /**
     * Returns the current sourceApp value for diagnostic use.
     */
    internal fun getSourceApp(): String = sourceApp

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun pushSourceAppProfile() {
        val profile = hashMapOf<String, Any>(EventConstants.PROP_SOURCE_APP to sourceApp)
        cleverTapInstance?.pushProfile(profile)
    }

    /**
     * Returns the CleverTap instance if ready, or logs a warning and returns null.
     * All event-pushing paths call this instead of accessing [cleverTapInstance] directly.
     */
    private fun getInstanceOrWarn(callerContext: String): CleverTapAPI? {
        if (!isInitialized || cleverTapInstance == null) {
            Log.e(
                LibraryConfig.LOG_TAG,
                "[$callerContext] SharedEventTracker is not initialized. " +
                "Call SharedEventTracker.initialize(context, sourceApp) in Application.onCreate() " +
                "BEFORE calling any track...() methods."
            )
            return null
        }
        return cleverTapInstance
    }
}

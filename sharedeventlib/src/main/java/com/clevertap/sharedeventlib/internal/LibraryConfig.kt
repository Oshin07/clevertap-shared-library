package com.clevertap.sharedeventlib.internal

/**
 * Holds the credentials and configuration for the library's own CleverTap account.
 *
 * These values are INTERNAL to the library — they are never exposed through the
 * public API and cannot be accessed by the consuming application.
 *
 * !! Replace COMMON_ACCOUNT_ID and COMMON_ACCOUNT_TOKEN with your real values !!
 *
 * DO NOT put these in the host app's AndroidManifest.xml.
 * They are baked into the library binary and passed programmatically.
 */
internal object LibraryConfig {

    // ── REPLACE WITH YOUR ACTUAL VALUES ──────────────────────────────────────

    /** The Account ID for the common/shared CleverTap project. */
    const val ACCOUNT_ID = "4W7-4R8-R65Z"

    /** The Account Token for the common/shared CleverTap project. */
    const val ACCOUNT_TOKEN = "b24-a04"

    /**
     * Region code. Valid values: "in1" (India), "eu1" (Europe), "us1" (US),
     * "sg1" (Singapore), "sk1" (Custom).
     * Set to null to use the default (US) region.
     */
    val ACCOUNT_REGION: String? = null

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * When true, the library's CleverTap instance operates in analytics-only mode.
     * This disables:
     *   - In-app notification display from the library instance
     *   - Push notification handling from the library instance
     *
     * RECOMMENDED: keep true until push/in-app is explicitly designed for this library.
     * Setting this to false when the host app also uses CleverTap push can cause
     * notification handling conflicts.
     */
    const val ANALYTICS_ONLY = true

    /** Library debug tag for logcat output. */
    const val LOG_TAG = "SharedEventTracker"
}

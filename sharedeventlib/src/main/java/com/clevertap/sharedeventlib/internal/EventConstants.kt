package com.clevertap.sharedeventlib.internal

/**
 * Canonical CleverTap event name strings and property key constants.
 *
 * Centralising these here means:
 *   1. Event names are defined exactly once — no typos scattered across methods.
 *   2. Renaming an event requires a single change here + a library release.
 *   3. These constants are internal — host apps cannot reference them and
 *      therefore cannot construct an event name string to pass to CleverTap directly.
 */
internal object EventConstants {

    // ── Event Names ───────────────────────────────────────────────────────────

    const val HOME_SCREEN_VIEWED = "Home Screen Viewed"
    const val CONTENT_PLAYED     = "Content Played"
    const val USER_REGISTERED    = "User Registered"
    const val SEARCH_PERFORMED   = "Search Performed"
    const val ITEM_ADDED_TO_CART = "Item Added to Cart"
    const val ORDER_PLACED       = "Order Placed"
    const val PROFILE_UPDATED    = "Profile Updated"
    const val FEATURE_DISCOVERED = "Feature Discovered"

    // ── Property Keys ─────────────────────────────────────────────────────────

    /** Mandatory property injected automatically on every event. */
    const val PROP_SOURCE_APP      = "source_app"

    const val PROP_CONTENT_ID      = "Content ID"
    const val PROP_CATEGORY        = "Category"
    const val PROP_QUERY           = "Query"
    const val PROP_RESULT_COUNT    = "Result Count"
    const val PROP_FILTER_APPLIED  = "Filter Applied"
    const val PROP_ITEM_ID         = "Item ID"
    const val PROP_ITEM_NAME       = "Item Name"
    const val PROP_PRICE           = "Price"
    const val PROP_ORDER_ID        = "Order ID"
    const val PROP_ORDER_VALUE     = "Order Value"
    const val PROP_USER_ID         = "User ID"
    const val PROP_REG_METHOD      = "Registration Method"
    const val PROP_FEATURE_NAME    = "Feature Name"
}

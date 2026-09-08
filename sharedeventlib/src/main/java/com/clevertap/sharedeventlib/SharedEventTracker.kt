package com.clevertap.sharedeventlib

import android.content.Context
import com.clevertap.sharedeventlib.internal.CleverTapManager
import com.clevertap.sharedeventlib.internal.EventConstants

/**
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │                        SharedEventTracker                               │
 * │                                                                         │
 * │  The ONLY public interface of the shared event library.                 │
 * │                                                                         │
 * │  Rules enforced by this design:                                         │
 * │  ─ CleverTapAPI / CleverTapInstanceConfig are NEVER exposed here.      │
 * │  ─ There is NO generic trackEvent(String) method.                       │
 * │  ─ Every event is a typed method. New events require a library change.  │
 * │  ─ source_app is injected automatically — callers cannot omit it.      │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * USAGE — consuming application:
 *
 *   // In Application.onCreate():
 *   SharedEventTracker.initialize(this, "App A")
 *
 *   // Anywhere in the app:
 *   SharedEventTracker.trackHomeScreenViewed()
 *   SharedEventTracker.trackContentPlayed("c_123", "Sports")
 */
object SharedEventTracker {

    // ── INITIALIZATION ────────────────────────────────────────────────────────

    /**
     * Initialises the shared event library.
     *
     * Must be called **once** from [android.app.Application.onCreate] before any
     * track…() method is used. Safe to call multiple times — subsequent calls are
     * no-ops with a logcat warning.
     *
     * @param context   Use Application context. An Activity context is accepted
     *                  but [context.applicationContext] is extracted internally.
     * @param sourceApp A stable, human-readable identifier for the calling application.
     *                  Agree on this value with the data team before integration.
     *                  Examples: "App A", "com.company.appa", "APPA"
     *                  Must be non-empty. If blank, defaults to "unknown" with a warning.
     */
    @JvmStatic
    fun initialize(context: Context, sourceApp: String) {
        CleverTapManager.initialize(context.applicationContext, sourceApp)
    }

    // ── APPROVED EVENTS ───────────────────────────────────────────────────────
    // To add a new event:
    //   1. Add the event name constant to EventConstants.kt
    //   2. Add any new property key constants to EventConstants.kt
    //   3. Add the typed method below
    //   4. Bump the library version
    //   5. Consumers upgrade to the new library version to use the new event
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tracks a Home Screen viewed event.
     * CleverTap event name: "Home Screen Viewed"
     * Automatic properties: source_app
     */
    @JvmStatic
    fun trackHomeScreenViewed() {
        CleverTapManager.pushEvent(EventConstants.HOME_SCREEN_VIEWED)
    }

    /**
     * Tracks a piece of content being played.
     * CleverTap event name: "Content Played"
     *
     * @param contentId  The unique identifier of the content item. Must not be null or blank.
     * @param category   The content category (e.g. "Sports", "News"). Must not be null or blank.
     */
    @JvmStatic
    fun trackContentPlayed(contentId: String, category: String) {
        require(contentId.isNotBlank()) { "contentId must not be blank" }
        require(category.isNotBlank())  { "category must not be blank"  }

        CleverTapManager.pushEvent(
            EventConstants.CONTENT_PLAYED,
            hashMapOf(
                EventConstants.PROP_CONTENT_ID to contentId,
                EventConstants.PROP_CATEGORY   to category
            )
        )
    }

    /**
     * Tracks a user registration event.
     * CleverTap event name: "User Registered"
     *
     * @param userId  Opaque user identifier (do not send PII directly unless encrypted).
     * @param method  Registration method: "Email", "Phone", "Google", "Apple", etc.
     */
    @JvmStatic
    fun trackUserRegistered(userId: String, method: String) {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(method.isNotBlank()) { "method must not be blank"  }

        CleverTapManager.pushEvent(
            EventConstants.USER_REGISTERED,
            hashMapOf(
                EventConstants.PROP_USER_ID    to userId,
                EventConstants.PROP_REG_METHOD to method
            )
        )
    }

    /**
     * Tracks a search performed by the user.
     * CleverTap event name: "Search Performed"
     *
     * @param query         The search query string.
     * @param resultCount   Number of results returned.
     * @param filterApplied Optional filter label applied (null if no filter).
     */
    @JvmStatic
    @JvmOverloads
    fun trackSearchPerformed(
        query: String,
        resultCount: Int,
        filterApplied: String? = null
    ) {
        require(query.isNotBlank())    { "query must not be blank"      }
        require(resultCount >= 0)      { "resultCount must be >= 0"     }

        val props = hashMapOf<String, Any>(
            EventConstants.PROP_QUERY        to query,
            EventConstants.PROP_RESULT_COUNT to resultCount
        )
        filterApplied?.let { props[EventConstants.PROP_FILTER_APPLIED] = it }

        CleverTapManager.pushEvent(EventConstants.SEARCH_PERFORMED, props)
    }

    /**
     * Tracks an item being added to the cart.
     * CleverTap event name: "Item Added to Cart"
     *
     * @param itemId    Unique item identifier.
     * @param itemName  Display name of the item.
     * @param price     Item price (in the account's default currency).
     */
    @JvmStatic
    fun trackItemAddedToCart(itemId: String, itemName: String, price: Double) {
        require(itemId.isNotBlank())   { "itemId must not be blank"     }
        require(itemName.isNotBlank()) { "itemName must not be blank"   }
        require(price >= 0)            { "price must be >= 0"           }

        CleverTapManager.pushEvent(
            EventConstants.ITEM_ADDED_TO_CART,
            hashMapOf(
                EventConstants.PROP_ITEM_ID   to itemId,
                EventConstants.PROP_ITEM_NAME to itemName,
                EventConstants.PROP_PRICE     to price
            )
        )
    }

    /**
     * Tracks a completed order placement.
     * CleverTap event name: "Order Placed"
     *
     * @param orderId     Unique order identifier.
     * @param orderValue  Total order value.
     */
    @JvmStatic
    fun trackOrderPlaced(orderId: String, orderValue: Double) {
        require(orderId.isNotBlank()) { "orderId must not be blank" }
        require(orderValue >= 0)      { "orderValue must be >= 0"   }

        CleverTapManager.pushEvent(
            EventConstants.ORDER_PLACED,
            hashMapOf(
                EventConstants.PROP_ORDER_ID    to orderId,
                EventConstants.PROP_ORDER_VALUE to orderValue
            )
        )
    }

    /**
     * Tracks a user discovering a new feature via onboarding or tooltips.
     * CleverTap event name: "Feature Discovered"
     *
     * @param featureName  The name of the feature as defined in the event schema.
     */
    @JvmStatic
    fun trackFeatureDiscovered(featureName: String) {
        require(featureName.isNotBlank()) { "featureName must not be blank" }

        CleverTapManager.pushEvent(
            EventConstants.FEATURE_DISCOVERED,
            hashMapOf(EventConstants.PROP_FEATURE_NAME to featureName)
        )
    }

    // ── DIAGNOSTIC HELPERS (not part of production API) ──────────────────────
    // These are useful during integration testing. Consider removing or gating
    // behind a BuildConfig.DEBUG flag in production.

    /**
     * Returns true if the library has been successfully initialized.
     * Use in test scenarios to verify initialization state.
     */
    @JvmStatic
    fun isInitialized(): Boolean = CleverTapManager.isReady()

    /**
     * Returns the source_app identifier currently set for this session.
     * Use in test scenarios to verify initialization arguments.
     */
    @JvmStatic
    fun getSourceApp(): String = CleverTapManager.getSourceApp()

    // ── DELIBERATELY ABSENT ───────────────────────────────────────────────────
    //
    // The following APIs are intentionally NOT present:
    //
    //   fun trackEvent(eventName: String)           ← no generic event push
    //   fun getCleverTapInstance(): CleverTapAPI    ← no raw instance access
    //   fun getConfig(): CleverTapInstanceConfig    ← no config access
    //
    // If you add any of these, you break the architecture contract.
    // ─────────────────────────────────────────────────────────────────────────
}

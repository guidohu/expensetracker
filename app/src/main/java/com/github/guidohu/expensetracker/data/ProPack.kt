package com.github.guidohu.expensetracker.data

/** What the free version allows; the Pro pack (a one-time Google Play purchase) lifts these limits. */
object FreeTier {
    const val CATEGORY_LIMIT = 3
    const val WISHLIST_ITEM_LIMIT = 20
}

/**
 * The Play Console product ID of the Pro pack — a one-time, non-consumable in-app product. Product
 * IDs can never be changed or reused once created, so this must match the console exactly.
 */
const val PRO_PRODUCT_ID = "pro_pack"

/** The Pro pack's selling points, in the order the upgrade dialog lists them. */
val ProFeatures: List<String> = listOf(
    "Unlimited categories",
    "Unlimited wishlist items",
    "Any category color with the full color picker",
)

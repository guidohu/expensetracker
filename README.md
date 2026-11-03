# Expense Tracker

A deliberately simple Android expense tracker: track categories, log expenses, see where your money goes.

## Features

- **Categories** — create custom categories with a name and color (three defaults are seeded on first run).
- **Expenses** — log an amount, category, optional note, and date from a bottom sheet; swipe through the list, delete with a tap.
- **Backup & restore** — Settings can export everything (expenses, categories, wishlist, settings) to a single `.zip` of CSV files and restore from one, replacing the current data.
- **Stats** — this month vs. all-time totals, a donut chart of spending by category, and a 6-month bar chart trend.

## Free vs. Pro

The app is free, with a one-time **Pro pack** in-app purchase (Play product ID `pro_pack`, see
`data/ProPack.kt`). Free users get up to 3 categories, up to 20 wishlist items and the preset
color palette. Pro removes those limits and unlocks the full color picker. Anyone who had completed
onboarding before the Pro pack shipped (versionCode 23) is grandfathered into Pro automatically.

Purchases go through Google Play Billing (`data/ProPurchases.kt`) and are verified on the device only;
the result is cached in `UserPreferences.isPro`, which every gated feature checks. Debug builds have
a "Debug: Pro state" row at the bottom of Settings that forces Pro on or off for testing.

## Stack

- Kotlin + Jetpack Compose (Material 3), single-activity with Navigation Compose
- Room for local persistence, exposed as `Flow`s
- No DI framework — a single `Application`-held repository passed down to screens, kept intentionally minimal
- Charts are hand-rolled Compose `Canvas`/layout components (no charting library dependency)

## Opening the project

This was scaffolded without a local JDK/Android SDK available, so it hasn't been built yet. To build it:

1. Open the project root in Android Studio (Ladybug/2024.2+ recommended for AGP 8.7 / Kotlin 2.0).
2. If Android Studio reports the Gradle wrapper is missing, let it generate one (File → the IDE will prompt), or run `gradle wrapper` once Gradle is available locally.
3. Let Gradle sync — it will pull AGP 8.7.2, Kotlin 2.0.21, Compose BOM 2024.12.01, and Room 2.6.1.
4. Run on a device/emulator with API 26+.

## Project layout

```
app/src/main/java/com/hungerbuehler/expensetracker/
  data/        Room entities, DAOs, database, repository
  ui/
    expenses/  Expense list + add-expense bottom sheet
    categories/ Category list + add-category dialog
    stats/     Aggregation (ViewModel) + donut/bar chart screen
    components/ Reusable DonutChart / BarChart composables
    theme/     Material 3 theme
  util/        Currency/date formatting helpers
```

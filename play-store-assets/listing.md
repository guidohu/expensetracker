# Play Store listing (en-US)

Paste each block into Play Console → Grow users → Store presence → Main store listing.
Limits: app name 30, short description 80, full description 4000 characters.

## App name

```
Expense Tracker & Budget App
```

Alternatives: `Expense Tracker: Budget & Mood`, `Spending Tracker & Budget`

## Short description

```
Budget planner & expense manager: track spending, see insights. No ads, private.
```

## Full description

```
Expense Tracker is a simple budgeting tool and expense manager that shows you where your money goes – and how you felt about spending it. No account, no ads, no bank login: your personal finance data stays on your phone.

<b>Track expenses in seconds</b>
• Log an expense with amount, category, date and an optional note
• Sort and group your spending log by date, category, amount or mood
• Search every expense instantly
• Multi-currency: spend abroad and see it converted to your home currency with daily exchange rates

<b>Financial insights that make sense</b>
• See this month's spending and your all-time total at a glance
• Donut charts of spending by category, by mood, or by need vs. want
• Bar charts of your spending trend over weeks, months or years
• Compare this week, last month, the last 12 months and more, with totals and daily averages

<b>Budget planner that keeps you on track</b>
• Set a monthly budget and watch how much you have left
• Get congratulated when you stay under budget
• Build no-spend streaks and celebrate the milestones
• Daily reminder to log today's expenses, at a time you choose

<b>Mood tagging: understand emotional spending</b>
Tag a purchase as happy, excited, neutral, stressed, regret or sad. Over time, your spending tracker reveals which purchases were worth it and which ones you regret, so you can make better money decisions.

<b>Wishlist: think before you buy</b>
• Save things you want, with price and a link to the product
• Mark each item as something you need or something you want
• Turn it into an expense in one tap when you finally buy it

<b>Private by design</b>
• No account or sign-up, no ads, no tracking
• Works offline. Your expenses never leave your device
• Back up everything to a file (CSV in a zip) and restore it on a new phone
• Dark mode and Material You colors

<b>Free, with an optional Pro pack</b>
The expense tracker is free to use with up to 3 categories and 20 wishlist items. A one-time Pro pack purchase unlocks unlimited categories, an unlimited wishlist and custom category colors. No subscription.

Take control of your daily expenses, stick to a monthly budget and build better money habits with a personal finance app that respects your privacy.
```

## Screenshots (phone)

Upload in this order from `listing/`. The first three are what most people see without scrolling.

1. `screenshot-1-insights.png`: See where your money goes
2. `screenshot-2-expenses.png`: Track every expense in seconds
3. `screenshot-3-mood.png`: How did that purchase feel?
4. `screenshot-4-wishlist.png`: Think before you buy
5. `screenshot-5-budget.png`: Stay on budget
6. `screenshot-6-private.png`: Private by design

Feature graphic: `listing/feature-graphic.png`

To regenerate: rerun `StoreAssetsTest` with `recordPaparazziDebug` (see the process in
`ScreenshotTest.kt`), then `python3 play-store-assets/make_listing_graphics.py`.

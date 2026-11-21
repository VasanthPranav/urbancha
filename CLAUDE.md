# Urban Cha — project context

Context for anyone (or any Claude Code session) working on this repo. It summarises the
design conversation with the shop owner. Keep it up to date when decisions change.

## The business
Urban Cha is a tea shop. The menu includes tea, ginger tea, butter bun, Nutella bun, Milkmaid bun,
idli and dosa. The raw materials are milk, sugar, tea powder, cups, batter, buns, butter, Nutella
and Milkmaid (condensed milk).

The owner wants a phone app to track, for every business day:
- stock at the start of the day, stock used, and stock left at the end
- stock that arrives during the day (restocks)
- products sold (how many teas, buns, …) and the money taken, as simple billing
- all of it **in sync across several phones**

## Decisions made with the owner (follow these)
1. **The business day is 3:00 PM → 3:00 AM next morning.** A shift is named by its start date:
   a sale at 1:30 AM on 22 Nov belongs to the 21 Nov shift. Closing just after 3 AM must still
   close the shift that is open (`AppViewModel.closableShift`). Logic: `data/ShiftClock.kt` (unit-tested).
2. **Every tea sold reduces cups by one.** For example, 10 cups in stock and 5 teas sold → 5 cups.
   This is done with an optional per-product link, "each one sold uses 1 <countable item>"
   (`Product.stockItemId`), so Tea → Cups and buns → Buns. The Sell screen shows "stock after this
   bill" live.
3. **Products are name + price only.** The owner explicitly said **no recipes**. The only extra is
   the optional "uses 1 cup/bun" link above. Do not bring back per-ingredient recipes unless the owner asks.
4. **Restock any time** during a shift, with a log of what arrived and when.
5. **New raw materials and new products can be added manually** from the app (name, unit
   g/kg/ml/L/pcs, current stock, optional "warn me below" level).
6. **Usage of loose items** (milk, sugar, tea powder…) comes from the **physical count at Close
   shift**: used = (opening + restocks) − counted. Mismatches against the expected amount show in red.
7. **Sync**: Firebase Cloud Firestore. All phones sign in with the **same shop email/password**.
   Stock and totals use atomic `FieldValue.increment`, so two phones billing at once never clash.
   It works offline through Firestore's local cache. Writes are deliberately not awaited.
8. Reports are per shift, with ‹ › to browse past shifts, and can be shared as text (WhatsApp).

## Screens
Home · Sell (billing, Cash/UPI) · Stock (restock, raw materials) · Opening stock (3 PM) ·
Close shift (3 AM) · Reports · Products.

Clickable design mockup (private to the owner): https://claude.ai/artifact/Tqi1ypy2bfZysQVB1XUNNk
Look: tea-leaf green `#1F4D3A`, saffron accent `#E8A33D`, warm off-white ground `#F6F5F1`.

## Tech
- Kotlin, Jetpack Compose (Material 3), single `app` module, no navigation library (a simple
  `Screen` enum in `ui/UrbanChaApp.kt`).
- AGP 8.7.3, Kotlin 2.0.21, compileSdk/targetSdk 35, minSdk 26, JDK 17.
- Firebase BOM 33.7.0 (Firestore, Auth). `app/google-services.json` is git-ignored. Without it the
  app still builds and shows a "Firebase is not set up yet" screen.
- Firestore layout and the setup steps are in `README.md`. Security rules: `firestore.rules`.

Code map: `data/` (ShiftClock, Models, ShopRepository = all Firestore reads/writes) ·
`ui/AppViewModel.kt` · `ui/UrbanChaApp.kt` (tabs + sign-in) · `ui/Components.kt` · `ui/screens/*`.

## Status (as of 21 Nov 2026)
- All features above are written, but the app has **never been compiled or run**. The first job
  is to build it (`./gradlew assembleDebug`, `./gradlew testDebugUnitTest`) and fix any compile errors.
- Building in a cloud session needs the Android SDK, which means accepting Google's Android SDK
  licences. **Ask the owner before accepting them.**
- Prices and stock amounts in the starter data (`ShopRepository.loadStarterData`) are placeholders.

## Possible next steps (not yet agreed)
Staff vs owner logins · cancel or edit a bill · weekly/monthly totals · Excel/PDF export ·
Bluetooth receipt printer.

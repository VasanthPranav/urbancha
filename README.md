# Urban Cha — stock & sales app (Android)

An Android app for the Urban Cha tea shop. It tracks stock, billing and daily totals,
synced live across every phone that signs in to the shop account.

## What it does

| Tab / screen | What it's for |
|---|---|
| **Home** | This shift's sales (total, bills, cash vs UPI, top items), running-low warnings, shortcuts |
| **Sell** | Billing: tap + / − per product, pay Cash or UPI. Shows "stock after this bill", e.g. *Cups 10 → 5* after 5 teas |
| **Stock** | Restock at any time during the shift, add new raw materials, edit or correct stock, warn-below levels |
| **Opening stock** | At 3 PM: carries last shift's stock forward, add what arrived, start the shift |
| **Close shift** | At 3 AM: In (opening + restocks) / Used / Left, type the real count, mismatches shown in red |
| **Reports** | Any past shift with ‹ › arrows: totals, products sold, stock used, latest bills, share as text (WhatsApp) |
| **Analytics** | 1 / 7 / 30 / 90 days, in ₹ or %: sales vs the previous period, daily sales, Cash vs UPI, raw-material cost and margin (from closed shifts), product share, days of stock left, missing cups/buns |
| **Products** | Name + price only. Optionally "each one sold uses 1 cup/bun" so that stock goes down automatically |

### The business day
A shift runs **3:00 PM → 3:00 AM the next morning** and is named after the day it starts.
A tea sold at 1:30 AM on 22 Nov counts in the **21 Nov** shift. Closing a little after 3 AM still
closes the shift that is open.

### Sync across devices
Data is stored in **Cloud Firestore** under `shops/{account id}`:

```
shops/{uid}/products/{id}       name, price, stockItemId
shops/{uid}/materials/{id}      name, unit, qty (live stock), lowAlert, cost (₹ per unit, optional)
shops/{uid}/shifts/{yyyy-MM-dd} status, opening, restocked, closing, salesTotal, cashTotal, upiTotal,
                                billCount, itemCount, items.{productId}.{name,qty,amount}
                    /sales/{id}     one document per bill
                    /restocks/{id}  one document per restock
```

* Every phone signs in with the **same shop email and password**, so they all see the same data.
* Stock and totals change with atomic `FieldValue.increment`, so two phones billing at the same
  moment never overwrite each other.
* Firestore keeps a copy on each phone, so billing keeps working **offline** and uploads when the
  internet is back.

## One-time setup (about 10 minutes)

1. Go to <https://console.firebase.google.com> and **create a project** (e.g. "urban-cha").
2. **Add an Android app** with package name `com.urbancha.inventory`. Download
   **`google-services.json`** and put it in `app/`. This file is git-ignored.
3. In **Build → Authentication → Sign-in method**, enable **Email/Password**.
4. In **Build → Firestore Database**, click **Create database** (production mode, a region near
   you, e.g. `asia-south1` Mumbai).
5. In Firestore **Rules**, paste the contents of [`firestore.rules`](firestore.rules) and **Publish**.
   This limits each shop's data to its own account.
6. Build and install (below). On the first phone tap **"First time? Create the shop account"**.
   On every other phone, **Sign in** with the same email and password.
7. On Home tap **Load starter items** to get Tea, Buns, Milk, Cups and so on, then set your real
   prices in **Products** and your current stock in **Stock**.

The free Firebase (Spark) plan easily covers one shop.

## Build

Requirements: Android Studio (Ladybug or newer) or the Android SDK with platform 35, JDK 17+.

* **Android Studio:** File → Open → this `urban-cha-android` folder → Run ▶ on a phone or emulator.
* **Command line:**
  ```
  ./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/app-debug.apk
  ./gradlew testDebugUnitTest    # shift-time tests
  ```
  Copy the APK to each phone and install it (allow "install unknown apps").

Without `google-services.json` the app still builds, but it shows "Firebase is not set up yet".

## Code map

```
app/src/main/java/com/urbancha/inventory/
  MainActivity.kt              entry point, checks Firebase is configured
  data/ShiftClock.kt           3 PM – 3 AM shift logic (unit-tested)
  data/Models.kt               Product, Material, Shift, …
  data/ShopRepository.kt       all Firestore reads (live) and writes (atomic, offline-safe)
  ui/AppViewModel.kt           live state for the screens, sign-in
  ui/UrbanChaApp.kt            bottom tabs + sign-in screen
  ui/screens/*.kt              Home, Sell, Stock, Opening, Close shift, Reports, Products
```

## Ideas for later
* Separate staff logins with owner-only screens (prices, closing, reports)
* Cancel or edit a bill
* Weekly and monthly totals, and an Excel/PDF export
* Bluetooth receipt printer

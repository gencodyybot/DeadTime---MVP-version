# DeadTime

Real-time flash deal marketplace connecting local businesses with nearby customers during slow hours.

## What it is

Restaurants, salons, gyms, and other local businesses lose money every week during predictable dead hours. DeadTime lets them post a flash deal in under 30 seconds — 20% off for the next 2 hours, 5 spots only. Nearby users get notified instantly and can claim a deal with one tap. When they show up, they scan a QR code. Done.

No subscriptions. No listing fees. The platform takes a 5% cut only when a deal is redeemed.

## How it works

**For businesses**
- Set up a profile once (under 4 minutes)
- Post a deal in 3 taps
- Watch live views, claims, and QR scans on the dashboard
- Get paid weekly via Stripe

**For users**
- See live deals within your radius
- Claim with one tap, get a QR code
- Show up, scan, save money

## Tech stack

- **Android** — Kotlin, Jetpack Compose
- **Backend** — Firebase (Realtime Database, Cloud Functions, Cloud Messaging)
- **Maps** — Google Maps SDK, Places API
- **Payments** — Stripe Connect
- **Auth** — Firebase Auth

## Color theme

| Token | Hex | Usage |
|---|---|---|
| Background | #0E0E0E | App root |
| Surface | #181818 | Cards, deal tiles |
| Accent | #F26419 | Buttons, timers, CTAs |
| Accent text | #FF8040 | Discount %, countdown digits |
| Text primary | #F5F2EE | Titles, deal names |
| Text secondary | #888880 | Subtitles, distances |
| Success | #22C55E | Spots left, earnings |
| Danger | #EF4444 | Expiring soon, sold out |

## Getting started

1. Clone the repo
2. Create a Firebase project and drop google-services.json into /app
3. Enable Realtime Database, Cloud Messaging, and Auth in Firebase console
4. Add your Google Maps API key to local.properties
5. Set up a Stripe Connect account and add your publishable key
6. Run on emulator or physical device (API 26+)

## Status

MVP in development. Currently targeting a single-neighborhood launch in Brampton, ON.

## License

MIT

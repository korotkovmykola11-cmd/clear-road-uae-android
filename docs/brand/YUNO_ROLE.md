# YUNO — Final Product Role

**Stage:** 29.0 foundation  
**Status:** Design constitution — subordinate to `CONTEX.md` v4.0  
**Visual reference:** `docs/design/img.png`, `docs/design/img_1.png` (filtered — not pixel spec)

This document locks YUNO placement and character so implementation stays consistent.

---

## Name

**YUNO**

## Meaning

**Your UAE Road Advisor**

| YUNO is | YUNO is not |
|---------|-------------|
| UAE route advisor mark | AI chatbot |
| Brand recognition | Mascot for decoration |
| Decision support | Game character |

---

## Screen placement

### Home (always)

- YUNO **always visible** on the main screen.
- Does **not** disappear after route input.
- Greets the user — like Michelin or Duolingo owl, but **less intrusive**.
- Small, professional, never half the screen.

### Results (Fastest / No Tolls / Calm + route list)

- YUNO stays on screen (same home scroll).
- Embedded in the flow — supports the decision.
- Role (future copy, not Stage 29.0): *I analyzed the routes. Here is the best choice.*

### Route Details — primary YUNO home

- Route Details is the **core product screen** for YUNO presence beside decision content.
- YUNO lives next to **Why this route** — the heart of Clear Road.
- **Never** beside map preview.
- **Never** beside handoff buttons.

### Handoff (future — not Stage 29.0)

After **Open in Google Maps** or **Open in Waze**, one short closing line only:

- Safe drive.
- Enjoy your trip.
- Best route selected.
- Drive safe.
- Have a smooth ride.

One sentence. No chatter. No speech bubbles.

---

## Character

| Not | Yes |
|-----|-----|
| Friend | Advisor |
| Clown | Calm |
| Chatterbox | Brief |
| AI therapist | Route-focused |
| ChatGPT in app | Your UAE Road Advisor |

YUNO appears when decision help matters. Then steps back.

---

## Stage 29.0 scope (visual foundation only)

- YUNO asset + reusable `YunoBrandBlock`
- Home + Route Details placement
- Static mark + name + tagline

**Out of scope for 29.0:**

- Animations
- Speech bubbles
- Phrases / voice
- Handoff copy
- Onboarding
- Chat / AI

---

## Design rules (locked)

| Yes | No |
|-----|-----|
| Small professional mark | Huge cartoon character |
| Fork-road brand symbol | Speech bubbles |
| Clear Sky Blue accent | Jokes, gamification |
| Secondary to Choice / Why / Tip | Replacing sacred blocks |

**Hero remains the driver.** YUNO supports — never owns the recommendation.

---

## One-page lock-in

```
NAME:     YUNO — Your UAE Road Advisor
NOT:      chatbot · clown · ChatGPT · decoration mascot

HOME:     Always visible · greets user · small
RESULTS:  Embedded · supports decision
DETAILS:  Beside Why this route · not map · not handoff
HANDOFF:  One line after navigate (future)

STAGE 29.0: Visual only — mark + label on Home and Details
```

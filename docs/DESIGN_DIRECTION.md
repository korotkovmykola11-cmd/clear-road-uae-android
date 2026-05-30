# Clear Road UAE — Design Direction Lock

**Stage:** 21.5  
**Status:** Locked  
**Type:** Documentation only — no code  
**References:** `docs/design/img.png`, `docs/design/img_1.png`  
**Companion docs:** `PRODUCT_IDENTITY.md`, `CONTEX.md`

---

## Purpose

Lock the visual and language direction for Clear Road UAE **before** Design System implementation (Stage 22).

This document filters design references into approved direction. It does not authorize full UI implementation.

---

## Product rules (unchanged)

```
Decision First
Map Second
Navigation Later
```

**Sacred blocks:** Choice · Why · Tip  
**Modes:** Fastest · No Tolls · Calm  
**Visible priority:** Time · Salik · Short Why  
**Fuel:** not part of visible UI (Stage 21.0)

**Core formula:**

```
Clear Road decides
→ User understands
→ Google Maps / Waze navigate
```

---

## Hero

**The hero is the driver — not YUNO, not the app, not the map.**

Clear Road helps the driver choose with confidence. The driver remains the protagonist of every screen.

**Hero moment:**

> Driver reads Choice / Why / Tip → understands the tradeoff → opens Maps or Waze with confidence.

YUNO may support the moment. YUNO must never replace it.

---

## YUNO — Guide Character

**Name:** YUNO  
**Form:** fork-in-the-road character (персонаж-развилка)  
**Role:** friendly visual guide — a signature brand detail, not the product brain

### Status lock

| Rule | Decision |
|------|----------|
| Presence | **Optional** — app works fully without YUNO visible |
| Tone | **Friendly** — warm, calm, reassuring |
| Dominance | **Not dominant** — never the largest or loudest element on screen |
| Chatbot | **Not a chatbot** — no free-form conversation UI |
| Assistant | **Not an assistant** — no “ask YUNO anything” |
| Duolingo | **Not Duolingo** — no guilt, pressure, streaks, or gamification |

### What YUNO may do

- Appear in optional guide slots (entry, decision, details)
- Deliver short supportive lines aligned with Tip tone
- Reinforce clarity — not replace Choice / Why / Tip
- Support onboarding and feature discovery in future stages

### What YUNO must not do

- Own the recommendation logic
- Replace sacred Choice / Why / Tip blocks
- Become the app face at the expense of decision content
- Introduce chat, voice assistant, or AI persona behavior
- Push the product toward navigation or map-first UX

### Implementation note (future)

Design System may reserve an optional `GuideSlot`. YUNO asset and animation are **not** Stage 22 scope.

---

## Visual identity

### Direction summary

**Light · Dubai · Calm · Premium · Decision-first**

The future Clear Road should feel:

- bright and clear, not dark dashboard
- locally UAE, not generic global
- calm, not noisy
- premium, not playful-startup
- decision surface, not navigation surface

### Palette & atmosphere

| Element | Lock |
|---------|------|
| **Sky Blue** | Primary brand accent — clarity, open road, Dubai clear sky |
| **Clouds** | Soft atmospheric layer — light, airy, non-dominant |
| **Dubai atmosphere** | Local mood through color and light — not tourist poster overload |
| **Premium light theme** | Default app theme — light mode first |
| **White surfaces** | Primary cards and content surfaces |
| **Minimal shadows** | Subtle elevation only — calm, not material-heavy |

### Visual yes / no

| Yes | No |
|-----|-----|
| Sky blue accent + soft cloud atmosphere | Burj Khalifa wallpaper on every in-app screen |
| White route cards on light background | Map-first blue navigation UI |
| Mode color accents (Fastest / No Tolls / Calm) | Neon startup chaos |
| Generous spacing | Dashboard metric overload |
| Calm typography hierarchy | Aggressive full-width CTAs |
| Short tag rows | Long explanatory paragraphs |

### Mode color language (from references)

| Mode | Direction |
|------|-----------|
| **Fastest** | Blue accent |
| **No Tolls** | Green accent |
| **Calm** | Purple accent |

Mode colors accent cards and labels. They do not replace semantic theme tokens in Stage 22.

---

## Route language

### Modes (product labels)

```
Fastest
No Tolls
Calm
```

### Short Why tags (target copy style)

Tags, not essays. Scannable in 2–3 seconds.

**Approved examples:**

```
8 min faster
0 Salik gates
Main roads
Light traffic
Fewer turns
```

**Rules:**

- Human UAE driver language
- Time and Salik first when relevant
- No fuel references in visible copy
- No technical scoring language
- No fake certainty (“guaranteed fastest”)

---

## Screen direction (from references — filtered)

### Aligned with product (approve for future Design System)

- Decision-first route cards without map on main screen
- Time + Salik gates on cards
- Short Why tag rows
- Recommended badge
- Route Details with “Why this route?” tag section
- External handoff: Open in Google Maps / Waze
- Premium light layout with white surfaces

### Not approved without separate product decision

- Bottom navigation shell (History / Favorites / Profile) as default app structure
- Mascot-first layout where YUNO dominates over decision content
- “Live traffic • Real-time data” bars that imply false precision
- Large navigation-style CTA buttons replacing calm text actions
- In-app map as primary screen
- Departure time / history / favorites as part of initial redesign scope

---

## Relationship to other docs

| Doc | Role |
|-----|------|
| `PRODUCT_IDENTITY.md` | Brand personality and promise |
| `DESIGN_DIRECTION.md` (this file) | Visual and language lock from references |
| `CONTEX.md` | Project constitution and development rules |

If conflict arises:

1. Product rules in `CONTEX.md` win  
2. Brand personality in `PRODUCT_IDENTITY.md` wins over visual preference  
3. This document wins for visual execution details  

---

## Stage gate

**Stage 21.5 complete when:**

- [x] Hero locked (driver, not YUNO)
- [x] YUNO status locked (optional, friendly, not dominant)
- [x] Visual identity locked (sky blue, clouds, premium light, white surfaces)
- [x] Route language locked (modes + short Why tags)
- [x] Product rules reaffirmed
- [x] No code changes

**Next stage:** Stage 22 — Theme tokens + UI presentation models

---

## One-page lock-in

```
HERO:           The driver
CHARACTER:      YUNO — optional fork-guide, friendly, not dominant
NOT:            chatbot · assistant · Duolingo · mascot-first UI

VISUAL:         Sky Blue · Clouds · Dubai atmosphere
THEME:          Premium light · White surfaces · Minimal shadows

MODES:          Fastest · No Tolls · Calm
WHY TAGS:       8 min faster · 0 Salik gates · Main roads · Light traffic · Fewer turns
PRIORITY:       Time · Salik · Short Why

PRODUCT:        Decision First · Map Second · Navigation Later
```

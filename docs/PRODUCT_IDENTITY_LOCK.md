# Clear Road UAE — Product Identity Lock

**Stage:** 27.0  
**Status:** Locked — subordinate to `CONTEX.md` v4.0  
**Read time:** ~2 minutes

This document answers one question: **What is Clear Road in one year?**

Detail lives in `CONTEX.md`, `PRODUCT_IDENTITY.md`, `DESIGN_DIRECTION.md`.  
This file is the **short lock** — read before any design, brand, or UI debate.

---

## 1. Who we are

**Clear Road = Route Decision Assistant** (UAE)

We help the driver choose a route and understand the tradeoff — before opening a navigator.

| We are | We are not |
|--------|------------|
| Decision assistant | Navigator |
| Calm, brief, UAE-aware | Google Maps clone |
| | Waze clone |
| | Chatbot or AI persona |

**Hero:** the driver — not the app, not the map, not YUNO.

---

## 2. Product formula

```
Choice   — what to pick
   ↓
Why      — why it matters (short tags)
   ↓
Tip      — what to expect
   ↓
Maps / Waze   — external navigation only
```

**Visible priority:** Time · Salik · Short Why  
**Modes:** Fastest · No Tolls · Calm

**Order (locked):**

```
Decision First
Map Second
Navigation Later
```

Sacred blocks (Choice · Why · Tip) may change wrapping — never semantics.

---

## 3. YUNO

**YUNO is needed.** Without it, Clear Road is just another route card app.

YUNO = brand recognition — fork-in-the-road character, not product brain.

```
Small   — micro accent, never the largest thing on screen
Rare    — not every screen, not every session
Smart   — one line in Tip tone, not chat
```

YUNO supports Tip. YUNO never replaces Choice / Why / Tip.

Full placement rules wait until YUNO implementation — not Stage 27.0.

---

## 4. Map

**Map Second — and the map stays.**

After Stage 26, Map Preview in Route Details is part of the product. **Do not remove it.**

```
Map supports decision.
Decision never supports map.
```

- Map Preview = **Route Details only**
- **No map on main screen**
- No in-app turn-by-turn
- No polyline handoff to external apps

---

## 5. Forbidden

Without explicit constitution change, these are **never approved**:

| Forbidden | Why |
|-----------|-----|
| Huge YUNO | Mascot-first |
| Speech bubbles | Assistant / chatbot drift |
| Burj (or photo Dubai) as in-app screen background | Poster, not product |
| Bottom navigation | Scope creep — not our architecture |
| Map on main screen | Map-first violation |

---

## After this document

**Stop design debates.** Functional base is stable — protect it.

**Next stage:**

```
Stage 27.1 — Real route testing
```

Verify Map Preview on real corridors:

- Short routes
- Long routes
- Ajman → Dubai
- Abu Dhabi → Dubai
- Marina → Airport

**Main risk now:** a route exists where the map looks wrong — not YUNO, not sky, not brand.

Sky, clouds, YUNO asset, hero polish — **only after 27.1 passes.**

---

## One-page lock-in

```
WHAT:     Route Decision Assistant — not navigator, not Maps, not Waze
FORMULA:  Choice → Why → Tip → Maps/Waze
ORDER:    Decision First · Map Second · Navigation Later
YUNO:     Needed · small · rare · smart
MAP:      Stays in Details — never on main
FORBIDDEN: huge YUNO · bubbles · Burj bg · bottom nav · map on main
NEXT:     Stage 27.1 — real routes
```

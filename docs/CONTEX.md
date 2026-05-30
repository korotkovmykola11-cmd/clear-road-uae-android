# Clear Road UAE — Project Constitution v4.0

**Status:** Primary project constitution  
**Current stable tag:** `stage-22.0-stable`  
**Companion docs:** `PRODUCT_IDENTITY.md`, `DESIGN_DIRECTION.md` (detail locks — subordinate to this file)

---

## Section 1 — Identity

**Clear Road = UAE Route Decision Assistant**

Clear Road is:

- NOT a navigation app
- NOT Google Maps
- NOT Waze
- NOT an AI chatbot
- NOT a map product
- NOT a route encyclopedia

Clear Road exists to help drivers quickly decide:

- which route to choose
- why this route matters
- what tradeoff they are accepting

**Product order (locked forever):**

```
Decision First
Map Second
Navigation Later
```

**Core formula:**

```
Clear Road decides
→ User understands
→ Google Maps / Waze navigate
```

Clear Road must feel:

- calm
- confident
- intentional
- useful
- human
- UAE-aware
- lightweight
- emotionally readable

Clear Road must NOT feel:

- noisy
- analytical
- robotic
- overloaded
- enterprise-heavy
- navigation-first

---

## Section 2 — Driver Priority

**Locked forever — visible product priority:**

```
Time · Salik · Short Why
```

These are the primary decision signals shown to the driver.

**Not primary visible value:**

- Fuel (removed from visible UI — Stage 21.0)
- Distance as a hero metric
- Raw metrics dashboards
- Optimization scores
- Cost obsession

**Distance** remains visible as supporting context. It is **not** the main decision signal.

**Fuel** may remain internal logic for scoring experiments. It must **never** return as visible UI or brand expression without explicit constitution change.

---

## Section 3 — Sacred Blocks

**These blocks must never disappear. They are the product core.**

```
Choice   — what to pick
Why      — why it matters
Tip      — what to expect
```

Rules:

- Any redesign may change wrapping, not semantics
- No mascot, map, or metric layer may replace them
- Short Why tags may augment Why — not replace Choice / Why / Tip

---

## Section 4 — Design Lock

**Locked in Stage 21.5 — Design Direction**

Visual direction:

- **Sky Blue** — primary brand accent, clarity
- **Clouds** — soft atmospheric layer
- **Dubai Atmosphere** — local mood through light and color
- **Premium Light Theme** — default app theme
- **White Surfaces** — primary cards and content
- **Clean Space** — generous spacing, minimal shadows

**Forbidden visual directions:**

- Dark cyberpunk
- Gaming UI
- Enterprise dashboard
- Map-first blue navigation UI
- Tourist postcard overload (Burj on every screen)
- Neon startup chaos
- Aggressive full-width CTAs

**Stage 22 foundation (locked):**

- Semantic theme tokens in `ui/theme/`
- UI presentation models (`ChoiceWhyTipUiModel`, `RouteCardUiModel`, `RouteDetailsUiModel`)
- Premium light Material theme — dynamic system color disabled for brand consistency
- Mode accent colors: Fastest (blue), No Tolls (green), Calm (purple)

**Design-first rules before visual redesign:**

1. UI models and theme tokens before layout overhaul (Stage 22 ✅)
2. Redesign touches composables and theme — not scoring, recommendation, or handoff
3. Product Identity and Design Direction locks filter all visual references
4. No feature scope hidden inside a “design PR”

---

## Section 5 — Hero Rule

**The hero is the driver making a clear choice.**

Clear Road is not the protagonist. The map is not the protagonist. YUNO is not the protagonist.

**Hero moment:**

> Driver reads Choice / Why / Tip → understands → opens Maps or Waze with confidence

---

### YUNO — Optional Rule (locked)

**Name:** YUNO — fork-in-the-road guide character

| Rule | Lock |
|------|------|
| Status | **Optional** — product works fully without YUNO |
| Tone | Friendly, calm |
| Dominance | **Not mascot-first** |
| Chatbot | **Not a chatbot** |
| Assistant | **Not an assistant** |
| Duolingo | **Not a Duolingo clone** |

YUNO may exist as a signature brand detail.  
**The product does not depend on YUNO.**

YUNO may support Tip tone. YUNO must never replace Choice / Why / Tip.

`GuideSlotUiModel` is reserved in UI models. YUNO assets are not required for any current stage.

---

## Section 6 — Map Rule

**Locked forever:**

```
Map supports decision.
Decision never supports map.
```

- Map = infrastructure
- Decision = product
- Preview must NOT live on the main decision screen
- Map Preview (when implemented) = Details screen only
- No in-app turn-by-turn navigation
- No route geometry handoff to external apps

---

## Section 7 — Navigation Rule

**Locked forever:**

```
Clear Road decides.
Google Maps navigates.
Waze navigates.
```

External navigation (Stage 20.0):

- Generic `ACTION_VIEW` handoff
- Origin and destination coordinates only
- No polyline, no selected alternative forcing
- No Google Maps SDK for navigation
- Waze fallback chain when app unavailable

Clear Road does NOT compete with Google Maps or Waze.

---

## Section 8 — UAE Intelligence

**Future product direction — not current scope**

Clear Road should evolve toward UAE-specific context:

- Marina congestion waves
- SZR lane stress
- Hessa Street pressure
- E311 relief flow
- Airport waves
- School traffic
- Event traffic
- Friday prayer patterns
- Evening merge behavior
- UAE merge psychology

This is a future differentiator. It must not drift the product toward navigation or map-first behavior.

---

## Section 9 — Route Personalities

**Locked brand modes:**

```
Fastest
No Tolls
Calm
```

These are product modes and brand language — not just filter labels.

Route cards and copy should express mode-appropriate tradeoffs:

- **Fastest** — time-first
- **No Tolls** — Salik-first
- **Calm** — comfort and flow-first

**Short Why tag examples (target language):**

```
8 min faster
0 Salik gates
Main roads
Light traffic
Fewer turns
```

Personality adjectives (supporting, not replacing modes):

Smooth · Balanced · Stable · Efficient · Quieter · Predictable · Calm · Dense

---

## Section 10 — Language Rule

**The app speaks like a UAE driver — human, local, emotionally readable.**

**Forbidden:**

- Traffic coefficient
- Congestion index
- Dynamic traffic matrix
- Route optimization score
- Predictive vector
- Flow intensity
- Metric obsession language

**Allowed:**

- Smoother drive
- Calm evening route
- Stable flow
- Busy merge area
- Heavier Salik pressure
- 8 min faster
- No Salik
- Main roads

**Multilanguage (future):**

English · Русский · Українська · العربية

Rules: meaning > literal translation · preserve emotional tone · RTL for Arabic · maintain UAE-driving feeling

---

## Section 11 — Architecture Rule

**Forbidden without explicit approval:**

- ViewModel
- Repository
- UseCases
- Dependency Injection
- Clean Architecture
- Enterprise patterns

**Reason:** the project must remain controllable, lightweight, stable, and understandable.

**Allowed architecture today:**

- `MainActivity` controls app flow intentionally
- Dumb extracted composables
- Small platform utilities (`NavigationHandoff.kt`, `DirectionsParsing.kt`)
- Domain copy/scoring helpers (`domain/RouteReasoning.kt`, etc.)
- UI presentation models assembled in MainActivity
- Semantic theme tokens

**Refactor allowed ONLY if it:**

- reduces risk
- improves stability
- prevents MainActivity collapse
- improves control
- reduces break probability

**Forbidden refactors:**

- beauty refactors
- architecture fashion
- enterprise refactors
- unnecessary abstraction
- uncontrolled AI rewrites

---

## Section 12 — Development Workflow

**Locked forever:**

```
ChatGPT  → Audit / Prompt
Cursor   → Code (controlled steps only)
User     → Build
User     → Screenshots
ChatGPT  → Verification
Git      → Checkpoint
```

**Critical dev rules:**

- never break working code
- small controlled steps only
- visual verification required
- no giant rewrites
- no hidden changes
- no uncontrolled Cursor behavior
- no speculative coding
- Cursor is controlled — NOT autonomous

---

## Section 13 — Current Stable Checkpoint

| Stage | Deliverable |
|-------|-------------|
| **20.0** | External navigation handoff (Google Maps / Waze, generic ACTION_VIEW) |
| **21.0** | Fuel UI removal — visible layer aligned with Time · Salik · Short Why |
| **21.5** | Design Direction Lock (docs — YUNO, visual identity, route language) |
| **22.0** | Theme tokens + UI presentation models (preparation foundation) |

**Current stable tag:** `stage-22.0-stable`

**Verified locks at this checkpoint:**

- Product Identity locked
- Design Direction locked
- Fuel removed from visible UI
- Decision First philosophy preserved
- Handoff external and working
- Theme foundation in code

---

## Section 14 — Next Stage

### Stage 23.0 — Visual Redesign Foundation

**Scope:** UI only

**Rules:**

- No scoring changes
- No recommendation changes
- No route logic changes
- No handoff changes
- No YUNO implementation
- No Map Preview
- No Multilanguage
- No Voice

**Goal:** apply Design Lock to composables — premium light cards, mode accents, clean spacing, Why tag row preparation

**After Stage 23 (planned, not started):**

- Stage 24 — Simplified Why Layer tags in UI
- Stage 25 — Map Preview (Details only)
- Stage 26+ — Multilanguage + RTL, optional YUNO slot, Voice

---

## Final Definition

```
Clear Road UAE
Lightweight UAE Route Decision Assistant

Decision First · Map Second · Navigation Later
Time · Salik · Short Why
Choice · Why · Tip
Premium Light · Sky Blue · Clean Space
Hero: the driver
YUNO: optional, not required
```

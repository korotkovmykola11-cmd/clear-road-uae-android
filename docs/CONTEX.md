# Clear Road UAE — Project Context

## 1. Project Idea

Clear Road is a route decision assistant for drivers in the UAE.

Goal:
Not just navigation (like Google Maps), but a lightweight decision engine that explains:

* which route to choose
* why it is better
* what to expect (cost, tolls, timing)

The app must stay:

* simple
* fast
* minimal
* human-readable

It must NOT become:

* a Google Maps clone
* a complex navigation platform
* an overengineered architecture project

---

## 2. Current Project Status

CURRENT STAGE:
Stage 10 — Foundation Route Intelligence Engine

Implemented:

* Android app running successfully
* Kotlin + Jetpack Compose
* Google Places Autocomplete
* Origin/Destination selection
* LatLng extraction
* Google Directions API integration
* alternatives=true enabled
* Multiple routes parsing
* Dynamic route cards
* Fastest / No Tolls / Calm modes
* Real recommendation switching
* Choice / Why / Tip logic
* Route scoring engine
* Fuel estimation (temporary local model)
* Toll estimation (temporary lightweight model)
* Total AED estimation
* AED-per-minute scoring influence
* Recommended route highlighting
* Route selection UI
* Emulator testing completed
* Git + GitHub connected

---

## 3. Product Logic (CORE)

Clear Road is NOT a navigation app.

It is a:
ROUTE DECISION ASSISTANT.

The app must:

* analyze multiple possible routes
* choose ONE recommended route
* explain the decision clearly

Output format:

* Choice
* Why
* Tip

---

## 4. Modes

### FASTEST

Prioritize shortest overall travel time.

### NO_TOLLS

Prioritize lower road/toll spending.

### CALM

Balanced route:

* smoother drive
* balanced cost
* balanced time

---

## 5. UAE-Specific Logic

Project focuses on UAE driving conditions:

* Salik (Dubai toll system)
* Darb (Abu Dhabi toll system)
* UAE fuel prices
* UAE driving behavior
* Traffic-heavy corridors
* Parking risk awareness

IMPORTANT:
Current fuel/toll calculations are still provisional.
Real UAE economic intelligence will be implemented gradually.

---

## 6. UX Rules

CRITICAL:

* No noise
* No clutter
* No overload
* Only useful information
* Short explanations
* Human-readable text
* No technical jargon

UI must stay:
clean and minimal.

---

## 7. Development Rules (CRITICAL)

### Core Rules

* No breaking working code
* Small steps only
* No random code generation
* Every change must be visually testable
* Always test after each step
* Cursor writes code
* ChatGPT audits and controls code quality
* User validates builds/screenshots manually

---

### Architecture Rules

DO NOT introduce:

* ViewModel
* Clean Architecture
* Repository layers
* UseCases
* Dependency Injection
* Enterprise patterns

UNLESS explicitly approved by the user.

---

### Refactor Rule (VERY IMPORTANT)

Refactoring is NOT allowed automatically.

AI / Cursor MUST ask user permission BEFORE any refactor.

Refactoring is allowed ONLY when it clearly reduces project risk:

* prevents MainActivity from becoming unmanageable
* isolates fragile logic
* reduces chance of AI/Cursor breaking working code
* improves controllability
* keeps behavior visually testable

Refactoring is NOT allowed when it only adds architecture.

Examples of forbidden refactors:

* adding ViewModel “because it is best practice”
* splitting files for style only
* rewriting working logic
* architecture redesign without user approval

---

## 8. Current Technical Reality

Current fuel model:
TEMPORARY / ESTIMATED.

Current toll model:
PARTIAL / LIGHTWEIGHT.

Current scoring:
REAL and ACTIVE.

The app already performs actual route comparison and recommendation logic.

However:
real UAE fuel/toll intelligence is still under development.

---

## 9. Current Priorities

Current priority is NOT:

* maps clone features
* navigation
* enterprise architecture

Current priority IS:

* stable route intelligence
* UAE-aware decisions
* controllable AI-assisted development
* keeping project manageable

---

## 10. Critical Philosophy

This project must remain:

A SIMPLE UAE ROUTE DECISION ASSISTANT.

NOT:

* a Google Maps replacement
* an AI-generated architecture experiment
* a bloated platform

The project must evolve carefully,
with small controlled improvements only.

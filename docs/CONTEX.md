# Clear Road UAE — Project Context

## 1. Project Idea

Clear Road is a route decision assistant for drivers in the UAE.

Goal:
Not just navigation (like Google Maps), but a **decision engine** that explains:

* which route to choose
* why it is better
* what to expect (traffic, tolls, timing)

---

## 2. Current Stage (IMPORTANT)

The project is at **Stage 1 — Domain Logic Implemented (Mock Data)**

What is DONE:

* Android app runs successfully on emulator

* Basic UI screen exists (Clear Road screen)

* Domain layer created:

    * RouteOption
    * PreferenceMode (FASTEST, NO_TOLLS, CALM)
    * TripCost (structure only)
    * DecisionResult
    * RouteDecisionEngine

* RouteDecisionEngine:

    * Contains mock UAE-style routes
    * Chooses best route based on mode:

        * FASTEST → shortest time
        * NO_TOLLS → lowest toll
        * CALM → least salik gates

* Returns:

    * choice
    * why
    * tip
    * tripCost (currently NULL)

---

## 3. What is NOT done yet

* No Google Maps API
* No real routing data
* No autocomplete
* No real cost calculation (fuel, total AED)
* UI is static (not connected to engine)

---

## 4. Next Step (VERY IMPORTANT)

We are starting:

### Stage 2 — Connect UI to Decision Engine

Goal:

* Call RouteDecisionEngine from UI
* Display REAL calculated result on screen
* Remove static text

---

## 5. Product Logic (CORE)

Clear Road must:

* Always explain decisions
* Be simple (no overload)
* Focus on UAE specifics:

    * Salik (Dubai toll)
    * Darb (Abu Dhabi toll)
    * Rush hours
    * Parking risks

---

## 6. UX Rules

* No noise
* Only useful info
* Short explanations
* Human-readable text
* No technical language

---

## 7. Dev Rules

* No breaking existing code
* Small steps only
* No random code generation
* No API integration until domain is stable
* Always test after each step

---

## 8. Current UI State

UI shows:

* Choice
* Why
* Tip

BUT:
👉 Data is hardcoded (fake)

---

## 9. What MUST be done next

1. Connect engine to UI
2. Replace static text with real result
3. Add mode switch (FASTEST / NO_TOLLS / CALM)

---

## 10. Critical Rule

This project must NOT turn into:

* another Google Maps clone
* overcomplicated system

It must stay:
👉 simple decision assistant

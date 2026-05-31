# Stage 30.2 — Road Dashboard (Approved + Implemented)

**Status:** Approved with 3 modifications — implemented  
**Direction:** Concept B — UAE route tool, not landing page  

---

## Approved modifications

1. **Hero = 42%** (within 40–45%), tool band = 58%
2. **No marketing headline** — Clear Road at `titleMedium`; advisor line only via existing `YunoBrandBlock`
3. **Choice / Why / Tip** on sand surface — same material family as tool band (no white card)

---

## Screen structure

```
┌────────────────────────────────────── 0%
│  ZONE A — DUBAI HERO  ~42%            │
│  home_hero_dubai.png (this box only)  │
│  Clear Road (product wordmark)        │
│  ───────── HORIZON ─── [ YUNO ]       │
├────────────────────────────────────── 42%
│  ZONE B — ROAD TOOL BAND              │
│  SandSecondary #E9E2D6                │
│  From / To / ModeTabs                 │
│  Choice / Why / Tip (same sand)       │
│  Route cards (RouteCardTint #F5F2EC)  │
└────────────────────────────────────── 100%
```

---

## Acceptance criteria

| Impression | Pass |
|------------|------|
| "I am in UAE" | Dubai hero dedicated, not wallpaper |
| "Advisor helps me choose a route" | YUNO on horizon + inputs visible |
| "I can start immediately" | From / To / Modes above fold |
| Fail: tourism poster | Hero too large or giant headline |
| Fail: generic form | White card stack |

---

## Files changed

- `HomeBackground.kt` — `HomeHeroZone` only
- `MainActivity.kt` — hero + sand scroll column
- `ClearRoadColors.kt` — `ToolBandRaised`, `RouteCardTint`, `ToolBandBorder`

---

## Visual sketch

`docs/design/stage_30_2_road_dashboard_sketch.png`

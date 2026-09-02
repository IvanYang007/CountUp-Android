# PATINA_IMPLEMENTATION_PLAN.md — Card Color Warmth Shift ("Patina") System

This specification defines the architecture, mathematical model, and Jetpack Compose implementation for the **Card Color Warmth Shift ("Patina")** system in CountUp, directly grounded in the interactive design prototype `patina_prototype.html`.

## 1. Design Vision & Philosophy
In CountUp's Zen-paper aesthetic, time is tangible:
- Newly planted habits and recent events start cool and fresh (**Zen Sage** `#7D9D8B`).
- As calendar days accumulate, the card matures through earthen **Sand** (`#B5A88F`), rich **Zen Ochre** (`#C88D58`), and **Deep Amber** (`#CF9A4F`).
- Reaching landmark maturity (180+ days) crowns the streak in luminous **Kintsugi Gold** (`#D6A848`).
- 100% deterministic, passive, and derived strictly from `daysSince(anchorDate, today)`. Zero background battery drain.

## 2. Mathematical Warmth Model
$$\text{warmth}(d) = \left(\min\left(\frac{d}{180.0}, 1.0\right)\right)^{0.72} \quad (d > 0)$$
- **Concave growth curve ($p = 0.72$)**: Gives perceptible warmth during formative 1–30 days.

## 3. Dynamic Alpha & Gradient Border (Variance B)
- **Alpha**: $\alpha = 0.28 + (0.44 \times \text{warmth}) \in [0.28, 0.72]$
- **Border**: Diagonal linear gradient brush (135°) from top-left leading color to bottom-right trailing color. Stroke width: `1.5.dp`.

## 4. Maturation Phases & Badges
| Phase | Day Range | Badge Text | Phase Name |
|---|---|---|---|
| `FRESH_GROWTH` | $1 \le d < 15$ | `COOL SAGE` | Cool Sage |
| `WARMING` | $15 \le d < 30$ | `WARMING` | Sage-Sand Blend |
| `GROUNDED` | $30 \le d < 90$ | `ZEN OCHRE` | Zen Ochre |
| `SEASONED` | $90 \le d < 180$ | `DEEP AMBER` | Deep Amber |
| `KINTSUGI` | $d \ge 180$ | `GOLD PATINA` | Kintsugi Gold |

## 5. Architecture & Standards Compliance
- **android-kotlin-architecture**: Pure domain separation, immutable models (`@Immutable PatinaData`), zero side-effects.
- **android-kotlin-compose**: `clearAndSetSemantics` TalkBack accessibility, WCAG AAA text contrast (`ZenInkBlack` on light, `ZenWhite` on dark), zero allocations in composition body.
- **android-kotlin-testing**: Comprehensive JVM unit tests in `PatinaTest.kt` verifying monotonicity, boundaries, colors, and token contracts.

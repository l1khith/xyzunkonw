# SplitSignal UI Design System

Adapted from the CashBuddy / PaisaPal Compose design system and its
`UI_UX_SPECIFICATION.md`. The component structure and spacing scale are reused;
the palette is remapped to SplitSignal's Linear-minimalist tokens.

## 1. Principles

1. Editorial clarity over decoration. Numbers, hairlines, and hierarchy.
2. One accent for action, one for success, one for danger. No gradients on text.
3. Tabular numerals (`fontFeatureSettings = "tnum"`) for every counter, timer,
   Elo, file size, and transfer speed.
4. 1 dp translucent hairlines. No heavy shadows on content cards.
5. Vector icons only. No emoji glyphs in production UI.

## 2. Color Tokens

File: `composeApp/src/commonMain/kotlin/com/splitsignal/app/ui/design/SsTokens.kt`

| Token | Value | Use |
|---|---|---|
| `BgCanvas` | `#08090A` | App background |
| `SurfaceCard` | `#101114` | Cards, nav bars, top bars |
| `SurfaceTile` | `#17191E` | Insets, chips, input wells |
| `SurfaceTileActive` | `#1F2229` | Pressed tiles |
| `BorderSubtle` | `#14FFFFFF` | Resting hairline |
| `BorderFocus` | `#2EFFFFFF` | Focused hairline |
| `BorderPress` | `#38FFFFFF` | Pressed border |
| `TextPrimary` | `#F7F8F8` | Primary ink |
| `TextSecondary` | `#8A8F98` | Secondary ink |
| `TextMuted` | `#555A64` | Disabled ink |
| `AccentIndigo` | `#5E6AD2` | Primary action, self chat bubble |
| `AccentEmerald` | `#26B57B` | Online, success, saved |
| `AccentCrimson` | `#E5484D` | Errors, destructive actions |
| `AccentAmber` | `#F5A623` | Streak, warnings, in-game |

## 3. Typography

File: `SsTokens.kt`

- Display / headline: sans-serif, semibold, tight tracking.
- Body: sans-serif, normal, 14 sp / 20 sp.
- Labels: 11-12 sp, semibold, 0.2-0.7 sp tracking.
- Tabular: monospace, semibold, `tnum`, used for all changing numbers.

Use `LinearTextStyles.TabularDigit` and `LinearTextStyles.TabularLabel` for
counters. Do not animate width-bearing text without `tnum`.

## 4. Spacing and Shape

- Spacing scale: `xxs 2`, `xs 4`, `sm 8`, `md 12`, `lg 16` dp.
- Shape scale: `small 4`, `medium 8`, `large 16`, `extraLarge 24` dp.
- Cards use 14 dp radius in SplitSignal for the editorial look.
- Buttons use 8 dp; pills use 999 dp.

## 5. Components

File: `composeApp/src/commonMain/kotlin/com/splitsignal/app/ui/design/SsComponents.kt`

### 5.1 `SsCard`
Surface card with top-rim highlight, 1 dp hairline, and configurable border color.
Replaces ad-hoc `LinearCard` styling for new surfaces.

### 5.2 `SsButton`
Primary, secondary, and ghost variants. Optional leading vector icon. Spring press
scale inherited from `TactileTile`.

### 5.3 `SsIconButton`
34 dp square icon action with hairline. Requires a `contentDescription`.

### 5.4 `SsChip`
Pill-shaped filter or state chip with selected animation. Optional leading icon.

### 5.5 `SsSectionHeader`
Uppercase section label with optional trailing text action.

### 5.6 `SsStatusBadge`
Compact status pill. Tones: `Neutral`, `Success`, `Warning`, `Danger`, `Accent`.

### 5.7 `SsEmptyState`
Centered vector icon, title, subtitle, and optional action button.

### 5.8 `SsTopBar`
Contextual 56 dp bar with title, optional trailing content, bottom hairline.

### 5.9 `SsBottomNav` / `SsNavRail`
Floating pill navigation imported from the CashBuddy bottom bar. Active item uses
an accent-tinted pill; the rail variant is used on expanded widths.

## 6. Iconography

File: `composeApp/src/commonMain/kotlin/com/splitsignal/app/ui/design/SsIcons.kt`

`SsIcons` exposes imported `DrawableResource` vectors and `ImageVector` fallbacks.
`SsIcon` renders either consistently at a requested size and tint.

Imported vector drawables live in
`composeApp/src/commonMain/composeResources/drawable/` and are rendered with
`painterResource(Res.drawable.ic_*)`.

Rules:

- Icon size: 16 dp inline, 18 dp toolbar, 20-24 dp navigation, 56 dp empty state.
- Icon tint follows the surrounding text color unless it carries semantics.
- Decorative glyphs such as checkmarks and crosses are never text.

## 7. Motion

- Press feedback: spring scale to `0.94f`, 120 ms border brighten.
- Screen transition: vertical slide 1/16 plus fade, 220 ms.
- Progress rings and radar pulses: infinite linear or eased loops.
- Respect reduced-motion where the platform exposes it.

## 8. Accessibility

- Every icon-only action has a `contentDescription`.
- Minimum touch target: 48 dp. `SsIconButton` renders 34 dp visual inside a
  larger hit slot where space allows.
- Contrast: primary ink on canvas exceeds 15:1. Do not place secondary ink on tiles.

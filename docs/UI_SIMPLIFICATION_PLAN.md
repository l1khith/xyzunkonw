# SplitSignal UI Simplification Plan

Status: In progress
Owner: UI / Compose Multiplatform
Scope: Ship a polished, finishable 3-pillar app without touching the deterministic game engines.

## 1. Decision

Keep the chat. Keep the P2P file transfer. Cut everything that is not required to
prove the KMP story.

The single strongest proof in this project is not a battleship grid. It is:

1. UDP peer discovery over local Wi-Fi.
2. Ktor CIO WebSocket chat.
3. 64 KB chunked streaming file transfer up to 1 GB.
4. One shared Compose Multiplatform UI on Android, Desktop, and Wasm.

That is the demo. Everything else is optional decoration.

## 2. Ruthless Cut List

Remove from navigation and default flows:

| Feature | Action | Reason |
|---|---|---|
| Battleship / grid battles | Remove entry points; keep engine code dormant | Real-time grid UI is the largest unfinished surface |
| Vault shop / currency store | Remove entry point; keep earned Elo and streak | Store UI adds screens without proving KMP |
| Voice notes | Delete unused parameters and callbacks | Native audio recording is not shipped |
| Sticker / emoji pickers | Delete unused parameters and callbacks | Not required for the demo |
| File previews and thumbnails | Never implement; icon + progress only | Avoids image decoding and platform work |
| Replay parsing | Not implemented | Out of scope |

Keep:

| Feature | Reason |
|---|---|
| Play hub | Primary product surface |
| Daily workout + solo practice | Depth without multiplayer scope |
| Network tab | Discovery, friends, presence |
| Direct chat | Proves WebSocket text sync |
| Send file | Proves streaming transfer and native save |
| Profile | Identity, Elo, streak |

## 3. Target Screens

The app has exactly three navigation destinations:

1. `Play` - Arena, daily workout, solo practice.
2. `Network` - Friends in range, discovered peers, offline friends, chat overlay.
3. `Profile` - Identity, Elo, streak, saved history.

Chat is an overlay pushed over `Network`, not a fourth tab. File transfer is a card
inside the chat thread.

## 4. Design System Import From CashBuddy

CashBuddy (`C:\Users\ailik\AndroidStudioProjects\CashBuddy`) is the source of the
component vocabulary. Import the structure and API shape, then remap the palette to
the SplitSignal Linear-minimalist tokens.

### 4.1 Source Files Reviewed

| CashBuddy source | Imported as | Notes |
|---|---|---|
| `presentation/theme/Color.kt` | `ui/design/SsTokens.kt` | Remapped to SplitSignal canvas/surface/accent |
| `presentation/theme/Typography.kt` | `ui/design/SsTokens.kt` | Imported scale, overridden with SplitSignal ink colors |
| `presentation/theme/Shape.kt` | `ui/design/SsTokens.kt` | 4 / 8 / 16 / 24 dp radii |
| `presentation/theme/Spacing.kt` | `ui/design/SsTokens.kt` | 2 / 4 / 8 / 12 / 16 dp grid + `LocalSsSpacing` |
| `presentation/theme/Easing.kt` | `ui/design/SsTokens.kt` | `SsEasing` cubic curves |
| `presentation/components/PaisaPalComponents.kt` | `ui/design/SsComponents.kt` | Chip + section header |
| `presentation/components/CashBuddyTopBar.kt` | `ui/design/SsComponents.kt` | Contextual top bar |
| `presentation/components/CashBuddyBottomBar.kt` | `ui/design/SsComponents.kt` | Floating nav pill + action button |
| `presentation/components/StatusBadge.kt` | `ui/design/SsComponents.kt` | Status pill + confidence bars |
| `presentation/components/EmptyStateView.kt` | `ui/design/SsComponents.kt` | Icon + title + subtitle + action |
| `composeResources/drawable/ic_*.xml` | `composeApp/src/commonMain/composeResources/drawable/` | Vector drawables, no bitmaps |
| `docs/UI_UX_SPECIFICATION.md` | `docs/UI_DESIGN_SYSTEM.md` | Adapted, not copied verbatim |

### 4.2 Palette Remap

CashBuddy token -> SplitSignal token:

| CashBuddy | SplitSignal | Value |
|---|---|---|
| `BackgroundDark` | `BgCanvas` | `#08090A` |
| `SurfaceDark` | `SurfaceCard` | `#101114` |
| `SurfaceElevatedDark` | `SurfaceTile` | `#17191E` |
| `PrimaryIndigo` | `AccentIndigo` | `#5E6AD2` |
| `AccentEmerald` | `AccentEmerald` | `#26B57B` |
| `DangerRed` | `AccentCrimson` | `#E5484D` |
| `WarningAmber` | `AccentAmber` | `#F5A623` |
| `TextPrimaryDark` | `TextPrimary` | `#F7F8F8` |
| `TextSecondaryDark` | `TextSecondary` | `#8A8F98` |
| `DividerDark` | `BorderSubtle` | `0x14FFFFFF` |

## 5. Iconography Rule

No emoji glyphs in production UI. Every pictogram is one of:

1. An imported `composeResources/drawable/ic_*.xml` vector.
2. A Material `ImageVector` from `compose.materialIconsExtended`.
3. A `Canvas` drawing for live indicators such as the radar pulse.

Text-only characters such as `+`, `=`, and digits are allowed. Decorative emoji
such as fire, checkmarks, crosses, and circles are replaced with vector icons.

### 5.1 Emoji Replacement Map

| Location | Current | Replacement |
|---|---|---|
| `SplitSignalApp.kt` streak chip | U+1F525 fire | `SsIcons.StreakFire` + tabular count |
| `MatiksSharedComponents.kt` copied label | U+2713 check | `Icons.Rounded.Check` |
| `BattleshipScreen.kt` hit / miss / sonar | U+2715, U+25CB | `Close`, `RadioButtonUnchecked`, `Square` vectors |
| `MatiksPuzzleScreens.kt` row / column lock | U+2713 | `Icons.Rounded.Check` |
| `MathPuzzleScreen.kt` clue state | U+2713 / U+25CB | `Check` / `RadioButtonUnchecked` |
| `OnboardingOverlay.kt` solve toast | U+2713 | `Icons.Rounded.CheckCircle` |
| `GamesHubScreen.kt` completion labels | U+2713 | `Icons.Rounded.Check` |
| `ProgressionStore.kt` scorecard text | U+1F525 fire | plain `Streak` word |

## 6. Implementation Phases

### Phase 1 - Foundation (this change)
- [x] Import CashBuddy vector drawables into `composeResources/drawable`.
- [x] Add `com.splitsignal.app.ui.design` package: tokens, spacing, shapes, components, icons.
- [x] Provide spacing and shapes through the theme.
- [x] Replace every emoji / decorative glyph with a vector icon.
- [x] Wire the app shell top bar, nav bar, and Network actions to the imported components.

### Phase 2 - Surface Cleanup
- [ ] Remove Battleship, Vault, Triathlon, and sticker/voice parameters from navigation and public UI APIs.
- [ ] Collapse `GamesHubScreen` into Arena + Daily Workout + Solo Practice cards.
- [ ] Collapse `DirectChatSheet` to text bubbles + file cards only.
- [ ] Delete dead composables after the new surfaces are verified.

### Phase 3 - Polish
- [ ] Tabular figures on every counter.
- [ ] Hairline border audit at 1 dp.
- [ ] Empty, loading, and error states for all three tabs.
- [ ] Desktop rail and mobile bottom bar visual parity.

### Phase 4 - Verification
- [ ] `./gradlew :composeApp:compileDebugKotlinAndroid`
- [ ] `./gradlew :composeApp:compileKotlinDesktop`
- [ ] `./gradlew :composeApp:compileKotlinWasmJs`
- [ ] Manual smoke: discover peer, chat, send a large file, save.

## 7. Definition of Done

- Three visible destinations only.
- Chat available from every discovered or saved friend.
- File transfer renders as icon + progress + saved state.
- No emoji or decorative Unicode glyphs in any `commonMain` UI source.
- Android, Desktop, and Wasm compile from `commonMain` without platform branches.

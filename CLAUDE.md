# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Sudoku Trio is an open-source (MIT) Sudoku for Android, to be published on Google Play: Classic, Sudoku X (diagonals) and Killer Sudoku, each with easy / medium / hard / expert. Kotlin, Jetpack Compose, Material 3 Expressive with dynamic color, Hilt, multi-module Clean Architecture. The UI is in pt-BR and English: every user-facing string lives in `values/strings.xml` and `values-pt-rBR/strings.xml` of its module, never hard-coded. The infrastructure (build-logic, CI, quality gates) is ported from Solo (`../paciencia`, github.com/vinaooo/solo). The user makes the product and design decisions, so ask before choosing one.

- applicationId `io.github.vinaooo.SudokuTrio` (as given); Kotlin packages and namespaces are lowercase `io.github.vinaooo.sudokutrio`, because ktlint rejects uppercase package names.

## Game decisions (agreed with the user)

- **Mistake** = a digit that differs from the unique solution. No limit, no loss. Counted silently for the score, never shown red; only visible conflicts (duplicates in a row, column, box, diagonal in X, cage in Killer) are highlighted.
- **Score:** base 1000 / 2000 / 4000 / 8000 (easy → expert) × variant factor (Classic 1.0, X 1.2, Killer 1.5), −1 per second, −100 per mistake, −200 per hint, clamped at 0. Ranking: highest score, per variant + difficulty.
- **Number pad:** `1 2 3 4 5` / `6 7 8 9 Erase` / full-width Notes toggle.
- **Hint:** first tap highlights the cells and names the technique; second tap places the digit. Counted once, at the reveal. If a wrong digit is on the board, the hint points at it first.
- **Generation:** at runtime from a seed. Difficulty = the solver's **score** (each step's technique weight added up) falling in a per-variant band, plus the techniques each level may use (user's choice over "hardest technique", which reached Classic hard/expert in under 10% of puzzles). Killer keeps a few givens at every level when its cages alone aren't enough (user's choice; cages-only boards were logically solvable in ~3% of layouts).
- Placing a digit removes it from the notes of every cell it sees; undo restores them. Mistakes survive undo.
- Variant and difficulty are chosen in Settings only. No contextual toolbar button, no stuck detection, no time limit.

## Commands

JDK 21 and Android SDK Platform 37 are required (compileSdk 37, targetSdk 36, minSdk 26).

```bash
./gradlew assembleDebug                          # build
./gradlew installDebug                           # build + install on the connected device
./gradlew ktlintCheck detekt lint                # static analysis (ktlintFormat fixes formatting)
./gradlew test koverVerify                       # all unit tests (screenshots included) + coverage gates
./gradlew recordRoborazziDebug                   # re-record screenshot goldens after an intended UI change
./gradlew :domain:pitest                         # mutation testing (gate: 80% killed, 90% coverage); ~3.5 min
./gradlew :domain:benchmarkGenerator -Pseeds=30  # generation time, givens, difficulty hit rate and uniqueness per mode
./gradlew :app:assembleRelease                   # minified (R8) release APK, signed when the upload key is configured
adb shell am start -n io.github.vinaooo.SudokuTrio/io.github.vinaooo.sudokutrio.MainActivity   # launch: applicationId and Kotlin package differ in case
adb shell am start -S -n io.github.vinaooo.SudokuTrio/io.github.vinaooo.sudokutrio.debug.DebugGameActivity --es game killer_near_win   # debug build: a test position (near_win, x_near_win, killer_near_win, wrong_digit, elimination, killer; --es state <code>: a bug report's "State:" block; --es load game.json: a report's attachment pushed to /sdcard/Android/data/io.github.vinaooo.SudokuTrio/files/)
```

- The user's phone often shows up twice over wireless adb: check `adb devices -l` and pin `ANDROID_SERIAL`.

The full gate, matching CI: `./gradlew ktlintCheck detekt lint test verifyRoborazziDebug koverVerify`.

- **Low RAM:** `gradle.properties` is sized for a low-RAM dev machine (2 GB Gradle heap, no parallel builds, 1 GB test workers). Don't raise these values. CI writes its own larger settings to `~/.gradle/gradle.properties`.
- **Warnings are errors** in every Kotlin compilation. Experimental APIs need a local `@OptIn`. Test compilations already opt in to `ExperimentalCoroutinesApi`.
- **Release signing:** `ReleaseSigning` (in `build-logic`) reads `sudokutrio.signing.{storeFile,storePassword,keyAlias,keyPassword}` from `local.properties` or the `SUDOKUTRIO_SIGNING_*` environment variables (environment wins). Nothing set → unsigned; a partial setup fails and names what's missing.
- **Ads IDs:** `AdIds` reads `sudokutrio.ads.appId` / `sudokutrio.ads.bannerId` (or `SUDOKUTRIO_ADS_*`) for release builds; debug builds always use Google's test IDs.
- **Versions** come from git (`AppVersion`): `versionCode` = `git rev-list --count HEAD`, `versionName` = latest `vX.Y.Z` tag (`0.0.0-g<hash>` before the first). A shallow clone fails the build, so workflows check out with `fetch-depth: 0`.
- **Play upload:** `.github/workflows/release.yml`, off until the repository variable `PLAY_UPLOAD_ENABLED` is `true`. The Play developer account is newer than 13 Nov 2023: a closed test (12+ testers, 14 days) is needed before production.

## Build setup

- Modules apply convention plugins from `build-logic/convention` (`sudokutrio.android.application`, `.android.library`, `.android.compose`, `.android.feature`, `.hilt`, `.jvm.library`, `.quality`, `.root.coverage`).
- AGP 9 has Kotlin built in (no `kotlin-android` plugin). Versions are pinned in `gradle/libs.versions.toml`. `material3` is pinned to a 1.5.0 alpha above the Compose BOM, because that's where the Expressive APIs are public.
- **Coverage:** the root project merges every module's Kover data with the same filters (`KoverFilters.kt`); the root floor is 70% of lines, `:domain` has its own 90% rule.
- `./gradlew test` also runs `build-logic`'s own tests.
- The Gradle wrapper was generated with `gradle wrapper --gradle-version 9.8.0 --gradle-distribution-sha256-sum …`, never copied.

## Tests

- JUnit 5 + Kotest (assertions, property tests) for plain unit tests; Robolectric tests are JUnit 4 and run through the vintage engine. Robolectric runs at SDK 36 (`src/test/resources/robolectric.properties`).
- Repository fakes go in `:domain`'s `testFixtures`; prefer them to mocks.

## Architecture

### `:domain` (pure Kotlin, package `io.github.vinaooo.sudokutrio.domain`)

- **Model:** `Grid` (cells 0–80 row by row), `Puzzle(givens, solution, cages)` (validated in `init`), `Board(values, notes)` (0 = empty), `GameState(puzzle, board, mode, score, mistakes, hintsUsed, moves, elapsedSeconds)`, `GameMode(Variant, Difficulty)`. All `@Serializable`; `isWon` = board equals the solution.
- **Constraints:** a variant is a list of `Constraint`s (`Row`, `Column`, `Box`, `Diagonal` for X, `Cage` for Killer) from `constraintsFor(variant)`; each yields groups of cells whose digits never repeat. `units(puzzle).peersOf(cell)` gives the cells a digit sees. A new variant adds constraints, not rules.
- **Rules:** `SudokuRules` (a `RuleSet`) dispatches to one `MoveRule` per `Move` (`Place`, `Erase`, `ToggleNote`). Givens are never editable; a won game accepts nothing. Placing clears the cell's notes and that digit from every peer's notes; a digit ≠ solution adds a mistake (`ScoreEvent.Mistake`).
- **Engine:** `GameEngine.newGame/apply/tick/legalMoves/isLegal`; `apply` returns `MoveOutcome.Applied(state, events)` or `Rejected`, counts the move and scores the events. `tick` charges each new second until the game is won.
- **Scoring:** `ScoringStrategy` (`startingScore`, `pointsFor`, `bounded`, `rankingOrder`) picked by `scoringFor(mode)`; only `PointsScoring` so far.
- **Conflicts:** `ConflictFinder` returns cells with a repeated digit in any group, plus whole cages over their sum (or full with a different sum).
- **Solver (`solver`):** `Candidates` (immutable 9-bit masks) + `SolverContext` (units, 9-cell houses, rows/columns, cages, Killer innies for the rule of 45, peers). One `Technique` per deduction (`NakedSingle`, `HiddenSingle`, `CageCombination`, `RuleOf45`, `LockedCandidates`, `NakedSubset`, `HiddenSubset`, `Fish` for X-wing/swordfish, `XyWing`); each returns a `Deduction` (placement or eliminations) or null. `TechniqueKind` holds each technique's minimum `Difficulty` and score `weight`. No technique assumes uniqueness, so a full logical solve proves the puzzle has one solution. Hidden singles/subsets, locked-candidate sources and the rule of 45 use only 9-cell houses (a cage needn't hold every digit); naked subsets work in any group.
- **`LogicalSolver`:** restarts from the cheapest technique after each step; `solve(puzzle, variant, maxDifficulty)` returns values, solved and score; `nextPlacement` drives hints.
- **Generator (`generator`):** `FullGridBuilder` (seeded backtracking per variant), `CagePartitioner` (Killer cages of 2–4 side-by-side cells, distinct digits), `DifficultyBands` (score bands per variant, tuned with `benchmarkGenerator`), `SeededPuzzleGenerator`: digs clues (symmetric pairs for Classic/X) while the solver, capped at the target level, still solves within the band's ceiling; up to 20 attempts, counted not timed, so a seed gives the same puzzle on every device; cancellable. `SolutionCounter` (test sources) independently checks uniqueness.
- **Hints:** `HintEngine` → `Hint.WrongDigit` first, else `Hint.Placement` from the board's digits (never the player's notes), else a revealed cell (`technique = null`). When eliminations must come first, `nextPlacement` credits the placement to the hardest of them (by weight) and highlights its cells plus the target, so the hint names the step the player actually needs. `Move.RevealHint` sets `GameState.pendingHint`, counts the hint (−200) and is neither a move nor undoable; `Move.ApplyHint` writes the digit (or erases the wrong one). Any other move or undo clears the pending hint. `GameSession.play` only pushes history when the board changed.
- **Codec:** `GameCodec` gzips + Base64s a `GameState` (892 chars for a half-played Killer game with notes) for bug reports; `decodeSession` reads a report's JSON.
- **Repositories / use cases:** `SavedGameRepository`, `ScoreRepository`, `StatsRepository` (stats per mode), `SettingsRepository`, `SeedSource`, `Clock`; fakes in `testFixtures`. `StartNewGame` (uses a matching `PreparedPuzzle` or generates on an injected dispatcher, then counts an unfinished game as a loss), `PreparePuzzle`, `RestartGame` (reuses `state.puzzle`, also a loss if unfinished), `ResumeGame`, `SaveGame`, `FinishGame` (score + win + clear save), `ObserveTopScores`, `ObserveStats`, `ObservePlayedModes` (Scores tabs = modes played at least once, user's choice).
- **History:** `UndoHistory` stacks `Board` snapshots only: undo/redo never touch score, mistakes, hints or clock; redo counts a move. `GameSession(seed, state, history)` plays, undoes, redoes and ticks; a won session can't undo.

### `:data` (package `io.github.vinaooo.sudokutrio.data`)

- **Room** (`SudokuTrioDatabase`, `sudokutrio.db`, version 1, schema exported to `data/schemas`): `scores` (one row per win, mode as `variant` + `difficulty` enum names) and `stats` (one row per mode, composite key). `ScoreDao.observeTop` sorts like `ScoreRecord.rankingFor` (points desc, time asc, earliest first); `StatsDao.observePlayedModes` lists modes with `played > 0`. `RoomStatsRepository.update` reads and writes in `withTransaction`. Rows of a mode this version doesn't know are skipped, not crashed on. Mappers live in `Mappers.kt`, apart from the entities.
- **Migrations:** every schema change is an `AutoMigration` (or manual) plus a case in `SudokuTrioDatabaseMigrationTest`, which builds the old database from its exported schema JSON and opens it with the current app. Never `fallbackToDestructiveMigration`.
- **Saved game:** `FileSavedGameRepository` writes `GameSession` JSON in a `{version, session}` envelope to `files/saved_game.json`, through a temp file + rename. A corrupt, rule-breaking or unknown-version file is discarded.
- **Settings:** `DataStoreSettingsRepository` (Preferences DataStore `settings`); unknown enum values read as defaults.
- **System:** `RandomSeedSource`, `SystemClock`. `di/DataModule.kt` provides the database, DAOs, DataStore and the saved-game file, and binds the repositories.

### Core modules

- **`:core:designsystem`:** `SudokuTrioTheme` (`MaterialExpressiveTheme`, `MotionScheme.expressive()`), dynamic color on Android 12+ or one of eight committed `ThemeColor` palettes (`PaletteColors.kt`, material-color-utilities TonalSpot; blue is the brand, green is Solo's palette). `BoardColors` maps board roles to scheme roles (cells, highlights, given/entered/conflict/note inks, lines, cages, `selectionBorder`), read via `SudokuTrioThemeExtras.boardColors`; `BoardColorsTest` checks text contrast ≥ 4.5:1 and lines ≥ 3:1 in every palette, light and dark. In light schemes the selected and same-digit fills are close, so the board outlines the selected cell in `selectionBorder`.
- **`:core:ads`:** `AdBannerProvider` → `AdMobBanner` (inline adaptive banner in the room `BannerSlot` gives it: full width × 60dp on phones in portrait; ≤ 320dp wide, 50dp tall in landscape; tablets capped at 320dp; the slot keeps its height with or without an ad; a new `AdView` per size; lifecycle pause/resume/destroy). Consent first (`DefaultAdConsent`, Google's order): update the UMP consent status on every launch, show the form if required, and only then start the Mobile Ads SDK (off the main thread, once); consent from an earlier session starts ads at once. UMP and Mobile Ads sit behind `ConsentClient` / `AdsSdk` (`UmpConsentClient`, `MobileAdsSdk`) so the order is unit-tested with fakes. `AdsConfig` comes from `:app`'s `AdsConfigModule` (BuildConfig IDs; debug simulates the EEA, which only works on test devices). `PlaceholderAdBanner` stays for app tests (`FakeAdsModule` also fakes `AdConsent`).
- **The user's phone (Moto `nio_retcn`) has no Google Play services:** UMP fails with "Error making request" and ads never load there. Test ads and the consent form on the `Pixel_9a_Android_16` emulator (Play Store image; start it with `-no-window -skin 1080x2424`).

### `:feature:game`

- **`GameViewModel`** (one `StateFlow<GameUiState>`, sealed `GameIntent`): selection, notes mode, digits/erase as `Move`s, undo/redo, the two-tap hint (`RevealHint` runs on the injected `@SearchDispatcher`; moves, undo, redo and new games cancel it, and a clock tick meanwhile doesn't drop it: the hint goes on the latest session, charged for the seconds that passed), new game/restart as one cancellable generation job (`loading` disables the pad and toolbar and stops the clock), and a Settings mode change starting a new game in the mode the collector received (never re-read from state another collector writes). Saves after every move, undo, redo, revealed hint and pause; never a won game. `FinishGame` runs once on the win.
- **Clock:** `Ticker` started/stopped only by `LifecycleResumeEffect` (never by a win: the tick itself skips won games, and stopping it left the next game's clock frozen). It charges a second whenever a board is shown and not won or loading, from the first frame (user's choice: no free study time). Each tick reads the latest state.
- **Silent mistakes:** a wrong digit gets the same sound, haptic and TalkBack announcement as a right one; `FeedbackEvent.REJECTED` is only for moves the rules refuse. The top bar shows only the mode and the clock (user's choice); score, mistakes and hints appear in the win dialog.
- **Board:** `BoardGeometry` (pure: largest square, top-aligned, `cellAt` hit-testing), `CellHighlighter` (conflict > selected > hint > same digit > peer; peers per variant, computed once per puzzle), `completedDigits` (number pad dims a digit at 9). `SudokuBoard` draws everything on one `Canvas` and takes `board`, `puzzle` and highlights, never `GameState`, so clock ticks don't redraw it. Killer cages are dashed just inside their cells with the sum in the cage's first cell; notes in that cell shrink below the sum. Sudoku X draws faint diagonals.
- **Screen (`GameScreen` + `GameLayouts`):**
  - **Portrait:** a fixed 88dp top region holding the mode and clock (`ModeAndTime`) or, while a hint shows, the `HintCard` in their place, with `NavigationButtons` always beside them (hiding Scores/Settings during a hint was a bug) (user-approved: the board never moves and is never covered, even at 360×640dp); the board as the largest square at the top or bottom of its room (Settings → board position); the number pad (`1–5` / `6–9 Erase` / Notes); the `GameToolbar`.
  - **Landscape:** `CenteredRow` (from Solo): mode, clock, hint and the Scores/Settings buttons on one side, the board centered at full height, and the 3-column pad (`1 2 3` / `4 5 6` / `7 8 9` / Notes + Erase) with a vertical toolbar on the preferred hand's side (user's choice).
  - **Left hand:** mirrors the pad (Erase on the thumb's side) and the toolbar, rendered right to left so its order, the `NewGameMenu` pills (they grow toward the middle) and the bug-report corner all mirror; icons are drawn left to right (`LtrIcon`) so undo and redo keep their arrows. The board itself never mirrors.
  - **Phone view** (tablets, smallest width ≥ 600dp): the board capped at 412dp on the chosen side.
  - `GameToolbar(ToolbarState, vertical, mirrored)`: undo, redo, hint → apply, new-game menu.
- **Bug reports** go to `vrpedrinho+trio@gmail.com` or a prefilled issue on `vinaooo/sudoku-trio`, with the `GameCodec` "State:" block.
- **Prepared puzzles** (user's choice, after Killer expert took up to 3.3 s): once a board shows, `PreparePuzzle` makes the next puzzle of the Settings mode in the background (`Deferred`, own seed); New game uses it (`StartNewGame(mode, prepared)`), waiting for it if still running. A mode change discards it; restart doesn't need one.
- **Generation timing** (release build, `compile -m speed-profile`, the user's Moto): Classic easy ~50–80 ms, Classic/X expert ~250–420 ms, Killer hard ~0.3–0.65 s, Killer expert 0.65–3.3 s; the loading indicator covers it.
- `:app` wires `DomainRulesModule` (engine, conflict finder, generator) and `UseCaseModule` (generation on `Dispatchers.Default`). Routes: `GameRoute` (banner below the game per the standard), `ScoresRoute`, `SettingsRoute`; `AdBannerGameScreenOnlyTest` checks the banner is on the game screen only. `FakeAdsModule` (in `src/sharedTest`) replaces `AdsModule` in app tests.

### `:feature:scores` and `:feature:settings`

- **Scores:** `ScoresViewModel` combines `ObservePlayedModes` (tabs, sorted by variant then difficulty), the chosen tab, `ObserveTopScores` and `ObserveStats` for it. A mode played but never won shows its stats and "Win a game to see your scores here." Tabs use `modeName`; one mode shows a title instead of tabs.
- **Settings:** `SettingsChange` (one subtype per row) applied by `SettingsViewModel`; a change of variant or difficulty while `ResumeGame` returns a game in progress waits in `pendingChange` for the "This will start a new game…" dialog. Game card: variant (segmented row), difficulty (connected icon `ToggleButton` group — grinning face, smiling face, neutral face, brain — with the chosen level's name and description bouncing in, `DifficultyChoice`), phone view and its side on tablets only. Appearance (theme, dynamic color, color row revealed when dynamic color is off, hand, board position), Feedback, Privacy (policy link, `privacy_policy_url` with `#en`/`#pt-br`; "Privacy options" shown only when UMP says it's required, reopening the consent form). Two columns from 600dp, at most 1040dp wide.

- **TalkBack:** `CellNodes` (`BoardAccessibility.kt`) lays 81 invisible nodes over the canvas, row by row: "row 3, column 5, 7, given", "row 1, column 1, empty, notes 1 7, cage of 15", "…, repeated" for a conflict; selected state and a click action that selects the cell. The announcer stays a 1dp live region (`GameAccessibilityTest`).
- A game only looked at (clock running, no move) isn't counted as a loss when abandoned (kept; the user didn't ask to change it).
- **Zero counts** in Scores use their own strings ("No mistakes" / "Nenhum erro"): Portuguese plural rules would print "0 erro".

### Test pitfalls

- A second `StandardTestDispatcher()` made inside a test with `Dispatchers.setMain` shares the main scheduler; `advanceUntilIdle()` on it then runs the clock loop forever and the test hangs. Give it its own `TestCoroutineScheduler()`.
- Local lint can crash with an LLFir exception after a branch switch: `rm -rf app/build/intermediates/*lint*` (and the module's), run `:app:lintDebug` alone, then the gate. Stale `core/ads/build` caused the same.

### Launcher icon

- Adaptive icon (`mipmap-anydpi/ic_launcher.xml`): brand-blue background (`#39608F`, `BlueColors.light.primary`), a foreground 3×3 grid of light cells with seven givens (1 · 9 / · 6 2 / 3 0 4, the user's digits, 0 included on purpose), and a separate monochrome layer: the cells with the digits cut out (`fillType="evenOdd"`). Digits are Roboto Bold glyph outlines (Apache 2.0) turned into paths with fontTools; the grid sits inside the 66dp safe circle. No wallpaper-colored icon (ask the user first). `LauncherIconScreenshotTest` keeps a golden of both layers.

## Device testing

- Never flip the phone's system settings (dark mode via `cmd uimode`, auto-rotate, font scale, …) to test: use the app's own settings, Roborazzi or an emulator, or ask the user to do it.
- Don't tap "Email" in the bug report on the user's phone: backing out of Gmail's compose saves a draft in their account.
- Release timing: `assembleRelease`, `zipalign -p 4` + `apksigner` with `~/.android/debug.keystore`, `adb install -r` over the debug build (data kept), `cmd package compile -m speed-profile -f io.github.vinaooo.SudokuTrio`, check `animator_duration_scale`; time with a temporary log line that is never committed.

## Store kit

- Made with an offline `Pixel_9a_Android_16` emulator (`cmd connectivity airplane-mode enable`: no ad, no consent form), SystemUI demo mode, the app's own locale (`cmd locale set-app-locales io.github.vinaooo.SudokuTrio --locales pt-BR`), dynamic color off, debug presets for the positions; each capture cropped above the ad slot and padded to 2:1 (1090×2180). The Play icon and the text-free feature graphic come from the launcher icon's glyph paths. The kit (listing text, graphics, screenshots, `CHECKLIST.md` with data safety, content rating, audience and the closed-test release steps) lives outside the repo.

## Git

Only `master` is long-lived: branch from it and open PRs against it; the user merges with merge commits. One PR per milestone; wait for the merge before stacking the next one. Commit, push and open PRs only when the user asks. CI runs on every PR and on pushes to `master`; Pitest runs nightly.

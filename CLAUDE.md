# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Sudoku Trio is an open-source (MIT) Sudoku for Android, to be published on Google Play: Classic, Sudoku X (diagonals) and Killer Sudoku, each with easy / medium / hard / expert. Kotlin, Jetpack Compose, Material 3 Expressive with dynamic color, Hilt, multi-module Clean Architecture. The UI is in pt-BR and English: every user-facing string lives in `values/strings.xml` and `values-pt-rBR/strings.xml` of its module, never hard-coded. The build, quality gates and the pieces every game shares (theme, ads, settings, scores, bug report, game frame) come from vinkit (`../vinkit`, github.com/vinaooo/vinkit), shared with Solo (`../paciencia`). The user makes the product and design decisions, so ask before choosing one.

- applicationId `io.github.vinaooo.SudokuTrio` (as given); Kotlin packages and namespaces are lowercase `io.github.vinaooo.sudokutrio`, because ktlint rejects uppercase package names.

## Game decisions (agreed with the user)

- **Mistake** = a digit that differs from the unique solution. No limit, no loss. Counted silently for the score, never shown red; only visible conflicts (duplicates in a row, column, box, diagonal in X, cage in Killer) are highlighted.
- **Score:** base 1000 / 2000 / 4000 / 8000 (easy → expert) × variant factor (Classic 1.0, X 1.2, Killer 1.5), −1 per second, −100 per mistake, −200 per hint, clamped at 0. Ranking: highest score, per variant + difficulty.
- **Number pad:** `1 2 3 4 5` / `6 7 8 9 Erase`; Notes mode is a pencil toggle in the toolbar (user's choice, replacing the full-width Notes key).
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
- **Release signing:** vinkit's `vinkit.android.application` reads `vinkit.signing.{storeFile,storePassword,keyAlias,keyPassword}` from `local.properties` or the `VINKIT_SIGNING_*` environment variables (environment wins). Nothing set → unsigned; a partial setup fails and names what's missing. The repository's GitHub secrets need the `VINKIT_*` names too.
- **Ads IDs:** vinkit's `AdIds` reads `vinkit.ads.appId` / `vinkit.ads.bannerId` (or `VINKIT_ADS_*`) for release builds; debug builds always use Google's test IDs.
- **Versions** come from git (vinkit's `AppVersion`): `versionCode` = `git rev-list --count HEAD`, `versionName` = latest `vX.Y.Z` tag (`0.0.0-g<hash>` before the first). A shallow clone fails the build, so workflows check out with `fetch-depth: 0`.
- **Play upload:** `.github/workflows/release.yml`, off until the repository variable `PLAY_UPLOAD_ENABLED` is `true`. The Play developer account is newer than 13 Nov 2023: a closed test (12+ testers, 14 days) is needed before production.

## Build setup

- **vinkit** (`github.com/vinaooo/vinkit`, from JitPack) brings the build and the shared pieces: `vinkit.tag` in `gradle.properties` picks the release, always the newest, never a local copy. Modules apply its convention plugins (`vinkit.android.application`, `.android.library`, `.android.compose`, `.android.feature`, `.hilt`, `.jvm.library`, `.quality`, `.root.coverage`; detekt rules ship with them). `vinkit.android.feature` adds `:domain` and its test fixtures, not `:core:*` or vinkit modules: each module lists those (`com.github.vinaooo.vinkit:<module>:<tag>`). A gap in vinkit is fixed there first and released as a tag (with a `CHANGELOG.md` entry), then the tag is bumped here.
- AGP 9 has Kotlin built in (no `kotlin-android` plugin). Versions are pinned in vinkit's catalog (`com.github.vinaooo.vinkit:catalog`), the `libs` catalog here. `material3` is pinned to a 1.5.0 alpha above the Compose BOM, because that's where the Expressive APIs are public.
- **Coverage:** the root project applies `vinkit.root.coverage`, which merges every module's Kover data with the modules' filters; the root floor is 70% of lines, `:domain` has its own 90% rule.
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
- **Scoring:** `ScoringStrategy` (`startingScore`, `pointsFor`, `bounded`, `rankingOrder`) picked by `scoringFor(mode)`; only `PointsScoring` so far. Scores are vinkit's `ScoreRecord`: `GameMode.key` ("KILLER_HARD", `gameModeOf` reads it back), mistakes and hints in its extras (`ScoreRecord.mistakes`/`hintsUsed`), `GameMode.ranking()` → `Ranking.HIGHEST_POINTS`; `GameState.toRecord`.
- **Conflicts:** `ConflictFinder` returns cells with a repeated digit in any group, plus whole cages over their sum (or full with a different sum).
- **Solver (`solver`):** `Candidates` (immutable 9-bit masks) + `SolverContext` (units, 9-cell houses, rows/columns, cages, Killer innies for the rule of 45, peers). One `Technique` per deduction (`NakedSingle`, `HiddenSingle`, `CageCombination`, `RuleOf45`, `LockedCandidates`, `NakedSubset`, `HiddenSubset`, `Fish` for X-wing/swordfish, `XyWing`); each returns a `Deduction` (placement or eliminations) or null. `TechniqueKind` holds each technique's minimum `Difficulty` and score `weight`. No technique assumes uniqueness, so a full logical solve proves the puzzle has one solution. Hidden singles/subsets, locked-candidate sources and the rule of 45 use only 9-cell houses (a cage needn't hold every digit); naked subsets work in any group.
- **`LogicalSolver`:** restarts from the cheapest technique after each step; `solve(puzzle, variant, maxDifficulty)` returns values, solved and score; `nextPlacement` drives hints.
- **Generator (`generator`):** `FullGridBuilder` (seeded backtracking per variant), `CagePartitioner` (Killer cages of 2–4 side-by-side cells, distinct digits), `DifficultyBands` (score bands per variant, tuned with `benchmarkGenerator`), `SeededPuzzleGenerator`: digs clues (symmetric pairs for Classic/X) while the solver, capped at the target level, still solves within the band's ceiling; up to 20 attempts, counted not timed, so a seed gives the same puzzle on every device; cancellable. `SolutionCounter` (test sources) independently checks uniqueness.
- **Hints:** `HintEngine` → `Hint.WrongDigit` first, else `Hint.Placement` from the board's digits (never the player's notes), else a revealed cell (`technique = null`). When eliminations must come first, `nextPlacement` credits the placement to the hardest of them (by weight) and highlights its cells plus the target, so the hint names the step the player actually needs. `Move.RevealHint` sets `GameState.pendingHint`, counts the hint (−200) and is neither a move nor undoable; `Move.ApplyHint` writes the digit (or erases the wrong one). Any other move or undo clears the pending hint. `GameSession.play` only pushes history when the board changed.
- **Codec:** `BoardCodec` (vinkit's `GameCodec(GameState.serializer())`) gzips + Base64s a `GameState` (892 chars for a half-played Killer game with notes) for bug reports; `decodeSession` reads a report's JSON.
- **Repositories / use cases:** `SavedGameRepository`, `SettingsRepository` (only the next game's `mode`), `SeedSource`, `Clock`, plus vinkit's `ScoreRepository`, `StatsRepository` (stats per mode key) and `AppSettingsRepository`; fakes in `testFixtures`. `StartNewGame` (uses a matching `PreparedPuzzle` or generates on an injected dispatcher, then counts an unfinished game as a loss), `PreparePuzzle`, `RestartGame` (reuses `state.puzzle`, also a loss if unfinished), `ResumeGame`, `SaveGame`, `FinishGame` (score + win + clear save).
- **History:** `UndoHistory` stacks `Board` snapshots only: undo/redo never touch score, mistakes, hints or clock; redo counts a move. `GameSession(seed, state, history)` plays, undoes, redoes and ticks; a won session can't undo.

### `:data` (package `io.github.vinaooo.sudokutrio.data`)

- **Scores:** vinkit's `ScoresDatabase` (`vinkit_scores.db`) with `RoomScoreRepository` / `RoomStatsRepository`; schema and migrations are vinkit's. The old `sudokutrio.db` is deleted when it opens (nothing was published).
- **Saved game:** `FileSavedGameRepository` writes `GameSession` JSON in a `{version, session}` envelope to `files/saved_game.json`, through a temp file + rename. A corrupt, rule-breaking or unknown-version file is discarded.
- **Settings:** one Preferences DataStore `settings`: `DataStoreSettingsRepository` keeps the mode (`variant`, `difficulty`), vinkit's `DataStoreAppSettingsRepository` the common settings under the same keys the game used; each writes only its own keys. Unknown enum values read as defaults.
- **System:** `RandomSeedSource`, `SystemClock`. `di/DataModule.kt` provides the scores database and repositories, DataStore, `AppSettingsRepository` (blue default) and the saved-game file, and binds the rest.

### Core modules

- **`:core:designsystem`:** `SudokuTrioTheme` wraps vinkit's `VinkitTheme` (Material 3 Expressive, dynamic color on Android 12+ or one of vinkit's eight palettes; blue is the brand) and provides the board's colors. `BoardColors` maps board roles to scheme roles (cells, highlights, given/entered/conflict/note inks, lines, cages, `selectionBorder`), read via `SudokuTrioThemeExtras.boardColors`; `BoardColorsTest` checks text contrast ≥ 4.5:1 and lines ≥ 3:1 in every palette, light and dark. In light schemes the selected and same-digit fills are close, so the board outlines the selected cell in `selectionBorder`.
- **Ads:** vinkit's `ads` module (`AdMobBanner` in its `BannerSlot`, UMP consent before the Mobile Ads SDK starts, `DefaultAdConsent`), built in `:app`'s `di/AdsModule` (BuildConfig IDs; debug simulates the EEA, which only works on test devices). `FakeAdsModule` (in `src/sharedTest`) replaces it in app tests with `PlaceholderAdBanner` and a fake `AdConsent`.
- **The user's phone (Moto `nio_retcn`) has no Google Play services:** UMP fails with "Error making request" and ads never load there. Test ads and the consent form on the `Pixel_9a_Android_16` emulator (Play Store image; start it with `-no-window -skin 1080x2424`).

### `:feature:game`

- **`GameViewModel`** (one `StateFlow<GameUiState>`, sealed `GameIntent`): selection, notes mode, digits/erase as `Move`s, undo/redo, the two-tap hint (`RevealHint` runs on the injected `@SearchDispatcher`; moves, undo, redo and new games cancel it, and a clock tick meanwhile doesn't drop it: the hint goes on the latest session, charged for the seconds that passed), new game/restart as one cancellable generation job (`loading` disables the pad and toolbar and stops the clock), and a Settings mode change starting a new game in the mode the collector received (never re-read from state another collector writes). Saves after every move, undo, redo, revealed hint and pause; never a won game. `FinishGame` runs once on the win.
- **Clock:** vinkit's `Ticker` started/stopped only by `LifecycleResumeEffect` (never by a win: the tick itself skips won games, and stopping it left the next game's clock frozen). It charges a second whenever a board is shown and not won or loading, from the first frame (user's choice: no free study time). Each tick reads the latest state.
- **Silent mistakes:** a wrong digit gets the same sound, haptic and TalkBack announcement as a right one; vinkit's `AndroidGameFeedback` (one `@Singleton` from `:app`'s `FeedbackModule`) plays them; `FeedbackEvent.REJECTED` is only for moves the rules refuse. The top bar shows only the mode and the clock (user's choice); score, mistakes and hints appear in the win dialog.
- **Board:** `BoardGeometry` (pure: largest square, top-aligned, `cellAt` hit-testing), `CellHighlighter` (conflict > selected > hint > same digit > peer; peers per variant, computed once per puzzle), `completedDigits` (number pad dims a digit at 9). `SudokuBoard` draws everything on one `Canvas` and takes `board`, `puzzle` and highlights, never `GameState`, so clock ticks don't redraw it. Killer cages are dashed just inside their cells with the sum in the cage's first cell; notes in that cell shrink below the sum. Sudoku X draws faint diagonals.
- **Screen (`GameScreen` on vinkit's `shell`):** `GameSurface` (screenshot for bug reports, TalkBack announcer) around `GameFrame(info, board, toolbar, controls)`; `GameToolbar` takes Sudoku's `toolbarActions` and `menuOptions`; `WinDialog(winLines)`.
  - **Portrait:** the frame's fixed 88dp top region holds the mode and clock (vinkit's `ModeAndTime`) or, while a hint shows, the `HintCard` in their place, with the Scores/Settings buttons always beside them (user-approved: the board never moves and is never covered, even at 360×640dp); the board as the largest square at the top or bottom of its room (Settings → board position); the number pad (`1–5` / `6–9 Erase`); the toolbar.
  - **Landscape:** mode, clock, hint and the Scores/Settings buttons on one side, the board centered at full height, and the 3-column pad (`1 2 3` / `4 5 6` / `7 8 9` / Erase) with a vertical toolbar on the preferred hand's side (user's choice).
  - **Left hand:** mirrors the pad (Erase on the thumb's side) and the toolbar (vinkit lays it right to left; undo and redo are `keepDirection` so they keep their arrows). The board itself never mirrors.
  - **Phone view** (tablets, smallest width ≥ 600dp): board, pad and toolbar as one 412dp column on the chosen side, the pad right under the board, at the top or bottom per the board position, in portrait and landscape (user's choice; the board shrinks to fit a short screen).
  - **Tablets** (user's choice after seeing them on a `Pixel_Tablet_Android_16` emulator): in portrait without phone view the pad and toolbar stay at most 480dp wide, centered, so keys don't stretch; in landscape from 1000dp wide (`FrameInfo.large`) the pad grows to 280dp with 72dp keys.
  - Toolbar actions: undo, redo, notes (`ToolbarAction.Toggle`, filled pencil while on), hint → apply; menu: new game, restart this board, report a bug.
- **Bug reports** (vinkit's `BugReportDialog`) go to `vrpedrinho+trio@gmail.com` or a prefilled issue on `vinaooo/sudoku-trio` (`SudokuTrioReports`), with `gameReport`'s settings and game lines, the `BoardCodec` "State:" block and `game.json`.
- **Badges** (vinkit's `achievements`, user's picks): `Achievement` (`:domain`, the enum name is the storage key, never rename) with ladders (games played, days in a row, games won, wins in a row), variant/expert wins, Trio, every mode, Killer master (10 Killer expert), Flawless, On your own, Perfect, speed per difficulty (4/8/15/25 min), Night owl (00:00–04:59) and Early bird (05:00–06:59) on the phone's local time (`Clock.now()`), Marathon (5 wins a day) and Weekend (a Saturday's win, then the Sunday's). `RecordAchievements` runs from `FinishGame` and from `AbandonGame`'s counted loss (which resets `Settings.winStreak`), and marks the day on each day's first move; its collected sets (`days_played`, `wins_today`, `saturdays_won`) are pruned on every write, because vinkit's DataStore repository never removes a key. A snackbar names new badges once no dialog or loading covers the game; the Badges screen is a third top button, before Scores and Settings (user's choice, even though it clips the hint card's last line at 360dp and in pt-BR).
- **Prepared puzzles** (user's choice, after Killer expert took up to 3.3 s): once a board shows, `PreparePuzzle` makes the next puzzle of the Settings mode in the background (`Deferred`, own seed); New game uses it (`StartNewGame(mode, prepared)`), waiting for it if still running. A mode change discards it; restart doesn't need one.
- **Generation timing** (release build, `compile -m speed-profile`, the user's Moto): Classic easy ~50–80 ms, Classic/X expert ~250–420 ms, Killer hard ~0.3–0.65 s, Killer expert 0.65–3.3 s; the loading indicator covers it.
- `:app` wires `DomainRulesModule` (engine, conflict finder, generator) and `UseCaseModule` (generation on `Dispatchers.Default`). Routes: `GameRoute` (banner below the game per the standard), `ScoresRoute`, `SettingsRoute`; `AdBannerGameScreenOnlyTest` checks the banner is on the game screen only. 

### `:feature:scores` and `:feature:settings`

- **Scores:** vinkit's Scores screen. `SudokuScoresViewModel` (a vinkit `ScoresViewModel`): a tab per variant played at least once (Classic, X, Killer), a section per difficulty in it (one not played yet shows zeros), each ranked by `GameMode.ranking()`. A mode played but never won shows its stats and "Win a game to see your scores here." Each score's line: mistakes · hints · date.
- **Settings:** vinkit's `SettingsScreen` with Sudoku's `GameSection` as its game section: variant (vinkit `Choice`) and difficulty (vinkit `IconChoice`: grinning face, smiling face, neutral face, brain, with the chosen level's name and description). `SettingsChange` (variant, difficulty) goes through `SettingsViewModel`; a mode change while `ResumeGame` returns a game in progress waits in `pendingChange` for vinkit's `NewGameConfirmDialog`. The common rows (theme, dynamic color, color, hand, board position, phone view on tablets, sound, vibration, privacy) are vinkit's and change `AppSettings` through `onAppChange`. pt-BR names the feedback section "Sons e vibração" (vinkit's).

- **TalkBack:** `CellNodes` (`BoardAccessibility.kt`) lays 81 invisible nodes over the canvas, row by row: "row 3, column 5, 7, given", "row 1, column 1, empty, notes 1 7, cage of 15", "…, repeated" for a conflict; selected state and a click action that selects the cell. The announcer stays a 1dp live region (`GameAccessibilityTest`).
- A game only looked at (clock running, no move) isn't counted as a loss when abandoned (kept; the user didn't ask to change it).
- **Zero counts** in Scores use their own strings ("No mistakes" / "Nenhum erro"): Portuguese plural rules would print "0 erro".

### Test pitfalls

- A second `StandardTestDispatcher()` made inside a test with `Dispatchers.setMain` shares the main scheduler; `advanceUntilIdle()` on it then runs the clock loop forever and the test hangs. Give it its own `TestCoroutineScheduler()`.
- Local lint can crash with an LLFir exception after a branch switch: `rm -rf app/build/intermediates/*lint*` (and the module's), run `:app:lintDebug` alone, then the gate.

### Launcher icon

- Adaptive icon (`mipmap-anydpi/ic_launcher.xml`): brand-blue background (`#39608F`, vinkit's blue `primary`), a foreground 3×3 grid of light cells with seven givens (1 · 9 / · 6 2 / 3 0 4, the user's digits, 0 included on purpose), and a separate monochrome layer: the cells with the digits cut out (`fillType="evenOdd"`). Digits are Roboto Bold glyph outlines (Apache 2.0) turned into paths with fontTools; the grid sits inside the 66dp safe circle. No wallpaper-colored icon (ask the user first). `LauncherIconScreenshotTest` keeps a golden of both layers.

## Device testing

- Never flip the phone's system settings (dark mode via `cmd uimode`, auto-rotate, font scale, …) to test: use the app's own settings, Roborazzi or an emulator, or ask the user to do it.
- Don't tap "Email" in the bug report on the user's phone: backing out of Gmail's compose saves a draft in their account.
- Release timing: `assembleRelease`, `zipalign -p 4` + `apksigner` with `~/.android/debug.keystore`, `adb install -r` over the debug build (data kept), `cmd package compile -m speed-profile -f io.github.vinaooo.SudokuTrio`, check `animator_duration_scale`; time with a temporary log line that is never committed.

## Store kit

- Made with an offline `Pixel_9a_Android_16` emulator (`cmd connectivity airplane-mode enable`: no ad, no consent form), SystemUI demo mode, the app's own locale (`cmd locale set-app-locales io.github.vinaooo.SudokuTrio --locales pt-BR`), dynamic color off, debug presets for the positions; each capture cropped above the ad slot and padded to 2:1 (1090×2180). The Play icon and the text-free feature graphic come from the launcher icon's glyph paths. The kit (listing text, graphics, screenshots, `CHECKLIST.md` with data safety, content rating, audience and the closed-test release steps) lives outside the repo.

## Git

Only `master` is long-lived: branch from it and open PRs against it; the user merges with merge commits. One PR per milestone; wait for the merge before stacking the next one. Commit, push and open PRs only when the user asks. CI runs on every PR and on pushes to `master`; Pitest runs nightly.

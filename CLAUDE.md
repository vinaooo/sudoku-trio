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
- **Repositories / use cases:** `SavedGameRepository`, `ScoreRepository`, `StatsRepository` (stats per mode), `SettingsRepository`, `SeedSource`, `Clock`; fakes in `testFixtures`. `StartNewGame` (generates on an injected dispatcher, then counts an unfinished game as a loss), `RestartGame` (reuses `state.puzzle`, also a loss if unfinished), `ResumeGame`, `SaveGame`, `FinishGame` (score + win + clear save), `ObserveTopScores`, `ObserveStats`, `ObservePlayedModes` (Scores tabs = modes played at least once, user's choice).
- **History:** `UndoHistory` stacks `Board` snapshots only: undo/redo never touch score, mistakes, hints or clock; redo counts a move. `GameSession(seed, state, history)` plays, undoes, redoes and ticks; a won session can't undo.

### `:data` (package `io.github.vinaooo.sudokutrio.data`)

- **Room** (`SudokuTrioDatabase`, `sudokutrio.db`, version 1, schema exported to `data/schemas`): `scores` (one row per win, mode as `variant` + `difficulty` enum names) and `stats` (one row per mode, composite key). `ScoreDao.observeTop` sorts like `ScoreRecord.rankingFor` (points desc, time asc, earliest first); `StatsDao.observePlayedModes` lists modes with `played > 0`. `RoomStatsRepository.update` reads and writes in `withTransaction`. Rows of a mode this version doesn't know are skipped, not crashed on. Mappers live in `Mappers.kt`, apart from the entities.
- **Migrations:** every schema change is an `AutoMigration` (or manual) plus a case in `SudokuTrioDatabaseMigrationTest`, which builds the old database from its exported schema JSON and opens it with the current app. Never `fallbackToDestructiveMigration`.
- **Saved game:** `FileSavedGameRepository` writes `GameSession` JSON in a `{version, session}` envelope to `files/saved_game.json`, through a temp file + rename. A corrupt, rule-breaking or unknown-version file is discarded.
- **Settings:** `DataStoreSettingsRepository` (Preferences DataStore `settings`); unknown enum values read as defaults.
- **System:** `RandomSeedSource`, `SystemClock`. `di/DataModule.kt` provides the database, DAOs, DataStore and the saved-game file, and binds the repositories.

### Core modules

- **`:core:designsystem`:** `SudokuTrioTheme` (`MaterialExpressiveTheme`, `MotionScheme.expressive()`), dynamic color on Android 12+ or one of eight committed `ThemeColor` palettes (`PaletteColors.kt`, material-color-utilities TonalSpot; blue is the brand, green is Solo's palette). `BoardColors` maps board roles to scheme roles (cells, highlights, given/entered/conflict/note inks, lines, cages, `selectionBorder`), read via `SudokuTrioThemeExtras.boardColors`; `BoardColorsTest` checks text contrast ≥ 4.5:1 and lines ≥ 3:1 in every palette, light and dark. In light schemes the selected and same-digit fills are close, so the board outlines the selected cell in `selectionBorder`.
- **`:core:ads`:** `AdBannerProvider` with `PlaceholderAdBanner` (bound in `AdsModule`), sized by `BannerSlot` (full width × 60dp on phones in portrait; ≤ 320dp wide, 50dp tall in landscape; tablets capped at 320dp). AdMob, UMP consent and their interfaces arrive at release prep.

## Device testing

- Never flip the phone's system settings (dark mode via `cmd uimode`, font scale, …) to test: use the app's own settings, Roborazzi or an emulator.

## Git

Only `master` is long-lived: branch from it and open PRs against it; the user merges with merge commits. One PR per milestone; wait for the merge before stacking the next one. Commit, push and open PRs only when the user asks. CI runs on every PR and on pushes to `master`; Pitest runs nightly.

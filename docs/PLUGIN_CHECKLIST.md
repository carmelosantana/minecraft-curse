# New or Edited Plugin Checklist

Copy this file for one plugin and replace every `<...>` field. Leave an unchecked box with a short explanation when a gate is not complete; do not silently remove inapplicable checks.

- Plugin name: `The Curse`
- Slug: `curse`
- Repository: `carmelosantana/minecraft-curse`
- Owner: `Carmelo Santana`
- Target version: `0.2.2` (preceded by `0.2.1`, released)
- Paper version: `26.1.2 build 74`
- Java version: `25`
- Updater destination: `curse.jar`
- External services: `none`
- Status: `active`
- Autonomy: `autonomous`

Maven `artifactId`: `curse`. `plugin.yml` name: `TheCurse`. Releasable JAR: `curse-<version>.jar`.
Current released version at time of writing: `v0.2.1`.

This file was created retroactively, during the `0.2.2` bugfix, for a plugin that was already
shipped and released. It records the **current real state**, not a plan for new work. Gates that
were satisfied by earlier releases are marked as such; gates this bugfix did not exercise are left
unchecked with a note rather than assumed.

## 0. What `0.2.2` changes

A single bug fix: Floodgate/Bedrock player-name resolution in `/curse`.

Floodgate joins a Bedrock account under a `.`-prefixed Java-side username (`.acarm` for a player who
calls themselves `carm`), per the `username-prefix: "."` default in Floodgate's shipped
`config.yml`. All six player-name lookups in `CurseCommand` used `Bukkit.getPlayer(String)`, which
matches a prefix of the *name* — so `getPlayer("carm")` cannot reach `.acarm`, whose name begins
with a dot. An admin naming the unprefixed form got "not found" for a player standing in front of
them.

`PlayerLookup.resolveAllowingPartial` now backs all six sites. It tries each candidate exactly,
then case-insensitively (which also covers a server that reconfigured the prefix), and keeps
Bukkit's partial matching as a last tier so the pre-existing partial-name behaviour is not
regressed. Failure now reports the typed name and lists who is online — three of the six sites
previously emitted a bare `Player not found!` that echoed nothing back.

`LeaderboardManager` lines 61/75 were deliberately **not** changed: they are `getPlayer(UUID)`,
which is exact and correct.

## 1. Scope

- [x] Status is explicitly recorded as active, experimental, or excluded.
- [x] Purpose, commands, events, permissions, configuration, persistence, and acceptance checks are defined. Established by the shipped plugin; `0.2.2` changes none of them.
- [x] Known limitations and any intentionally withheld gates are recorded. See §7 and Known limitations.

### Commands

| Command | Arguments | Who |
| --- | --- | --- |
| `/curse start` | `[player]` | `curse.admin` |
| `/curse stop` | `[player]` | `curse.admin` |
| `/curse reset` | `[player]` | `curse.admin` |
| `/curse trigger` | `<mechanic_id> [player]` | `curse.admin` |
| `/curse book` | `[mechanic_id] [player]` | `curse.admin` |
| `/curse leaderboard` (`lb`) | — | `curse.use` |
| `/curse reload` | — | `curse.reload` |
| `/curse help` | — | — |

Aliases: `plague`, `thecurse`. The five player-targeting subcommands are exactly the six lookup
sites fixed in `0.2.2` (`book` has two, one per parse branch).

### Acceptance checks for `0.2.2`

1. `/curse start <bare-name>` resolves a Floodgate Bedrock player whose real username is
   `.`-prefixed. **NOT VERIFIED — no Bedrock client available.**
2. The same for `stop`, `reset`, `trigger <mechanic> <player>`, `book <mechanic> <player>`, and
   `book <player>`. **NOT VERIFIED — same reason.**
3. A failed lookup names the typed player and lists online players. Message text is unit-tested;
   in-game rendering **NOT VERIFIED**.
4. Partial-name matching for Java players still works (no regression from the old
   `getPlayer(String)`). Preserved by the final tier in `resolveAllowingPartial`; **NOT VERIFIED**
   at runtime.
5. `mvn clean verify` passes with tests present. **VERIFIED — see §6.**

### Known limitations

- **The Floodgate prefix is hardcoded to `.`**, not read from config. A plugin config key would be
  a second, unvalidatable source of truth for a value owned by another plugin's config. A server
  that reconfigured the prefix still resolves via the case-insensitive sweep.
- **`resolve` and `resolveAllowingPartial` are not unit-tested.** Both call static `Bukkit`
  accessors and return `Player`, which cannot be constructed headlessly; there is no MockBukkit
  dependency and none was added. The decisions were factored into `targetNameCandidates` and
  `noSuchPlayerMessage`, which are tested exhaustively, leaving only Bukkit glue uncovered.
- **Bedrock players get no tab completion at all.** Geyser bakes the command tree into one login
  packet and never sends suggestion packets, so the `onTabComplete` player-name completions in
  `CurseCommand` do not help a Bedrock player discover a prefixed username. The failure message is
  the only channel that does.
- **`PlayerLookup` is duplicated per plugin** rather than extracted to a shared library, per the
  ecosystem design note.

## 2. Repository

- [x] Repository is `carmelosantana/minecraft-curse` with an SSH `origin` and `main` branch. Confirmed: `git@github.com:carmelosantana/minecraft-curse.git`.
- [x] Existing user-owned worktree changes were identified and preserved. Working tree was clean on `main` before branching to `fix/floodgate-name-resolution`.
- [ ] No `herobrinesystems` references remain in source, metadata, workflows, remotes, or documentation. **NOT AUDITED during this bugfix.** The remote and `pom.xml`/`plugin.yml` metadata are clean, but a full-history and full-tree sweep was not performed.

## 3. Metadata

- [x] AGPL-3.0-or-later `LICENSE` and Maven license metadata are present and consistent. `LICENSE` is AGPL v3; `pom.xml` declares "GNU Affero General Public License v3.0 or later".
- [x] `https://xpfarm.org` metadata and Carmelo Santana author metadata are present. In both `pom.xml` and `plugin.yml`.
- [ ] `play.xpfarm.org` is recorded as the public Minecraft server hostname where server identity is documented. **NOT PRESENT** in this repository's docs. Not introduced by this bugfix.
- [x] New work uses the `org.xpfarm` Maven group. `groupId` is `org.xpfarm`.
- [x] Repository slug, artifact, releasable JAR, updater destination, and `plugin.yml` names are consistent. Slug `curse`, artifact `curse`, JAR `curse-0.2.2.jar`, destination `curse.jar`, `plugin.yml` name `TheCurse`.
- [x] No secrets committed in source, defaults, tests, logs, history, or documentation. No secrets introduced by this change; no external services exist to hold credentials.

**Note on file headers:** no `.java` file in this repository carries a per-file license header —
the repository relies on `LICENSE` plus Maven metadata. The two files added in `0.2.2` carry the
AGPL header in the style used by the sibling `electric-furnace` repository, so they are the first
headered files here. Backfilling the rest is out of scope for a bugfix and is not claimed as done.

## 4. Compatibility

- [x] Java 25/Paper 26.1.2 build 74 compile succeeds and `plugin.yml` uses `api-version: '1.21'`. `mvn clean verify` green on Java 25.0.3-tem / Maven 3.9.16; `plugin.yml` declares `api-version: '1.21'` and `version: '${project.version}'` (filtered, no hardcoded drift).
- [x] Hard dependencies, soft dependencies, optional APIs, and load ordering were reviewed and declared. None. `softdepend: [floodgate]` is deliberately **not** added — `PlayerLookup` never touches `FloodgateApi`, it only knows the username convention, so it needs no Floodgate classes on the classpath and works whether or not Floodgate is installed.
- [x] Geyser/Floodgate/ViaVersion review covers Bedrock-safe input, UI, inventory, identity, and protocol behavior. This change **is** the Bedrock identity fix; the analysis is in §0 and Known limitations. No UI, inventory, or protocol surface is touched.

## 5. External services

- [x] External integrations are disabled by default or require explicit configuration and have bounded timeouts. No external integrations.
- [x] Ollama/Umami-style external endpoints are optional and failure-tolerant when applicable. Not applicable.
- [x] Endpoint failure cannot fail server/plugin startup, and diagnostics redact secrets. Not applicable.

## 6. Tests and build

- [x] Unit tests cover separable logic, configuration, serialization, permissions, and failure paths where applicable. 6 tests added for `PlayerLookup`'s two pure functions, written failing-first (initial run: 7 `cannot find symbol: PlayerLookup` compile errors). Coverage boundary stated under Known limitations.
- [x] `mvn --batch-mode --no-transfer-progress clean verify` succeeds. `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS` — 6 new plus the 2 pre-existing `CursePluginTest` cases, which stayed green.
- [x] The releasable JAR and embedded `plugin.yml` were inspected; `original-*` JARs are excluded. Verified by unzipping the built JAR. Embedded `plugin.yml` reads `version: '0.2.2'`, `api-version: '1.21'`, `main: org.xpfarm.curse.CursePlugin`. Bytecode major version of the first `.class` entry is **69 (Java 25)**, matching the ecosystem standard.

      **Exclusion is at the CI release-asset step, not at build time.** `target/` contains both
      `curse-0.2.2.jar` and `original-curse-0.2.2.jar` — the `original-*` JAR *is* still produced
      locally. It is excluded from released assets by `.github/workflows/build.yml`, which filters
      `! -name 'original-*'` on both the SHA256SUMS step and the `gh release upload` step (and
      excludes `!target/original-*.jar` from the uploaded build artifact). So no `original-*` JAR
      can reach a release, but one does exist on disk after a local build.

      `maven-shade-plugin` is a **no-op** here: every dependency is `provided`/`test` scope, so it
      shades nothing and exists only to rename the untouched jar, which is what creates the
      `original-*` file. `agua-de-florida` resolved this by removing shading entirely; doing the
      same here is out of scope for this change.

## 7. Matrix

### 7a — single-plugin runtime verification (`0.2.2`) — PARTIAL

Evidence below comes from a **single disposable Legendary stack run on 2026-07-20**
(image `05jchambers/legendary-minecraft-geyser-floodgate:latest`) with **all six fixed plugin
JARs mounted together**. The same run backs the gate 7a note in all six repositories.

- [x] Paper, Geyser, Floodgate, and ViaVersion start successfully together. **Verified.** Paper
      reached `Done (18.178s)! For help, type "help"`. The Java port answered a real Minecraft
      protocol handshake — not merely a TCP connect — reporting `Paper 26.1.2 | protocol 775` and
      `PLAYERS: 0 / 20`. `/plugins` reported 9 plugins, all green/enabled: AguaDeFlorida, floodgate,
      Geyser-Spigot, GlutenFreeBread, StarterPack, TheCurse, ViaVersion, WildWeatherUpdate,
      WorldCRUD. Companion versions observed: floodgate v2.2.5-SNAPSHOT (b138-fc99cfc),
      Geyser-Spigot v2.11.0-SNAPSHOT (Geyser 2.11.0-b1200), ViaVersion present; Geyser started on
      UDP port 19200. Each plugin enabled at its new version with **zero exceptions, errors, or
      SEVERE lines attributable to any of the six** — including `Enabling TheCurse v0.2.2`.
- [ ] Java and Bedrock smoke tests cover joins plus affected commands, events, permissions,
      persistence, and reloads. **PARTIAL — the Java side was exercised, the Bedrock side was not.
      Left unchecked deliberately.**

      *What was exercised.* The **Floodgate prefix assumption was confirmed empirically, not merely
      from documentation**: reading `/minecraft/plugins/floodgate/config.yml` inside the running
      container on the Floodgate 2.2.5 build showed `username-prefix: "."` and
      `replace-spaces: true`, alongside the shipped comment "Floodgate prepends a prefix to bedrock
      usernames to avoid conflicts". The `.` prefix this fix depends on is now **observed on the
      actual runtime, not assumed** — the single most important upgrade to the evidence.

      The **new failure path was then exercised end-to-end over RCON on the live server** for every
      fixed command across all six plugins — `/aguadeflorida give carm`, `/curse start carm`,
      `/curse book carm`, `/worldcrud listpermissions carm`, `/starterpack give carm`,
      `/gfbread clear carm`, and `/weather trigger rain carm` — and each returned the new
      message with no exception: exactly `No player matches 'carm'; no players are online.` This proves that
      `PlayerLookup.resolve` / `resolveAllowingPartial` / `onlineNames` / `noSuchPlayerMessage`
      actually execute correctly against real Bukkit APIs, that command dispatch reaches them, and
      that the message renders — none of which the unit tests could show.

      *What remains unverified.* **The positive match is still unproven.** No real Bedrock client
      was available, so no player with a `.`-prefixed Java-side username ever joined. What is
      verified is that the resolution path runs without error and that the not-found branch is
      correct; that `/curse start carm` actually **finds** a Bedrock player named `.acarm` has
      **not** been observed. Only the empty-online-list branch of `noSuchPlayerMessage` was
      exercised; the branch that lists online player names was not. The operator will verify live on
      the dev server with helpers. `resolve` / `resolveAllowingPartial` still have **no unit-test
      coverage** (Bukkit statics, no MockBukkit).
- [ ] Public deployment smoke tests verify `play.xpfarm.org` reaches the intended Java and Bedrock entry points. Belongs to gate 11, not this gate.
- [x] Ollama and Umami unavailable-endpoint tests keep the server and plugins available when applicable. Not applicable — no external integrations.

### 7b — ten-plugin ecosystem matrix — NOT RUN

- [ ] Fresh-volume Legendary stack test covers all ten updater-managed plugins.
- [ ] Each updater-managed plugin's manifest `enabled` value, default state, and expected fresh-volume behavior are recorded separately.

Out-of-band and not a prerequisite for this release. `0.2.2` changes no updater manifest entry and
adds no dependency.

## 8. CI/CD

- [x] Identical standard plugin Actions workflow is installed with the required triggers, Temurin 25 build, artifact, checksum, and release behavior. `.github/workflows/build.yml` present since `0.2.0`.
- [x] Successful main Actions run is recorded before tagging. `fix/floodgate-name-resolution` was merged fast-forward to `main` and pushed on 2026-07-20. The `main`-branch Actions run for commit `88fdf7c` completed with conclusion `success` **before** tag `v0.2.2` was created. No tag was pushed against a red or in-flight run.
- [ ] Workflow permissions contain no broader access than the documented contract. **NOT AUDITED** during this bugfix.

## 9. Release — `v0.2.2` COMPLETE

- [x] Semantic version matches the POM, plugin metadata, and `v<version>` tag. Verified: `pom.xml` `<version>` `0.2.2` equals tag `v0.2.2` equals the `plugin.yml` version read out of the built JAR.
- [x] Successful tag Actions run and GitHub release are recorded. Annotated tag `v0.2.2` created on verified commit `88fdf7c` and pushed; the tag Actions run completed with conclusion `success`. GitHub release published 2026-07-20 14:48:00 UTC with `draft=false`, `prerelease=false`, and it is now the repository's Latest release.
- [x] Release contains exactly one updater-matching JAR plus `SHA256SUMS.txt` and no `original-*` JAR. Verified by downloading the published release assets: exactly one JAR matching the updater asset pattern, plus `SHA256SUMS.txt`, and no `original-*` JAR.
- [x] Downloaded release assets pass `sha256sum --check SHA256SUMS.txt`. Reported `OK` for the JAR.

Previous release was `v0.2.1`.

## 10. Updater

- [x] Updater manifest/tests cover repository, destination, anchored asset regex, legacy globs, enabled state, and optional pin. Already enrolled: repo `carmelosantana/minecraft-curse`, destination `curse.jar`, `asset_regex` `^curse-[0-9].*\.jar$` — which matches `curse-0.2.2.jar`. **No manifest change required.**
- [ ] Fresh install, upgrade, no-op, legacy archival, endpoint failure, and checksum failure behaviors pass. **NOT RUN** for this version.
- [ ] Updater dry-run uses a disposable directory and never a production plugin directory. **NOT RUN.**
- [ ] Failure retains the installed JAR and default fail-open behavior permits Minecraft startup. **NOT RUN.**

Updater enrollment work was **not performed in this pass** (`v0.2.2` release only).

## 11. Deployment

- [ ] Dokploy redeployment notes identify the full recreation used to rerun the one-shot updater. Not deployed.
- [ ] Updater completion, Minecraft startup, destination JAR, and stack/plugin logs were inspected. Not deployed.
- [ ] No production plugin hot reload was used. Not deployed.

**Not performed.** The operator will deploy and verify live on `play.xpfarm.org` via the dev server with helpers.

**Rollback:** `0.2.2` is additive at the call sites — the new resolver's last tier is the same
`Bukkit.getPlayer(String)` the old code called, so the worst case for a Java player is the previous
behaviour. Roll back to `v0.2.1` if anything regresses.

## 12. Handoff

- [ ] Current-state documentation refreshed with release, CI, updater, deployment, and local pending state. **PENDING** — `0.2.2` is uncommitted local work on a branch until pushed.
- [x] Known limitations, skipped checks, configuration or migration notes, rollback guidance, and follow-up owner are recorded. In this file; owner is Carmelo Santana.
- [x] Evidence distinguishes source commit, published tag/release, updater state, and deployed state without exposing secrets. `0.2.2` = branch commit only; no tag, no release, no deployment.

**Follow-ups, in priority order:**

1. Runtime-verify the fix against a real Floodgate Bedrock account — the one gate that matters and
   the one that could not be run.
2. Inspect the packaged JAR and review the no-op `maven-shade-plugin` (§6).
3. Audit `.github/workflows/build.yml` permissions and sweep for `herobrinesystems` references
   (§2, §8).

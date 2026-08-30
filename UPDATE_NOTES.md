# MCOptimizer → Paper 26.2 update

This is a decompiled-and-patched rebuild, not a rewrite from your original
source (which was lost). Read this before you assume it's done.

## How to build

You need JDK 25 (the wrapper will try to auto-download one via Foojay if
you don't have it) and a normal internet connection (this project needs
Maven Central + repo.papermc.io, both blocked in the sandbox this was built
in, so this has never actually been compiled - see "What I could not
verify" below).

```
./gradlew build
```

Output jar lands in `build/libs/`. If it fails, paste me the error, first
compile against a real API surface is exactly where decompile artifacts
tend to surface.

## What was actually changed

1. **`plugin.yml` / `paper-plugin.yml`**: `api-version` bumped from `'1.21'`
   to `'26.2'`.

2. **`utils/ServerUtils.java`**: rewritten. It previously used reflection
   into internal NMS methods (`getRecentTps`, `getTickTime`, `getServer`,
   deprecated `getSystemCpuLoad`) to work around there being no public API
   for TPS at the time it was written. Paper has had `Bukkit.getTPS()` /
   `Bukkit.getAverageTickTime()` as real, non-deprecated public API for a
   long time, confirmed still present and undeprecated in 26.2. Switched to
   that. Also switched CPU load reading from reflection to
   `com.sun.management.OperatingSystemMXBean#getCpuLoad()` directly
   (non-deprecated, doesn't need reflection at all). This removes the only
   code in the plugin that depended on Minecraft-internal method names,
   which were the actual risk given 26.1's move to a fully unobfuscated
   server jar.

3. **`config/analyzer/PaperYmlOptimizer.java`**: disabled (no longer
   registered in `ConfigurationAnalyzer`), not fixed. Read the comment at
   the top of that file. Short version: it reads/writes settings using the
   key schema of the old single-file `paper.yml`, which stopped being
   Paper's actual config file back in Minecraft 1.19 - this was already
   silently broken before this update, unrelated to the 26.2 jump. Paper
   now splits that config into `config/paper-global.yml` and
   `config/paper-world-defaults.yml` with a different, non-overlapping
   structure. Faking a working analysis here would tell you your Paper
   config is optimized when nothing was actually checked, which is worse
   than doing nothing. A real fix means re-mapping every setting by hand
   against the current schema and testing against a live server, which
   I couldn't do in this sandbox. Everything else in the plugin (entity
   optimization, TNT, redstone, chunk management, memory leak detection,
   lag prediction, stress testing, etc.) doesn't touch this file and is
   unaffected.

4. **`config/analyzer/SpigotYmlOptimizer.java`**: one cross-check that
   read `paper.yml` for Velocity support detection was neutralized to
   always return `false` (the same safe fallback it already had), since
   the file and schema it depended on are gone. Everything else in this
   file is untouched.

## What I did NOT change

- `AsyncPlayerChatEvent` / `ChatColor` usage: both still exist and work in
  26.2 (still deprecated, not removed). Left as-is. Worth migrating to
  `AsyncChatEvent` / Adventure `Component` eventually, but that's a
  modernization, not a compatibility fix.
- The reflection in `integration/CrossPlatformManager.java`: already
  wrapped in try/catch throughout, degrades safely. The one genuinely
  fragile part (reaching into the NMS player handle for a `networkManager`
  field) already does nothing with the result even when it works - it was
  dead code before this update too. Not worth the risk of touching.
- The `this.config.getClass().getMethod("isXEnabled")` reflection pattern
  in `TNTOptimizer`, `TaskOptimizer`, `PerformanceReportManager`,
  `NetworkOptimizer`, `StressTestManager`: the methods it's reflecting for
  all exist directly on `ConfigManager`, so this reflection is pointless
  but not broken. Style issue, not a compatibility issue, left alone.

## What I could not verify

I could not compile this anywhere. This sandbox has no access to Maven
Central or Paper's Maven repo, so there has been no compiler pass over
this code at all since the decompile - what you have is source-level
patching based on documentation research, not a tested build. Decompiled
code from CFR is generally accurate but occasionally mangles edge cases
(complex generics, some lambda forms, switch expressions). If `./gradlew
build` throws errors, that's expected as a possibility, send them back and
I'll fix the actual lines.

## Original plugin info

- Name: MCOptimizer, version 1.2.2
- Original author handle: juicyyfruittsnackss (your old "fruitsnacks dev"
  brand)
- Previously targeted api-version 1.21

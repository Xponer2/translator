# Translator: the ladder of versions (new design, prototype)

The new design of the Translator: mods made for a newer Minecraft version run on an older one by stepping down
between **neighbouring versions**, and mods of different versions share one game. This folder is the working prototype
of its first steps, **NeoForge 1.21.1 → 1.21 → 1.20.6 → 1.20.4**. It is separate from the frozen 1.21.1 → 1.12.2
build in the rest of the repository. See the [main README](../README.md) for the goal and the roadmap.

## How a step works

1. **Generated renames.** For each pair of neighbouring versions, Mojang's official mappings of both are joined through
   Fabric's intermediary names (the same in every version). A class or member whose Mojang name changed is found
   that way; the rest keep their names.
2. **Carried code.** A class, method or static constant that is new in the upper version is copied down as it is when
   its own code links on the lower version (needing only other carried code, nothing missing, no registration).
3. **Hand-written code only for the roots:** an existing class that gained new state (an `ItemStack`'s data
   components), a registry the lower version doesn't have, the loader's API.
4. **One pass.** The renames of all steps compose, so a mod is converted once, whatever its version.

**Shared objects.** Mods of different versions must see the same stack, block or entity. Five rules make that so:
one target class per source class for every mod; shared types are mapped to the game's own classes, never copied;
`extends`, super calls and `new` go to a facade, every other use (descriptors, `instanceof`, casts, arrays) to the
game's class; one copy of carried code per target; a member the game's class lacks is called through a dispatcher.
`tools/identity-test` checks them on the JVM (12 cases; without the dispatcher the first new method fails).

**Strict.** A mod counts as converted only when nothing was stubbed, skipped or left unresolved, mixin selectors
included. Formats the converter can't check yet (a mod's own recipe types) are listed apart.

## Where it is

Over the 300 most popular NeoForge 1.21.1 mods on Modrinth:

| Step | Resolved by generated renames | Of the rest, carried down as real code | Places that need hand-written code |
|---|---|---|---|
| 1.21.1 → 1.21 | 99.9% | 75% | 8 |
| 1.21 → 1.20.6 | 97.7% | 66% | 198 |
| 1.20.6 → 1.20.4 (item data components) | 93.2% | 39% | 409 |

20 of the 300 convert to 1.20.4 with nothing missing. **Alternate Current** (a redstone mod for 1.21.1) runs on the
real NeoForge 1.20.4: its mixins are applied to 1.20.4's redstone code, and a behaviour test on a 1.20.4 server builds
the same redstone line with the mod on and off: the powers are vanilla's (15 down to 0) both times, and the mod's own
update path runs (1 + 1 neighbour notifications against vanilla's 106 + 449).

1.20.5 has no step of its own: NeoForge only had betas for it, and 1.20.6 is its hotfix, so the step 1.20.6 → 1.20.4
holds its changes.

## Layout

| Path | What |
|---|---|
| `v1_21_1`, `v1_21`, `v1_20_6`, `v1_20_4` | One Gradle project per version (ModDevGradle): the Mojang-named game + NeoForge jar of that version |
| `v1_20_4/src` | `LadderProbe`: behaviour tests of converted mods on a real 1.20.4 server |
| `tools/stage0/Stage0.java` | Walks every reference of a mod corpus down the steps; writes per step the generated rows, what stops, what can be carried and the roots left for hand-written code |
| `tools/convert/Convert.java` | Converts a mod (or a folder of mods) down to 1.20.4 in one pass: classes, mixin selectors, embedded jars, metadata, data folders, recipe results |
| `tools/identity-test` | The shared-object rules as a bytecode rewrite and a checker |
| `tools/fetch_mappings.py` | Downloads Mojang's mappings and the intermediary names to `mappings/` (stays on your machine) |

## Running it

Needs a JDK 21, Python 3, Git Bash or another POSIX shell, and an internet connection for the first build (ModDevGradle
downloads Minecraft and NeoForge for each version). Nothing of Minecraft is in this repository.

```bash
cd next
./gradlew createMinecraftArtifacts printLoaderJars
python tools/fetch_mappings.py
tools/ladder.sh stage0 <folder of NeoForge 1.21.1 mod jars>
tools/ladder.sh convert <mod.jar> <converted.jar> <report.md>
tools/identity-test/run.sh
```

To run a converted mod: put it in `v1_20_4/run-server/mods/` (or `run-client/mods/`) and start
`./gradlew :v1_20_4:runServer` (or `runClient`). The server writes `v1_20_4/run-server/probe.json` with the behaviour
tests' results and stops.

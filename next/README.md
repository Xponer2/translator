# Translator: the ladder of versions (new design, prototype)

The new design of the Translator: mods made for a newer Minecraft version run on an older one by stepping down
between **neighbouring versions**, and mods of different versions share one game. This folder is the working prototype
of its first steps, **NeoForge 1.21.1 → 1.21 → 1.20.6 → 1.20.4**. It is separate from the frozen 1.21.1 → 1.12.2
build in the rest of the repository. See the [main README](../README.md) for the goal and the roadmap.

## How a step works

1. **Generated renames.** For each pair of neighbouring versions, Mojang's official mappings of both are joined through
   Fabric's intermediary names (the same in every version). A class or member whose Mojang name changed is found
   that way; the rest keep their names.
2. **Carried code.** A class or static method that is new in the upper version is copied down as it is when its own
   code works on the lower version: it needs only other carried code, reaches only public members below (a private
   static helper of its own class comes along), registers nothing, is no event (the lower game never posts it) and
   keeps the lower version's interface contracts. All carried code goes into one library per target
   (`ladder-carried-1.20.4.jar`, a game library in its own package tree), shared by every converted mod.
3. **Generated bridges.** A member whose types changed between the versions, every change a known conversion (now:
   `Holder<X>` above, `X` below: mob effects, attributes, game events, sounds, potions), gets a generated bridge that
   converts the values and calls the lower member: field reads, method calls and constructors (`new X(...)` becomes the
   bridge's factory). The conversions themselves are written once.
4. **Hand-written adapters only for the roots** (`adapters_1_20_4`, a mod for the real 1.20.4): where an existing class
   gained new state (an `ItemStack`'s data components), a method was split (1.20.5's `useItemOn` / `useWithoutItem`),
   a registry or a loader API changed. The converter applies them from a rules file (`ladder-rules.tsv`): provided
   classes, overrides of newer methods (the mod class gets the adapters' interface, 1.20.4's call sites call it through
   mixins), call redirects. Each rule's target is checked against the built adapters.
5. **One pass.** The renames of all steps compose, so a mod is converted once, whatever its version.

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

**Converted and checked in the real 1.20.4** (dedicated server and client, with the carried library and the adapters):

- **Macaw's Bridges, Fences and Windows** (635 blocks, 641 items, 793 recipes): their blocks, items and recipes are
  recorded on 1.20.4 by the same probes as on the real 1.21.1 and compared: **7948 of 7948 facts as in 1.21.1**
  (hardness, blast resistance, light, collision, drops, block entities, stack sizes, recipe results). A right click
  with an empty hand opens and closes a shutter (its 1.20.5 `useItemOn`, called from 1.20.4 through the adapters); with
  its own item in hand nothing happens, as in 1.21.1.
- **Alternate Current** (redstone): the same redstone line with the mod on and off: the powers are vanilla's both times,
  and the mod's own update path runs (1 + 1 neighbour notifications against vanilla's 106 + 449).
- **Cherished Worlds, Searchables** (client mods on carried code): the 1.20.4 client reaches its title screen with no
  error.
- **Test mod A** (`testmods/ladder_test_a`, made for NeoForge 1.21.1: mob effects and attributes through holders, a
  carried factory): the same 10 results natively on 1.21.1 and converted on 1.20.4.

Over the whole corpus, 36 of 300 mods convert with nothing left (strict). What holds the others back most, in order:
item data components, the 1.20.5 network payloads, the 1.21 render buffers, recipes on `RecipeInput`, FML's extension
points.

1.20.5 has no step of its own: NeoForge only had betas for it, and 1.20.6 is its hotfix, so the step 1.20.6 → 1.20.4
holds its changes.

## Layout

| Path | What |
|---|---|
| `v1_21_1`, `v1_21`, `v1_20_6`, `v1_20_4` | One Gradle project per version (ModDevGradle): the Mojang-named game + NeoForge jar of that version |
| `v1_20_4/src` | `LadderProbe`, `LadderProbeClient`, `LadderFacts`: behaviour tests of converted mods on a real 1.20.4 server and client, and the facts compared with the 1.21.1 reference |
| `adapters_1_20_4` | The hand-written adapters of the step down to 1.20.4 and their rules (`ladder-rules.tsv`) |
| `testmods/ladder_test_a` | A test mod for NeoForge 1.21.1: run on its own version and converted, the results compared |
| `tools/stage0/Stage0.java` | Walks every reference of a mod corpus down the steps; writes per step the generated rows, what stops, what can be carried and the roots left for hand-written code |
| `tools/convert/Convert.java` | Converts a mod (or a folder of mods) down to 1.20.4 in one pass: classes, carried code, bridges, the adapters' rules, mixin selectors, embedded jars, metadata, data (folders, recipe results, loot predicates); strict |
| `tools/identity-test` | The shared-object rules as a bytecode rewrite and a checker |
| `tools/run-probe.sh` | Runs the 1.20.4 server or client with the probes and a watchdog |
| `tools/fetch_mappings.py` | Downloads Mojang's mappings and the intermediary names to `mappings/` (stays on your machine) |

## Running it

Needs a JDK 21, Python 3, Git Bash or another POSIX shell, and an internet connection for the first build (ModDevGradle
downloads Minecraft and NeoForge for each version). Nothing of Minecraft is in this repository.

```bash
cd next
./gradlew createMinecraftArtifacts printLoaderJars
python tools/fetch_mappings.py
tools/ladder.sh convert <folder of NeoForge 1.21.1 mod jars> out/converted out/report.tsv
./gradlew :adapters_1_20_4:jar
tools/ladder.sh convert <folder of NeoForge 1.21.1 mod jars> out/converted out/report.tsv
tools/identity-test/run.sh
```

The first conversion makes the carried library the adapters are compiled against; the second applies the adapters'
rules. To run converted mods: put them with `out/converted/ladder-carried-1.20.4.jar` and
`adapters_1_20_4/build/libs/ladder-adapters-1.20.4-*.jar` in `v1_20_4/run-server/mods/` (or `run-client/mods/`) and run
`tools/run-probe.sh server` (or `client`); the results go to `v1_20_4/run-server/probe.json` and `facts.json`.

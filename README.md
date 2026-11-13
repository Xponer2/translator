# Translator

[![License: PolyForm Noncommercial](https://img.shields.io/badge/license-PolyForm%20Noncommercial%201.0.0-blue.svg?style=flat-square)](LICENSE)
[![Minecraft 1.12.2](https://img.shields.io/badge/Minecraft-1.12.2-green.svg?style=flat-square)](#)
![Made with AI (Claude)](https://img.shields.io/badge/made%20with-AI%20(Claude)-8A2BE2?style=flat-square)

The **Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2** (Forge). It runs **original NeoForge 1.21.1 mod jars**
on 1.12.2: put them in `mods/1.21.1/` (or `mods/`) next to the Translator and start the game.

**Made with AI:** the Translator is made entirely with AI (Claude), from the code to the in-game testing.

**Experimental.** Many blocks, items, menus and recipes of the tested mods work. Rendering, world generation and
some screens are still incomplete. A mod can load and still miss parts of its behaviour.

**Development environment only, for now.** The converter maps the mods to Minecraft's development names (MCP).
The normal game uses obfuscated (SRG) names, so running original 1.21.1 jars there doesn't work yet. Ports built on
the Translator work in both.

## Goal and roadmap

**The goal:** run mods of **any version on any lower version** of Minecraft, and **mix versions** in one game: a
target version *x* with any number of mods made for versions above it, e.g. 1.12.2 with mods for 1.13, 1.16, 1.21
and later, all together. 1.12.2 is the first target, not the last: lower targets come later too.

**The route:** a ladder of small steps down between neighbouring versions (… → 1.21 → 1.20 → … → *x*). Each mod
enters the ladder at its own version and steps down to the target; the target is where the ladder stops. A new
Minecraft version adds one step at the top, an older target one step at the bottom. Each step is the API difference
of two neighbouring versions, taken from the official mappings and the loaders' porting notes.

| Stage | What | State |
|---|---|---|
| 1 | The tools every step uses: the jar converter, the 1.21.1 classes carried from the player's own game, the bridges, a fast in-game self-test, an offline ranking of the missing API over hundreds of mods | ✅ done, improved as we go |
| 2 | **First (long) step: NeoForge 1.21.1 → 1.12.2**, closed by the missing API ranked over the 300 most popular 1.21.1 mods | 🔨 **now** (see the numbers below) |
| 3 | Mods for 1.13–1.20 lifted up to 1.21.1 (mostly renames), so they run on 1.12.2 through stage 2 | ⬜ next |
| 4 | Small steps down from the top (newer versions → 1.21 → 1.20 → …): every step opens one more target version | ⬜ planned |
| 5 | Fabric mods, targets below 1.12.2 | ⬜ planned |

**Where stage 2 is:** 99.4% of the API references of the tested mods are bridged (the missing ones are listed under
[What is left to bridge](#what-is-left-to-bridge)); the table below shows each mod's state in the self-test. Twilight
Forest passes 97% of its blocks and items, Supplementaries 96%.

**Corpus** (the 300 most popular NeoForge 1.21.1 mods on Modrinth, converted outside the game with v236): all 300
convert; **74 need nothing the Translator lacks yet**, 98 miss 1–10 calls, 128 miss more (23 074 call sites in
all). The largest gaps are converter-wide (stack map frames naming missing classes: 128 mods; mixin fields 1.12.2
doesn't have: 106 mods), so they are closed first.

## Mods

<!-- mods:start -->

Tested with converter v233, 2026-11-13. 90 mods: 12 work, 65 partly work, 4 don't work, 9 not tested yet.

✅ works: every block, item and screen of the mod passed the self-test, no crash, all its mixins applied. 🟡 partly works: it loads and plays, some content fails. ❌ doesn't work: it doesn't load or crashes the game. ⬜ not tested yet.

| Mod | Version | Status | Self-test | Notes |
|---|---|---|---|---|
| Accessories | 1.1.0-beta.48 | 🟡 Partly works | 0 / 1 | library, 2 mixins not applied, inside another jar |
| Another Furniture | 4.0.3 | 🟡 Partly works | 334 / 335 |  |
| AppleSkin | 3.0.9 | ✅ Works | 1 / 1 | library |
| Applied Energistics 2 | 19.2.18 | 🟡 Partly works | 270 / 470 |  |
| Ars Nouveau | 5.13.4 | 🟡 Partly works | 316 / 393 | 3 mixins not applied |
| Artifacts | 13.2.5 | 🟡 Partly works | 38 / 42 | 1 mixin not applied |
| Biomes O' Plenty | 21.1.0.14 | 🟡 Partly works | 928 / 936 |  |
| Chat Heads | 0.15.7 | 🟡 Partly works | 1 / 1 | library, 1 mixin not applied |
| Cloth Config v15 API | 15.0.140 | ✅ Works | 1 / 1 | library |
| CodecUI | 1.4.7 | 🟡 Partly works | 0 / 1 | library, 1 mixin not applied, inside another jar |
| Comforts | 9.0.5 | 🟡 Partly works | 65 / 68 |  |
| Common Networking | 1.0.21 | 🟡 Partly works | 0 / 1 | library |
| Configured | 2.6.3 | 🟡 Partly works | 0 / 1 | library |
| Controlling | 19.0.5 | ✅ Works | 1 / 1 | library |
| CoroUtil | 1.3.9 | ⬜ Not tested yet | — | library |
| CosmeticArmorReworked | 1 | ⬜ Not tested yet | — | library |
| Create | 6.0.10 | 🟡 Partly works | 1292 / 1360 | 3 mixins not applied |
| Cristel Lib | 3.1.7 | ❌ Doesn't work | 1 / 1 | library |
| Cucumber Library | 8.0.16 | ⬜ Not tested yet | — | library |
| Cumulus | 2.0.7 | 🟡 Partly works | 0 / 1 | library, inside another jar |
| Curios API | 9.5.1 | 🟡 Partly works | 0 / 1 | library |
| ExpandAbility | — | ⬜ Not tested yet | — | library, inside another jar |
| Farmer's Delight | 1.3.4 | 🟡 Partly works | 317 / 320 | 1 mixin not applied |
| Flywheel | 1.0.6 | 🟡 Partly works | 0 / 1 | library, 2 mixins not applied, inside another jar |
| Forgified Fabric API Base | — | ⬜ Not tested yet | — | library, inside another jar |
| Framework | 0.13.11 | ⬜ Not tested yet | — | library |
| Fusion | 1.3.16 | 🟡 Partly works | 0 / 1 | library, 2 mixins not applied |
| GeckoLib 4 | 4.9.3 | 🟡 Partly works | 0 / 1 | library, 2 mixins not applied |
| GlitchCore | 2.1.0.2 | 🟡 Partly works | — | library, 2 mixins not applied |
| GuideME | 21.1.21 | 🟡 Partly works | 0 / 1 |  |
| Handcrafted | 4.0.3 | 🟡 Partly works | 430 / 555 | 1 mixin not applied |
| Infiniverse | — | ✅ Works | 1 / 1 | library, inside another jar |
| Iron Furnaces | 4.3.2 | 🟡 Partly works | 57 / 59 |  |
| Iron Jetpacks | 8.0.11 | 🟡 Partly works | 9 / 11 |  |
| Iron's Lib | 2.3.0 | 🟡 Partly works | 4 / 6 | 1 mixin not applied |
| Iron's Spells 'n Spellbooks | 3.16.3 | 🟡 Partly works | 298 / 346 | 5 mixins not applied |
| Jade | 15.10.6 | 🟡 Partly works | 0 / 1 | library, 1 mixin not applied |
| Journeymap | 6.0.9 | 🟡 Partly works | 0 / 1 | library |
| JourneyMap API | 2.0.0 | 🟡 Partly works | 0 / 1 | library, inside another jar |
| Just Enough Items | 19.64.0.455 | ✅ Works | 1 / 1 | library |
| LambDynamicLights (API) | 4.5.1 | ❌ Doesn't work | 1 / 1 | library, inside another jar |
| Macaw's Bridges | 3.1.2 | 🟡 Partly works | 289 / 293 |  |
| Macaw's Doors | 1.1.5 | 🟡 Partly works | 453 / 458 |  |
| Macaw's Fences and Walls | 1.2.1 | 🟡 Partly works | 357 / 362 |  |
| Macaw's Furniture | 3.4.1 | 🟡 Partly works | 1290 / 1310 |  |
| Macaw's Lights and Lamps | 1.1.5 | 🟡 Partly works | 278 / 282 |  |
| Macaw's Roofs | 2.3.2 | 🟡 Partly works | 1203 / 1214 |  |
| Macaw's Trapdoors | 1.1.5 | 🟡 Partly works | 368 / 372 |  |
| Macaw's Windows | 2.4.2 | 🟡 Partly works | 621 / 627 |  |
| Mekanism | 10.7.19.85 | 🟡 Partly works | 247 / 291 |  |
| MezzConfig | 0.6.6 | ✅ Works | 1 / 1 | library, inside another jar |
| Mining Gadgets | 1.18.8 | 🟡 Partly works | 29 / 34 |  |
| MixinSquared | 0.3.7 | ❌ Doesn't work | — | library, inside another jar |
| Modular Routers | 13.2.7 | 🟡 Partly works | 33 / 56 |  |
| Moonlight Lib | 3.7.2 | 🟡 Partly works | 2 / 5 | 3 mixins not applied |
| Mouse Tweaks | 2.26.1 | ✅ Works | 1 / 1 | library |
| MrCrayfish's Furniture Mod: Refurbished | 1.0.22 | 🟡 Partly works | 523 / 922 | 1 mixin not applied |
| Mystical Agriculture | 8.0.29 | 🟡 Partly works | 610 / 635 |  |
| Nature's Compass | 3.4.0 | 🟡 Partly works | 0 / 3 |  |
| Nitrogen | 1.1.25 | ✅ Works | 1 / 1 | library, inside another jar |
| Nuggets | 1.1.0.48 | 🟡 Partly works | 0 / 1 | library, inside another jar |
| oωo | 0.12.15.6-beta.1 | 🟡 Partly works | 0 / 1 | library, 5 mixins not applied |
| Patchouli | 93 | 🟡 Partly works | 1 / 1 |  |
| Pipez | 1.2.31 | 🟡 Partly works | 18 / 19 |  |
| Player Animator | 2.0.4 | 🟡 Partly works | — | library |
| Ponder | 1.0.82 | 🟡 Partly works | 0 / 1 | library, inside another jar |
| Powah | 6.2.10 | 🟡 Partly works | 74 / 257 |  |
| Rechiseled | 1.2.6 | 🟡 Partly works | 435 / 7257 |  |
| Refined Storage | 2.0.9 | 🟡 Partly works | 435 / 786 |  |
| Resourceful Lib | 3.0.12 | 🟡 Partly works | 0 / 1 | library |
| Sable Companion | 1.6.0 | ❌ Doesn't work | 1 / 1 | library, inside another jar |
| Searchables | 1.0.2 | ⬜ Not tested yet | — | library |
| Sophisticated Backpacks | 3.26.17.2232 | 🟡 Partly works | 72 / 80 |  |
| Sophisticated Core | 1.5.25.2451 | 🟡 Partly works | 3 / 4 |  |
| Sophisticated Storage | 1.6.15.2219 | 🟡 Partly works | 135 / 200 |  |
| SpectreLib | 0.17.2 | ✅ Works | 1 / 1 | library, inside another jar |
| Storage Drawers | 13.11.4 | 🟡 Partly works | 214 / 304 |  |
| SuperMartijn642's Config Library | 1.1.8 | ⬜ Not tested yet | — | library |
| SuperMartijn642's Core Lib | 1.1.26 | ✅ Works | 1 / 1 | library |
| Supplementaries | 4.0.2 | 🟡 Partly works | 464 / 496 | 5 mixins not applied |
| TerraBlender | 4.1.0.8 | ⬜ Not tested yet | — | library |
| The Aether | 1.5.10 | 🟡 Partly works | 410 / 460 | 1 mixin not applied |
| The Twilight Forest | 4.8.3345 | 🟡 Partly works | 1177 / 1281 |  |
| Tom's Simple Storage Mod | 2.4.2 | 🟡 Partly works | 29 / 39 |  |
| Torchmaster | 21.1.13 | 🟡 Partly works | 6 / 9 |  |
| Towns and Towers | 1.13.11 | ✅ Works | 1 / 1 | library |
| What Are They Up To | 1.2.7 | 🟡 Partly works | 0 / 1 | library, 1 mixin not applied |
| Xaero's Minimap | 26.6.0 | ✅ Works | 1 / 1 | library |
| Xaero's World Map | 1.47.0 | 🟡 Partly works | 0 / 1 | library |
| XaeroLib | 1.7.3 | 🟡 Partly works | 0 / 1 | library, inside another jar |

<!-- mods:end -->

## What is left to bridge

<!-- stubs:start -->

Converter v220, 2026-11-13: **2058 stubs left in 72 mods**, 1315 distinct API methods, fields and classes still missing (99.4% of the mods' API references are bridged).

An *API reference* is a place in a mod's code that names a Minecraft 1.21.1 or NeoForge class, method or field. The converter maps each one to 1.12.2 or to the Translator. When it finds nothing to map it to, it leaves a *stub*: the call does nothing, or the method throws when it runs. The mod still loads, but that part of it doesn't work. The stubs are the work that is left. The counts come from converting the mods outside the game, not from playing: a bridged call can still behave differently from 1.21.1, so the in-game state is the table above.

| Converter | Date | Stubs left | Missing API parts | Bridged |
|---|---|---|---|---|
| v197 | 2026-11-11 | 4134 | 2544 | 98.9% |
| v202 | 2026-11-11 | 3541 | 2169 | 99.0% |
| v208 | 2026-11-13 | 2697 | 1712 | 99.3% |
| v220 | 2026-11-13 | 2058 | 1315 | 99.4% |

<details><summary>Stubs by mod (most first)</summary>

| Mod | Stubs left | Missing API parts | API references | Bridged |
|---|---|---|---|---|
| Supplementaries | 228 | 182 | 27527 | 99.2% |
| The Twilight Forest | 226 | 162 | 45747 | 99.5% |
| Ars Nouveau | 216 | 161 | 30409 | 99.3% |
| Mekanism | 167 | 105 | 19406 | 99.1% |
| Create | 140 | 102 | 47810 | 99.7% |
| The Aether | 112 | 90 | 17839 | 99.4% |
| oωo | 97 | 73 | 2827 | 96.6% |
| Applied Energistics 2 | 95 | 67 | 14883 | 99.4% |
| Iron's Spells 'n Spellbooks | 86 | 75 | 24400 | 99.6% |
| Moonlight Lib | 69 | 57 | 8300 | 99.2% |
| GuideME | 62 | 49 | 2835 | 97.8% |
| Fusion | 54 | 36 | 1921 | 97.2% |
| Sophisticated Core | 34 | 17 | 6351 | 99.5% |
| Jade | 33 | 29 | 3226 | 99.0% |
| MrCrayfish's Furniture Mod: Refurbished | 28 | 22 | 7355 | 99.6% |
| Just Enough Items | 24 | 17 | 4148 | 99.4% |
| Journeymap | 23 | 18 | 7522 | 99.7% |
| Mystical Agriculture | 23 | 13 | 4644 | 99.5% |
| Refined Storage | 23 | 10 | 6088 | 99.6% |
| Sophisticated Storage | 22 | 19 | 5669 | 99.6% |
| SuperMartijn642's Core Lib | 22 | 21 | 1857 | 98.8% |
| Tom's Simple Storage Mod | 18 | 12 | 3652 | 99.5% |
| Xaero's World Map | 18 | 7 | 2290 | 99.2% |
| Farmer's Delight | 17 | 7 | 6768 | 99.7% |
| Resourceful Lib | 17 | 15 | 1093 | 98.4% |
| Xaero's Minimap | 17 | 8 | 3547 | 99.5% |
| Iron's Lib | 15 | 11 | 1858 | 99.2% |
| What Are They Up To | 14 | 12 | 793 | 98.2% |
| Artifacts | 13 | 8 | 3443 | 99.6% |
| Curios API | 12 | 10 | 1928 | 99.4% |
| Comforts | 11 | 10 | 859 | 98.7% |
| Modular Routers | 11 | 9 | 4316 | 99.7% |
| Handcrafted | 10 | 10 | 1735 | 99.4% |
| Patchouli | 9 | 7 | 2058 | 99.6% |
| Pipez | 9 | 7 | 2240 | 99.6% |
| Rechiseled | 9 | 3 | 1108 | 99.2% |
| Powah | 8 | 6 | 3446 | 99.8% |
| Framework | 7 | 5 | 1074 | 99.3% |
| Configured | 6 | 5 | 1038 | 99.4% |
| Cucumber Library | 6 | 4 | 1603 | 99.6% |
| GlitchCore | 6 | 3 | 265 | 97.7% |
| Sophisticated Backpacks | 6 | 5 | 4428 | 99.9% |
| GeckoLib 4 | 5 | 4 | 1702 | 99.7% |
| Storage Drawers | 5 | 4 | 2720 | 99.8% |
| AppleSkin | 4 | 1 | 404 | 99.0% |
| Biomes O' Plenty | 3 | 2 | 7541 | 100.0% |
| Player Animator | 3 | 1 | 184 | 98.4% |
| Another Furniture | 2 | 2 | 1835 | 99.9% |
| Chat Heads | 2 | 2 | 213 | 99.1% |
| Iron Jetpacks | 2 | 1 | 779 | 99.7% |
| Nature's Compass | 2 | 2 | 629 | 99.7% |
| Common Networking | 1 | 1 | 114 | 99.1% |
| Controlling | 1 | 1 | 300 | 99.7% |
| Macaw's Doors | 1 | 1 | 290 | 99.7% |
| Macaw's Fences and Walls | 1 | 1 | 401 | 99.8% |
| Mouse Tweaks | 1 | 0 | 123 | 99.2% |
| TerraBlender | 1 | 1 | 439 | 99.8% |
| Torchmaster | 1 | 1 | 779 | 99.9% |
| Cloth Config v15 API | 0 | 0 | 1471 | 100.0% |
| CoroUtil | 0 | 0 | 210 | 100.0% |
| CosmeticArmorReworked | 0 | 0 | 579 | 100.0% |
| Cristel Lib | 0 | 0 | 270 | 100.0% |
| Iron Furnaces | 0 | 0 | 1798 | 100.0% |
| Macaw's Bridges | 0 | 0 | 636 | 100.0% |
| Macaw's Furniture | 0 | 0 | 1081 | 100.0% |
| Macaw's Lights and Lamps | 0 | 0 | 788 | 100.0% |
| Macaw's Roofs | 0 | 0 | 797 | 100.0% |
| Macaw's Trapdoors | 0 | 0 | 98 | 100.0% |
| Macaw's Windows | 0 | 0 | 781 | 100.0% |
| Mining Gadgets | 0 | 0 | 2177 | 100.0% |
| Searchables | 0 | 0 | 45 | 100.0% |
| SuperMartijn642's Config Library | 0 | 0 | 59 | 100.0% |

</details>

<!-- stubs:end -->

## How it works

At startup each 1.21.1 jar is converted for 1.12.2, then loaded like any mod:

- **Names.** 1.21 class, method and field names are mapped to their 1.12.2 counterparts. Where 1.12.2 has nothing
  like them, they are mapped to Translator classes that give the 1.21 API on top of 1.12.2: registries, data
  components, codecs, block states and shapes, menus and screens, network payloads, capabilities, configs, rendering.
- **Bytecode.** The Java 21 bytecode is downgraded to Java 8 with
  [JvmDowngrader](https://github.com/unimined/JvmDowngrader).
- **Mixins.** The mods' mixins are converted the same way and applied (needs a Mixin loader such as MixinBooter).
- **Missing calls.** A call the Translator can't run yet is reported in the log and skipped where that is safe, so
  one missing piece doesn't crash the game.
- **Cache.** Converted jars are kept in `translator/cache/` and rebuilt when the jar or the Translator changes.
- **More than 4096 blocks.** Many 1.21.1 mods together have more blocks than Forge 1.12.2 takes. The Translator raises
  the limit to 65536: the extra id bits of a chunk are saved next to 1.12.2's own (worlds without such blocks are
  saved as before), and world generation keeps them too. No JustEnoughIDs or RoughlyEnoughIDs needed.

The Minecraft 1.21.1 and NeoForge classes without a 1.12.2 or Translator counterpart come from a 1.21.1 API jar on
your own machine: the NeoForge development cache, or `-Dtranslator.api=<jar>`. Libraries the mods use (JOML,
night-config) come from your Minecraft 1.21.1 libraries. Nothing of Minecraft 1.21.1 is shipped with the Translator.

The 1.21 vanilla ids are translated to 1.12.2 ids and metadata (`minecraft:white_wool` is `minecraft:wool` 0).
Recipe results that 1.12.2 doesn't have get something similar (`minecraft:tuff` gives andesite). Recipes that need
content 1.12.2 has nothing like are left out. Copper, which 1.12.2 doesn't have and modern recipes use, is added
(ingot and ore, with ore generation). When another mod adds copper (`ingotCopper`), the Translator turns its own copper
off and uses that mod's.

## Self-test

`/translatortest [modid]` (operator, single player) tries every block and item of the converted mods next to the
player:

- **Blocks.** Each block is placed, saved and loaded, ticked, right-clicked and broken.
- **Menus and screens.** A menu that opens gets every slot clicked, its screen is checked on the client.
- **Items.** Each item is used in the air and on a block.
- **Models.** Models are checked for missing textures and faces.

The report goes to `translator/selftest/`: what failed and which 1.21 calls are still missing, grouped by how many
blocks and items they break.

## Ports

Mods can also be ported to build on the Translator directly. A port keeps its 1.21 code and data, and the Translator
runs it on 1.12.2:
[Mystical Agriculture: Back to Life](https://github.com/Xponer2/mystical-agriculture-back-to-life) and
[Cucumber Library: Back to Life](https://github.com/Xponer2/cucumber-back-to-life).

## Requirements

- Minecraft 1.12.2, Forge 14.23.5.2847 or newer.
- For original 1.21.1 mods: MixinBooter, and the Minecraft 1.21.1 / NeoForge files described above.

## Building

```bash
./gradlew build
```

Needs a JDK 21 to run Gradle. The jar is in `build/libs`.

## License

[PolyForm Noncommercial License 1.0.0](LICENSE), © Xponer2.

- Free to use, change and share for any noncommercial purpose: in your own ports and mods, modpacks, servers,
  videos, forks.
- Keep the credit to the author: the `Required Notice` line of the license and the license header at the top of
  every source file stay in every copy and in every changed version.
- No commercial use.

Bundled libraries:

- DataFixerUpper: [MIT](licenses/MIT-DataFixerUpper.txt), © Microsoft.
- JvmDowngrader, unchanged: [LGPL 2.1](licenses/LGPL-2.1-JvmDowngrader.txt), © William Gray.
- ASM, inside JvmDowngrader: [BSD 3-Clause](licenses/BSD-3-ASM.txt), © INRIA, France Telecom.

The 1.21.1 mods you run keep their own licenses. Converting them happens on your machine only.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.

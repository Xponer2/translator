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

## Mods

<!-- mods:start -->

Tested with converter v138, 2026-11-05. 90 mods: 2 work, 60 partly work, 7 don't work, 21 not tested yet.

✅ works: every block, item and screen of the mod passed the self-test, no crash, all its mixins applied. 🟡 partly works: it loads and plays, some content fails. ❌ doesn't work: it doesn't load or crashes the game. ⬜ not tested yet.

| Mod | Version | Status | Self-test | Notes |
|---|---|---|---|---|
| Accessories | 1.1.0-beta.48 | 🟡 Partly works | — | library, 7 mixins not applied, inside another jar |
| Another Furniture | 4.0.3 | 🟡 Partly works | 262 / 333 | 1 mixin not applied |
| AppleSkin | 3.0.9 | ⬜ Not tested yet | — | library |
| Applied Energistics 2 | 19.2.18 | 🟡 Partly works | 391 / 466 |  |
| Ars Nouveau | 5.13.4 | ❌ Doesn't work | — |  |
| Artifacts | 13.2.5 | 🟡 Partly works | 37 / 39 | 4 mixins not applied |
| Biomes O' Plenty | 21.1.0.14 | 🟡 Partly works | 817 / 861 |  |
| Chat Heads | 0.15.7 | 🟡 Partly works | — | library, 1 mixin not applied |
| Cloth Config v15 API | 15.0.140 | ⬜ Not tested yet | — | library |
| CodecUI | 1.4.7 | 🟡 Partly works | — | library, 1 mixin not applied, inside another jar |
| Comforts | 9.0.5 | 🟡 Partly works | 1 / 1 | 1 mixin not applied |
| Common Networking | 1.0.21 | ⬜ Not tested yet | — | library |
| Configured | 2.6.3 | ⬜ Not tested yet | — | library |
| Controlling | 19.0.5 | 🟡 Partly works | — | library, 1 mixin not applied |
| CoroUtil | 1.3.9 | ⬜ Not tested yet | — | library |
| CosmeticArmorReworked | 1 | ⬜ Not tested yet | — | library |
| Create | 6.0.10 | 🟡 Partly works | 1090 / 1335 | 18 mixins not applied |
| Cristel Lib | 3.1.7 | ❌ Doesn't work | — | library |
| Cucumber Library | 8.0.16 | ⬜ Not tested yet | — | library |
| Cumulus | 2.0.7 | 🟡 Partly works | — | library, 5 mixins not applied, inside another jar |
| Curios API | 9.5.1 | 🟡 Partly works | — | library, 1 mixin not applied |
| ExpandAbility | — | ⬜ Not tested yet | — | library, inside another jar |
| Farmer's Delight | 1.3.4 | 🟡 Partly works | 286 / 317 | 1 mixin not applied |
| Flywheel | 1.0.6 | 🟡 Partly works | — | library, 2 mixins not applied, inside another jar |
| Forgified Fabric API Base | — | ⬜ Not tested yet | — | library, inside another jar |
| Framework | 0.13.11 | ⬜ Not tested yet | — | library |
| Fusion | 1.3.16 | 🟡 Partly works | — | library, 2 mixins not applied |
| GeckoLib 4 | 4.9.3 | 🟡 Partly works | — | library, 2 mixins not applied |
| GlitchCore | 2.1.0.2 | 🟡 Partly works | — | library, 2 mixins not applied |
| GuideME | 21.1.21 | 🟡 Partly works | 0 / 1 |  |
| Handcrafted | 4.0.3 | 🟡 Partly works | 462 / 540 | 1 mixin not applied |
| Infiniverse | — | ⬜ Not tested yet | — | library, inside another jar |
| Iron Furnaces | 4.3.2 | 🟡 Partly works | 13 / 57 |  |
| Iron Jetpacks | 8.0.11 | 🟡 Partly works | 5 / 9 |  |
| Iron's Lib | 2.3.0 | 🟡 Partly works | 1 / 3 | 2 mixins not applied |
| Iron's Spells 'n Spellbooks | 3.16.3 | 🟡 Partly works | 111 / 154 | 8 mixins not applied |
| Jade | 15.10.6 | 🟡 Partly works | — | library, 2 mixins not applied |
| Journeymap | 6.0.9 | 🟡 Partly works | — | library, 4 mixins not applied |
| JourneyMap API | 2.0.0 | ⬜ Not tested yet | — | library, inside another jar |
| Just Enough Items | 19.64.0.455 | 🟡 Partly works | — | library |
| LambDynamicLights (API) | 4.5.1 | ❌ Doesn't work | — | library, inside another jar |
| Macaw's Bridges | 3.1.2 | 🟡 Partly works | 289 / 291 |  |
| Macaw's Doors | 1.1.5 | 🟡 Partly works | 456 / 456 |  |
| Macaw's Fences and Walls | 1.2.1 | 🟡 Partly works | 295 / 360 |  |
| Macaw's Furniture | 3.4.1 | 🟡 Partly works | 833 / 1306 |  |
| Macaw's Lights and Lamps | 1.1.5 | 🟡 Partly works | 245 / 280 |  |
| Macaw's Roofs | 2.3.2 | 🟡 Partly works | 1163 / 1212 |  |
| Macaw's Trapdoors | 1.1.5 | ✅ Works | 370 / 370 |  |
| Macaw's Windows | 2.4.2 | 🟡 Partly works | 613 / 625 |  |
| Mekanism | 10.7.19.85 | 🟡 Partly works | — |  |
| MezzConfig | 0.6.6 | ⬜ Not tested yet | — | library, inside another jar |
| Mining Gadgets | 1.18.8 | ✅ Works | 32 / 32 |  |
| MixinSquared | 0.3.7 | ❌ Doesn't work | — | library, inside another jar |
| Modular Routers | 13.2.7 | 🟡 Partly works | 48 / 54 |  |
| Moonlight Lib | 3.7.2 | ❌ Doesn't work | — | 4 mixins not applied |
| Mouse Tweaks | 2.26.1 | ⬜ Not tested yet | — | library |
| MrCrayfish's Furniture Mod: Refurbished | 1.0.22 | 🟡 Partly works | 453 / 919 | 3 mixins not applied |
| Mystical Agriculture | 8.0.29 | 🟡 Partly works | 618 / 633 |  |
| Nature's Compass | 3.4.0 | 🟡 Partly works | 0 / 1 |  |
| Nitrogen | 1.1.25 | 🟡 Partly works | — | library, inside another jar |
| Nuggets | 1.1.0.48 | 🟡 Partly works | — | library, 4 mixins not applied, inside another jar |
| oωo | 0.12.15.6-beta.1 | 🟡 Partly works | — | library, 9 mixins not applied |
| Patchouli | 93 | 🟡 Partly works | 1 / 1 | 2 mixins not applied |
| Pipez | 1.2.31 | 🟡 Partly works | 10 / 17 |  |
| Player Animator | 2.0.4 | 🟡 Partly works | — | library, 1 mixin not applied |
| Ponder | 1.0.82 | 🟡 Partly works | — | library, 5 mixins not applied, inside another jar |
| Powah | 6.2.10 | 🟡 Partly works | 131 / 254 |  |
| Rechiseled | 1.2.6 | 🟡 Partly works | 1416 / 1506 |  |
| Refined Storage | 2.0.9 | 🟡 Partly works | 16 / 352 |  |
| Resourceful Lib | 3.0.12 | ⬜ Not tested yet | — | library |
| Sable Companion | 1.6.0 | ❌ Doesn't work | — | library, inside another jar |
| Searchables | 1.0.2 | ⬜ Not tested yet | — | library |
| Sophisticated Backpacks | 3.26.17.2232 | 🟡 Partly works | 71 / 77 |  |
| Sophisticated Core | 1.5.25.2451 | 🟡 Partly works | 2 / 2 |  |
| Sophisticated Storage | 1.6.15.2219 | 🟡 Partly works | 103 / 198 |  |
| SpectreLib | 0.17.2 | ⬜ Not tested yet | — | library, inside another jar |
| Storage Drawers | 13.11.4 | 🟡 Partly works | 290 / 296 |  |
| SuperMartijn642's Config Library | 1.1.8 | ⬜ Not tested yet | — | library |
| SuperMartijn642's Core Lib | 1.1.26 | ⬜ Not tested yet | — | library |
| Supplementaries | 4.0.2 | ❌ Doesn't work | — | 12 mixins not applied |
| TerraBlender | 4.1.0.8 | ⬜ Not tested yet | — | library |
| The Aether | 1.5.10 | 🟡 Partly works | 249 / 293 | 24 mixins not applied |
| The Twilight Forest | 4.8.3345 | 🟡 Partly works | 923 / 1155 |  |
| Tom's Simple Storage Mod | 2.4.2 | 🟡 Partly works | 18 / 37 |  |
| Torchmaster | 21.1.13 | 🟡 Partly works | 5 / 7 |  |
| Towns and Towers | 1.13.11 | ⬜ Not tested yet | — | library |
| What Are They Up To | 1.2.7 | 🟡 Partly works | — | library, 1 mixin not applied |
| Xaero's Minimap | 26.6.0 | 🟡 Partly works | — | library |
| Xaero's World Map | 1.47.0 | 🟡 Partly works | — | library |
| XaeroLib | 1.7.3 | 🟡 Partly works | — | library, inside another jar |

<!-- mods:end -->

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

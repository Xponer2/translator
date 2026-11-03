# Translator

[![License: PolyForm Noncommercial](https://img.shields.io/badge/license-PolyForm%20Noncommercial%201.0.0-blue.svg?style=flat-square)](LICENSE)
[![Minecraft 1.12.2](https://img.shields.io/badge/Minecraft-1.12.2-green.svg?style=flat-square)](#)
![Made with AI (Claude)](https://img.shields.io/badge/made%20with-AI%20(Claude)-8A2BE2?style=flat-square)

The **Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2** (Forge). It runs **original NeoForge 1.21.1 mod jars**
on 1.12.2: put them in `mods/1.21.1/` (or `mods/`) next to the Translator and start the game.

**Made with AI:** the Translator is made entirely with AI (Claude), from the code to the in-game testing.

**Experimental.** Many blocks, items, menus and recipes of the tested mods work. Rendering, world generation and
some screens are still incomplete. A mod can load and still miss parts of its behaviour.

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

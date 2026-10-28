# Translator

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](LICENSE)
[![Minecraft 1.12.2](https://img.shields.io/badge/Minecraft-1.12.2-green.svg?style=flat-square)](#)
![Made with AI (Claude)](https://img.shields.io/badge/made%20with-AI%20(Claude)-8A2BE2?style=flat-square)

The **Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2** (Forge): a library for ports of modern mods to 1.12.2.
A ported mod keeps its 1.21 code and data: registries, data components, codecs, recipes, tags and conditions in the
1.21 JSON formats, menus, network payloads, capabilities, data driven enchantments, block shapes, rendering. The
Translator runs them on 1.12.2. It has no content of its own.

The 1.21 vanilla ids are translated to 1.12.2 ids and metadata (`minecraft:white_wool` is `minecraft:wool` 0).
Recipe results that 1.12.2 doesn't have get something similar (`minecraft:tuff` gives andesite). Recipes that need
content 1.12.2 has nothing like are left out.

Used by [Mystical Agriculture: Back to Life](https://github.com/Xponer2/mystical-agriculture-back-to-life) and
[Cucumber Library: Back to Life](https://github.com/Xponer2/cucumber-back-to-life).

**Made with AI:** this library is made entirely with AI (Claude), from the code to the in-game testing.

**Work in progress.**

## Requirements

- Minecraft 1.12.2, Forge 14.23.5.2847 or newer.

## Building

```bash
./gradlew build
```

Needs a JDK 21 to run Gradle. The jar is in `build/libs`.

## License

[MIT](LICENSE), © Xponer2. Bundled DataFixerUpper: [MIT](licenses/MIT-DataFixerUpper.txt), © Microsoft.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.

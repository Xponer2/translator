# Contributing

Thanks for wanting to help! The Translator runs mods made for newer Minecraft on older Minecraft. Right now that is
original NeoForge 1.21.1 mod jars on Minecraft 1.12.2 (Forge). The 1.21.1 API is huge, so every report and every
small fix moves it forward.

The Translator is made entirely with AI (Claude). Pull requests written by people are just as welcome, and so are
pull requests made with AI tools.

## 1. Test mods and report what breaks (no coding needed)

This is the most useful help right now. Run the 1.21.1 mods you like through the Translator and tell us what
happens. See [Requirements](README.md#requirements) for the setup. A simpler install for the normal game comes with
the first alpha release.

Open an issue with the **Crash** or **Mod doesn't work** template and attach:

- `logs/latest.log`;
- the crash report from `crash-reports/`, if the game crashed;
- the self-test report from `translator/selftest/`, if you ran `/translatortest <modid>` in a creative world;
- the mod's list of missing calls, `translator/cache/<mod>-….stubbed.txt`, if there is one.

One issue per mod, please. If several mods break the same way, one issue for all of them is fine.

## 2. Bridge a missing call (small code tasks)

Issues labelled [`good first issue`](https://github.com/Xponer2/translator/labels/good%20first%20issue) each name
one 1.21.1 method, field or class the Translator doesn't have yet, and the mods that need it. Most of them take
10 to 30 lines.

### How a bridge works

At startup each 1.21.1 jar is converted: every 1.21.1 name is mapped to its 1.12.2 counterpart, or to a Translator
class where 1.12.2 has nothing like it. The tables are in `src/main/resources/assets/translator/runtime/*.tsv`.

When the 1.12.2 class lacks a method that the 1.21.1 class has, an `R` line sends the call to a static helper:

```
R	net/minecraft/world/storage/MapDecoration	io/github/xponer2/translator/world/MapDecorations
```

With this line, a mod's `decoration.x()` (missing in 1.12.2) becomes `MapDecorations.x(decoration)`: the same name,
with the object as the first parameter. Static methods keep their parameters. A missing constructor
`new MapDecoration(...)` becomes the helper's factory `newMapDecoration(...)`. See
[`world/MapDecorations.java`](src/main/java/io/github/xponer2/translator/world/MapDecorations.java) for a complete
example.

### Steps

1. Find the 1.12.2 class the 1.21.1 class is mapped to (the `C` lines of `neoforge-1.21.1.tsv`; the issue usually
   says it).
2. Add a static method to that class's helper (find its `R` line), or make a new helper and add an `R` line for it.
3. Build with `./gradlew build` (Gradle needs JDK 21; the mod itself runs on Java 8, so use only Java 8 library
   APIs).
4. Open a pull request: what the call does in 1.21.1, how your bridge does it on 1.12.2, and which mod you checked
   it with.

Leave `JarConverter.VERSION` as it is: it is raised at release, and that makes the players' cached conversions be
made again.

### Rules

- **Bridge the API, not one mod.** Make the call behave like 1.21.1 for every mod that uses it.
- **Keep the license header** at the top of every new file (copy it from any existing file).
- **No Minecraft code or assets.** Don't copy decompiled Minecraft or NeoForge code or Mojang's files into the repo.
  Write the bridge from what the method does.
- **Comments in English,** short and plain.

## License of contributions

The Translator is under the [PolyForm Noncommercial License 1.0.0](LICENSE). By sending a pull request you agree that
your contribution is published under the same license. Code taken from other projects needs a compatible license
and keeps its notice (see [`licenses/`](licenses)).

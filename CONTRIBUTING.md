# Contributing

Thanks for wanting to help! The Translator is a prototype: there is no release for players yet, so the help that
counts now is in the code and in checking it against the real game.

## Where to start

- **Run the converter on a mod you know** (see [Running it](README.md#running-it)) and read its report. Every problem
  it lists is something a step can't convert yet, named by the newer version's class or method.
- **Compatibility modules.** Where Minecraft really changed between two versions (an object that gained new state, a
  method that was split, a loader API that changed), a hand-written adapter in `adapters_1_20_4` covers it for every
  mod. The converter applies it from `ladder-rules.tsv`.
- **Tests against the real game.** Each module is checked by a test mod made for NeoForge 1.21.1 (`testmods/`): it
  runs on 1.21.1 and converted on 1.20.4, and both must give the same results.
- **Reports.** A mod that doesn't convert clean, or a converted mod that behaves differently from 1.21.1: open an
  issue with the converter's report.

## How changes are made

- **Strict.** A change is done when the converter reports no problem for what it covers and a test shows the same
  behaviour as on 1.21.1. No stubs, no skipped calls, no silent defaults.
- **Fix the mechanism, not one mod.** A rule or an adapter covers every mod that uses that part of the game.
- **Keep the license header** at the top of every new file (copy it from any existing file).
- **No Minecraft code or mappings in the repository.** Nothing decompiled; the tables are made on each machine from
  the official downloads.

## License of contributions

The Translator is under the [MIT License](LICENSE). By sending a pull request you agree that your contribution is
published under it. Code taken from other projects needs a compatible license and keeps its notice
([THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) lists them).

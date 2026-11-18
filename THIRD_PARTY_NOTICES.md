# Third-party notices

What the Translator takes from other projects, and under which license. Nothing of Minecraft is included: Mojang's
mappings and game files are read on each machine from the official downloads and never committed.

## Libraries the converter uses (downloaded by Gradle, not copied into this repository)

| Library | What for | License | Copyright |
|---|---|---|---|
| [Sinytra Adapter](https://github.com/Sinytra/Adapter) (`org.sinytra.adapter:core`) | Repairs mods' mixins whose target methods changed between two versions | MIT | © 2023 Sinytra |
| [Mixin](https://github.com/FabricMC/Mixin) (`net.fabricmc:sponge-mixin`) | Mixin's own tooling, used by Adapter | MIT | © SpongePowered and contributors |
| [MixinExtras](https://github.com/LlamaLad7/MixinExtras) | Mixin extensions, used by Adapter | MIT | © LlamaLad7 |
| [ASM](https://asm.ow2.io/) | Reading and writing class files | BSD 3-Clause | © INRIA, France Telecom |
| [Guava](https://github.com/google/guava), [fastutil](https://fastutil.di.unimi.it/), [Log4j](https://logging.apache.org/log4j/) | Collections and logging, used by Adapter | Apache 2.0 | © Google; © Sebastiano Vigna; © The Apache Software Foundation |
| [DataFixerUpper](https://github.com/Mojang/DataFixerUpper), [Gson](https://github.com/google/gson), [SLF4J](https://www.slf4j.org/) | Codecs, JSON, logging | MIT; Apache 2.0; MIT | © Microsoft; © Google; © QOS.ch |

## Code taken from other projects

None yet. Code taken from another project keeps its license notice and gets a row here.

## Ideas

Projects that solve related problems and whose public designs informed ours (no code taken):
[Sinytra Connector](https://github.com/Sinytra/Connector), [Legacy Connector](https://github.com/kylepitcock/Legacy-Connector),
[Retromod](https://github.com/Bownlux/RetroMod), [ViaVersion](https://github.com/ViaVersion/ViaVersion).

#!/usr/bin/env bash
# The class path of the ladder's tools: ASM (core, commons, tree, analysis, util), Gson and night-config from the Gradle cache. They are there once
# `./gradlew createMinecraftArtifacts` has run (NeoForge needs them too). Sets CP and JAVA; source it:  . tools/libs.sh
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
find_jar() {
    find "$CACHE/$1" -name "$2" 2>/dev/null | grep -v -- '-sources' | sort -V | tail -1
}
JARS=("$(find_jar org.ow2.asm/asm 'asm-9.*.jar')" "$(find_jar org.ow2.asm/asm-commons 'asm-commons-9.*.jar')"
      "$(find_jar org.ow2.asm/asm-tree 'asm-tree-9.*.jar')" "$(find_jar org.ow2.asm/asm-analysis 'asm-analysis-9.*.jar')"
      "$(find_jar org.ow2.asm/asm-util 'asm-util-9.*.jar')" "$(find_jar com.google.code.gson/gson 'gson-2.*.jar')"
      "$(find_jar com.electronwill.night-config/core 'core-3.8.*.jar')" "$(find_jar com.electronwill.night-config/toml 'toml-3.8.*.jar')")
for j in "${JARS[@]}"; do
    if [ -z "$j" ]; then
        echo "ASM 9, Gson 2 or night-config 3.8 (the loader's TOML parser) not found in $CACHE: run ./gradlew createMinecraftArtifacts first" >&2
        return 1 2>/dev/null || exit 1
    fi
done
SEP=":"
if command -v cygpath >/dev/null 2>&1; then
    # Git Bash on Windows: a Windows class path
    SEP=";"
    for i in "${!JARS[@]}"; do JARS[$i]=$(cygpath -w "${JARS[$i]}"); done
fi
# the converter's own libraries (Sinytra Adapter...), listed by ./gradlew :printToolJars
if [ -f "$(dirname "${BASH_SOURCE[0]}")/tool-jars.txt" ]; then
    while IFS= read -r j; do
        [ -n "$j" ] && JARS+=("$(command -v cygpath >/dev/null 2>&1 && cygpath -w "$j" || echo "$j")")
    done < "$(dirname "${BASH_SOURCE[0]}")/tool-jars.txt"
fi
CP=$(IFS="$SEP"; echo "${JARS[*]}")
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
export CP SEP JAVA JAVAC

#!/usr/bin/env bash
# The class path of the ladder's tools: ASM (core, commons, tree) and Gson from the Gradle cache. They are there once
# `./gradlew createMinecraftArtifacts` has run (NeoForge needs them too). Sets CP and JAVA; source it:  . tools/libs.sh
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"
find_jar() {
    find "$CACHE/$1" -name "$2" 2>/dev/null | grep -v -- '-sources' | sort -V | tail -1
}
JARS=("$(find_jar org.ow2.asm/asm 'asm-9.*.jar')" "$(find_jar org.ow2.asm/asm-commons 'asm-commons-9.*.jar')"
      "$(find_jar org.ow2.asm/asm-tree 'asm-tree-9.*.jar')" "$(find_jar com.google.code.gson/gson 'gson-2.*.jar')")
for j in "${JARS[@]}"; do
    if [ -z "$j" ]; then
        echo "ASM 9 or Gson 2 not found in $CACHE: run ./gradlew createMinecraftArtifacts first" >&2
        return 1 2>/dev/null || exit 1
    fi
done
SEP=":"
if command -v cygpath >/dev/null 2>&1; then
    # Git Bash on Windows: a Windows class path
    SEP=";"
    for i in "${!JARS[@]}"; do JARS[$i]=$(cygpath -w "${JARS[$i]}"); done
fi
CP=$(IFS="$SEP"; echo "${JARS[*]}")
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
export CP SEP JAVA JAVAC

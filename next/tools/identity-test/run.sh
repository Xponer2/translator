#!/usr/bin/env bash
# Identity rules check: mods of two API versions rewritten by the rules (USE -> native, SUPER/new -> facade,
# DISPATCH -> static dispatcher), loaded with the target classes in one class loader, every case checked.
#   ./run.sh             all five rules: every check passes
#   ./run.sh nodispatch  only the first four: NoSuchMethodError on the first new method
# Needs a JDK 21 (JAVA_HOME or on the PATH) and ASM in the Gradle cache (see ../libs.sh).
set -e
cd "$(dirname "$0")"
. ../libs.sh
rm -rf build && mkdir -p build/api build/mods build/target build/tool build/converted
"$JAVAC" -d build/api api/src/CropBlock.java api/srcb/CropBlock.java
"$JAVAC" -XDstringConcat=inline -cp build/api -d build/mods mods/modA/*.java mods/modB/*.java
"$JAVAC" -d build/target target/eng/*.java target/fac/*.java
"$JAVAC" -cp "$CP" -d build/tool tool/*.java
"$JAVA" -Dnodispatch=$([ "$1" = nodispatch ] && echo true || echo false) -cp "$CP${SEP}build/tool" Rewrite build/mods build/converted
"$JAVA" -cp build/tool Check build/target build/converted

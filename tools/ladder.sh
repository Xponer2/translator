#!/usr/bin/env bash
# The ladder's tools on the versions of this project (top first):
#   tools/ladder.sh stage0  <folder of 1.21.1 mod jars> [out dir]       walk every reference down the steps, report
#   tools/ladder.sh convert <mod jar | folder> <out jar | folder> <report>  convert to the bottom version, strict
# Before: ./gradlew createMinecraftArtifacts printLoaderJars  and  python tools/fetch_mappings.py
set -e
cd "$(dirname "$0")/.."
COMMAND="$1"
shift || true
. tools/libs.sh
path() {
    if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else echo "$1"; fi
}
mkdir -p tools/build
# a failed compile stops here: never run the tools' older classes
if ! COMPILE=$("$JAVAC" -nowarn -cp "$CP" -d tools/build tools/stage0/Stage0.java tools/convert/Convert.java 2>&1); then
    echo "$COMPILE" >&2
    exit 1
fi
echo "$COMPILE" | grep -v -E '^Note:|^$' >&2 || true
VERSIONS=()
for pair in "1.21.1 v1_21_1" "1.21 v1_21" "1.20.6 v1_20_6" "1.20.4 v1_20_4"; do
    read -r version project <<< "$pair"
    jar=$(ls "$project"/build/moddev/artifacts/neoforge-*.jar 2>/dev/null | grep -v -E 'sources|merged|client-extra' | head -1)
    if [ -z "$jar" ] || [ ! -f "$project/loader-jars.txt" ] || [ ! -f "mappings/$version/client.txt" ]; then
        echo "missing for $version: run ./gradlew createMinecraftArtifacts printLoaderJars and python tools/fetch_mappings.py" >&2
        exit 1
    fi
    VERSIONS+=("$version=$(path "$PWD/$jar")=$(path "$PWD/mappings/$version")=$(path "$PWD/$project/loader-jars.txt")")
done
case "$COMMAND" in
    stage0)
        "$JAVA" -Xmx10G -cp "$CP${SEP}tools/build" Stage0 "$(path "${2:-$PWD/out/stage0}")" "$(path "$1")" "${VERSIONS[@]}"
        ;;
    convert)
        # the hand-written adapters' rules and jar, when they are built (./gradlew :adapters_1_20_4:jar)
        RULES=()
        ADAPTERS=$(ls adapters_1_20_4/build/libs/ladder-adapters-1.20.4-*.jar 2>/dev/null | head -1)
        if [ -n "$ADAPTERS" ]; then
            RULES=("-Dladder.rules=$(path "$PWD/adapters_1_20_4/src/main/resources/ladder-rules.tsv")" "-Dladder.adapters=$(path "$PWD/$ADAPTERS")")
        fi
        "$JAVA" -Xmx8G ${LADDER_JAVA_OPTS:-} "${RULES[@]}" -cp "$CP${SEP}tools/build" Convert "$(path "$1")" "$(path "$2")" "$(path "$3")" "${VERSIONS[@]}"
        ;;
    *)
        sed -n '2,5p' "$0"
        exit 2
        ;;
esac

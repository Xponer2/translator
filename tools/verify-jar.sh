#!/usr/bin/env bash
# Links and verifies converted jars against the bottom version, offline (tools/verify/VerifyJar.java): every call,
# field and type resolves with the right kind, every method passes ASM's verifier.
#   tools/verify-jar.sh <carried jar> <converted jar>...
#   e.g. tools/verify-jar.sh out/converted/ladder-carried-1.20.4.jar out/converted/foo.jar
set -e
cd "$(dirname "$0")/.."
. tools/libs.sh
mkdir -p tools/build/verify
"$JAVAC" -nowarn -cp "$CP" -d tools/build/verify tools/verify/VerifyJar.java
CARRIED="$1"
shift
GAME=$(ls v1_20_4/build/moddev/artifacts/neoforge-*.jar | grep -v -E 'sources|merged|client-extra' | head -1)
ADAPTERS=$(ls adapters_1_20_4/build/libs/ladder-adapters-1.20.4-*.jar | head -1)
# the bottom game's own run class paths (ModDevGradle writes them per run; the client's has the client libraries), the
# launcher's module path (the -p line of the run's JVM arguments), the loader, the adapters and the carried library
MODDEV=v1_20_4/build/moddev
[ -f "$MODDEV/clientLegacyClasspath.txt" ] || MODDEV=../translator-ladder/v1_20_4/build/moddev
LIST=$(mktemp)
{
    echo "$GAME"
    for f in "$MODDEV/clientLegacyClasspath.txt" "$MODDEV/serverLegacyClasspath.txt" v1_20_4/loader-jars.txt; do
        if [ -f "$f" ]; then cat "$f"; echo; fi
    done
    grep -A1 -m1 '^-p$' "$MODDEV/serverRunVmArgs.txt" | tail -1
    echo "$ADAPTERS"
    echo "$CARRIED"
} > "$LIST"
set +e
"$JAVA" -Xmx4G -cp "$CP${SEP}tools/build/verify" VerifyJar "$LIST" "$@"
status=$?
rm -f "$LIST"
exit $status

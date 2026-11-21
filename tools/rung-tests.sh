#!/usr/bin/env bash
# The test mods converted down the rungs and run in each lower game, compared with their native 1.21.1 results:
#   tools/rung-tests.sh <native results folder> <work folder> [1.21] [1.20.6] [1.20.4]
# <native results folder> holds run1/, run2/ (test A, B, E server results) and client/ (test-a-client, test-c, test-f).
# Test mods: testmods/*/build/libs (built); the adapters: adapters_*/build/libs; for 1.20.4 the corpus's carried library
# (LADDER_CARRIED_1204, a ladder-carried-1.20.4.jar) the adapters were compiled against.
set -u
cd "$(dirname "$0")/.."
NATIVE="$1"
WORK="$2"
shift 2
TARGETS=("$@")
[ ${#TARGETS[@]} -eq 0 ] && TARGETS=(1.21 1.20.6 1.20.4)
mkdir -p "$WORK"
jar() {
    local j
    j="$(ls testmods/"$1"/build/libs/*.jar 2>/dev/null | head -1)"
    if [ -z "$j" ]; then
        echo "testmods/$1 isn't built (./gradlew :testmods:$1:jar)" >&2
        exit 2
    fi
    echo "$j"
}
compare() {
    python -I tools/compare_results.py "$1" "$2" 2>&1 | head -6
}
server_run() { # <target> <dir> <mods...>
    local target="$1" dir="$2"
    shift 2
    rm -rf "$dir/run-server/mods" "$dir/run-server/world" "$dir"/run-server/test-*.json
    mkdir -p "$dir/run-server/mods"
    cp "$@" "$dir/run-server/mods/"
    LADDER_TARGET="$target" bash tools/run-until.sh ":$dir:runServer" "$dir/run-server/test-b.json" 400 > /dev/null 2>&1
    for t in a b e; do compare "$NATIVE/run1/test-$t.json" "$dir/run-server/test-$t.json"; done
}
client_a() { # <target> <dir> <mods...>
    local target="$1" dir="$2"
    shift 2
    local keep="$dir/run-client/mods-keep"
    rm -rf "$keep"
    [ -d "$dir/run-client/mods" ] && mv "$dir/run-client/mods" "$keep"
    mkdir -p "$dir/run-client/mods"
    cp "$@" "$dir/run-client/mods/"
    rm -f "$dir/run-client/test-a-client.json"
    LADDER_TARGET="$target" bash tools/run-until.sh ":$dir:runClient" "$dir/run-client/test-a-client.json" 300 > /dev/null 2>&1
    compare "$NATIVE/client/test-a-client.json" "$dir/run-client/test-a-client.json"
    rm -rf "$dir/run-client/mods"
    [ -d "$keep" ] && mv "$keep" "$dir/run-client/mods"
}
client_cf() { # <target> <dir> <mods...>
    local target="$1" dir="$2"
    shift 2
    rm -rf "$dir/run-client/mods" "$dir/run-client/saves" "$dir"/run-client/test-c.json "$dir"/run-client/test-f.json
    mkdir -p "$dir/run-client/mods"
    cp "$@" "$dir/run-client/mods/"
    LADDER_TARGET="$target" bash tools/run-probe.sh client-world 300 > /dev/null 2>&1
    compare "$NATIVE/client/test-c.json" "$dir/run-client/test-c.json"
    [ -f "$NATIVE/client/test-f.json" ] && compare "$NATIVE/client/test-f.json" "$dir/run-client/test-f.json"
}
# rung 1: every test mod to 1.21
R1="$WORK/r1"
rm -rf "${R1:?}"
mkdir -p "$R1"
for t in a b c e f; do
    LADDER_TO=1.21 bash tools/ladder.sh convert "$(jar ladder_test_$t)" "$R1/ladder-test-$t.jar" "$R1/test-$t.md" > /dev/null 2>&1
    head -3 "$R1/test-$t.md" | tail -1
done
A121="$(ls adapters_1_21/build/libs/ladder-adapters-1.21-*.jar | head -1)"
for target in "${TARGETS[@]}"; do
    case "$target" in
        1.21)
            echo "== 1.21"
            server_run 1.21 v1_21 "$R1"/ladder-test-{a,b,e}.jar "$A121"
            client_a 1.21 v1_21 "$R1"/ladder-test-a.jar "$A121"
            client_cf 1.21 v1_21 "$R1"/ladder-test-{c,f}.jar "$A121" ;;
        1.20.6)
            echo "== 1.20.6"
            IN="$WORK/r2in" OUT2="$WORK/r2"
            rm -rf "${IN:?}" "${OUT2:?}"
            mkdir -p "$IN" "$OUT2"
            cp "$R1"/ladder-test-*.jar "$A121" "$IN/"
            [ -f "$R1/ladder-carried-1.21.jar" ] && cp "$R1/ladder-carried-1.21.jar" "$IN/"
            LADDER_FROM=1.21 LADDER_TO=1.20.6 bash tools/ladder.sh convert "$IN" "$OUT2" "$OUT2.tsv" > "$OUT2.log" 2>&1
            tail -1 "$OUT2.log"
            A1206="$(ls adapters_1_20_6/build/libs/ladder-adapters-1.20.6-*.jar | head -1)"
            CARRIED=("$OUT2"/ladder-carried-*.jar)
            server_run 1.20.6 v1_20_6 "$OUT2"/ladder-test-{a,b,e}.jar "$OUT2/$(basename "$A121")" "${CARRIED[@]}" "$A1206"
            client_a 1.20.6 v1_20_6 "$OUT2"/ladder-test-a.jar "$OUT2/$(basename "$A121")" "${CARRIED[@]}" "$A1206"
            client_cf 1.20.6 v1_20_6 "$OUT2"/ladder-test-{c,f}.jar "$OUT2/$(basename "$A121")" "${CARRIED[@]}" "$A1206" ;;
        1.20.4)
            echo "== 1.20.4"
            IN="$WORK/r4in" OUT4="$WORK/r4"
            rm -rf "${IN:?}" "${OUT4:?}"
            mkdir -p "$IN" "$OUT4"
            for t in a b c e f; do cp "$(jar ladder_test_$t)" "$IN/"; done
            [ -n "${LADDER_CARRIED_1204:-}" ] && cp "$LADDER_CARRIED_1204" "$OUT4/"
            bash tools/ladder.sh convert "$IN" "$OUT4" "$OUT4.tsv" > "$OUT4.log" 2>&1
            tail -1 "$OUT4.log"
            A1204="$(ls adapters_1_20_4/build/libs/ladder-adapters-1.20.4-*.jar | head -1)"
            server_run 1.20.4 v1_20_4 "$OUT4"/ladder-test-{a,b,e}-*.jar "$OUT4/ladder-carried-1.20.4.jar" "$A1204"
            client_a 1.20.4 v1_20_4 "$OUT4"/ladder-test-a-*.jar "$OUT4/ladder-carried-1.20.4.jar" "$A1204"
            client_cf 1.20.4 v1_20_4 "$OUT4"/ladder-test-{c,f}-*.jar "$OUT4/ladder-carried-1.20.4.jar" "$A1204" ;;
    esac
done

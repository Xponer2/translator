#!/usr/bin/env bash
# Runs a target game (1.20.4 by default) with the mods in v<target>/run-<side>/mods and waits for the probe's results; a watchdog stops the
# game when they don't come (a mod that breaks loading, a hang).
#   tools/run-probe.sh server [seconds]   ->  v1_20_4/run-server/probe.json
#   tools/run-probe.sh client [seconds]   ->  v1_20_4/run-client/probe-client.json
#   tools/run-probe.sh client-world [s]   ->  the same, after a fresh world was entered and the inventory opened
#   tools/run-probe.sh client-connect [s] ->  the same, on the dedicated server of tools/run-net.sh
# LADDER_TARGET picks the game (1.20.4 by default; 1.20.6, 1.21: v1_20_6, v1_21). Those have no probe mod: a run ends
# with the test mods' results (server: run-server/test-b.json, client: run-client/test-c.json) or LADDER_RESULT's file.
set -u
cd "$(dirname "$0")/.."
TARGET="${LADDER_TARGET:-1.20.4}"
DIR="v$(echo "$TARGET" | tr . _)"
if [ ! -f "$DIR/build.gradle" ]; then
    echo "no target $TARGET ($DIR)" >&2
    exit 2
fi
SIDE="${1:-server}"
LIMIT="${2:-240}"
EXTRA=()
RUN="$SIDE"
if [ "$SIDE" = client-world ]; then RUN=client; EXTRA=(-Pladder.world); fi
# onto the dedicated server of tools/run-net.sh (a real connection: payloads are encoded)
if [ "$SIDE" = client-connect ]; then RUN=client; EXTRA=(-Pladder.connect=localhost:25570); fi
if [ "$RUN" = client ]; then TASK=runClient; else TASK=runServer; fi
if [ "$DIR" = v1_20_4 ]; then
    if [ "$RUN" = client ]; then RESULT=$DIR/run-client/probe-client.json; else RESULT=$DIR/run-server/probe.json; fi
else
    if [ "$RUN" = client ]; then RESULT=$DIR/run-client/test-c.json; else RESULT=$DIR/run-server/test-b.json; fi
fi
RESULT="${LADDER_RESULT:-$RESULT}"
LOG=$DIR/run-$SIDE.log
# the machine's guard before another game JVM (free memory, nobody playing): LADDER_GUARD (a command; non-zero exit = no)
GUARD="${LADDER_GUARD:-}"
if [ -n "$GUARD" ] && ! $GUARD; then
    echo "guard said no: not starting the game" >&2
    exit 2
fi
rm -f "$RESULT"
if [ "$RUN" = client ] && [ ! -f $DIR/run-client/options.txt ]; then
    # straight to the title screen (no accessibility onboarding), quiet
    mkdir -p $DIR/run-client
    printf 'onboardAccessibility:false\nsoundCategory_master:0.0\npauseOnLostFocus:false\nrenderDistance:4\nsimulationDistance:5\n' > $DIR/run-client/options.txt
fi
./gradlew ":$DIR:$TASK" "${EXTRA[@]}" --console=plain > "$LOG" 2>&1 &
start=$(date +%s)
# a stalled game (no new log lines, no result: stuck in a menu, a screen nobody answers, a hang) is stopped early
STALL="${LADDER_STALL:-90}"
echo "game: $(basename "$LOG"), timeout ${LIMIT}s, stall ${STALL}s" >&2
last_size=-1
last_change=$start
while [ ! -f "$RESULT" ]; do
    size=$(stat -c %s "$LOG" 2>/dev/null || echo 0)
    if [ "$size" != "$last_size" ]; then
        last_size=$size
        last_change=$(date +%s)
    elif [ $(( $(date +%s) - last_change )) -ge "$STALL" ]; then
        echo "watchdog: stalled, no log output for ${STALL}s and no result (the test did not run: a menu, a dialog, a hang)" >&2
        tail -3 "$LOG" >&2
        break
    fi
    if grep -q -E 'Exception in thread "main"|BUILD FAILED|Mod Loading has failed|Crash report saved|FAILURE:|Failed to (load|create) mod' "$LOG" 2>/dev/null; then
        break
    fi
    if [ $(( $(date +%s) - start )) -ge "$LIMIT" ]; then
        echo "watchdog: no result after ${LIMIT}s" >&2
        break
    fi
    sleep 2
done
sleep 3
stop_game() {
    if command -v powershell >/dev/null 2>&1; then
        powershell -NoProfile -Command "\$b = [string][char]92 + [char]92; \$p = '[/' + \$b + ']' + [regex]::Escape('$(basename "$(pwd)")') + '[/' + \$b + ']'; Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match '${RUN}RunVmArgs' -and \$_.CommandLine -match \$p } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Confirm:\$false }" >/dev/null 2>&1
    else
        pkill -f "${RUN}RunVmArgs" 2>/dev/null
    fi
}
stop_game
wait 2>/dev/null
echo "$(( $(date +%s) - start ))s; log: $LOG"
if [ -f "$RESULT" ]; then cat "$RESULT"; exit 0; fi
grep -E 'ERROR|Exception|Mod Loading has failed|Caused by' "$LOG" | head -20
exit 1

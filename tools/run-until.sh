#!/usr/bin/env bash
# Runs a Gradle run task (a game server or client of this checkout) until a result file appears, then stops that game
# (only the JVM started from this checkout: other checkouts' games keep running).
#   tools/run-until.sh <gradle task> <result file> [seconds]
#   e.g. tools/run-until.sh :testmods:ladder_test_d:runServer testmods/ladder_test_d/run-server/test-d.json
set -u
cd "$(dirname "$0")/.."
TASK="$1"
RESULT="$2"
LIMIT="${3:-300}"
LOG="out/run-$(echo "$TASK" | tr ':' '_').log"
mkdir -p out
GUARD="${LADDER_GUARD:-}"
if [ -n "$GUARD" ] && ! $GUARD; then
    echo "guard said no: not starting the game" >&2
    exit 2
fi
rm -f "$RESULT"
case "$TASK" in
    *:runClient)
        # a fresh client folder opens the accessibility onboarding, not the title screen: the test would never start
        RUNDIR="$(echo "${TASK%:runClient}" | sed 's/^://; s/:/\//g')/run-client"
        if [ ! -f "$RUNDIR/options.txt" ]; then
            mkdir -p "$RUNDIR"
            printf 'onboardAccessibility:false\nsoundCategory_master:0.0\npauseOnLostFocus:false\nrenderDistance:4\nsimulationDistance:5\n' > "$RUNDIR/options.txt"
        fi
        ;;
esac
./gradlew "$TASK" --console=plain > "$LOG" 2>&1 &
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
sleep 2
# this checkout's folder name is in the game JVM's command line (its run files)
HERE=$(basename "$(pwd)")
if command -v powershell >/dev/null 2>&1; then
    powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match 'RunVmArgs' -and \$_.CommandLine -like '*$HERE*' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Confirm:\$false }" >/dev/null 2>&1
else
    pkill -f "/$HERE/.*RunVmArgs" 2>/dev/null
fi
wait 2>/dev/null
echo "$(( $(date +%s) - start ))s; log: $LOG"
if [ -f "$RESULT" ]; then cat "$RESULT"; exit 0; fi
grep -E 'ERROR|Exception|Mod Loading has failed|Caused by' "$LOG" | head -20
exit 1

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
./gradlew "$TASK" --console=plain > "$LOG" 2>&1 &
start=$(date +%s)
while [ ! -f "$RESULT" ]; do
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

#!/usr/bin/env bash
# Runs the 1.20.4 game with the mods in v1_20_4/run-<side>/mods and waits for the probe's results; a watchdog stops the
# game when they don't come (a mod that breaks loading, a hang).
#   tools/run-probe.sh server [seconds]   ->  v1_20_4/run-server/probe.json
#   tools/run-probe.sh client [seconds]   ->  v1_20_4/run-client/probe-client.json
set -u
cd "$(dirname "$0")/.."
SIDE="${1:-server}"
LIMIT="${2:-240}"
if [ "$SIDE" = client ]; then RESULT=v1_20_4/run-client/probe-client.json; TASK=runClient; else RESULT=v1_20_4/run-server/probe.json; TASK=runServer; fi
LOG=v1_20_4/run-$SIDE.log
# the machine's guard before another game JVM (free memory, nobody playing): LADDER_GUARD (a command; non-zero exit = no)
GUARD="${LADDER_GUARD:-}"
if [ -n "$GUARD" ] && ! $GUARD; then
    echo "guard said no: not starting the game" >&2
    exit 2
fi
rm -f "$RESULT"
if [ "$SIDE" = client ] && [ ! -f v1_20_4/run-client/options.txt ]; then
    # straight to the title screen (no accessibility onboarding), quiet
    mkdir -p v1_20_4/run-client
    printf 'onboardAccessibility:false\nsoundCategory_master:0.0\npauseOnLostFocus:false\n' > v1_20_4/run-client/options.txt
fi
./gradlew ":v1_20_4:$TASK" --console=plain > "$LOG" 2>&1 &
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
sleep 3
stop_game() {
    if command -v powershell >/dev/null 2>&1; then
        powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match '${SIDE}RunVmArgs' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Confirm:\$false }" >/dev/null 2>&1
    else
        pkill -f "${SIDE}RunVmArgs" 2>/dev/null
    fi
}
stop_game
wait 2>/dev/null
echo "$(( $(date +%s) - start ))s; log: $LOG"
if [ -f "$RESULT" ]; then cat "$RESULT"; exit 0; fi
grep -E 'ERROR|Exception|Mod Loading has failed|Caused by' "$LOG" | head -20
exit 1

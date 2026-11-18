#!/usr/bin/env bash
# A client on a dedicated 1.20.4 server over a real connection (single player passes packets as objects, so a mod's
# payload codecs only run between two processes): starts v1_20_4's serverNet (run-server-net/mods, port 25570), waits
# until it is up, runs the client probe connected to it (tools/run-probe.sh client-connect), then stops the server.
#   tools/run-net.sh [seconds]  ->  v1_20_4/run-client/probe-client.json and the test mods' client results
set -u
cd "$(dirname "$0")/.."
LIMIT="${1:-400}"
GUARD="${LADDER_GUARD:-}"
if [ -n "$GUARD" ] && ! $GUARD; then
    echo "guard said no: not starting the games" >&2
    exit 2
fi
LOG=v1_20_4/run-server-net.log
./gradlew :v1_20_4:runServerNet --console=plain > "$LOG" 2>&1 &
start=$(date +%s)
until grep -q -E 'Done \(' "$LOG" 2>/dev/null; do
    if grep -q -E 'BUILD FAILED|Mod Loading has failed|Crash report saved|FAILURE:' "$LOG" 2>/dev/null || [ $(( $(date +%s) - start )) -ge 300 ]; then
        echo "the server didn't start; log: $LOG" >&2
        break
    fi
    sleep 2
done
LADDER_GUARD=true bash tools/run-probe.sh client-connect "$LIMIT"
code=$?
if command -v powershell >/dev/null 2>&1; then
    powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match 'serverNetRunVmArgs' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Confirm:\$false }" >/dev/null 2>&1
else
    pkill -f serverNetRunVmArgs 2>/dev/null
fi
wait 2>/dev/null
grep -E 'ERROR|Exception' "$LOG" | head -10
exit $code

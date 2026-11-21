#!/usr/bin/env bash
# A client on a dedicated 1.20.4 server over a real connection (single player passes packets as objects, so a mod's
# payload codecs only run between two processes): starts the target's serverNet (run-server-net/mods, port 25570), waits
# until it is up, runs the client probe connected to it (tools/run-probe.sh client-connect), then stops the server.
#   tools/run-net.sh [seconds]  ->  v1_20_4/run-client/probe-client.json and the test mods' client results
# LADDER_TARGET picks the game (1.20.4 by default; 1.20.6, 1.21), as in tools/run-probe.sh.
set -u
cd "$(dirname "$0")/.."
LIMIT="${1:-400}"
TARGET="${LADDER_TARGET:-1.20.4}"
DIR="v$(echo "$TARGET" | tr . _)"
GUARD="${LADDER_GUARD:-}"
if [ -n "$GUARD" ] && ! $GUARD; then
    echo "guard said no: not starting the games" >&2
    exit 2
fi
LOG=$DIR/run-server-net.log
./gradlew :$DIR:runServerNet ${LADDER_GRADLE_ARGS:-} --console=plain > "$LOG" 2>&1 &
start=$(date +%s)
until grep -q -E 'Done \(' "$LOG" 2>/dev/null; do
    if grep -q -E 'BUILD FAILED|Mod Loading has failed|Crash report saved|FAILURE:' "$LOG" 2>/dev/null || [ $(( $(date +%s) - start )) -ge 300 ]; then
        echo "the server didn't start; log: $LOG" >&2
        break
    fi
    sleep 2
done
LADDER_GUARD=true LADDER_TARGET="$TARGET" bash tools/run-probe.sh client-connect "$LIMIT"
code=$?
if command -v powershell >/dev/null 2>&1; then
    powershell -NoProfile -Command "\$b = [string][char]92 + [char]92; \$p = '[/' + \$b + ']' + [regex]::Escape('$(basename "$(pwd)")') + '[/' + \$b + ']'; Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match 'serverNetRunVmArgs' -and \$_.CommandLine -match \$p } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Confirm:\$false }" >/dev/null 2>&1
else
    pkill -f serverNetRunVmArgs 2>/dev/null
fi
wait 2>/dev/null
grep -E 'ERROR|Exception' "$LOG" | head -10
exit $code

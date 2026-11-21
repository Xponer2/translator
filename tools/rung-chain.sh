#!/usr/bin/env bash
# The corpus down two rungs, each on its own: rung 1 (1.21.1 -> 1.21) on the corpus, then rung 2 (1.21 -> 1.20.6) on rung
# 1's clean jars with rung 1's adapters and carried jar (the input a 1.21 game would have).
#   tools/rung-chain.sh <corpus folder> <out folder>  ->  <out>/r1.tsv, <out>/r2.tsv
set -eu
cd "$(dirname "$0")/.."
CORPUS="$1"
OUT="$2"
# the out folder is the script's own: a fresh one, or one an earlier chain made (its r1.log); never the corpus or above it
case "$(realpath -m "$CORPUS")/" in
    "$(realpath -m "$OUT")"/*) echo "the out folder $OUT holds the corpus" >&2; exit 2;;
esac
if [ -e "$OUT" ] && [ ! -f "$OUT/r1.log" ]; then
    echo "$OUT exists and isn't an earlier chain's out folder: not touching it" >&2
    exit 2
fi
rm -rf "${OUT:?}"
mkdir -p "$OUT/r1" "$OUT/r2in" "$OUT/r2"
LADDER_TO=1.21 bash tools/ladder.sh convert "$CORPUS" "$OUT/r1" "$OUT/r1.tsv" > "$OUT/r1.log" 2>&1 || true
tail -1 "$OUT/r1.log"
# rung 1's clean jars (no problem, none unverified-only either: the report's problems column is empty)
python -I - "$OUT/r1.tsv" "$OUT/r1" "$OUT/r2in" <<'PY'
import shutil, sys
report, src, dst = sys.argv[1:4]
n = 0
for line in open(report, encoding='utf-8'):
    cols = line.rstrip('\n').split('\t')
    if len(cols) > 1 and (len(cols) < 5 or not cols[4].strip()):
        try:
            shutil.copy(src + '/' + cols[1], dst + '/' + cols[1])
            n += 1
        except FileNotFoundError:
            pass
print(n, 'clean jars of rung 1')
PY
cp "$OUT/r1/ladder-carried-1.21.jar" "$OUT/r2in/" 2>/dev/null || true
cp adapters_1_21/build/libs/ladder-adapters-1.21-*.jar "$OUT/r2in/"
LADDER_FROM=1.21 LADDER_TO=1.20.6 bash tools/ladder.sh convert "$OUT/r2in" "$OUT/r2" "$OUT/r2.tsv" > "$OUT/r2.log" 2>&1 || true
tail -1 "$OUT/r2.log"
# the reports are what is kept: the converted jars (but the carried ones) go, the disk fills fast
rm -rf "${OUT:?}/r2in"
find "${OUT:?}/r1" "${OUT:?}/r2" -name '*.jar' ! -name 'ladder-carried-*' -delete

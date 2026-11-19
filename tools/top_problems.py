"""The problems of a batch report by kind and member, the most mods first: where a generic fix pays most.

usage: python -I tools/top_problems.py <convert-batch.tsv> [regex filter] [count]
"""
import collections
import re
import sys

path = sys.argv[1]
want = re.compile(sys.argv[2]) if len(sys.argv) > 2 and sys.argv[2] else None
count = int(sys.argv[3]) if len(sys.argv) > 3 else 40
mods = collections.defaultdict(set)
for line in open(path, encoding='utf-8'):
    parts = line.rstrip('\n').split('\t')
    if len(parts) < 5 or not parts[4]:
        continue
    for problem in parts[4].split(' | '):
        p = re.sub(r'^in META-INF/\S+: ', '', problem.strip())
        p = re.sub(r' \(1\.2[^)]*\)$', '', p)
        # a member's descriptor is noise for ranking: owner.name is enough
        p = re.sub(r'(\.[\w$<>]+) \([^ ]*\)\S*', r'\1', p)
        p = re.sub(r'(\.[\w$]+) L[^ ]*;', r'\1', p)
        if want is None or want.search(p):
            mods[p[:220]].add(parts[1])
for p, m in sorted(mods.items(), key=lambda kv: -len(kv[1]))[:count]:
    print(f'{len(m):4d}  {p}')

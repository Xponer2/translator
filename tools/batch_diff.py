"""Compares two corpus batch reports (tools/ladder.sh convert <folder> ... <report.tsv>): the mods that became clean,
the ones that stopped being clean (regressions), and the problems that appeared or went away, by kind.

usage: python -I tools/batch_diff.py <before.tsv> <after.tsv> [--problems]
"""
import collections
import re
import sys


def load(path):
    mods = {}
    for line in open(path, encoding='utf-8'):
        cols = line.rstrip('\n').split('\t')
        if len(cols) >= 2:
            mods[cols[1]] = set(p.strip() for p in cols[4].split(' | ')) - {''} if len(cols) > 4 else set()
    return mods


def kind(problem):
    p = re.sub(r' \(1\.[0-9.]+ -> 1\.[0-9.]+\)$', '', problem)
    p = re.sub(r'\(([^()]*)\).*$', '', p)
    return p[:120]


def main():
    before, after = load(sys.argv[1]), load(sys.argv[2])
    show = '--problems' in sys.argv
    clean_b = {m for m, p in before.items() if not p}
    clean_a = {m for m, p in after.items() if not p}
    print('clean: %d -> %d of %d' % (len(clean_b), len(clean_a), len(after)))
    print('problems: %d -> %d' % (sum(map(len, before.values())), sum(map(len, after.values()))))
    for m in sorted(clean_a - clean_b):
        print('  newly clean: ' + m)
    lost = sorted(clean_b - clean_a)
    for m in lost:
        print('  REGRESSION, no longer clean: %s (%s)' % (m, ' | '.join(sorted(after.get(m, {'missing from the report'}))[:3])))
    gained, gone = collections.Counter(), collections.Counter()
    for m in set(before) | set(after):
        b, a = before.get(m, set()), after.get(m, set())
        for p in a - b:
            gained[kind(p)] += 1
        for p in b - a:
            gone[kind(p)] += 1
    if show:
        print('\nproblems that appeared (by kind):')
        for k, n in gained.most_common(25):
            print('  +%d  %s' % (n, k))
        print('\nproblems that went away (by kind):')
        for k, n in gone.most_common(25):
            print('  -%d  %s' % (n, k))
    sys.exit(1 if lost else 0)


main()

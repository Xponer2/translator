"""Compares a test mod's results natively (the upper version) and converted (the lower one): the flat JSON files the
test mods write (test-a.json, test-b.json, test-c.json, test-d.json...). Every key must be there on both sides with the
same value.

usage: python -I tools/compare_results.py <native.json> <converted.json> [more pairs...]
"""
import json
import sys


def main():
    args = sys.argv[1:]
    if not args or len(args) % 2:
        sys.exit(__doc__)
    failed = False
    for native, converted in zip(args[0::2], args[1::2]):
        a = json.load(open(native, encoding='utf-8'))
        b = json.load(open(converted, encoding='utf-8'))
        diff = sorted(k for k in set(a) | set(b) if a.get(k) != b.get(k))
        print('%s: %d of %d results the same' % (converted, len(a) - len([k for k in diff if k in a]), len(a)))
        for k in diff:
            failed = True
            print('  %s: native %r, converted %r' % (k, a.get(k, '<missing>'), b.get(k, '<missing>')))
    sys.exit(1 if failed else 0)


main()

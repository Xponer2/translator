"""Which items of a version get which default data component (lambdas of Items too), from `javap -v -p -c net.minecraft.world.item.Items` on stdin.

usage: javap -v -p -c -cp <game jar> net.minecraft.world.item.Items | python -I tools/item_defaults.py [COMPONENT...]
"""
import re
import sys

text = sys.stdin.read()
# lambda bodies: name -> set of DataComponents fields they read
bodies = {}
for m in re.finditer(r'\n  (?:private )?static [^\n]*? (lambda\$static\$\d+)\([^\n]*\n(.*?)(?=\n\n|\n  [a-z])', text, re.S):
    bodies[m.group(1)] = set(re.findall(r'DataComponents\.([A-Z_]+)', m.group(2)))
# bootstrap index -> lambda name
boot = {}
bm = text[text.find('BootstrapMethods:'):]
for m in re.finditer(r'\n\s+(\d+): #\d+ REF_invokeStatic [^\n]*\n\s+Method arguments:\n(?:\s+#\d+ [^\n]*\n)*?\s+#\d+ REF_invokeStatic net/minecraft/world/item/Items\.(lambda\$static\$\d+)', bm):
    boot[int(m.group(1))] = m.group(2)
clinit = text[text.find('static {};'):]
pending = []
for line in clinit.split('\n'):
    m = re.search(r'InvokeDynamic #(\d+):apply', line)
    if m:
        pending.append(boot.get(int(m.group(1))))
    m = re.search(r'getstatic .*DataComponents\.([A-Z_]+)', line)
    if m:
        pending.append('=' + m.group(1))
    m = re.search(r'putstatic .*Field ([A-Z_0-9]+):Lnet/minecraft/world/item/Item;', line)
    if m:
        comps = set()
        for p in pending:
            if p is None:
                continue
            if p.startswith('='):
                comps.add(p[1:])
            else:
                comps |= bodies.get(p, set())
        want = set(sys.argv[1:])
        hit = comps & want if want else comps
        if hit:
            print(m.group(1), sorted(hit))
        pending = []

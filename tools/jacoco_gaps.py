"""Parse jacocoCoreReport.xml → per-class missed line/branch + totals."""
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1] if len(sys.argv) > 1 else 'app/build/reports/jacoco/core/jacocoCoreReport.xml'
t = ET.parse(path).getroot()
rows = []
total_line = total_br = 0
for pkg in t.findall('package'):
    for cl in pkg.findall('class'):
        name = cl.get('name').replace('/', '.')
        ml = mb = cl_cov = 0
        for m in cl.findall('method'):
            for c in m.findall('counter'):
                typ = c.get('type')
                if typ == 'LINE':
                    ml += int(c.get('missed')); cl_cov += int(c.get('covered'))
                elif typ == 'BRANCH':
                    mb += int(c.get('missed'))
        total_line += ml
        total_br += mb
        if ml > 0 or mb > 0:
            rows.append((ml, mb, name))
rows.sort(reverse=True)
for ml, mb, name in rows:
    print(f"{ml:4d} lines / {mb:4d} branches missed  {name}")
print(f"\nTOTAL missed lines={total_line}  branches={total_br}")

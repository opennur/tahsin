"""Print missed methods per class from jacocoCoreReport.xml."""
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1] if len(sys.argv) > 1 else 'app/build/reports/jacoco/core/jacocoCoreReport.xml'
filter_cls = sys.argv[2] if len(sys.argv) > 2 else None
t = ET.parse(path).getroot()
for pkg in t.findall('package'):
    for cl in pkg.findall('class'):
        name = cl.get('name').replace('/', '.')
        if filter_cls and filter_cls not in name:
            continue
        misses = []
        for m in cl.findall('method'):
            ml = sum(int(c.get('missed')) for c in m.findall('counter') if c.get('type') == 'LINE')
            if ml > 0:
                misses.append(f"  {m.get('name')}{m.get('desc')}: {ml} lines")
        if misses:
            print(name)
            print('\n'.join(misses))

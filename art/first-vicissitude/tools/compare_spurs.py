"""Compare the saved pre-edit runtime mesh with the current export."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
before = json.loads((ROOT / 'build/spur-review/before.mesh.json').read_text())
after = json.loads((ROOT / 'src/main/resources/assets/maledict/models/entity/first_vicissitude.mesh.json').read_text())
old = {p['name']: p for p in before['parts']}
new = {p['name']: p for p in after['parts']}
assert old.keys() == new.keys()
changed = []
for name, part in old.items():
    current = new[name]
    if part == current:
        continue
    assert name.startswith(('Fractured crystal shell', 'Crystal edge clasp', 'Vestment pennant')), name
    def vertices(p):
        return {tuple(v[:3]) for t in p['triangles'] for v in t['v']}
    a, b = vertices(part), vertices(current)
    changed.append({'name': name, 'before': len(part['triangles']), 'after': len(current['triangles']), 'common_vertices': len(a & b)})
report = {'changed_parts': changed, 'unchanged_parts': len(old)-len(changed),
          'before_triangles': sum(len(p['triangles']) for p in old.values()),
          'after_triangles': sum(len(p['triangles']) for p in new.values())}
target = ROOT / 'art/first-vicissitude/preview/blender/spur-comparison/geometry-comparison.json'
target.write_text(json.dumps(report, indent=2)+'\n')
print(json.dumps(report, indent=2))

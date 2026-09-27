"""Verify the optimization against the saved runtime mesh, without rendering."""
import json
from pathlib import Path
from collections import Counter

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT/'build/first-vicissitude-review'
old = json.loads((OUT/'baseline/first_vicissitude.mesh.json').read_text())
new = json.loads((ROOT/'src/main/resources/assets/maledict/models/entity/first_vicissitude.mesh.json').read_text())
before = {p['name']:p for p in old['parts']}
after = {p['name']:p for p in new['parts']}
assert before.keys() == after.keys()
audit = json.loads((OUT/'optimization-audit.json').read_text())
buried = {p['part'] for p in audit['faces']}
curves = ('Fate arc', 'Fate ring stock', 'Ossuary socket seat', 'High crescent',
          'Middle crescent', 'Low crescent', 'Trailing crescent', 'Root upper hook',
          'Upper hooked spur', 'Middle returning spur', 'Low returning spur', 'Trailing inner spur')
changed = []
def triangles(part):
    return Counter(json.dumps(t, sort_keys=True) for t in part['triangles'])
for name, a in before.items():
    b = after[name]
    for field in ('joint', 'shed_delay', 'deploy_scale'):
        assert a.get(field) == b.get(field), (name, field)
    if a == b:
        continue
    assert name in buried or name.startswith(curves), name
    if name in buried:
        assert not triangles(b)-triangles(a), 'Surviving surface changed: '+name
    assert 'shed_delay' not in a, 'Feather geometry and its rotation center must remain unchanged'
    changed.append(dict(name=name, before=len(a['triangles']), after=len(b['triangles'])))
report = dict(before=sum(len(p['triangles']) for p in before.values()),
              after=sum(len(p['triangles']) for p in after.values()),
              unchanged_parts=len(before)-len(changed), changed=changed,
              feather_geometry_uv_and_centers_unchanged=True,
              surviving_buried_part_faces_identical=True)
(OUT/'optimization-comparison.json').write_text(json.dumps(report, indent=2))
print(json.dumps(report, indent=2))

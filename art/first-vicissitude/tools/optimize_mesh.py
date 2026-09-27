"""Remove fully buried polygons inside opaque solids on the same rigid joint.

The intersection of a closed mesh's inward triangle halfspaces is a conservative
interior cell, even for a concave mesh. Never use its convex hull as an occluder.
Keep original vertices, surviving polygon loops, UVs and surface-island names.
"""
import json
import sys
from collections import Counter
from pathlib import Path
import bpy
sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).parent))
from audit_internal_faces import enclosed


def solid_cell(obj):
    if obj.get('material_index') in (7, 9, 10, 11, 12) or 'shed_delay' in obj:
        return None
    data = obj.data
    edges = Counter(tuple(sorted((a, b))) for p in data.polygons
                    for a, b in zip(p.vertices, list(p.vertices[1:])+[p.vertices[0]]))
    if not edges or any(n != 2 for n in edges.values()):
        return None
    data.calc_loop_triangles()
    transform = obj.matrix_parent_inverse @ obj.matrix_basis
    points = [transform @ v.co for v in data.vertices]
    planes = []
    for tri in data.loop_triangles:
        a, b, c = [points[i] for i in tri.vertices]
        normal = (b-a).cross(c-a).normalized()
        if normal.length < .5:
            return None
        # Require a small burial margin; coplanar exterior surfaces stay intact.
        planes.append((a-normal*1e-4, normal))
    return planes


def optimize(objects, apply=True):
    cells = {o.name: solid_cell(o) for o in objects}
    report = []
    for obj in objects:
        if 'shed_delay' in obj:
            continue
        covers = [cells[o.name] for o in objects if o != obj and cells[o.name]
                  and o['runtime_joint'] == obj['runtime_joint']
                  and o.get('deploy_scale', 1) == obj.get('deploy_scale', 1)]
        if not covers:
            continue
        transform = obj.matrix_parent_inverse @ obj.matrix_basis
        removed = []
        for poly in obj.data.polygons:
            points = [transform @ obj.data.vertices[i].co for i in poly.vertices]
            if enclosed(points, covers):
                removed.append(poly.index)
                report.append(dict(part=obj.name, polygon=poly.index, triangles=len(poly.vertices)-2))
        if not removed or not apply:
            continue
        old = obj.data
        kept = [p for p in old.polygons if p.index not in removed]
        assert kept, 'Keep an object with no surviving faces for manual review'
        data = bpy.data.meshes.new(old.name+' optimized')
        data.from_pydata([v.co[:] for v in old.vertices], [], [list(p.vertices) for p in kept])
        for material in old.materials:
            data.materials.append(material)
        for old_layer in old.uv_layers:
            layer = data.uv_layers.new(name=old_layer.name)
            for new_poly, old_poly in zip(data.polygons, kept):
                for new_loop, old_loop in zip(new_poly.loop_indices, old_poly.loop_indices):
                    layer.data[new_loop].uv = old_layer.data[old_loop].uv
        for new_poly, old_poly in zip(data.polygons, kept):
            new_poly.material_index = old_poly.material_index
            new_poly.use_smooth = old_poly.use_smooth
        if 'surface_islands' in obj:
            islands = json.loads(obj['surface_islands'])
            obj['surface_islands'] = json.dumps([islands[p.index] for p in kept])
        obj.data = data
    return dict(removed_triangles=sum(p['triangles'] for p in report), removed_polygons=len(report), faces=report)


if __name__ == '__main__':
    objects = [o for o in bpy.context.scene.objects if o.type == 'MESH' and 'runtime_joint' in o]
    result = optimize(objects, apply=False)
    out = Path(__file__).resolve().parents[3]/'build/first-vicissitude-review/optimization-audit.json'
    out.write_text(json.dumps(result, indent=2))
    print(json.dumps(result, indent=2))

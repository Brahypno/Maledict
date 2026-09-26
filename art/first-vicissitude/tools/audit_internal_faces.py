"""Read-only audit of faces enclosed in a solid plate on the same rigid joint.

Triangular prisms partition concave extruded plates without using their larger
convex hull. Subtract their union from each candidate face to check complete
coverage, rather than relying on a camera view or testing vertices alone.
"""
import json
import bpy
from mathutils import Vector
from mathutils.geometry import tessellate_polygon

PLATES=('Pectoral remnant','Thoracic remnant','Broken dorsal scapula',
        'Upper sternal keel','Lower ossuary bridge','Ventral ossuary segment',
        'Dorsal vertebral plate','Transverse ossuary flange')
CANDIDATES=PLATES+('Thoracic load arch','Clavicular bone arch','Scapular wing load bridge',
                  'Cervical fork','Lumbar connection','Wrapping costal arch','Vertebral spindle',
                  'Pectoral bone ridge')

def prisms(obj):
    vs=[obj.matrix_basis @ v.co for v in obj.data.vertices]
    n=len(vs)//2
    front=vs[:n]
    for triangle in tessellate_polygon([front]):
        ids=[v if isinstance(v,int) else min(range(n),key=lambda i:(front[i]-v).length_squared) for v in triangle]
        points=[vs[i] for i in ids]+[vs[i+n] for i in ids]
        center=sum(points,Vector())/6
        planes=[]
        for face in ((0,1,2),(3,5,4),(0,3,4,1),(1,4,5,2),(2,5,3,0)):
            a,b,c=[points[i] for i in face[:3]]
            normal=(b-a).cross(c-a).normalized()
            if normal.dot(center-a)>0: normal=-normal
            planes.append((a,normal))
        yield planes

def audit(objects):
    plates=[o for o in objects if o.name.startswith(PLATES)]
    cells={o.name:list(prisms(o)) for o in plates}
    report=[]
    checked=0
    for obj in objects:
        if not obj.name.startswith(CANDIDATES): continue
        # Distinct animated joints must never occlude one another for this proof.
        covers=[o for o in plates if o!=obj and o['runtime_joint']==obj['runtime_joint']]
        for poly in obj.data.polygons:
            checked+=1
            points=[obj.matrix_basis @ obj.data.vertices[i].co for i in poly.vertices]
            cover=next((o for o in covers if enclosed(points,cells[o.name])),None)
            if cover:
                report.append(dict(part=obj.name,face=poly.index,triangles=len(poly.vertices)-2,
                                   occluder=cover.name,joint=obj['runtime_joint']))
    return dict(checked_faces=checked,occluding_plates=len(plates),
                eligible_triangles=sum(r['triangles'] for r in report),faces=report,
                proof='complete face covered by the union of solid plate prisms on the same rigid joint')

def area(points):
    return sum((points[i]-points[0]).cross(points[i+1]-points[0]).length/2
               for i in range(1,len(points)-1)) if len(points)>=3 else 0

def clip(points,a,normal,inside):
    result=[]
    for p,q in zip(points,points[1:]+points[:1]):
        d,e=normal.dot(p-a),normal.dot(q-a)
        accept=d<=0 if inside else d>=0
        if accept: result.append(p)
        if (d<0 and e>0) or (d>0 and e<0): result.append(p.lerp(q,d/(d-e)))
    return result

def enclosed(points,cells):
    fragments=[]
    for tri in tessellate_polygon([points]):
        fragments.append([points[v] if isinstance(v,int) else v for v in tri])
    original_area=sum(area(p) for p in fragments)
    if original_area<1e-8: return False
    for planes in cells:
        remaining=[]
        for polygon in fragments:
            inside=polygon
            for a,normal in planes:
                outside=clip(inside,a,normal,False)
                if area(outside)>1e-8: remaining.append(outside)
                inside=clip(inside,a,normal,True)
                if area(inside)<1e-8: break
        fragments=remaining
        if not fragments: return True
    return False

if __name__=='__main__':
    # Analytic controls: buried, outside, and crossing despite a center inside.
    cube=[(Vector((s if k==0 else 0,s if k==1 else 0,s if k==2 else 0)),
           Vector((s if k==0 else 0,s if k==1 else 0,s if k==2 else 0)))
          for k in range(3) for s in (-1,1)]
    assert enclosed([Vector((0,0,0)),Vector((.5,0,0)),Vector((0,.5,0))],[cube])
    assert not enclosed([Vector((2,0,0)),Vector((3,0,0)),Vector((2,.5,0))],[cube])
    assert not enclosed([Vector((-2,0,0)),Vector((2,0,0)),Vector((0,.5,0))],[cube])
    objects=[o for o in bpy.context.scene.objects if o.type=='MESH' and 'runtime_joint' in o]
    result=audit(objects)
    from pathlib import Path
    out=Path(__file__).resolve().parents[3]/'build/first-vicissitude-review/internal-face-audit.json'
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,indent=2))
    print(json.dumps(result,indent=2))

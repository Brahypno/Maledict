"""Count runtime triangles and optional external Java/Bedrock geometry without copying it."""
import argparse
import collections
import json
import re
from pathlib import Path

def group(p):
    name=p['name']
    if 'shed_delay' in p: return 'feathers'
    if any(name.startswith(s) for s in ('Fate ring','Ossuary socket','Socket lip')): return 'chest_rings'
    if p['joint'].startswith('halo_'): return 'halo'
    if p['joint'].startswith('wing_'): return 'bone_wings'
    if any(s in p['joint'] for s in ('arm','hand')): return 'arms'
    return 'body_head_tail'

def runtime(path):
    parts=json.loads(path.read_text())['parts']
    groups=collections.Counter()
    for p in parts: groups[group(p)]+=len(p['triangles'])
    return dict(triangles=sum(groups.values()),groups=dict(groups),parts=len(parts),
                phase_two=sum(len(p['triangles']) for p in parts if 'shed_delay' not in p))

def reference(path):
    boxes=[]
    explicit_faces=[]
    if path.suffix=='.java':
        text=re.sub(r'/\*.*?\*/|//[^\n]*','',path.read_text(encoding='utf-8'),flags=re.S)
        number=r'(-?\d+(?:\.\d+)?)[Ff]?'
        pattern=r'\.addBox\(\s*'+r'\s*,\s*'.join([number]*6)
        matches=list(re.finditer(pattern,text))
        assert len(matches)==len(re.findall(r'\.addBox\(',text)), 'Unsupported addBox overload; inspect manually'
        for m in matches:
            dims=[float(v) for v in m.groups()[3:6]]
            tail=text[m.end():]
            deformation=re.match(r'\s*,\s*(?:new CubeDeformation\(\s*)?'+number,tail)
            inflate=float(deformation.group(1)) if deformation else 0
            boxes.append([d+2*inflate for d in dims])
    else:
        data=json.loads(path.read_text())
        for geometry in data['minecraft:geometry']:
            for bone in geometry['bones']:
                for cube in bone.get('cubes',[]):
                    dims=[d+2*cube.get('inflate',0) for d in cube['size']]
                    boxes.append(dims)
                    faces=cube['uv'] if isinstance(cube['uv'],dict) else dict.fromkeys(('north','south','east','west','up','down'))
                    areas={'north':dims[0]*dims[1],'south':dims[0]*dims[1],
                           'east':dims[2]*dims[1],'west':dims[2]*dims[1],
                           'up':dims[0]*dims[2],'down':dims[0]*dims[2]}
                    explicit_faces.extend(areas[f] for f in faces)
    # Six quads per ordinary cube; a zero-width sheet contributes only two nondegenerate faces.
    zeros=collections.Counter(sum(abs(d)<1e-8 for d in b) for b in boxes)
    return dict(boxes=len(boxes),nominal_triangle_slots=len(explicit_faces)*2 if explicit_faces else len(boxes)*12,
                nondegenerate_triangle_equivalent=sum(abs(a)>1e-8 for a in explicit_faces)*2 if explicit_faces else zeros[0]*12+zeros[1]*4,
                zero_dimension_boxes=dict(zeros),scope='model definitions, not per-frame GPU measurement')

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--mesh',type=Path)
    parser.add_argument('--reference',type=Path,action='append',default=[])
    parser.add_argument('--output',type=Path)
    args=parser.parse_args()
    result={}
    if args.mesh: result['runtime']=runtime(args.mesh)
    for p in args.reference: result[p.stem]=reference(p)
    payload=json.dumps(result,indent=2)
    print(payload)
    if args.output: args.output.write_text(payload)

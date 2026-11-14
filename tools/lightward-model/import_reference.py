"""Import exact Malum 6646111 ModelPart geometry for editable/offline exports.

The original geometry is used for offline exports and comparisons.
Run with Blender --background --python tools/lightward-model/import_reference.py.
"""
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'build/malum-decompiled/com/sammy/malum/client/model'
NUMBER = r'[-+]?(?:\d+\.?\d*|\.\d+)[fF]?'


def numbers(text):
    return [float(s.rstrip('fF')) for s in text.split(',') if s.strip()]


def parse(name, texture, keep):
    path = SOURCE / (name+'.java')
    source = path.read_text(encoding='utf-8')
    aliases = {var: child for var, child in re.findall(
        r'PartDefinition\s+(\w+)\s*=\s*root.getChild\("([^"]+)"\)', source)}
    nodes = {}
    pattern = re.compile(r'PartDefinition\s+(\w+)\s*=\s*(\w+)\.addOrReplaceChild\(\s*"([^"]+)"\s*,(.*?)PartPose\.(offsetAndRotation|offset)\(([^)]*)\)\s*\);', re.S)
    tokens = re.compile(
        r'\.texOffs\(\s*(\d+)\s*,\s*(\d+)\s*\)'
        r'|\.mirror\((false)?\)'
        r'|\.addBox\(\s*('+NUMBER+r'(?:\s*,\s*'+NUMBER+r'){5})\s*,\s*new CubeDeformation\(('+NUMBER+r')\)\s*\)')
    for match in pattern.finditer(source):
        variable, parent, part, builder, pose_kind, pose = match.groups()
        if parent == 'root':
            # Separate glow roots are not drawn when the original model has no active runes.
            continue
        parent = aliases.get(parent, parent)
        cubes, uv, mirror = [], [0, 0], False
        for token in tokens.finditer(builder):
            if token.group(1) is not None:
                uv = [int(token.group(1)), int(token.group(2))]
            elif token.group(0).startswith('.mirror'):
                mirror = token.group(3) is None
            else:
                values = numbers(token.group(4))
                cubes.append(dict(pos=values[:3],size=values[3:],uv=uv[:],
                                  inflate=float(token.group(5).rstrip('fF')),mirror=mirror))
        assert len(cubes) == builder.count('.addBox('), (part, 'unparsed cube')
        values = numbers(pose)
        nodes[variable] = dict(name=part,parent=parent,offset=values[:3],
                               rotation=values[3:] if pose_kind == 'offsetAndRotation' else [0,0,0],cubes=cubes)
    def bone(key):
        parent = nodes[key]['parent']
        return bone(parent) if parent in nodes else parent
    selected = {key: value for key,value in nodes.items() if bone(key) in keep}
    return dict(class_name=name,texture=texture,nodes=selected,
                decompiled_source_sha256=hashlib.sha256(path.read_bytes()).hexdigest())


data = dict(dependency='curse.maven:malum-484064:6646111',
            helmet=parse('SoulStainedSteelArmorModel','soul_stained_steel_reforged',{'head'}),
            chestplate=parse('MalignantStrongholdArmorModel','malignant_stronghold',{'body','left_arm','right_arm'}),
            leggings=parse('SoulStainedSteelArmorModel','soul_stained_steel_reforged',
                           {'leggings','left_legging','right_legging'}),
            boots=parse('SoulStainedSteelArmorModel','soul_stained_steel_reforged',{'left_foot','right_foot'}))
target = Path(__file__).with_name('reference_geometry.json')
target.write_text(json.dumps(data,indent=2),encoding='utf-8')
for slot in ('helmet','chestplate','leggings','boots'):
    nodes = data[slot]['nodes']
    print(slot, len(nodes),'parts,',sum(len(n['cubes']) for n in nodes.values()),'original cubes')

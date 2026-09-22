"""Validate the saved deliverable and render grey/detail/night review images."""
import bpy
import json
import math
import sys
import shutil
from pathlib import Path
from mathutils import Vector, Matrix

ROOT = Path(__file__).resolve().parents[3]
ART = ROOT/'art/first-vicissitude'
OUT = ART/'preview/blender'
scene = bpy.context.scene
scene.frame_set(1)
bpy.context.view_layer.update()
data = json.loads((ROOT/'src/main/resources/assets/maledict/models/entity/first_vicissitude.mesh.json').read_text())
meshes = {o.name:o for o in scene.objects if o.type=='MESH' and 'runtime_joint' in o}
assert len(meshes)==len(data['parts'])
count = 0
wing_samples = set()
for part in data['parts']:
    obj = meshes[part['name']]
    obj.data.calc_loop_triangles()
    assert len(obj.data.loop_triangles)==len(part['triangles'])
    for tri in part['triangles']:
        assert all(math.isfinite(c) for v in tri['v'] for c in v)
        assert abs(Vector(tri['n']).length-1)<.0001
        for v in tri['v']:
            assert 0<=v[3]<=1 and 0<=v[4]<=1
            if part['joint'].startswith('wing_'):
                wing_samples.add(part['joint'].upper()+','+','.join(str(c) for c in v[:3])+(','+'1' if 'shed_delay' in part else ',0'))
        count+=1
(ROOT/'build/rig-tool').mkdir(parents=True,exist_ok=True)
(ROOT/'build/rig-tool/wing-vertices.csv').write_text('\n'.join(sorted(wing_samples)))

depsgraph = bpy.context.evaluated_depsgraph_get()
aperture = []
for x in (-.6,0,.6):
    for z in (7,9,11,13):
        hit,*_ = scene.ray_cast(depsgraph,Vector((x,-100,z)),Vector((0,1,0)))
        aperture.append(not hit)
assert all(aperture), 'Geometry obstructs the central chest aperture'
for x,z in [(0,17),(8,10),(-8,10),(0,0)]:
    hit,*_=scene.ray_cast(depsgraph,Vector((x,-100,z)),Vector((0,1,0)))
    assert hit, 'Pectoral/flank/abdominal body mass is missing around the aperture'
hub=json.loads(scene['runtime_rig'])['chest_hub']
for i in range(32):
    a=math.tau*i/32
    hit,*_=scene.ray_cast(depsgraph,Vector((hub[0]+3.9*math.cos(a),-100,-hub[1]+3.9*math.sin(a))),Vector((0,1,0)))
    assert not hit, 'Circular chest opening is obstructed away from its centerline'
# Check exported ring geometry against its actual runtime rotation hub, not just its pivots.
rig=json.loads(scene['runtime_rig'])
pivots={j['name']:j['pivot'] for j in rig['joints']}
ring_radii=[]
ring_samples=set()
for part in data['parts']:
    if part['name'].startswith('Fate ring stock'):
        pivot=pivots[part['joint']]
        ring_radii += [math.hypot(v[0]+pivot[0]-hub[0],v[1]+pivot[1]-hub[1])
                       for tri in part['triangles'] for v in tri['v']]
        ring_samples.update(part['joint'].upper()+','+','.join(str(c) for c in v[:3])
                            for tri in part['triangles'] for v in tri['v'])
assert ring_radii and min(ring_radii)>4.4 and max(ring_radii)<5.6, 'Ring stock is eccentric to the runtime hub'
(ROOT/'build/rig-tool/chest-ring-vertices.csv').write_text('\n'.join(sorted(ring_samples)))
atlas=bpy.data.images.load(str(ROOT/'src/main/resources/assets/maledict/textures/entity/first_vicissitude.png'),check_existing=False)
pixels=atlas.pixels[:]
bone=pixels[(32*256+160)*4:(32*256+160)*4+3]
violet=pixels[(32*256+96)*4:(32*256+96)*4+3]
assert min(bone)>.60 and sum(bone)/3-sum(violet)/3>.25, 'The bone atlas is darkened or lacks material contrast'
assert violet[2]>violet[0]>violet[1], 'The violet shell hue has been lost'
# Cutout feather materials must keep real gaps and cannot accidentally become emissive cards.
feather_alpha=[]
for tile in range(9,13):
    values=[pixels[(y*256+x)*4+3]
            for y in range((tile//4)*64+3,(tile//4)*64+61)
            for x in range((tile%4)*64+3,(tile%4)*64+61)]
    assert min(values)==0 and max(values)==1, 'Feather cutout lost its opaque/transparent pixels'
    feather_alpha.append(sum(v==0 for v in values))
bpy.data.images.remove(atlas)

# Sample the core's actual projected extent, rather than merely inspecting a pretty render.
core = [o for o in meshes.values() if o['runtime_joint']=='head_core']
points = [o.matrix_world@v.co for o in core for v in o.data.vertices]
min_x,max_x = min(p.x for p in points),max(p.x for p in points)
min_z,max_z = min(p.z for p in points),max(p.z for p in points)
visible,total = 0,0
for i in range(15):
    for k in range(21):
        x=min_x+(max_x-min_x)*(i+.5)/15
        z=min_z+(max_z-min_z)*(k+.5)/21
        hit,loc,n,index,obj,matrix=scene.ray_cast(depsgraph,Vector((x,-100,z)),Vector((0,1,0)))
        if hit:
            total+=1
            visible+=obj.get('runtime_joint')=='head_core'
assert visible/max(total,1)<.5, 'The head shell exposes most of the core'
report={'mesh_parts':len(meshes),'triangles':count,'finite_vertices_and_unit_normals':True,
        'uvs_in_atlas':True,'chest_clear_rays':sum(aperture),'chest_sample_rays':len(aperture),
        'front_core_visible_fraction':visible/max(total,1),
        'surrounding_body_mass_present':True,'bone_srgb_sample':bone,'violet_srgb_sample':violet,
        'feather_cutout_clear_pixels':feather_alpha,
        'circular_socket_clear_rays':32,'chest_ring_radius_range':[min(ring_radii),max(ring_radii)]}
(OUT/'validation.json').write_text(json.dumps(report,indent=2))
print('VALIDATION',json.dumps(report),flush=True)
if '--check-only' in sys.argv:
    sys.exit(0)

camera=scene.camera
def view(name,pos,target,scale,frame=1):
    scene.frame_set(frame)
    camera.location=pos
    camera.rotation_euler=(Vector(target)-camera.location).to_track_quat('-Z','Y').to_euler()
    camera.data.ortho_scale=scale
    scene.render.filepath=str(OUT/(name+'.png'))
    bpy.ops.render.render(write_still=True)
    print('REVIEW_DONE',name,flush=True)

scene.frame_set(1)
bpy.context.view_layer.update()
hand_center=(bpy.data.objects['hand_left'].matrix_world.translation+
             bpy.data.objects['forearm_left'].matrix_world.translation)*.5
if '--views-only' not in sys.argv:
    view('hand_and_elbow_detail',hand_center+Vector((22,-100,15)),hand_center,29)

for phase,frame in ([] if '--details-only' in sys.argv else [('phase_one',1),('phase_two',41)]):
    for direction,position,scale in [('front',(0,-210,10),141),
                                     ('side',(210,0,10),110),('back',(0,210,10),141)]:
        name=phase+'_'+direction
        view(name,position,(0,0,10),scale,frame)
        shutil.copyfile(OUT/(name+'.png'),ART/'preview'/(name+'.png'))

if '--views-only' in sys.argv:
    sys.exit(0)
view('chest_and_crown_detail',(18,-130,30),(0,0,21),66)
view('death_reveal',(60,-190,50),(0,0,10),141,81)
view('wing_front_detail',(40,-160,34),(36,10,15),72)
view('wing_back_detail',(40,170,34),(36,10,15),72)
# Keep the alpha silhouette when checking a cutout wing in clay.
original_materials={obj.name:list(obj.data.materials) for obj in meshes.values()}
for obj in meshes.values():
    material=obj.data.materials[0].copy()
    shader=material.node_tree.nodes.get('Principled BSDF')
    for link in list(material.node_tree.links):
        if link.to_socket in (shader.inputs['Base Color'],shader.inputs['Emission Color']):
            material.node_tree.links.remove(link)
    shader.inputs['Base Color'].default_value=(.32,.32,.32,1)
    shader.inputs['Emission Strength'].default_value=0
    shader.inputs['Metallic'].default_value=0
    shader.inputs['Roughness'].default_value=.8
    obj.data.materials.clear()
    obj.data.materials.append(material)
view('grey_front',(0,-210,10),(0,0,10),141)
view('grey_side',(210,0,10),(0,0,10),110)
view('grey_back',(0,210,10),(0,0,10),141)
for obj in meshes.values():
    obj.data.materials.clear()
    for material in original_materials[obj.name]:
        obj.data.materials.append(material)
scene.world.node_tree.nodes.get('Background').inputs[1].default_value=.15
for obj in scene.objects:
    if obj.type=='LIGHT': obj.data.energy*=.20
view('night',(65,-190,48),(0,0,10),141)

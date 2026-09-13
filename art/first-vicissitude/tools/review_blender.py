"""Validate the saved deliverable and render grey/detail/night review images."""
import bpy
import json
import math
import sys
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
                wing_samples.add(part['joint'].upper()+','+','.join(str(c) for c in v[:3]))
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
for x,z in [(0,17),(5,10),(-5,10),(0,3)]:
    hit,*_=scene.ray_cast(depsgraph,Vector((x,-100,z)),Vector((0,1,0)))
    assert hit, 'Pectoral/flank/abdominal body mass is missing around the aperture'
atlas=bpy.data.images.load(str(ROOT/'src/main/resources/assets/maledict/textures/entity/first_vicissitude.png'),check_existing=False)
pixels=atlas.pixels[:]
bone=pixels[(32*256+160)*4:(32*256+160)*4+3]
violet=pixels[(32*256+96)*4:(32*256+96)*4+3]
assert min(bone)>.60 and sum(bone)/3-sum(violet)/3>.25, 'The bone atlas is darkened or lacks material contrast'
assert violet[2]>violet[0]>violet[1], 'The violet shell hue has been lost'
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
        'surrounding_body_mass_present':True,'bone_srgb_sample':bone,'violet_srgb_sample':violet}
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

view('chest_and_crown_detail',(18,-130,30),(0,0,21),66)
view('death_reveal',(60,-190,50),(0,0,10),141,81)
grey=bpy.data.materials.new('Review clay')
grey.use_nodes=True
grey.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value=(.32,.32,.32,1)
grey.node_tree.nodes.get('Principled BSDF').inputs['Roughness'].default_value=.8
scene.view_layers[0].material_override=grey
view('grey_front',(0,-210,10),(0,0,10),141)
view('grey_side',(210,0,10),(0,0,10),110)
view('grey_back',(0,210,10),(0,0,10),141)
scene.view_layers[0].material_override=None
scene.world.node_tree.nodes.get('Background').inputs[1].default_value=.15
for obj in scene.objects:
    if obj.type=='LIGHT': obj.data.energy*=.20
view('night',(65,-190,48),(0,0,10),141)

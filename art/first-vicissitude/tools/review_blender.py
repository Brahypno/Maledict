"""Validate the saved deliverable and render grey/detail/night review images."""
import bpy
import json
import math
import sys
import time
import uuid
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent))
from preview_paths import DIAGNOSTICS, image_target
from mathutils import Vector, Matrix

ROOT = Path(__file__).resolve().parents[3]
ART = ROOT/'art/first-vicissitude'
OUT = DIAGNOSTICS
OUT.mkdir(parents=True,exist_ok=True)
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
    for source,exported in zip(obj.data.loop_triangles,part['triangles']):
        for loop,vertex in zip(source.loops,exported['v']):
            uv=obj.data.uv_layers.active.data[loop].uv
            assert abs(uv.x-vertex[3])<.000001 and abs(1-uv.y-vertex[4])<.000001, 'Runtime UV differs from Blender'
    for tri in part['triangles']:
        assert all(math.isfinite(c) for v in tri['v'] for c in v)
        assert abs(Vector(tri['n']).length-1)<.0001
        for v in tri['v']:
            assert 0<=v[3]<=1 and 0<=v[4]<=1
            if part['joint'].startswith('wing_'):
                wing_samples.add(part['joint'].upper()+','+','.join(str(c) for c in v[:3])+(','+'1' if 'shed_delay' in part else ',0')+','+str(part.get('deploy_scale',1)))
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
layout=json.loads((OUT/'atlas-layout.json').read_text())
atlas_size=layout['size']
regions={r['name']:r for r in layout['regions']}
occupied=set()
for region in regions.values():
    cells={(x,y) for x in range(region['x'],region['x']+region['width'])
           for y in range(region['y'],region['y']+region['height'])}
    assert all(0<=x<atlas_size and 0<=y<atlas_size for x,y in cells)
    assert not occupied & cells, 'Atlas regions overlap'
    occupied.update(cells)
for obj in meshes.values():
    islands=json.loads(obj['surface_islands']) if 'surface_islands' in obj else None
    for poly in obj.data.polygons:
        region=regions[islands[poly.index] if islands else obj['atlas_region']]
        for loop in poly.loop_indices:
            uv=obj.data.uv_layers.active.data[loop]
            x,y=uv.uv[0]*atlas_size,uv.uv[1]*atlas_size
            assert region['x']<=x<region['x']+region['width']
            assert region['y']<=y<region['y']+region['height'], 'UV escapes its surface island'
mirror_pairs=0
for obj in meshes.values():
    if 'surface_islands' not in obj or 'left' not in obj.name: continue
    other=meshes[obj.name.replace('left','right')]
    assert set(json.loads(obj['surface_islands']))==set(json.loads(other['surface_islands'])), 'Mirrored parts must reuse matching surface islands'
    mirror_pairs+=1
ring_parts=[o for o in meshes.values() if o.name.startswith(('Fate ring stock','Ossuary socket seat','Fate arc'))]
assert len(ring_parts)==11, 'Missing structural ring segments'
assert not any(o.name.startswith(('Socket lip','Fate ring inset','Fate ring clasp',
                                 'Halo silver rail','Halo inner recess','Halo transverse seal')) for o in meshes.values()), 'Decorative ring geometry remains'
for obj in ring_parts:
    names=set(json.loads(obj['surface_islands']))
    assert {regions[n]['surface'] for n in names}=={'front','back','inner','outer','cap0','cap1'}, 'Ring surfaces share or lack required UV islands'
def color(region,x,y):
    i=((region['y']+y)*atlas_size+region['x']+x)*4
    return pixels[i:i+3]
bone=max((color(r,x,y) for r in regions.values() if r['material']==14
          for x in range(r['width']) for y in range(r['height'])),key=sum)
violet_region=next(r for r in regions.values() if r['material']==13 and r.get('surface')=='front')
violet_samples=sorted((color(violet_region,x,y) for x in range(violet_region['width'])
                       for y in range(violet_region['height'])),key=sum)
violet=violet_samples[len(violet_samples)//2]
assert min(bone)>.60 and sum(bone)/3-sum(violet)/3>.25, 'Bone/shell contrast lost'
assert violet[2]>violet[0]>violet[1], 'The violet shell hue has been lost'
feather_alpha=[]
for region in regions.values():
    if not 9<=region['material']<=12: continue
    values=[pixels[((region['y']+y)*atlas_size+region['x']+x)*4+3]
            for y in range(region['height']) for x in range(16)]
    assert min(values)==0 and max(values)==1, 'Feather cutout lost its gaps'
    feather_alpha.append(sum(v==0 for v in values))
bpy.data.images.remove(atlas)

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
report={'atlas_regions':len(regions),'atlas_occupied_pixels':len(occupied),
        'authored_surface_parts':sum('surface_islands' in o for o in meshes.values()),
        'shared_mirror_pairs':mirror_pairs,'runtime_uv_matches_blender':True,
        'painted_ring_segments':len(ring_parts),'ring_trim_geometry_removed':True,
        'mesh_parts':len(meshes),'triangles':count,'finite_vertices_and_unit_normals':True,
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
def view(name,pos,target,scale,frame=1,ring_turn=0):
    scene.frame_set(frame)
    turned=[]
    if ring_turn:
        for name_joint in ('chest_ring_left','chest_ring_right','chest_ring_bottom','halo_root'):
            obj=bpy.data.objects[name_joint]
            action=obj.animation_data.action if obj.animation_data else None
            original=obj.matrix_basis.copy()
            turned.append((obj,original,action))
            if action: obj.animation_data.action=None
            angle=math.radians(ring_turn*(-1 if name_joint=='halo_root' else 1))
            rotation=Matrix.Rotation(angle,4,'Y')
            if name_joint.startswith('chest_ring_'):
                local=Vector((hub[0],hub[1],0))-Vector(pivots['torso'])
                center=Vector((local.x,local.z,-local.y))
                obj.matrix_basis=Matrix.Translation(center) @ rotation @ Matrix.Translation(-center) @ original
                # Segment pivots are offset from the shared hub; rotation must stay concentric.
                for child in obj.children:
                    if child.type!='MESH': continue
                    for vertex in child.data.vertices:
                        point=child.matrix_local @ vertex.co
                        before=original @ point-center
                        after=obj.matrix_basis @ point-center
                        assert abs(math.hypot(before.x,before.z)-math.hypot(after.x,after.z))<.0001
            else:
                obj.matrix_basis=original @ rotation
    camera.location=pos
    camera.rotation_euler=(Vector(target)-camera.location).to_track_quat('-Z','Y').to_euler()
    camera.data.ortho_scale=scale
    destination=image_target(name)
    scene.render.filepath=str(destination)
    # Render to a temporary path first: overwriting a PNG open in a Windows preview can fail.
    bpy.ops.render.render(write_still=False)
    temporary=ROOT/'build/rig-tool'/('review-'+uuid.uuid4().hex+'.png')
    bpy.data.images['Render Result'].save_render(str(temporary),scene=scene)
    for attempt in range(4):
        try:
            temporary.replace(destination)
            break
        except PermissionError:
            if attempt==3: raise
            time.sleep(.25)
    print('REVIEW_DONE',name,flush=True)
    for obj,basis,action in turned:
        obj.matrix_basis=basis
        if action: obj.animation_data.action=action

scene.frame_set(1)
bpy.context.view_layer.update()
hand_center=(bpy.data.objects['hand_left'].matrix_world.translation+
             bpy.data.objects['forearm_left'].matrix_world.translation)*.5
if '--lower-only' in sys.argv:
    scene.cycles.samples=20
    for phase,frame in [('phase_one',1),('phase_two',41)]:
        view(phase+'_hero',(65,-190,48),(0,0,10),175,frame)
        for direction,position in [('front',(20,-130,8)),('back',(-20,130,8)),('side',(130,-20,8))]:
            view('lower_'+phase+'_'+direction,position,(0,2,-6),52,frame)
    view('death_reveal',(60,-190,50),(0,0,10),175,81)
    scene.world.node_tree.nodes.get('Background').inputs[1].default_value=.15
    for obj in scene.objects:
        if obj.type=='LIGHT': obj.data.energy*=.20
    view('night',(65,-190,48),(0,0,10),175)
    sys.exit(0)
if '--ring-turn-only' in sys.argv:
    scene.render.resolution_x=1100
    scene.render.resolution_y=1100
    scene.cycles.samples=20
    view('ring_turn_detail',(12,-130,27),(0,0,18),55,ring_turn=70)
    sys.exit(0)
if '--rings-only' in sys.argv:
    scene.render.resolution_x=1100
    scene.render.resolution_y=1100
    scene.cycles.samples=20
    view('ring_chest_detail',(8,-100,15),(0,0,10),20)
    view('ring_halo_detail',(12,-130,38),(0,9,29),40)
    view('ring_halo_back',(-12,130,38),(0,9,29),40)
    view('ring_turn_detail',(12,-130,27),(0,0,18),55,ring_turn=70)
    sys.exit(0)
if '--sample-only' in sys.argv:
    scene.render.resolution_x=1100
    scene.render.resolution_y=1100
    scene.cycles.samples=20
    view('surface_sample_arm',hand_center+Vector((25,-100,16)),hand_center+Vector((0,0,5)),38)
    view('surface_sample_wing',(48,-190,46),(46,12,30),88,41)
    view('surface_body_front',(30,-130,30),(0,0,14),58)
    view('surface_body_back',(-30,130,32),(0,0,14),58)
    view('surface_phase_two',(40,-210,38),(0,0,12),178,41)
    view('ring_chest_detail',(8,-100,15),(0,0,10),20)
    view('ring_halo_detail',(12,-130,38),(0,9,29),40)
    view('ring_halo_back',(-12,130,38),(0,9,29),40)
    view('ring_turn_detail',(12,-130,27),(0,0,18),55,ring_turn=70)
    # Texture-only inspection: emission strength one, Standard transform.
    for material in bpy.data.materials:
        if not material.use_nodes: continue
        nodes=material.node_tree.nodes
        texture=next((n for n in nodes if n.type=='TEX_IMAGE' and 'emissive' not in n.image.name),None)
        if texture is None: continue
        out=next(n for n in nodes if n.type=='OUTPUT_MATERIAL')
        emission=nodes.new('ShaderNodeEmission')
        material.node_tree.links.new(texture.outputs['Color'],emission.inputs['Color'])
        transparent=nodes.new('ShaderNodeBsdfTransparent')
        mix=nodes.new('ShaderNodeMixShader')
        material.node_tree.links.new(texture.outputs['Alpha'],mix.inputs[0])
        material.node_tree.links.new(transparent.outputs[0],mix.inputs[1])
        material.node_tree.links.new(emission.outputs[0],mix.inputs[2])
        material.node_tree.links.new(mix.outputs[0],out.inputs['Surface'])
    scene.view_settings.view_transform='Standard'
    view('surface_sample_arm_unlit',hand_center+Vector((25,-100,16)),hand_center+Vector((0,0,5)),38)
    view('surface_body_unlit',(30,-130,30),(0,0,14),58)
    view('surface_phase_two_unlit',(30,-130,30),(0,0,14),58,41)
    from html import escape
    svg=['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024">',
         '<rect width="1024" height="1024" fill="#17151e"/>']
    for obj in meshes.values():
        if 'surface_islands' not in obj or 'right' in obj.name: continue
        for poly in obj.data.polygons:
            points=' '.join(f'{obj.data.uv_layers.active.data[l].uv.x*1024:.2f},{(1-obj.data.uv_layers.active.data[l].uv.y)*1024:.2f}' for l in poly.loop_indices)
            svg.append(f'<polygon points="{points}" fill="none" stroke="#9ec5dd" stroke-width=".4"/>')
    for n,r in enumerate(regions.values()):
        if r.get('source') not in ('authored_surface_sample','lower_surface_detail'): continue
        x,y=r['x']*1024/atlas_size,(atlas_size-r['y']-r['height'])*1024/atlas_size
        svg.append(f'<rect x="{x}" y="{y}" width="{r["width"]*1024/atlas_size}" height="{r["height"]*1024/atlas_size}" fill="none" stroke="#b8864a" stroke-width=".5"><title>{escape(r["name"])}</title></rect>')
        svg.append(f'<text x="{x+1}" y="{y+6}" font-size="5" fill="white">{n}</text>')
        r['review_id']=n
    svg.append('</svg>')
    (OUT/'surface_sample_uv.svg').write_text('\n'.join(svg))
    (OUT/'surface_sample_islands.json').write_text(json.dumps([r for r in regions.values() if r.get('source') in ('authored_surface_sample','lower_surface_detail')],indent=2))
    sys.exit(0)
if '--views-only' not in sys.argv:
    view('hand_and_elbow_detail',hand_center+Vector((22,-100,15)),hand_center,29)

for phase,frame in ([] if '--details-only' in sys.argv else [('phase_one',1),('phase_two',41)]):
    for direction,position,scale in [('hero',(65,-190,48),175),('front',(0,-210,10),175),
                                     ('side',(210,0,10),110),('back',(0,210,10),175)]:
        name=phase+'_'+direction
        view(name,position,(0,0,10),scale,frame)

if '--views-only' in sys.argv:
    sys.exit(0)
view('chest_and_crown_detail',(18,-130,30),(0,0,21),66)
view('death_reveal',(60,-190,50),(0,0,10),175,81)
view('wing_front_detail',(40,-160,34),(36,10,15),72)
view('wing_back_detail',(40,170,34),(36,10,15),72)
view('phase_two_wing_detail',(45,-190,38),(45,10,20),100,41)
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
view('grey_front',(0,-210,10),(0,0,10),175)
view('grey_side',(210,0,10),(0,0,10),110)
view('grey_back',(0,210,10),(0,0,10),175)
for obj in meshes.values():
    obj.data.materials.clear()
    for material in original_materials[obj.name]:
        obj.data.materials.append(material)
scene.world.node_tree.nodes.get('Background').inputs[1].default_value=.15
for obj in scene.objects:
    if obj.type=='LIGHT': obj.data.energy*=.20
view('night',(65,-190,48),(0,0,10),175)

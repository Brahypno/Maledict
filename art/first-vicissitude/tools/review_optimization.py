"""Matched lit/unlit views of the saved baseline and optimized mesh."""
import sys
from pathlib import Path
import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[3]
tag = sys.argv[sys.argv.index('--')+1]
out = ROOT/'build/first-vicissitude-review/comparison'/tag
out.mkdir(parents=True, exist_ok=True)
scene = bpy.context.scene
scene.render.resolution_x = 720
scene.render.resolution_y = 720
scene.render.resolution_percentage = 100
scene.cycles.samples = 12
scene.cycles.seed = 17
scene.cycles.use_denoising = True
camera = scene.camera

def render(name, position, target, scale, frame):
    scene.frame_set(frame)
    camera.location = position
    camera.rotation_euler = (Vector(target)-camera.location).to_track_quat('-Z', 'Y').to_euler()
    camera.data.ortho_scale = scale
    scene.render.filepath = str(out/(name+'.png'))
    bpy.ops.render.render(write_still=True)
    print('OPTIMIZATION_VIEW', tag, name, flush=True)

for phase, frame in [('one', 1), ('two', 41)]:
    for view, position in [('front', (0,-210,10)), ('back', (0,210,10)), ('side', (210,0,10))]:
        render(phase+'_'+view, position, (0,0,10), 175 if view!='side' else 110, frame)
render('close', (18,-130,25), (0,0,14), 64, 41)
render('death', (65,-190,48), (0,0,10), 175, 81)
for phase, frame in [('one',1), ('two',41)]:
    render(phase+'_hero', (65,-190,48), (0,0,10), 175, frame)

for material in bpy.data.materials:
    if not material.use_nodes:
        continue
    nodes = material.node_tree.nodes
    tex = next((n for n in nodes if n.type=='TEX_IMAGE' and n.image and 'emissive' not in n.image.name), None)
    if tex is None:
        continue
    output = next(n for n in nodes if n.type=='OUTPUT_MATERIAL')
    emission = nodes.new('ShaderNodeEmission')
    transparent = nodes.new('ShaderNodeBsdfTransparent')
    mix = nodes.new('ShaderNodeMixShader')
    links = material.node_tree.links
    links.new(tex.outputs['Color'], emission.inputs['Color'])
    links.new(tex.outputs['Alpha'], mix.inputs[0])
    links.new(transparent.outputs[0], mix.inputs[1])
    links.new(emission.outputs[0], mix.inputs[2])
    links.new(mix.outputs[0], output.inputs['Surface'])
scene.view_settings.view_transform = 'Standard'
render('unlit_front', (18,-130,25), (0,0,14), 64, 41)
render('unlit_back', (18,130,25), (0,0,14), 64, 41)
render('unlit_wings', (0,210,10), (0,0,10), 175, 41)

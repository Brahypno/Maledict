"""Matched before/after views, including edge-on thickness and unlit paint."""
import bpy
import sys
from pathlib import Path
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[3]
tag=sys.argv[sys.argv.index('--')+1]
out=ROOT/'art/first-vicissitude/preview/blender/spur-comparison'/tag
out.mkdir(parents=True,exist_ok=True)
scene=bpy.context.scene
scene.render.resolution_x=900
scene.render.resolution_y=900
scene.render.resolution_percentage=100
scene.cycles.samples=20
scene.cycles.seed=17
camera=scene.camera
views=[('phase_two_front',(30,-130,18),(0,0,0),65,41),
       ('phase_two_back',(-30,130,18),(0,0,0),65,41),
       ('phase_two_side',(130,-25,18),(0,0,0),65,41),
       ('phase_one',(30,-130,18),(0,0,0),65,1),
       ('hook_close',(20,-100,8),(0,2,-8),38,41),
       ('overall',(40,-210,38),(0,0,12),178,41)]
def render(name,pos,target,scale,frame):
    scene.frame_set(frame)
    camera.location=pos
    camera.rotation_euler=(Vector(target)-camera.location).to_track_quat('-Z','Y').to_euler()
    camera.data.ortho_scale=scale
    scene.render.filepath=str(out/(name+'.png'))
    bpy.ops.render.render(write_still=True)
    print('SPUR_REVIEW',tag,name,flush=True)
for v in views: render(*v)
for material in bpy.data.materials:
    if not material.use_nodes: continue
    nodes=material.node_tree.nodes
    tex=next((n for n in nodes if n.type=='TEX_IMAGE' and n.image and 'emissive' not in n.image.name),None)
    if tex is None: continue
    shader=nodes.get('Principled BSDF')
    output=next(n for n in nodes if n.type=='OUTPUT_MATERIAL')
    emission=nodes.new('ShaderNodeEmission')
    transparent=nodes.new('ShaderNodeBsdfTransparent')
    mix=nodes.new('ShaderNodeMixShader')
    links=material.node_tree.links
    links.new(tex.outputs['Color'],emission.inputs['Color'])
    links.new(tex.outputs['Alpha'],mix.inputs[0])
    links.new(transparent.outputs[0],mix.inputs[1])
    links.new(emission.outputs[0],mix.inputs[2])
    links.new(mix.outputs[0],output.inputs['Surface'])
scene.view_settings.view_transform='Standard'
render('hook_unlit',*views[4][1:])

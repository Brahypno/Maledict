"""Original pixel profiles for structural rings, replacing separate trim meshes.

Each strip is longitudinal, with independently painted front/back/side/end faces.
Digits index the palette; transverse seals and fractures have explicit positions.
"""

COLORS = ('201d2b', '373040', '57485f', '727a91',
          '9ba9bf', 'bdccdb', 'e0e5e8', '84678f')
RINGS = {
    'ring_socket': dict(length=40,width=8,depth=4,
                        front='23565321',back='12343210',
                        seals=(9,29),fractures=((18,0),(19,1),(19,2))),
    'ring_chest': dict(length=40,width=8,depth=4,
                       front='10365301',back='12034321',
                       seals=(7,31),fractures=((20,7),(21,6),(21,5))),
    'ring_halo': dict(length=64,width=12,depth=6,
                      front='120356530021',back='123403210721',
                      seals=(10,30,51),fractures=((21,0),(22,1),(22,2),(43,11),(43,10),(44,9))),
}

def paint_ring(atlas, region, role, face):
    design=RINGS[role]
    width,height=region['width'],region['height']
    cap=face.startswith('cap')
    for y in range(height):
        for x in range(width):
            if cap:
                edge=min(x,width-1-x,y,height-1-y)
                ink=4 if edge==0 else 2
                if x==width//2+(1 if y>=height//2 else 0): ink=0
                if x==width//2-1 and y<height//2: ink=5
            elif face in ('front','back'):
                ink=int(design[face][y])
                if x<2 or x>=width-2:
                    ink=3 if y in (0,height-1) else 1
                for seal in design['seals']:
                    if face=='front':
                        if x==seal-1 and 1<=y<height-1: ink=0
                        if x==seal and 1<=y<height-1: ink=5
                        if x==seal+1 and 1<=y<height-1: ink=6 if y<height//2 else 4
                        if x==seal+2 and 1<=y<height-1: ink=3
                    else:
                        if (x-seal,y) in ((0,2),(1,2),(2,2),(2,3),(2,4)): ink=0
                        if (x-seal,y)==(1,3): ink=7
                cracks=design['fractures']
                if role in ('ring_chest','ring_socket'):
                    marks={(12,3),(13,3),(14,3),(14,4),(15,4),
                           (25,4),(26,4),(26,3),(27,3)}
                    if (x,y) in marks: ink=0
                    elif (x,y-1) in marks: ink=4
                    if (x,y) in ((5,1),(17,1),(28,6),(34,2)): ink=6
                if (x,y) in cracks: ink=0
                elif (x-1,y) in cracks: ink=4
            else:
                profile='4321' if height==4 else '433211'
                ink=int(profile[y])
                if face=='inner': ink=max(0,ink-1)
                if x in design['seals'] and y<2: ink=4
                if x<2 or x>=width-2: ink=2
            rgb=[int(COLORS[ink][k:k+2],16)/255 for k in (0,2,4)]
            i=((region['y']+y)*256+region['x']+x)*4
            atlas.base[i:i+4]=rgb+[1]
            atlas.emission[i:i+4]=[0,0,0,0]
            if role=='ring_halo':
                # Violet carrier and interrupted luminous script replace silver bands.
                halo=('21192f','34253f','4c365e','715283',
                      '9472aa','b999ce','e4d7ef','b68ccf')
                rgb=[int(halo[ink][k:k+2],16)/255 for k in (0,2,4)]
                lit=False
                if face in ('front','back'):
                    rail=y in (4,5) and x%21 not in (0,1,2,17,18,19,20)
                    rune=any((x-seal,y) in ((-2,3),(-1,2),(0,1),(1,2),(2,3),
                                           (1,6),(0,7),(-1,8),(0,9)) for seal in design['seals'])
                    lit=rail or rune
                    if lit: rgb=[.80,.64,.95] if rune else [.63,.43,.83]
                atlas.base[i:i+4]=rgb+[1]
                if lit: atlas.emission[i:i+4]=rgb+[1]

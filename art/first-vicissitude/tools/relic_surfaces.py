"""Original crystal and vestment drawings in the existing per-part atlas."""
COLORS=('211c2d','393047','54435f','776183','9b8eaa','bac7d7','e0e7ea')

def paint_relic(atlas, region):
    name=region['name']
    crystal='Fractured crystal shell' in name
    cloth='Vestment pennant' in name
    if not (crystal or cloth): return False
    variant=int(name.rsplit(' ',1)[-1]) if crystal else 0
    for y in range(region['height']):
        for x in range(region['width']):
            alpha=1
            if crystal:
                seam=4+y//3 if variant%2==0 else 11-y//4
                ink=3 if x<seam else 1
                if x==seam: ink=5
                if x==seam+1: ink=0
                if y<2: ink=4
                if y>12: ink=2 if x%4 else 4
                if (x,y) in ((3,7),(4,8),(4,9),(5,10),(11,4),(10,5)): ink=0
                if (x,y) in ((2,7),(3,8),(3,9),(4,10)): ink=4
                if variant in (2,5) and 2<x<seam-1 and y<11: ink=4
            else:
                fold=4+(1 if y>8 else 0)+(1 if y>20 else 0)
                ink=2 if x<8 else 1
                if x in (fold,fold+1): ink=3
                if x==fold+2: ink=0
                if x==11-y//12: ink=2
                if x in (1,14) and y<23: ink=3
                if x in (2,13) and y<23: ink=0
                if y<3: ink=1 if y else 0
                if x==3 and y in (5,6,10,14,15,19): ink=4
                if (x,y) in ((10,8),(11,9),(10,10),(9,11),(10,12)): ink=3
                hem=(24,27,29,26,30,31,27,23,26,29,25,28,30,26,23,25)[x]
                if y>=hem: alpha=0
                if y==hem-1: ink=3
                if (x,y) in ((0,12),(0,13),(1,13),(15,17),(14,18),(15,18),
                             (8,20),(8,21),(9,21),(9,22),(9,23)): alpha=0
                if (x,y) in ((7,20),(7,21),(8,22)): ink=3
            rgb=[int(COLORS[ink][k:k+2],16)/255 for k in (0,2,4)]
            i=((region['y']+y)*256+region['x']+x)*4
            atlas.base[i:i+4]=rgb+[alpha]
            atlas.emission[i:i+4]=[0,0,0,0]
    return True

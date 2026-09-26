"""Original crystal and vestment drawings in the existing per-part atlas."""
COLORS=('211c2d','393047','54435f','776183','9b8eaa','bac7d7','e0e7ea')
CLOTH_COLORS=('211c2b','2c2537','393044','493c54','5b4c66','6e5d79','85748f')
# Fine woven border and one elongated broken-ring emblem per cloth panel.
# Empty field between these marks preserves the long folds underneath.
VESTMENT_EMBLEM={
    6:(7,8), 7:(6,9), 8:(5,10), 9:(5,10), 10:(6,9),
    11:(7,8), 12:(7,8), 13:(6,9), 14:(5,10),
    15:(5,10), 16:(6,9), 17:(7,8), 18:(7,), 19:(7,)}

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
                # The first rows sit under the abdominal rim. A shaded rolled
                # attachment opens into broad folds, with a darker return face.
                # Stable lengthwise folds: changing the whole profile every
                # eight rows created conspicuous horizontal mosaic blocks.
                profile=(2.4,2.6,3.0,3.6,4.1,4.4,4.3,4.0,
                         3.4,2.8,2.4,2.6,3.0,3.3,3.1,2.7)
                u=max(0,min(15,x-.45*y/31))
                ix=int(u); fraction=u-ix
                ink=profile[ix]*(1-fraction)+profile[min(15,ix+1)]*fraction
                ink-=.3*y/31
                if y==0: ink-=1.35
                elif y==1: ink-=.6
                # Stitching follows the inner edge; the adjacent dark thread
                # distinguishes embroidery from a glowing painted stripe.
                if 3<=y<=25:
                    if x==2: ink+=1.25
                    elif x==3: ink-=.55
                if y in (3,4) and 2<=x<=13:
                    ink+=.65 if y==3 else -.35
                if x in VESTMENT_EMBLEM.get(y,()): ink+=1.45
                elif x-1 in VESTMENT_EMBLEM.get(y,()): ink-=.4
                hem=(24,27,29,26,30,31,27,23,26,29,25,28,30,26,23,25)[x]
                if y>=hem: alpha=0
                if y==hem-1: ink=min(5,ink+.2)
                if (x,y) in ((0,12),(0,13),(1,13),(15,17),(14,18),(15,18),
                             (8,20),(8,21),(9,21),(9,22),(9,23)): alpha=0
                if (x,y) in ((7,20),(7,21),(8,22)): ink+=.15
            palette=CLOTH_COLORS if cloth else COLORS
            low=int(ink); high=min(len(palette)-1,low+1); blend=ink-low
            rgb=[(int(palette[low][k:k+2],16)*(1-blend)+
                  int(palette[high][k:k+2],16)*blend)/255 for k in (0,2,4)]
            i=((region['y']+y)*256+region['x']+x)*4
            atlas.base[i:i+4]=rgb+[alpha]
            atlas.emission[i:i+4]=[0,0,0,0]
    return True

import sys
from PIL import Image, ImageDraw, ImageFont
W=512; im=Image.new("RGB",(W,W),(24,26,24)); d=ImageDraw.Draw(im)
# subtle vignette bands
for i in range(40): d.rectangle([i,i,W-i,W-i],outline=(24+i//3,27+i//3,24+i//3))
# house
c=(225,220,205)
d.polygon([(256,70),(116,190),(396,190)],outline=c,width=10)
d.rectangle([146,186,366,340],outline=c,width=10)
# boarded door
d.rectangle([226,250,286,340],outline=c,width=8)
for y in (268,302):
    d.line([(212,y),(300,y+14)],fill=(150,110,70),width=12)
# check
d.line([(300,140),(330,170),(400,92)],fill=(110,200,90),width=16,joint="curve")
F=lambda s:ImageFont.truetype("/usr/share/fonts/noto/NotoSans-Black.ttf",s)
for txt,y,s,col in (("CLEARED ROOMS",372,50,c),("STAY CLEAR",428,50,(110,200,90))):
    f=F(s); w=d.textlength(txt,font=f); d.text(((W-w)/2,y),txt,font=f,fill=col)
im.save(sys.argv[1]); im.resize((256,256),Image.LANCZOS).save(sys.argv[2])

#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform sampler2D uCamTexture;
varying mediump vec2 vCamTextureCoord;
const lowp float R = 4.0;
const int iD = 5;
const float maxdelta = 0.08;
uniform mediump float xStep;
uniform mediump float yStep;
const mediump mat3 rgb2yuv = mat3(0.299,-0.147,0.615,0.587,-0.289,-0.515,0.114,0.436,-0.1);
void main(){
    vec4 color = texture2D(uCamTexture,vCamTextureCoord);
    vec3 yuv = rgb2yuv*color.rgb;
    if(yuv.g<-0.225 || yuv.g>0.0 || yuv.b<0.022 || yuv.b>0.206){
        gl_FragColor = color;
        return;
    }
    float xStart;
    float yStart;
    float xfS = vCamTextureCoord.x - (xStep/2.0)*R;
    float yfS = vCamTextureCoord.y - (yStep/2.0)*R;
    int x,y;
    float yf=yfS;
    float xf=xfS;
    lowp vec4 sum=vec4(0.0,0.0,0.0,0.0);
    lowp vec4 fact=vec4(0.0,0.0,0.0,0.0);
    lowp vec4 tmp;
    lowp vec4 rowsum;
    lowp vec4 rowfact;
    lowp vec4 color2;
    lowp float r=0.0,g=0.0,b=0.0;
    for(y=0;y<iD;y+=1){
        if (yf < 0.0 || yf > 1.0){
            yf+=yStep;
            continue;
        }
        rowsum.r=0.0;
        rowsum.g=0.0;
        rowsum.b=0.0;
        rowfact.r=0.0;
        rowfact.g=0.0;
        rowfact.b=0.0;
        for(x=0;x<iD;x+=1){
            if (xf < 0.0 || xf > 1.0){
                xf+=xStep;
                continue;
            }
            color2 = texture2D(uCamTexture,vec2(xf,yf));
            tmp = color - color2;
            if (tmp.r < maxdelta && tmp.r > -maxdelta){
                rowsum.r += color2.r;
                rowfact.r +=1.0;
            }
            if (tmp.g < maxdelta && tmp.g > -maxdelta){
                rowsum.g += color2.g;
                rowfact.g +=1.0;
            }
            if (tmp.b < maxdelta && tmp.b > -maxdelta){
                rowsum.b += color2.b;
                rowfact.b +=1.0;
            }
            xf+=xStep;
        }
        sum+=rowsum;
        fact+=rowfact;
        yf+=yStep;
        xf=xfS;
    }
    vec4 res = sum/fact;
    if(fact.r<1.0){
        r = color.r;
    }else{
        r = res.r;
    }
    if(fact.g<1.0){
        g = color.g;
    }else{
        g = res.g;
    }
    if(fact.b<1.0){
        b = color.b;
    }else{
        b = res.b;
    }
    gl_FragColor = vec4(r,g,b,1.0);
}

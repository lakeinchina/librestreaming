#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES uCamTexture;
varying mediump vec2 vCamTextureCoord;
const highp float D = 6.0;
const int iD = 7;
const float maxdelta = 0.1;
uniform highp float xStep;
uniform highp float yStep;
void main(){
    vec4 color = texture2D(uCamTexture,vCamTextureCoord);
    highp float xStart;
    highp float yStart;
    highp float xfS = vCamTextureCoord.x - xStep*D/2.0f;
    highp float yfS = vCamTextureCoord.y - yStep*D/2.0f;
    int x,y;
    highp float yf=yfS;
    highp float xf=xfS;
    vec4 sum;
    vec4 fact;
    vec4 tmp;
    vec4 rowsum;
    vec4 rowfact;
    vec4 color2;
    float r,g,b;
    for(y=0;y<iD;y+=1){
        if (yf < 0.0f || yf > 1.0f){
            yf+=yStep;
            continue;
        }
        rowsum.r=0.0f;
        rowsum.g=0.0f;
        rowsum.b=0.0f;
        rowfact.r=0.0f;
        rowfact.g=0.0f;
        rowfact.b=0.0f;
        for(x=0;x<iD;x+=1){
            if (xf < 0.0f || xf > 1.0f){
                xf+=xStep;
                continue;
            }
            color2 = texture2D(uCamTexture,vec2(xf,yf));
            tmp = color - color2;
            if (tmp.r < maxdelta && tmp.r > -maxdelta){
                rowsum.r += color2.r;
                rowfact.r +=1.0f;
            }
            if (tmp.g < maxdelta && tmp.g > -maxdelta){
                rowsum.g += color2.g;
                rowfact.g +=1.0f;
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
    if(fact.r<1.0f){
        r = color.r;
    }else{
        r = res.r;
    }
    if(fact.g<1.0f){
        g = color.g;
    }else{
        g = res.g;
    }
    if(fact.b<1.0f){
        b = color.b;
    }else{
        b = res.b;
    }
    gl_FragColor = vec4(r,g,b,1.0f);
}

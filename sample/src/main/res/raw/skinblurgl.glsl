#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES uCamTexture;
varying mediump vec2 vCamTextureCoord;
const highp float R = 4.0;
const int iD = 5;
const float maxdelta = 0.08;
uniform highp float xStep;
uniform highp float yStep;
void main(){
    vec4 color = texture2D(uCamTexture,vCamTextureCoord);
    highp float xStart;
    highp float yStart;
    highp float xfS = vCamTextureCoord.x - xStep*R;
    highp float yfS = vCamTextureCoord.y - yStep*R;
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
        if (yf < 0.0 || yf > 1.0){
            yf+=(2.0*yStep);
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
                xf+=(2.0*xStep);
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
            xf+=(2.0*xStep);
        }
        sum+=rowsum;
        fact+=rowfact;
        yf+=(2.0*yStep);
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

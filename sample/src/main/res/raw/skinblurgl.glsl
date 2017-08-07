precision highp float;
uniform sampler2D uCamTexture;
varying highp vec2 vCamTextureCoord;
const float maxdelta = 0.08;
uniform highp float xStep;
uniform highp float yStep;
const highp mat3 rgb2yuv = mat3(0.299,-0.147,0.615,0.587,-0.289,-0.515,0.114,0.436,-0.1);
const highp mat3 gaussianMap = mat3(0.142,0.131,0.104,0.131,0.122,0.096,0.104,0.096,0.075);
void main(){
    vec4 color = texture2D(uCamTexture,vCamTextureCoord);
    vec3 yuv = rgb2yuv*color.rgb;
    if(yuv.g<-0.225 || yuv.g>0.0 || yuv.b<0.022 || yuv.b>0.206){
        gl_FragColor = color;
        return;
    }
    float xfS = vCamTextureCoord.x - xStep*2.0;
    float yf = vCamTextureCoord.y - yStep*2.0;
    int x,y;
    float xf=xfS;
    vec4 sum=vec4(0.0,0.0,0.0,0.0);
    vec4 fact=vec4(0.0,0.0,0.0,0.0);
    vec4 tmp;
    vec4 color2;
    float gauss;
    for(y=-2;y<3;y+=1){
        if (yf < 0.0 || yf > 1.0){
            yf+=yStep;
            continue;
        }
        for(x=-2;x<3;x+=1){
            if (xf < 0.0 || xf > 1.0){
                xf+=xStep;
                continue;
            }
            color2 = texture2D(uCamTexture,vec2(xf,yf));
            tmp = color - color2;
            gauss = gaussianMap[x<0?-x:x][y<0?-y:y];
            if (abs(tmp.r) < maxdelta){
                sum.r += (color2.r*gauss);
                fact.r +=gauss;
            }
            if (abs(tmp.g) < maxdelta){
                sum.g += color2.g*gauss;
                fact.g +=gauss;
            }
            if (abs(tmp.b) < maxdelta){
                sum.b += color2.b*gauss;
                fact.b +=gauss;
            }
            xf+=xStep;
        }
        yf+=yStep;
        xf=xfS;
    }
    vec4 res = sum/fact;
    if(fact.r<1.0){
        tmp.r = color.r;
    }else{
        tmp.r = res.r;
    }
    if(fact.g<1.0){
        tmp.g = color.g;
    }else{
        tmp.g = res.g;
    }
    if(fact.b<1.0){
        tmp.b = color.b;
    }else{
        tmp.b = res.b;
    }
    gl_FragColor = vec4(tmp.rgb,1.0);
}



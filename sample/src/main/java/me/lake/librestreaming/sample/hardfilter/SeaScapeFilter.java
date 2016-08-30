package me.lake.librestreaming.sample.hardfilter;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.FloatBuffer;

import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.tools.GLESTools;

/**
 * Created by lake on 12/08/16.
 * Librestreaming project.
 * modified from https://www.shadertoy.com/view/Ms2SD1
 * "Seascape" by Alexander Alekseev aka TDM - 2014
 * License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 * http://creativecommons.org/licenses/by-sa/3.0/
 * Contact: tdmaav@gmail.com
 */
public class SeaScapeFilter extends BaseHardVideoFilter {
    protected int glProgram;
    protected int iGlobalTimeLoc;
    protected int iResolutionLoc;
    protected int glCamPostionLoc;
    protected int glCamTextureCoordLoc;
    protected String vertexShader_filter = "" +
            "attribute vec4 aCamPosition; \n" +
            "attribute vec2 aCamTextureCoord; \n" +
            "varying vec2 fragCoord;\n" +
            "void main(){ \n" +
            "    gl_Position= aCamPosition; \n" +
            "    fragCoord = aCamTextureCoord;\n" +
            "}";
    protected String fragmentshader_filter = "" +
            "precision highp float;\n" +
            "varying highp vec2 fragCoord;\n" +
            "uniform vec2 iResolution;\n" +
            "uniform float iGlobalTime;\n" +
            "const int NUM_STEPS = 8;\n" +
            "const float PI\t \t= 3.1415;\n" +
            "const float EPSILON\t= 1e-3;\n" +
            "float EPSILON_NRM;\n" +
            "\n" +
            "// sea\n" +
            "const int ITER_GEOMETRY = 3;\n" +
            "const int ITER_FRAGMENT = 5;\n" +
            "const float SEA_HEIGHT = 0.6;\n" +
            "const float SEA_CHOPPY = 4.0;\n" +
            "const float SEA_SPEED = 0.8;\n" +
            "const float SEA_FREQ = 0.16;\n" +
            "const vec3 SEA_BASE = vec3(0.1,0.19,0.22);\n" +
            "const vec3 SEA_WATER_COLOR = vec3(0.8,0.9,0.6);\n" +
            "float SEA_TIME;\n" +
            "mat2 octave_m = mat2(1.6,1.2,-1.2,1.6);\n" +
            "\n" +
            "// math\n" +
            "mat3 fromEuler(vec3 ang) {\n" +
            "\tvec2 a1 = vec2(sin(ang.x),cos(ang.x));\n" +
            "    vec2 a2 = vec2(sin(ang.y),cos(ang.y));\n" +
            "    vec2 a3 = vec2(sin(ang.z),cos(ang.z));\n" +
            "    mat3 m;\n" +
            "    m[0] = vec3(a1.y*a3.y+a1.x*a2.x*a3.x,a1.y*a2.x*a3.x+a3.y*a1.x,-a2.y*a3.x);\n" +
            "\tm[1] = vec3(-a2.y*a1.x,a1.y*a2.y,a2.x);\n" +
            "\tm[2] = vec3(a3.y*a1.x*a2.x+a1.y*a3.x,a1.x*a3.x-a1.y*a3.y*a2.x,a2.y*a3.y);\n" +
            "\treturn m;\n" +
            "}\n" +
            "float hash( vec2 p ) {\n" +
            "\tfloat h = dot(p,vec2(127.1,311.7));\n" +
            "    return fract(sin(h)*43758.5453123);\n" +
            "}\n" +
            "float noise( in vec2 p ) {\n" +
            "    vec2 i = floor( p );\n" +
            "    vec2 f = fract( p );\n" +
            "\tvec2 u = f*f*(3.0-2.0*f);\n" +
            "    return -1.0+2.0*mix( mix( hash( i + vec2(0.0,0.0) ),\n" +
            "                     hash( i + vec2(1.0,0.0) ), u.x),\n" +
            "                mix( hash( i + vec2(0.0,1.0) ),\n" +
            "                     hash( i + vec2(1.0,1.0) ), u.x), u.y);\n" +
            "}\n" +
            "\n" +
            "// lighting\n" +
            "float diffuse(vec3 n,vec3 l,float p) {\n" +
            "    return pow(dot(n,l) * 0.4 + 0.6,p);\n" +
            "}\n" +
            "float specular(vec3 n,vec3 l,vec3 e,float s) {\n" +
            "    float nrm = (s + 8.0) / (3.1415 * 8.0);\n" +
            "    return pow(max(dot(reflect(e,n),l),0.0),s) * nrm;\n" +
            "}\n" +
            "\n" +
            "// sky\n" +
            "vec3 getSkyColor(vec3 e) {\n" +
            "    e.y = max(e.y,0.0);\n" +
            "    vec3 ret;\n" +
            "    ret.x = pow(1.0-e.y,2.0);\n" +
            "    ret.y = 1.0-e.y;\n" +
            "    ret.z = 0.6+(1.0-e.y)*0.4;\n" +
            "    return ret;\n" +
            "}\n" +
            "\n" +
            "// sea\n" +
            "float sea_octave(vec2 uv, float choppy) {\n" +
            "    uv += noise(uv);\n" +
            "    vec2 wv = 1.0-abs(sin(uv));\n" +
            "    vec2 swv = abs(cos(uv));\n" +
            "    wv = mix(wv,swv,wv);\n" +
            "    return pow(1.0-pow(wv.x * wv.y,0.65),choppy);\n" +
            "}\n" +
            "\n" +
            "float map(vec3 p) {\n" +
            "    float freq = SEA_FREQ;\n" +
            "    float amp = SEA_HEIGHT;\n" +
            "    float choppy = SEA_CHOPPY;\n" +
            "    vec2 uv = p.xz; uv.x *= 0.75;\n" +
            "\n" +
            "    float d, h = 0.0;\n" +
            "    for(int i = 0; i < ITER_GEOMETRY; i++) {\n" +
            "    \td = sea_octave((uv+SEA_TIME)*freq,choppy);\n" +
            "    \td += sea_octave((uv-SEA_TIME)*freq,choppy);\n" +
            "        h += d * amp;\n" +
            "    \tuv *= octave_m; freq *= 1.9; amp *= 0.22;\n" +
            "        choppy = mix(choppy,1.0,0.2);\n" +
            "    }\n" +
            "    return p.y - h;\n" +
            "}\n" +
            "\n" +
            "float map_detailed(vec3 p) {\n" +
            "    float freq = SEA_FREQ;\n" +
            "    float amp = SEA_HEIGHT;\n" +
            "    float choppy = SEA_CHOPPY;\n" +
            "    vec2 uv = p.xz; uv.x *= 0.75;\n" +
            "\n" +
            "    float d, h = 0.0;\n" +
            "    for(int i = 0; i < ITER_FRAGMENT; i++) {\n" +
            "    \td = sea_octave((uv+SEA_TIME)*freq,choppy);\n" +
            "    \td += sea_octave((uv-SEA_TIME)*freq,choppy);\n" +
            "        h += d * amp;\n" +
            "    \tuv *= octave_m; freq *= 1.9; amp *= 0.22;\n" +
            "        choppy = mix(choppy,1.0,0.2);\n" +
            "    }\n" +
            "    return p.y - h;\n" +
            "}\n" +
            "\n" +
            "vec3 getSeaColor(vec3 p, vec3 n, vec3 l, vec3 eye, vec3 dist) {\n" +
            "    float fresnel = 1.0 - max(dot(n,-eye),0.0);\n" +
            "    fresnel = pow(fresnel,3.0) * 0.65;\n" +
            "\n" +
            "    vec3 reflected = getSkyColor(reflect(eye,n));\n" +
            "    vec3 refracted = SEA_BASE + diffuse(n,l,80.0) * SEA_WATER_COLOR * 0.12;\n" +
            "\n" +
            "    vec3 color = mix(refracted,reflected,fresnel);\n" +
            "\n" +
            "    float atten = max(1.0 - dot(dist,dist) * 0.001, 0.0);\n" +
            "    color += SEA_WATER_COLOR * (p.y - SEA_HEIGHT) * 0.18 * atten;\n" +
            "\n" +
            "    color += vec3(specular(n,l,eye,60.0));\n" +
            "\n" +
            "    return color;\n" +
            "}\n" +
            "\n" +
            "// tracing\n" +
            "vec3 getNormal(vec3 p, float eps) {\n" +
            "    vec3 n;\n" +
            "    n.y = map_detailed(p);\n" +
            "    n.x = map_detailed(vec3(p.x+eps,p.y,p.z)) - n.y;\n" +
            "    n.z = map_detailed(vec3(p.x,p.y,p.z+eps)) - n.y;\n" +
            "    n.y = eps;\n" +
            "    return normalize(n);\n" +
            "}\n" +
            "\n" +
            "float heightMapTracing(vec3 ori, vec3 dir, out vec3 p) {\n" +
            "    float tm = 0.0;\n" +
            "    float tx = 1000.0;\n" +
            "    float hx = map(ori + dir * tx);\n" +
            "    if(hx > 0.0) return tx;\n" +
            "    float hm = map(ori + dir * tm);\n" +
            "    float tmid = 0.0;\n" +
            "    for(int i = 0; i < NUM_STEPS; i++) {\n" +
            "        tmid = mix(tm,tx, hm/(hm-hx));\n" +
            "        p = ori + dir * tmid;\n" +
            "    \tfloat hmid = map(p);\n" +
            "\t\tif(hmid < 0.0) {\n" +
            "        \ttx = tmid;\n" +
            "            hx = hmid;\n" +
            "        } else {\n" +
            "            tm = tmid;\n" +
            "            hm = hmid;\n" +
            "        }\n" +
            "    }\n" +
            "    return tmid;\n" +
            "}\n" +
            "\n" +
            "// main\n" +
            "void main() {\n" +
            "    SEA_TIME =  iGlobalTime * SEA_SPEED;\n" +
            "    EPSILON_NRM\t= 0.1 / iResolution.x;\n" +
            "    vec2 uv = fragCoord.xy;\n" +
            "    uv = uv * 2.0 - 1.0;\n" +
            "    uv.x *= iResolution.x / iResolution.y;\n" +
            "    float time = iGlobalTime * 0.3;\n" +
            "\n" +
            "    // ray\n" +
            "    vec3 ang = vec3(sin(time*3.0)*0.1,sin(time)*0.2+0.3,time);\n" +
            "    vec3 ori = vec3(0.0,3.5,time*5.0);\n" +
            "    vec3 dir = normalize(vec3(uv.xy,-2.0)); dir.z += length(uv) * 0.15;\n" +
            "    dir = normalize(dir) * fromEuler(ang);\n" +
            "\n" +
            "    // tracing\n" +
            "    vec3 p;\n" +
            "    heightMapTracing(ori,dir,p);\n" +
            "    vec3 dist = p - ori;\n" +
            "    vec3 n = getNormal(p, dot(dist,dist) * EPSILON_NRM);\n" +
            "    vec3 light = normalize(vec3(0.0,1.0,0.8));\n" +
            "\n" +
            "    // color\n" +
            "    vec3 color = mix(\n" +
            "        getSkyColor(dir),\n" +
            "        getSeaColor(p,n,light,dir,dist),\n" +
            "    \tpow(smoothstep(0.0,-0.05,dir.y),0.3));\n" +
            "\n" +
            "    // post\n" +
            "\tgl_FragColor = vec4(pow(color,vec3(0.75)), 1.0);\n" +
            "}";
    protected long startTime;

    public SeaScapeFilter() {
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
        glProgram = GLESTools.createProgram(vertexShader_filter, fragmentshader_filter);
        GLES20.glUseProgram(glProgram);
        glCamPostionLoc = GLES20.glGetAttribLocation(glProgram, "aCamPosition");
        glCamTextureCoordLoc = GLES20.glGetAttribLocation(glProgram, "aCamTextureCoord");
        iGlobalTimeLoc = GLES20.glGetUniformLocation(glProgram, "iGlobalTime");
        iResolutionLoc = GLES20.glGetUniformLocation(glProgram, "iResolution");
    }


    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        GLES20.glUseProgram(glProgram);
        GLES20.glUniform2f(iResolutionLoc,(float)SIZE_WIDTH,(float)SIZE_HEIGHT);
        GLES20.glUniform1f(iGlobalTimeLoc, (float) (System.currentTimeMillis() - startTime) / 1000.0f);
        GLES20.glEnableVertexAttribArray(glCamPostionLoc);
        GLES20.glEnableVertexAttribArray(glCamTextureCoordLoc);
        shapeBuffer.position(0);
        GLES20.glVertexAttribPointer(glCamPostionLoc, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, shapeBuffer);
        textrueBuffer.position(0);
        GLES20.glVertexAttribPointer(glCamTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, textrueBuffer);
        GLES20.glViewport(0, 0, SIZE_WIDTH, SIZE_HEIGHT);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawIndecesBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, drawIndecesBuffer);
        GLES20.glFinish();
        GLES20.glDisableVertexAttribArray(glCamPostionLoc);
        GLES20.glDisableVertexAttribArray(glCamTextureCoordLoc);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteProgram(glProgram);
    }
}

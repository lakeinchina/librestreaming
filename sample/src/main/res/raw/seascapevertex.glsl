attribute vec4 aCamPosition; 
attribute vec2 aCamTextureCoord; 
varying vec2 fragCoord;
void main(){ 
    gl_Position= aCamPosition; 
    fragCoord = aCamTextureCoord;
}
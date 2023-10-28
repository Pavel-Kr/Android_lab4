uniform mat4 uMVPMatrix;
uniform mat4 model;
attribute vec4 vPosition;
attribute vec3 normal;
varying vec4 Normal;
varying vec4 FragPos;
void main() {
    gl_Position = uMVPMatrix * vPosition;
    Normal = vec4(normal, 0.0);
    FragPos = model * vPosition;
}

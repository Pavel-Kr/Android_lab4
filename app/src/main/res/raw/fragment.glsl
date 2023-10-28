precision mediump float;
uniform vec4 objectColor;
uniform vec4 lightColor;
uniform vec4 lightPos;
uniform vec4 viewPos;
varying vec4 Normal;
varying vec4 FragPos;
void main() {
    float ambientStrength = 0.1;
    vec4 ambient = lightColor * ambientStrength;

    vec4 norm = normalize(Normal);
    vec4 lightDir = normalize(FragPos - lightPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec4 diffuse = diff * lightColor;

    float specularStrength = 0.2;
    vec3 viewDir = vec3(normalize(viewPos - FragPos));
    vec3 reflectDir = vec3(reflect(lightDir, norm));
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);
    vec3 specular = specularStrength * spec * vec3(lightColor);

    vec4 result = (ambient + diffuse + vec4(specular, 1.0)) * objectColor;
    gl_FragColor = result;
}

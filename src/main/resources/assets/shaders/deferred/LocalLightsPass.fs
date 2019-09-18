//
// This file is part of Light Engine
//
// Copyright (C) 2016-2019 Lux Vacuos
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//

#include struct Light

in vec2 textureCoords;

out vec4 out_Color;

uniform vec3 cameraPosition;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 inverseProjectionMatrix;
uniform mat4 inverseViewMatrix;

uniform Light pointLights[128];
uniform int totalPointLights;
uniform mat4 biasMatrix;

uniform int useShadows;

uniform sampler2D gDiffuse;
uniform sampler2D gNormal;
uniform sampler2D gMask;
uniform sampler2D gPBR; // R = roughness, B = metallic
uniform sampler2D gDepth;
uniform sampler2D image;

#include variable pi

#include function positionFromDepth

#include function DistributionGGX

#include function GeometrySchlickGGX

#include function GeometrySmith

#include function fresnelSchlick

#include function calcPointLight

#include variable MASK

void main() {
	vec4 composite = texture(image, textureCoords);
	vec4 mask = texture(gMask, textureCoords);
	if (mask.a == PBR_OBJECT) {
		vec4 diffuse = texture(gDiffuse, textureCoords);
		vec2 pbr = texture(gPBR, textureCoords).rg;
		float depth = texture(gDepth, textureCoords).r;
		vec3 position =
			positionFromDepth(textureCoords, depth, inverseProjectionMatrix, inverseViewMatrix);
		vec3 normal = texture(gNormal, textureCoords).rgb;

		vec3 N = normalize(normal);
		vec3 V = normalize(cameraPosition - position);

		float roughness = pbr.r;
		float metallic = pbr.g;

		vec3 F0 = vec3(0.04);
		F0 = mix(F0, diffuse.rgb, metallic);

		vec3 Lo = vec3(0.0);
		for (int i = 0; i < totalPointLights; i++) {
			Lo += calcPointLight(pointLights[i], position, diffuse.rgb, N, V, F0, roughness, metallic);
		}
		composite.rgb += Lo;
	}
	out_Color = composite;
}

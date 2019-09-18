/*
 * This file is part of Light Engine
 * 
 * Copyright (C) 2016-2019 Lux Vacuos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package engine.glv2.shaders.data;

import engine.gl.light.PointLightInternal;

public class UniformPointLight extends UniformObject {

	private UniformVec3 position, color;
	private UniformFloat radius, intensity;
	private UniformBoolean visible;

	public UniformPointLight(String name) {
		position = new UniformVec3(name + ".position");
		color = new UniformVec3(name + ".color");
		radius = new UniformFloat(name + ".radius");
		intensity = new UniformFloat(name + ".intensity");
		visible = new UniformBoolean(name + ".visible");
		super.storeUniforms(position, color, radius, intensity, visible);
	}

	public void loadLight(PointLightInternal light) {
		position.loadVec3(light.x, light.y, light.z);
		color.loadVec3(light.color);
		intensity.loadFloat(light.intensity);
		radius.loadFloat(light.radius);
		visible.loadBoolean(light.visible);
	}

}

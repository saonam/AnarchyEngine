/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.observer;

import engine.gl.shader.BaseShader;
import engine.lua.type.data.Matrix4;
import engine.lua.type.object.insts.Prefab;

public interface RenderableInstance {
	public void render(BaseShader shader);
	public Prefab getPrefab();
	public Matrix4 getWorldMatrix();
}

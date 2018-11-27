package luaengine.type.object;

import org.luaj.vm2.LuaValue;

import engine.gl.light.Light;
import luaengine.type.data.Color3;
import luaengine.type.data.Vector3;
import luaengine.type.object.Instance;

public abstract class LightBase extends Instance {
	public LightBase(String typename) {
		super(typename);

		this.defineField("Position", Vector3.newInstance(0, 0, 0), false);
		this.defineField("Intensity", LuaValue.valueOf(1), false);
		this.defineField("Color", Color3.white(), false);
		this.defineField("Shadows", LuaValue.valueOf(true), false);
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}
	
	public abstract Light getLightInternal();

	public void setIntensity(float intensity) {
		this.set("Intensity", LuaValue.valueOf(intensity));
	}

	public void setPosition(int x, int y, int z) {
		this.set("Position", Vector3.newInstance(x, y, z));
	}
	
	public void setPosition(Vector3 position) {
		this.set("Position", position.clone());
	}
}

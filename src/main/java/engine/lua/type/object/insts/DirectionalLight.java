package engine.lua.type.object.insts;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.GameSubscriber;
import engine.gl.IPipeline;
import engine.gl.Pipeline;
import engine.gl.light.Light;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.Instance;
import engine.lua.type.object.LightBase;
import engine.lua.type.object.TreeViewable;
import engine.observer.RenderableWorld;
import ide.layout.windows.icons.Icons;
import lwjgui.paint.Color;

public class DirectionalLight extends LightBase implements TreeViewable,GameSubscriber {

	private engine.gl.light.DirectionalLightInternal light;
	private IPipeline pipeline;

	public DirectionalLight() {
		super("DirectionalLight");
		
		// Remove position field (from LightBase)
		this.undefineField(C_POSITION);
		
		// Define direction field
		this.defineField("Direction", new Vector3(1,1,-1), false);
		
		// Update on game update
		Game.getGame().subscribe(this);
		
		this.changedEvent().connect((args)->{
			LuaValue key = args[0];
			LuaValue value = args[1];
			
			if ( light != null ) {
				if ( key.eq_b(C_INTENSITY) ) {
					light.intensity = value.tofloat();
				} else if ( key.eq_b(C_COLOR) ) {
					Color color = ((Color3)value).toColor();
					light.color = new Vector3f( Math.max( color.getRed(),1 )/255f, Math.max( color.getGreen(),1 )/255f, Math.max( color.getBlue(),1 )/255f );
				} else if ( key.eq_b(C_PARENT) ) {
					onParentChange();
				}
			}
		});
	}
	
	private void onParentChange() {
		LuaValue t = this.getParent();
		if ( t == null ) {
			onDestroy();
			return;
		}
		
		while ( t != null && !t.isnil() ) {
			if ( t instanceof RenderableWorld ) {
				IPipeline tempPipeline = Pipeline.get((RenderableWorld)t);
				if ( pipeline != null && pipeline.equals(tempPipeline) )
					break;
				
				if ( pipeline != null )
					onDestroy();
				
				pipeline = tempPipeline;
				makeLight();
				break;
			}
			t = ((Instance)t).getParent();
		}
	}

	@Override
	public Light getLightInternal() {
		return light;
	}

	@Override
	public void onDestroy() {
		if ( light != null ) {
			pipeline.getDirectionalLightHandler().removeLight(light);
			light = null;
			System.out.println("Destroyed light");
		}
	}
	
	private void makeLight() {
		if ( pipeline == null )
			return;
		
		if ( light != null )
			return;
		
		// Create light
		Vector3f direction = ((Vector3)this.get("Direction")).toJoml();
		float intensity = this.get("Intensity").tofloat();
		light = new engine.gl.light.DirectionalLightInternal(direction, intensity);
		
		// Color it
		Color color = ((Color3)this.get("Color")).toColor();
		light.color = new Vector3f( Math.max( color.getRed(),1 )/255f, Math.max( color.getGreen(),1 )/255f, Math.max( color.getBlue(),1 )/255f );
		
		// Add it to pipeline
		pipeline.getDirectionalLightHandler().addLight(light);
	}
	
	@Override
	public Icons getIcon() {
		return Icons.icon_light_directional;
	}

	@Override
	public void gameUpdateEvent(boolean important) {
		if ( !important )
			return;
		
		onParentChange();
	}
}
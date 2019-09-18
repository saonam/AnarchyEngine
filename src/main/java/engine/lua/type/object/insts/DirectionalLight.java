package engine.lua.type.object.insts;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.GameSubscriber;
import engine.InternalGameThread;
import engine.InternalRenderThread;
import engine.gl.IPipeline;
import engine.gl.Pipeline;
import engine.gl.light.DirectionalLightInternal;
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

	private DirectionalLightInternal light;
	private IPipeline pipeline;

	private static final LuaValue C_SHADOWDISTANCE = LuaValue.valueOf("ShadowDistance");
	private static final LuaValue C_DIRECTION = LuaValue.valueOf("Direction");

	public DirectionalLight() {
		super("DirectionalLight");
		
		// Define direction field
		this.defineField(C_DIRECTION.toString(), new Vector3(1,1,-1), false);
		
		// Shadow distance
		this.defineField(C_SHADOWDISTANCE.toString(), LuaValue.valueOf(50), false);
		
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
				} else if ( key.eq_b(C_SHADOWDISTANCE) ) {
					light.distance = value.toint();
				} else if ( key.eq_b(C_DIRECTION) ) {
					light.direction = ((Vector3)value).toJoml();
				}
			}
			
			if ( key.eq_b(C_PARENT) ) {
				onParentChange();
			} 
		});
		
		InternalGameThread.runLater(()->{
			Game.lighting().changedEvent().connect((args)->{
				if ( light == null )
					return;
	
				LuaValue key = args[0];
				LuaValue value = args[1];
				
				if ( key.eq_b(LuaValue.valueOf("ShadowMapSize")) ) {
					light.setSize(value.toint());
				}
			});
		});
	}
	
	private void onParentChange() {
		LuaValue t = this.getParent();
		if ( t.isnil() ) {
			onDestroy();
			return;
		}
		
		// Search for renderable world
		while ( t != null && !t.isnil() ) {
			if ( t instanceof RenderableWorld ) {
				IPipeline tempPipeline = Pipeline.get((RenderableWorld)t);
				if ( tempPipeline == null )
					continue;
				
				// Light exists inside old pipeline. No need to recreate.
				if ( pipeline != null && pipeline.equals(tempPipeline) )
					break;
				
				// Destroy old light
				if ( pipeline != null )
					onDestroy();
				
				// Make new light. Return means we can live for another day!
				pipeline = tempPipeline;
				makeLight();
				return;
			}
			
			// Navigate up tree
			t = ((Instance)t).getParent();
		}
		
		// Cant make light, can't destroy light. SO NO LIGHT!
		onDestroy();
	}

	@Override
	public Light getLightInternal() {
		return light;
	}

	@Override
	public void onDestroy() {
		if ( light != null ) {
			DirectionalLightInternal tempLight = light;
			InternalRenderThread.runLater(()->{
				pipeline.getDirectionalLightHandler().removeLight(tempLight);
			});
			light = null;
			pipeline = null;
			
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
		light.distance = this.get(C_SHADOWDISTANCE).toint();
		
		// Color it
		Color color = ((Color3)this.get("Color")).toColor();
		light.color = new Vector3f( Math.max( color.getRed(),1 )/255f, Math.max( color.getGreen(),1 )/255f, Math.max( color.getBlue(),1 )/255f );
		
		// Add it to pipeline
		InternalGameThread.runLater(()->{
			InternalRenderThread.runLater(()->{
				pipeline.getDirectionalLightHandler().addLight(light);
			});
		});
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

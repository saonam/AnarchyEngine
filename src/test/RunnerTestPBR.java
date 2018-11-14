package test;

import org.luaj.vm2.LuaValue;
import org.lwjgl.glfw.GLFW;

import engine.Game;
import ide.RunnerClient;
import luaengine.RunnableArgs;
import luaengine.type.data.Color3;
import luaengine.type.data.Matrix4;
import luaengine.type.data.Vector3;
import luaengine.type.object.insts.GameObject;
import luaengine.type.object.insts.Material;
import luaengine.type.object.insts.Mesh;
import luaengine.type.object.insts.Prefab;
import luaengine.type.object.insts.Texture;

public class RunnerTestPBR extends RunnerClient {
	
	@Override
	public void loadScene(String[] args) {
		super.loadScene(args);
		
		// Move camera
		Game.runService().renderSteppedEvent().connect( new RunnableArgs() {
			final int CAMERA_DIST = 256;
			double t = Math.PI/2f;
			
			@Override
			public void run(LuaValue[] args) {
				double delta = args[0].todouble();
				
				// Get direction value
				int d = 0;
				if ( Game.userInputService().isKeyDown(GLFW.GLFW_KEY_E) )
					d++;
				if ( Game.userInputService().isKeyDown(GLFW.GLFW_KEY_Q) )
					d--;
				t += d*delta;
				
				// Rotate the camera based on the direction
				float xx = (float) (Math.cos(t) * CAMERA_DIST);
				float yy = (float) (Math.sin(t) * CAMERA_DIST);

				Game.workspace().getCurrentCamera().setPosition(Vector3.newInstance(xx,yy,0));
				Game.workspace().getCurrentCamera().setLookAt(Vector3.newInstance(0, 0, 0));
			}
		});
		Game.workspace().getCurrentCamera().setFov(2.5f);
		
		// Set ambient
		Game.lighting().setAmbient(Color3.newInstance(64, 64, 64));
		
		// Mesh
		Mesh mesh = new Mesh();
		mesh.setFilePath("Resources/Testing/Sphere.mesh");
		mesh.setParent(Game.assets().meshes());
		
		
		// Textures
		Texture texture1 = new Texture();
		texture1.setFilePath("Resources/Testing/PBR/scratch/normal.png");
		texture1.setParent(Game.assets().textures());
		
		Texture texture2 = new Texture();
		texture2.setFilePath("Resources/Testing/PBR/scratch/roughness.png");
		texture2.setParent(Game.assets().textures());
		
		Texture texture3 = new Texture();
		texture3.setFilePath("Resources/Testing/PBR/scratch/metallic.png");
		texture3.setParent(Game.assets().textures());
		
		// Base material
		Material mat = new Material();
		mat.setNormalMap(texture1);
		mat.setRoughMap(texture2);
		mat.setMetalMap(texture3);
		
		// Make rows of balls
		makeRow( mesh, mat,  1, Color3.newInstance(255, 255, 255),	Color3.newInstance(255, 255, 255),	0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f);
		makeRow( mesh, mat,  0, Color3.newInstance(0, 128, 16),		Color3.newInstance(0, 128, 16),		0.0f, 1.0f, 0.2f, 0.2f, 0.3f, 0.0f);
		makeRow( mesh, mat, -1, Color3.newInstance(255,220,96),		Color3.newInstance(255, 220, 96),	0.0f, 1.0f, 0.8f, 0.8f, 1.0f, 0.0f);
		makeRow( mesh, mat, -2, Color3.red(),						Color3.red(),						0.0f, 0.0f, 0.2f, 0.2f, 0.0f, 0.4f);
	}
	
	private void makeRow( Mesh mesh, Material mat, float y, Color3 fromColor, Color3 toColor, float fromRough, float toRough, float fromMetal, float toMetal, float fromReflective, float toReflective ) {
		int balls = 8;
		for (int i = 0; i < balls; i++) {
			float frac = i/(float)(balls-1);
			
			// Material
			Material material = (Material) mat.clone();
			material.setName("Material"+i);
			int cx = (int) (fromColor.getR() + (toColor.getR()-fromColor.getR())*frac);
			int cy = (int) (fromColor.getG() + (toColor.getG()-fromColor.getG())*frac);
			int cz = (int) (fromColor.getB() + (toColor.getB()-fromColor.getB())*frac);
			material.setColor(Color3.newInstance(cx, cy, cz));
			material.setRoughness(fromRough + (toRough-fromRough)*frac);
			material.setMetalness(fromMetal + (toMetal-fromMetal)*frac);
			material.setReflective(fromReflective + (toReflective-fromReflective)*frac);
			material.setParent(Game.assets().materials());
			
			// Create prefab
			Prefab p = new Prefab();
			p.setName("Prefab"+i);
			p.addModel(mesh, material);
			p.setParent(Game.assets().prefabs());
			
			// Calculate matrix
			float t = ((balls-1)/2f)-i;
			Vector3 pos = Vector3.newInstance(t*2.2f, 0, y*2.2f);
			Matrix4 matrix = new Matrix4(pos);
			
			// Create game object
			GameObject obj = new GameObject();
			obj.setPrefab(p);
			obj.setParent(Game.workspace());
			obj.setWorldMatrix(matrix);
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}
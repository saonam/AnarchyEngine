package ide.layout.windows;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.luaj.vm2.LuaValue;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NFDPathSet;
import org.lwjgl.util.nfd.NativeFileDialog;

import engine.Game;
import engine.GameSubscriber;
import engine.lua.type.object.Instance;
import engine.lua.type.object.ScriptBase;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.Prefab;
import engine.lua.type.object.insts.Texture;
import engine.lua.type.object.services.Assets;
import engine.util.FileUtils;
import ide.IDE;
import ide.layout.IdePane;
import ide.layout.windows.icons.Icons;
import lwjgui.collections.ObservableList;
import lwjgui.scene.Node;
import lwjgui.scene.control.ContextMenu;
import lwjgui.scene.control.MenuItem;
import lwjgui.scene.control.ScrollPane;
import lwjgui.scene.control.SeparatorMenuItem;
import lwjgui.scene.control.TreeBase;
import lwjgui.scene.control.TreeItem;
import lwjgui.scene.control.TreeView;

public class IdeExplorerNew extends IdePane implements GameSubscriber {
	private ScrollPane scroller;
	private TreeView<Instance> tree;

	private HashMap<Instance, TreeItem<Instance>> instanceMap;
	private HashMap<Instance, TreeItem<Instance>> instanceMapTemp;
	private HashMap<TreeItem<Instance>, Instance> treeItemMap;
	private ArrayList<TreeItem<Instance>> treeItems;
	private boolean updating;
	
	public IdeExplorerNew() {
		super("Explorer New", true);

		this.scroller = new ScrollPane();
		this.scroller.setFillToParentHeight(true);
		this.scroller.setFillToParentWidth(true);
		this.getChildren().add(scroller);

		tree = new TreeView<Instance>();
		this.scroller.setContent(tree);
		
		tree.setOnSelectItem(event -> {
			if ( updating )
				return;
			
			TreeItem<Instance> item = event.object;
			Instance inst = item.getRoot();
			Game.select(inst);
		});
		tree.setOnDeselectItem(event -> {
			if ( updating )
				return;
			
			TreeItem<Instance> item = event.object;
			Instance inst = item.getRoot();
			Game.deselect(inst);
		});
		
		instanceMap = new HashMap<>();
		treeItemMap = new HashMap<>();
		instanceMapTemp = new HashMap<>();
		treeItems = new ArrayList<TreeItem<Instance>>();
		
		Game.getGame().subscribe(this);
		update(true);
	}

	private long lastUpdate = -1;

	private void update(boolean b) {
		if ( updating )
			return;

		if (System.currentTimeMillis()-lastUpdate < 50 && !b )
			return;
		if (System.currentTimeMillis()-lastUpdate < 4 && b ) {
			lastUpdate = System.currentTimeMillis();
			return;
		}
		
		lastUpdate = System.currentTimeMillis();
		updating = true;
		
		// Refresh the tree
		if ( b ) {
			instanceMapTemp.clear();
			instanceMapTemp.putAll(instanceMap);
			instanceMap.clear();

			treeItemMap.clear();
			treeItems.clear();
			
			list(tree, Game.game());
		}
		
		// Handle selections
		for (int i = 0; i < treeItems.size(); i++) {
			tree.deselectItem(treeItems.get(i));

			// Update names
			TreeItem<Instance> item = treeItems.get(i);
			item.setText(item.getRoot().getName());
		}
		List<Instance> selected = Game.selected();
		for (int i = 0; i < selected.size(); i++) {
			Instance sel = selected.get(i);
			TreeItem<Instance> t = instanceMap.get(sel);
			if ( t != null ) {
				tree.selectItem(t);
			}
		}
		
		updating = false;
	}
	
	private void list(TreeBase<Instance> treeItem, Instance root) {
		// Remove all the items in this tree item that are no longer parented to it
		ObservableList<TreeItem<Instance>> items = treeItem.getItems();
		for (int i = 0; i < items.size(); i++) {
			TreeItem<Instance> item = items.get(i);
			Instance obj = item.getRoot();
			LuaValue par = obj.getParent();
			if( par == null || par.isnil() || par != root ) {
				items.remove(item);
				instanceMapTemp.remove(obj);
			}
		}
		
		// Start adding items to tree
		List<Instance> c = root.getChildren();
		for ( int i = 0; i < c.size(); i++) {
			Instance inst = c.get(i);
			
			// Get the tree item
			TreeItem<Instance> newTreeItem = instanceMapTemp.get(inst);
			if ( newTreeItem == null ) {
				// What graphic does it need?
				Node graphic = Icons.icon_wat.getView();
				if ( inst instanceof TreeViewable )
					graphic = ((TreeViewable)inst).getIcon().getView();
				
				// New one
				newTreeItem = new TreeItem<Instance>(inst, graphic);

				// Create context menu
				ContextMenu con = getContetxMenu(inst);
				newTreeItem.setContextMenu(con);
				
				// Open a script
				newTreeItem.setOnMouseClicked(event -> {
					int clicks = event.getClickCount();
					if ( clicks == 2 ) {
						if ( inst instanceof ScriptBase ) {
							IdeLuaEditor lua = new IdeLuaEditor((ScriptBase) inst);
							IDE.layout.getCenter().dock(lua);
						}
					}
				});
				
				// Add it to the tree
				treeItem.getItems().add(newTreeItem);
			} else {
				// Add this item in if it was reparented.
				Instance obj = newTreeItem.getRoot();
				if ( obj == inst && !treeItem.getItems().contains(newTreeItem) ) {
					treeItem.getItems().add(newTreeItem);
				}
			}
			
			// Update name
			newTreeItem.setText(inst.getName());
			
			// cache it for easier lookups
			instanceMap.put(inst, newTreeItem);
			treeItemMap.put(newTreeItem, inst);
			treeItems.add(newTreeItem);
			
			// Look ma it's recursion!
			list(newTreeItem, inst);
		}
	}
	
	private ContextMenu getContetxMenu(Instance inst) {
		ContextMenu c = new ContextMenu();
		c.setAutoHide(false);

		// Cut
		MenuItem cut = new MenuItem("Cut", Icons.icon_cut.getView());
		cut.setOnAction(event -> {
			if ( inst.isInstanceable() ) {
				Instance t = inst.clone();
				if ( t == null || t.isnil() )
					return;
				Game.copiedInstance = t;
				inst.destroy();
			}
		});
		c.getItems().add(cut);

		// Copy
		MenuItem copy = new MenuItem("Copy", Icons.icon_copy.getView());
		copy.setOnAction(event -> {
			if ( inst.isInstanceable() ) {
				Instance t = inst.clone();
				if ( t == null || t.isnil() )
					return;
				Game.copiedInstance = t;
			}
		});
		c.getItems().add(copy);

		// Paste
		MenuItem paste = new MenuItem("Paste", Icons.icon_paste.getView());
		paste.setOnAction(event -> {
			Instance t = Game.copiedInstance;
			if ( t == null )
				return;
			t.clone().forceSetParent(inst);
		});
		c.getItems().add(paste);

		// Copy
		MenuItem duplicate = new MenuItem("Duplicate", Icons.icon_copy.getView());
		duplicate.setOnAction(event -> {
			if ( inst.isInstanceable() ) {
				Instance t = inst.clone();
				if ( t == null || t.isnil() )
					return;
				t.forceSetParent(inst.getParent());
			}
		});
		c.getItems().add(duplicate);
		
		// Separate
		c.getItems().add(new SeparatorMenuItem());

		// New Model
		if ( inst instanceof Prefab ) {
			// New Prefab
			MenuItem pref = new MenuItem("Add Model", Icons.icon_wat.getView());
			pref.setOnAction(event -> {
				((Prefab)inst).get("AddModel").invoke(LuaValue.NIL,LuaValue.NIL,LuaValue.NIL);
			});
			c.getItems().add(pref);

			// Create gameobject
			MenuItem gobj = new MenuItem("Create GameObject", Icons.icon_gameobject.getView());
			gobj.setOnAction(event -> {
				GameObject g = new GameObject();
				g.setPrefab((Prefab) inst);
				g.setParent(Game.workspace());
			});
			c.getItems().add(gobj);
			
			
		}
		
		// Asset functions
		if ( inst instanceof Assets ) {
			
			// New Prefab
			MenuItem prefi = new MenuItem("Import Prefab", Icons.icon_model.getView());
			prefi.setOnAction(event -> {
				String path = "";
				PointerBuffer outPath = MemoryUtil.memAllocPointer(1);
				int result = NativeFileDialog.NFD_OpenDialog(Mesh.getFileTypes(), new File("").getAbsolutePath(), outPath);
				if ( result == NativeFileDialog.NFD_OKAY ) {
					path = outPath.getStringUTF8(0);
					Game.assets().importPrefab(path);
				} else {
					return;
				}
			});
			c.getItems().add(prefi);
			
			// Import Texture
			MenuItem texi = new MenuItem("Import Texture", Icons.icon_texture.getView());
			texi.setOnAction(event -> {
				NFDPathSet outPaths = NFDPathSet.calloc();
				int result = NativeFileDialog.NFD_OpenDialogMultiple(Texture.getFileTypes(), new File("").getAbsolutePath(), outPaths);
				if ( result == NativeFileDialog.NFD_OKAY ) {
					long count = NativeFileDialog.NFD_PathSet_GetCount(outPaths);
					for (long i = 0; i < count; i++) {
						String path = NativeFileDialog.NFD_PathSet_GetPath(outPaths, i);
						Instance t = Game.assets().importTexture(path);
						File ff = new File(path);
						if ( ff.exists() ) {
							t.forceSetName(FileUtils.getFileNameWithoutExtension(ff.getName()));
						}
					}
				} else {
					return;
				}
			});
			c.getItems().add(texi);
			
			// Separate
			c.getItems().add(new SeparatorMenuItem());
			
			// New Prefab
			MenuItem pref = new MenuItem("New Prefab", Icons.icon_model.getView());
			pref.setOnAction(event -> {
				Game.getService("Assets").get("NewPrefab").invoke();
			});
			c.getItems().add(pref);
			
			// New Mesh
			MenuItem mesh = new MenuItem("New Mesh", Icons.icon_mesh.getView());
			mesh.setOnAction(event -> {
				Game.getService("Assets").get("ImportMesh").invoke(LuaValue.NIL);
			});
			c.getItems().add(mesh);

			// New Material
			MenuItem mat = new MenuItem("New Material", Icons.icon_material.getView());
			mat.setOnAction(event -> {
				Game.getService("Assets").get("NewMaterial").invoke();
			});
			c.getItems().add(mat);

			// New Texture
			MenuItem tex = new MenuItem("New Texture", Icons.icon_texture.getView());
			tex.setOnAction(event -> {
				Game.getService("Assets").get("NewTexture").invoke();
			});
			c.getItems().add(tex);
			
			// Separate
			c.getItems().add(new SeparatorMenuItem());
		}


		// Cut
		MenuItem insert = new MenuItem("Insert Object  \u25ba", Icons.icon_new.getView());
		insert.setOnAction(event -> {
			new InsertWindow(inst);
		});
		c.getItems().add(insert);
		
		return c;
	}

	@Override
	public void gameUpdateEvent(boolean important) {
		update(important);
	}

	@Override
	public void onOpen() {
		//
	}

	@Override
	public void onClose() {
		//
	}

}

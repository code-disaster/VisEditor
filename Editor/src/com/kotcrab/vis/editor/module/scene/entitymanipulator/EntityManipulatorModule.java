/*
 * Copyright 2014-2015 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotcrab.vis.editor.module.scene.entitymanipulator;

import com.artemis.Entity;
import com.artemis.utils.EntityBuilder;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Timer;
import com.google.common.eventbus.Subscribe;
import com.kotcrab.vis.editor.App;
import com.kotcrab.vis.editor.Editor;
import com.kotcrab.vis.editor.entity.ExporterDropsComponent;
import com.kotcrab.vis.editor.entity.PixelsPerUnitComponent;
import com.kotcrab.vis.editor.entity.PositionComponent;
import com.kotcrab.vis.editor.entity.UUIDComponent;
import com.kotcrab.vis.editor.event.*;
import com.kotcrab.vis.editor.event.bus.Event;
import com.kotcrab.vis.editor.event.bus.EventListener;
import com.kotcrab.vis.editor.module.InjectModule;
import com.kotcrab.vis.editor.module.editor.StatusBarModule;
import com.kotcrab.vis.editor.module.project.*;
import com.kotcrab.vis.editor.module.scene.*;
import com.kotcrab.vis.editor.module.scene.action.EntitiesAddedAction;
import com.kotcrab.vis.editor.module.scene.action.EntitiesRemovedAction;
import com.kotcrab.vis.editor.module.scene.action.GroupAction;
import com.kotcrab.vis.editor.module.scene.action.MoveEntitiesAction;
import com.kotcrab.vis.editor.module.scene.entitymanipulator.tool.PolygonTool;
import com.kotcrab.vis.editor.module.scene.entitymanipulator.tool.SelectionTool;
import com.kotcrab.vis.editor.module.scene.entitymanipulator.tool.Tool;
import com.kotcrab.vis.editor.module.scene.entitymanipulator.tool.Tools;
import com.kotcrab.vis.editor.plugin.EditorEntitySupport;
import com.kotcrab.vis.editor.proxy.EntityProxy;
import com.kotcrab.vis.editor.proxy.GroupEntityProxy;
import com.kotcrab.vis.editor.scene.EditorScene;
import com.kotcrab.vis.editor.scene.Layer;
import com.kotcrab.vis.editor.ui.scene.GroupBreadcrumb;
import com.kotcrab.vis.editor.ui.scene.GroupBreadcrumb.GroupBreadcrumbListener;
import com.kotcrab.vis.editor.ui.scene.LayersDialog;
import com.kotcrab.vis.editor.ui.scene.SceneOutline;
import com.kotcrab.vis.editor.ui.scene.entityproperties.EntityProperties;
import com.kotcrab.vis.editor.util.gdx.DummyMusic;
import com.kotcrab.vis.editor.util.gdx.MenuUtils;
import com.kotcrab.vis.editor.util.undo.UndoableActionGroup;
import com.kotcrab.vis.editor.util.vis.ProtoEntity;
import com.kotcrab.vis.runtime.assets.*;
import com.kotcrab.vis.runtime.component.*;
import com.kotcrab.vis.runtime.system.RenderBatchingSystem;
import com.kotcrab.vis.runtime.util.ImmutableArray;
import com.kotcrab.vis.ui.util.dialog.DialogUtils;
import com.kotcrab.vis.ui.widget.PopupMenu;
import com.kotcrab.vis.ui.widget.VisTable;

import static com.kotcrab.vis.editor.module.scene.entitymanipulator.EntityMoveTimerTask.*;

/** @author Kotcrab */
public class EntityManipulatorModule extends SceneModule implements EventListener {
	@InjectModule private StatusBarModule statusBar;

	@InjectModule private SceneIOModule sceneIO;
	@InjectModule private SupportModule supportModule;

	@InjectModule private CameraModule camera;
	@InjectModule private UndoModule undoModule;
	@InjectModule private TextureCacheModule textureCache;
	@InjectModule private ParticleCacheModule particleCache;
	@InjectModule private FontCacheModule fontCache;
	@InjectModule private RendererModule rendererModule;

	private ShapeRenderer shapeRenderer;
	private EntityProxyCache entityProxyCache;
	private ZIndexManipulatorManager zIndexManipulator;
	private GroupIdProviderSystem groupIdProvider;
	private GroupProxyProviderSystem groupProxyProvider;
	private RenderBatchingSystem renderBatchingSystem;

	//TODO [misc] create common class for scene ui dialogs
	private EntityProperties entityProperties;
	private GroupBreadcrumb groupBreadcrumb;
	private LayersDialog layersDialog;
	private AlignmentToolsDialog alignmentToolsDialog;
	private SceneOutline sceneOutline;

	private Tool currentTool;
	private SelectionTool selectionTool;
	private PolygonTool polygonTool;

	private int currentSelectionGid = -1;
	private Array<EntityProxy> selectedEntities = new Array<>();
	private ImmutableArray<EntityProxy> immutableSelectedEntities = new ImmutableArray<>(selectedEntities);

	private float copyAttachX, copyAttachY;
	private Array<ProtoEntity> entitiesClipboard = new Array<>();

	private boolean mouseDragged;
	private float menuX, menuY;
	private PopupMenu generalPopupMenu;
	private PopupMenu entityPopupMenu;

	private EntityMoveTimerTask entityMoveTimerTask;
	private MoveEntitiesAction keyMoveAction;

	private VisTable toolPropertiesContainer;

	@Override
	public void init () {
		shapeRenderer = rendererModule.getShapeRenderer();
		entityProxyCache = engineConfiguration.getManager(EntityProxyCache.class);
		zIndexManipulator = engineConfiguration.getManager(ZIndexManipulatorManager.class);
		groupIdProvider = engineConfiguration.getSystem(GroupIdProviderSystem.class);
		groupProxyProvider = engineConfiguration.getSystem(GroupProxyProviderSystem.class);
		renderBatchingSystem = engineConfiguration.getSystem(RenderBatchingSystem.class);

		entityProperties = new EntityProperties(sceneContainer, sceneTab, selectedEntities);
		groupBreadcrumb = new GroupBreadcrumb(new GroupBreadcrumbListener() {
			@Override
			public void clicked (int gid) {
				currentSelectionGid = gid;
				selectAll();
			}

			@Override
			public void rootClicked () {
				currentSelectionGid = -1;
				groupBreadcrumb.resetHierarchy();
				resetSelection();
			}
		});
		layersDialog = new LayersDialog(sceneTab, engineConfiguration, sceneContainer);
		alignmentToolsDialog = new AlignmentToolsDialog(sceneContainer, selectedEntities);
		sceneOutline = new SceneOutline(sceneContainer, selectedEntities);
		createGeneralMenu();

		toolPropertiesContainer = new VisTable();

		entityMoveTimerTask = new EntityMoveTimerTask(scene, this, immutableSelectedEntities);

		selectionTool = new SelectionTool();
		polygonTool = new PolygonTool();

		selectionTool.setModules(sceneContainer, scene);
		polygonTool.setModules(sceneContainer, scene);

		switchTool(selectionTool);

		scene.addObservable(notificationId -> {
			if (notificationId == EditorScene.ACTIVE_LAYER_CHANGED) {
				resetSelection();
			}
		});

		App.oldEventBus.register(this);
		App.eventBus.register(this);
	}

	@Override
	public void postInit () {
		entityProperties.loadSupportsSpecificTables(projectContainer.get(SupportModule.class));
	}

	private void createGeneralMenu () {
		entityPopupMenu = new PopupMenu();
		buildEntityPopupMenu(null);

		generalPopupMenu = new PopupMenu();
		generalPopupMenu.addItem(MenuUtils.createMenuItem("Paste", this::paste));
		generalPopupMenu.addItem(MenuUtils.createMenuItem("Select All", this::selectAll));
	}

	private void buildEntityPopupMenu (Array<EntityProxy> entities) {
		entityPopupMenu.clearChildren();

		if (entities != null) {
			if (isMenuItemEnterIntoGroupValid(entities)) {
				entityPopupMenu.addItem(MenuUtils.createMenuItem("Enter into group", () -> {

					if (isMenuItemEnterIntoGroupValid(entities) == false) {
						DialogUtils.showErrorDialog(Editor.instance.getStage(), "Group was deselected");
						return;
					}

					GroupEntityProxy groupEntityProxy = (GroupEntityProxy) entities.peek();
					currentSelectionGid = groupEntityProxy.getGroupId();
					groupBreadcrumb.addGroup(currentSelectionGid);
					selectAll();
				}));
				entityPopupMenu.addSeparator();
			}
		}

		entityPopupMenu.addItem(MenuUtils.createMenuItem("Cut", this::cut));
		entityPopupMenu.addItem(MenuUtils.createMenuItem("Copy", this::copy));
		entityPopupMenu.addItem(MenuUtils.createMenuItem("Paste", this::paste));
		entityPopupMenu.addItem(MenuUtils.createMenuItem("Remove", this::deleteSelectedEntities));
		entityPopupMenu.addItem(MenuUtils.createMenuItem("Select All", this::selectAll));
	}

	private boolean isMenuItemEnterIntoGroupValid (Array<EntityProxy> entities) {
		return (entities.size == 1 && entities.peek() instanceof GroupEntityProxy);
	}

	private void copy () {
		if (selectedEntities.size > 0) {
			entitiesClipboard.clear();
			selectedEntities.forEach(proxy -> proxy.getEntities().forEach(
					entity -> entitiesClipboard.add(sceneIO.createProtoEntity(entityEngine, entity, false))));

			EntityProxy proxy = selectedEntities.peek();

			if (entityPopupMenu.getParent() != null) { //is menu visible
				copyAttachX = menuX - proxy.getX();
				copyAttachY = menuY - proxy.getY();
			} else {
				copyAttachX = proxy.getWidth() / 2;
				copyAttachY = proxy.getHeight() / 2;
			}
		} else
			statusBar.setText("Nothing to copy!");
	}

	private void paste () {
		if (entitiesClipboard.size > 0) {
			selectedEntities.clear();

			Array<EntityProxy> proxies = new Array<>(selectedEntities.size);
			ObjectSet<Entity> entities = new ObjectSet<>(selectedEntities.size);

			entitiesClipboard.forEach(protoEntity -> {
				Entity entity = protoEntity.build();
				entities.add(entity);
				if (scene.getActiveLayer().visible == false) entity.edit().add(new InvisibleComponent());
				proxies.add(entityProxyCache.get(entity));
			});

			float x = camera.getInputX();
			float y = camera.getInputY();

			EntityProxy baseProxy = proxies.peek();
			float xOffset = baseProxy.getX();
			float yOffset = baseProxy.getY();

			for (EntityProxy proxy : proxies) {
				float px = x - copyAttachX + (proxy.getX() - xOffset);
				float py = y - copyAttachY + (proxy.getY() - yOffset);

				proxy.setPosition(px, py);
				proxy.setLayerId(scene.getActiveLayerId());
			}

			undoModule.add(new EntitiesAddedAction(sceneContainer, entityEngine, entities));

			selectedEntitiesChanged();
		} else
			statusBar.setText("Nothing to paste!");
	}

	private void cut () {
		copy();
		deleteSelectedEntities();
	}

	private void deleteSelectedEntities () {
		ObjectSet<Entity> entities = new ObjectSet<>();

		selectedEntities.forEach(proxy -> entities.addAll(proxy.getEntities()));
		selectedEntities.clear();
		selectedEntitiesChanged();

		undoModule.execute(new EntitiesRemovedAction(sceneContainer, entityEngine, entities));
	}

	public void processDropPayload (Payload payload) {
		if (scene.getActiveLayer().locked) {
			statusBar.setText("Layer is locked!");
			return;
		}

		Object obj = payload.getObject();

		Entity entity = null;

		if (obj instanceof TextureAssetDescriptor) {
			TextureAssetDescriptor asset = (TextureAssetDescriptor) obj;

			entity = new EntityBuilder(entityEngine)
					.with(new SpriteComponent(textureCache.getSprite(asset, scene.pixelsPerUnit)),
							new AssetComponent(asset),
							new RenderableComponent(0), new LayerComponent(scene.getActiveLayerId()))
					.build();

		} else if (obj instanceof BmpFontAsset || obj instanceof TtfFontAsset) {
			VisAssetDescriptor asset = (VisAssetDescriptor) obj;

			entity = new EntityBuilder(entityEngine)
					.with(new TextComponent(fontCache.getGeneric(asset, scene.pixelsPerUnit), FontCacheModule.DEFAULT_TEXT),
							new PixelsPerUnitComponent(scene.pixelsPerUnit),
							new AssetComponent(asset),
							new RenderableComponent(0), new LayerComponent(scene.getActiveLayerId()),
							new ExporterDropsComponent(PixelsPerUnitComponent.class))
					.build();

		} else if (obj instanceof PathAsset) {
			PathAsset asset = (PathAsset) obj;

			if (asset.getPath().startsWith("sound/")) {
				entity = new EntityBuilder(entityEngine)
						.with(new SoundComponent(null), new PositionComponent(), //editor does not require sound to be loaded, we can pass null sound here
								new AssetComponent(asset),
								new RenderableComponent(0), new LayerComponent(scene.getActiveLayerId()),
								new ExporterDropsComponent(PositionComponent.class, RenderableComponent.class, LayerComponent.class, GroupComponent.class))
						.build();

			}

			if (asset.getPath().startsWith("music/")) {
				entity = new EntityBuilder(entityEngine)
						.with(new MusicComponent(new DummyMusic()), new PositionComponent(),
								new AssetComponent(asset),
								new RenderableComponent(0), new LayerComponent(scene.getActiveLayerId()),
								new ExporterDropsComponent(PositionComponent.class, RenderableComponent.class, LayerComponent.class, GroupComponent.class))
						.build();

			}

			if (asset.getPath().startsWith("particle/")) {
				float scale = 1f / scene.pixelsPerUnit;
				entity = new EntityBuilder(entityEngine)
						.with(new ParticleComponent(particleCache.get(asset, scale)), new PixelsPerUnitComponent(scene.pixelsPerUnit),
								new AssetComponent(asset),
								new RenderableComponent(0), new LayerComponent(scene.getActiveLayerId()),
								new ExporterDropsComponent(PixelsPerUnitComponent.class))
						.build();
			}
		}

		for (EditorEntitySupport support : supportModule.getSupports()) {
			Entity supportEntity = support.processDropPayload(entityEngine, scene, obj);
			if (supportEntity != null) {
				entity = supportEntity;
				break;
			}
		}

		if (entity != null) {
			entity.edit().add(new UUIDComponent());

			EntityProxy proxy = entityProxyCache.get(entity);

			float x = camera.getInputX() - proxy.getWidth() / 2;
			float y = camera.getInputY() - proxy.getHeight() / 2;
			proxy.setPosition(x, y);

			undoModule.add(new EntitiesAddedAction(sceneContainer, entityEngine, entity));

			if (currentSelectionGid != -1)
				proxy.addGroup(currentSelectionGid);
		}
	}

	public void switchTool (Tool tool) {
		if (currentTool != null) currentTool.deactivated();
		currentTool = tool;
		currentTool.activated();

		toolPropertiesContainer.reset();
		VisTable table = currentTool.getToolPropertiesUI();
		if (table != null)
			toolPropertiesContainer.add(table).expandX().fillX();
	}

	public void findEntityBaseGroupAndSelect (EntityProxy proxy) {
		groupBreadcrumb.resetHierarchy();
		IntArray array = proxy.getGroupsIds();
		if (array.size > 0) {
			array.reverse();

			currentSelectionGid = array.peek();

			for (int i = 0; i < array.size; i++) {
				int gid = array.get(i);
				groupBreadcrumb.addGroup(gid);
			}
		}

		if (proxy instanceof GroupEntityProxy)
			selectAll();
		else
			select(proxy);
	}

	public void select (Entity entity) {
		select(entityProxyCache.get(entity));
	}

	public void select (EntityProxy proxy) {
		Layer layer = scene.getLayerById(proxy.getLayerID());
		if (layer.locked) return;
		scene.setActiveLayer(layer.id);

		selectedEntities.clear();

		checkProxyGid(proxy);

		selectAddToList(proxy);
		selectedEntitiesChanged();
	}

	/** Appends to current selection, however if entity layer is different than current layer then selection will be reset */
	public void selectAppend (Entity entity) {
		selectAppend(entityProxyCache.get(entity));
	}

	/** Appends to current selection, however if entity layer is different than current layer then selection will be reset */
	public void selectAppend (EntityProxy proxy) {
		Layer layer = scene.getLayerById(proxy.getLayerID());
		if (layer.locked) return;

		if (scene.getActiveLayerId() != layer.id) {
			scene.setActiveLayer(layer.id);
			selectedEntities.clear();
		}

		checkProxyGid(proxy);

		selectAddToList(proxy);
		selectedEntitiesChanged();
	}

	private void checkProxyGid (EntityProxy proxy) {
		int proxyGid = proxy.getLastGroupId();
		if (proxy.groupsContains(currentSelectionGid) == false) {
			if (groupBreadcrumb.isInHierarchy(proxyGid)) {
				currentSelectionGid = proxyGid;
				groupBreadcrumb.trimToGid(proxyGid);
			} else {
				currentSelectionGid = -1;
				groupBreadcrumb.resetHierarchy();
			}
		}
	}

	public void selectAll () {
		if (scene.getActiveLayer().locked) return;

		selectedEntities.clear();

		if (currentSelectionGid == -1) {
			int layerId = scene.getActiveLayerId();

			entityProxyCache.getCache().values().forEach(proxy -> {
				if (proxy.getLayerID() == layerId) {
					selectAddToList(proxy);
				}
			});

		} else {
			GroupEntityProxy proxy = groupProxyProvider.getGroupEntityProxy(currentSelectionGid);
			proxy.getProxies().forEach(this::selectAddToList);
		}

		selectedEntitiesChanged();
	}

	private void selectAddToList (EntityProxy proxy) {
		int lastGid = currentSelectionGid == -1 ? proxy.getLastGroupId() : proxy.getGroupIdBefore(currentSelectionGid);
		if (proxy instanceof GroupEntityProxy == false && lastGid != -1) {
			if (isGroupIdAlreadySelected(lastGid) == false) {
				selectedEntities.add(groupProxyProvider.getGroupEntityProxy(lastGid));
			}
		} else
			selectedEntities.add(proxy);
	}

	public boolean isSelected (EntityProxy proxy) {
		return selectedEntities.contains(proxy, true);
	}

	public boolean isGroupIdAlreadySelected (int gid) {
		for (EntityProxy proxy : selectedEntities) {
			if (proxy instanceof GroupEntityProxy) {
				GroupEntityProxy groupProxy = (GroupEntityProxy) proxy;
				if (groupProxy.getGroupId() == gid) return true;
			}
		}

		return false;
	}

	public void resetSelection () {
		selectedEntities.clear();
		selectedEntitiesChanged();
	}

	public void deselect (EntityProxy result) {
		selectedEntities.removeValue(result, true);
		selectedEntitiesChanged();
	}

	public void selectedEntitiesChanged () {
		entityProperties.selectedEntitiesChanged();
		sceneOutline.selectedEntitiesChanged();
		currentTool.selectedEntitiesChanged();
		markSceneDirty();
	}

	public void selectedEntitiesValuesChanged () {
		entityProperties.selectedEntitiesValuesChanged();
		currentTool.selectedEntitiesValuesChanged();
		markSceneDirty();
	}

	public void groupSelection () {
		if (selectedEntities.size <= 1) {
			statusBar.setText("Noting to group!");
			return;
		}

		int gid = groupIdProvider.getFreeGroupIndex();

		undoModule.execute(new GroupAction(selectedEntities, gid, true));

		sceneOutline.rebuildOutline();

		GroupEntityProxy groupProxy = new GroupEntityProxy(selectedEntities, gid);
		resetSelection();
		select(groupProxy);
	}

	public void ungroupSelection () {
		if (selectedEntities.size == 0) {
			statusBar.setText("Noting to ungroup!");
			return;
		}

		GroupEntityProxy selectionProxy = null; //proxy that will be used later to select group objects

		UndoableActionGroup actionGroup = new UndoableActionGroup("Ungroup");

		for (EntityProxy entity : selectedEntities) {
			if (entity instanceof GroupEntityProxy) {
				GroupEntityProxy group = (GroupEntityProxy) entity;
				selectionProxy = group;
				actionGroup.add(new GroupAction(group.getProxies(), group.getGroupId(), false));
			}
		}

		if (selectionProxy != null) {
			actionGroup.finalizeGroup();
			undoModule.execute(actionGroup);

			groupBreadcrumb.resetHierarchy();
			currentSelectionGid = -1;

			sceneOutline.rebuildOutline();

			resetSelection();
			selectionProxy.getProxies().forEach(this::selectAppend);
		} else
			statusBar.setText("No group selected!");
	}

	public void markSceneDirty () {
		sceneTab.dirty();
	}

	@Override
	public void render (Batch batch) {
		batch.end();
		shapeRenderer.setProjectionMatrix(camera.getCombinedMatrix());

		if (selectedEntities.size > 0) {
			shapeRenderer.setColor(Color.WHITE);
			shapeRenderer.begin(ShapeType.Line);

			for (EntityProxy entity : selectedEntities) {
				Rectangle bounds = entity.getBoundingRectangle();
				shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
			}

			shapeRenderer.end();

		}

		currentTool.render(shapeRenderer);

		batch.begin();

		currentTool.render(batch);
	}

	@Override
	public boolean onEvent (Event event) {
		if (event instanceof UndoEvent || event instanceof RedoEvent) {
			sceneOutline.rebuildOutline();
			renderBatchingSystem.markDirty();
		}

		return false;
	}

	@Subscribe
	public void handleToolSwitch (ToolSwitchedEvent event) {
		if (event.newToolId == Tools.SELECTION_TOOL)
			switchTool(selectionTool);
		if (event.newToolId == Tools.POLYGON_TOOL)
			switchTool(polygonTool);
	}

	@Override
	public void dispose () {
		App.oldEventBus.unregister(this);
		App.eventBus.unregister(this);
		layersDialog.dispose();
		entityProperties.dispose();
	}

	public EntityProperties getEntityProperties () {
		return entityProperties;
	}

	public LayersDialog getLayersDialog () {
		return layersDialog;
	}

	public GroupBreadcrumb getGroupBreadcrumb () {
		return groupBreadcrumb;
	}

	public AlignmentToolsDialog getAlignmentToolsDialog () {
		return alignmentToolsDialog;
	}

	public SceneOutline getSceneOutline () {
		return sceneOutline;
	}

	public VisTable getToolPropertiesContainer () {
		return toolPropertiesContainer;
	}

	public ImmutableArray<EntityProxy> getSelectedEntities () {
		return immutableSelectedEntities;
	}

	@Override
	public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
		if (scene.getActiveLayer().locked) return false;
		return currentTool.touchDown(event, x, y, pointer, button);
	}

	@Override
	public void touchDragged (InputEvent event, float x, float y, int pointer) {
		if (scene.getActiveLayer().locked) return;
		mouseDragged = true;
		currentTool.touchDragged(event, x, y, pointer);
	}

	@Override
	public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
		if (scene.getActiveLayer().locked) return;
		currentTool.touchUp(event, x, y, pointer, button);

		if (button == Buttons.RIGHT && mouseDragged == false) {
			if (selectedEntities.size > 0) {
				menuX = x;
				menuY = y;

				buildEntityPopupMenu(selectedEntities);
				entityPopupMenu.showMenu(event.getStage(), event.getStageX(), event.getStageY());
			} else
				generalPopupMenu.showMenu(event.getStage(), event.getStageX(), event.getStageY());
		}

		mouseDragged = false;
	}

	@Override
	public boolean mouseMoved (InputEvent event, float x, float y) {
		if (scene.getActiveLayer().locked) return false;
		return currentTool.mouseMoved(event, x, y);
	}

	@Override
	public void enter (InputEvent event, float x, float y, int pointer, Actor fromActor) {
		if (scene.getActiveLayer().locked) return;
		currentTool.enter(event, x, y, pointer, fromActor);
	}

	@Override
	public void exit (InputEvent event, float x, float y, int pointer, Actor toActor) {
		if (scene.getActiveLayer().locked) return;
		currentTool.exit(event, x, y, pointer, toActor);
	}

	@Override
	public boolean scrolled (InputEvent event, float x, float y, int amount) {
		if (scene.getActiveLayer().locked) return false;
		return currentTool.scrolled(event, x, y, amount);
	}

	@Override
	public boolean keyDown (InputEvent event, int keycode) {
		if (scene.getActiveLayer().locked) return false;
		boolean result = currentTool.keyDown(event, keycode);

		if (result == false) {
//			cancelMoveEntityTask();

			if (keycode == Keys.FORWARD_DEL) { //Delete
				deleteSelectedEntities();
				return true;
			}

			if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && keycode == Keys.S) sceneTab.save();
			if (keycode == Keys.F1) {
				switchTool(selectionTool);
				App.eventBus.post(new ToolSwitchedEvent(Tools.SELECTION_TOOL));
			}
			if (keycode == Keys.F2) {
				switchTool(polygonTool);
				App.eventBus.post(new ToolSwitchedEvent(Tools.POLYGON_TOOL));
			}

			if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && keycode == Keys.A) selectAll();
			if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && keycode == Keys.C) copy();
			if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && keycode == Keys.V) paste();
			if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && keycode == Keys.X) cut();
			if (Gdx.input.isKeyPressed(Keys.PAGE_UP))
				zIndexManipulator.moveSelectedEntities(getSelectedEntities(), true);
			if (Gdx.input.isKeyPressed(Keys.PAGE_DOWN))
				zIndexManipulator.moveSelectedEntities(getSelectedEntities(), false);

			float delta = 10;
			if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)) delta *= 10;
			if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)) delta *= 10;

			delta = delta / scene.pixelsPerUnit;

			int direction = 0;

			if (Gdx.input.isKeyPressed(Keys.UP)) direction = direction | UP;
			else if (Gdx.input.isKeyPressed(Keys.DOWN)) direction = direction | DOWN;
			if (Gdx.input.isKeyPressed(Keys.LEFT)) direction = direction | LEFT;
			else if (Gdx.input.isKeyPressed(Keys.RIGHT)) direction = direction | RIGHT;

			if (direction > 0) {
				entityMoveTimerTask.set(direction, delta);

				if (entityMoveTimerTask.isScheduled() == false) {
					keyMoveAction = new MoveEntitiesAction(this, selectedEntities);

					entityMoveTimerTask.run();
					float keyRepeatInitialTime = 0.4f;
					float keyRepeatTime = 0.05f;
					Timer.schedule(entityMoveTimerTask, keyRepeatInitialTime, keyRepeatTime);
				}
				return true;
			}

			return false;
		}

		return true;
	}

	@Override
	public boolean keyUp (InputEvent event, int keycode) {
		if (scene.getActiveLayer().locked) return false;
		if ((Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.RIGHT)) == false)
			cancelMoveEntityTask(); //do not cancel task untill all keys are released
		return currentTool.keyUp(event, keycode);
	}

	private void cancelMoveEntityTask () {
		entityMoveTimerTask.cancel();
		if (keyMoveAction != null) {
			keyMoveAction.saveNewData();
			undoModule.add(keyMoveAction);
			keyMoveAction = null;
		}
	}

	@Override
	public boolean keyTyped (InputEvent event, char character) {
		if (scene.getActiveLayer().locked) return false;
		return currentTool.keyTyped(event, character);
	}
}

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

package com.kotcrab.vis.runtime.system;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Wire;
import com.artemis.utils.Bag;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.kotcrab.vis.runtime.component.LayerComponent;
import com.kotcrab.vis.runtime.component.RenderableComponent;
import com.kotcrab.vis.runtime.component.ShaderComponent;
import com.kotcrab.vis.runtime.scene.LayerCordsSystem;
import net.mostlyoriginal.api.system.delegate.EntityProcessAgent;
import net.mostlyoriginal.api.system.delegate.EntityProcessPrincipal;
import net.mostlyoriginal.api.utils.BagUtils;

/**
 * Plugable render system that uses subsystems to render entities. Entities are rendered sorted by their layer and zIndex
 * , if entity layer or zIndex has changed, {@link #markDirty()} must be called in order to resort the scene. System
 * will automatically resort if entity was added.
 * @author Daan van Yperen
 */
@Wire(failOnNull = false)
public class RenderBatchingSystem extends BaseSystem implements EntityProcessPrincipal {
	private CameraManager cameraManager;
	private LayerManager layerManager;

	private ComponentMapper<LayerComponent> layerCm;
	private ComponentMapper<RenderableComponent> renderableCm;
	private ComponentMapper<ShaderComponent> shaderCm;

	private boolean sortedDirty = false;
	private final Bag<Job> sortedJobs = new Bag<Job>();

	private Batch batch;
	private boolean usingFromEditor;

	public RenderBatchingSystem (Batch batch, boolean usingFromEditor) {
		this.batch = batch;
		this.usingFromEditor = usingFromEditor;
	}

	/**
	 * Declare entity relevant for agent.
	 * <p>
	 * After this is called, the principal can use the agent
	 * interface to begin/end and process the given entity.
	 * @param e entity to process
	 * @param agent interface to dispatch with.
	 */
	@Override
	public void registerAgent (Entity e, EntityProcessAgent agent) {
		if (!renderableCm.has(e))
			throw new RuntimeException("RenderBatchingSystem requires agents entities to have component Renderable.");
		// register new job. this will influence sorting order.
		sortedJobs.add(new Job(e, agent));
		sortedDirty = true;
	}

	/**
	 * Revoke relevancy of entity for agent.
	 * <p>
	 * After this is called, the principal should no longer
	 * attempt to process the entity with the agent.
	 * @param e entity to process
	 * @param agent interface to dispatch with.
	 */
	@Override
	public void unregisterAgent (Entity e, EntityProcessAgent agent) {
		// forget about the job.
		final Object[] data = sortedJobs.getData();
		for (int i = 0, s = sortedJobs.size(); i < s; i++) {
			final Job e2 = (Job) data[i];
			if (e2.entity == e && e2.agent == agent) {
				sortedJobs.remove(i);
				sortedDirty = true;
				break;
			}
		}
	}

	@Override
	protected void processSystem () {
		cameraManager.getCamera().update();
		cameraManager.getUiCamera().update();

		LayerCordsSystem activeCordsSystem = LayerCordsSystem.WORLD;
		batch.setProjectionMatrix(cameraManager.getCombined());

		if (usingFromEditor == false) batch.begin();

		if (sortedDirty) {
			sortedDirty = false;
			BagUtils.sort(sortedJobs);
		}

		// iterate through all the jobs.
		EntityProcessAgent activeAgent = null;
		final Object[] data = sortedJobs.getData();
		for (int i = 0, s = sortedJobs.size(); i < s; i++) {
			final Job job = (Job) data[i];
			final EntityProcessAgent agent = job.agent;

			boolean changedBatchState = false;
			final boolean shaderUsed = shaderCm.has(job.entity);
			LayerCordsSystem cordsSystem = null;

			if (usingFromEditor == false)
				cordsSystem = layerManager.getData(layerCm.get(job.entity).layerId).cordsSystem;

			// agent changed? end() the last agent, and begin() the next agent.
			if (agent != activeAgent) {
				if (activeAgent != null) {
					activeAgent.end();
				}
				activeAgent = agent;
				activeAgent.begin();
			}

			if (shaderUsed) {
				changedBatchState = true;
				batch.end();
				batch.setShader(shaderCm.get(job.entity).shader);
			}

			if (usingFromEditor == false && cordsSystem != activeCordsSystem) {
				activeCordsSystem = cordsSystem;

				switch (activeCordsSystem) {
					case WORLD:
						batch.setProjectionMatrix(cameraManager.getCombined());
						break;
					case SCREEN:
						batch.setProjectionMatrix(cameraManager.getUiCombined());
						break;
				}
			}

			if (changedBatchState) batch.begin();

			agent.process(job.entity);

			if (shaderUsed) batch.setShader(null);
		}

		// finished, terminate final agent.
		if (activeAgent != null) {
			activeAgent.end();
		}

		if (usingFromEditor == false) batch.end();
	}

	public Batch getBatch () {
		return batch;
	}

	public void markDirty () {
		sortedDirty = true;
	}

	public boolean isDirty () {
		return sortedDirty;
	}

	/** Rendering job wrapper. */
	public class Job implements Comparable<Job> {
		public final Entity entity;
		public final EntityProcessAgent agent;

		/**
		 * @param entity entity we will process
		 * @param agent agent responsible for processing.
		 */
		public Job (final Entity entity, final EntityProcessAgent agent) {
			this.entity = entity;
			this.agent = agent;
		}

		@Override
		public int compareTo (Job o) {
			int layerResult = (int) Math.signum(layerCm.get(this.entity).layerId - layerCm.get(o.entity).layerId);

			if (layerResult == 0)
				return (int) Math.signum(renderableCm.get(this.entity).zIndex - renderableCm.get(o.entity).zIndex);
			else
				return layerResult;
		}
	}
}

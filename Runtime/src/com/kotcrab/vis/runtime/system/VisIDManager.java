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

import com.artemis.*;
import com.artemis.EntitySubscription.SubscriptionListener;
import com.artemis.annotations.Wire;
import com.artemis.utils.ImmutableBag;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.kotcrab.vis.runtime.component.IDComponent;

/**
 * Allows to get entities by their string id that was set in VisEditor
 * @author Kotcrab
 */
@Wire
public class VisIDManager extends Manager {
	private ComponentMapper<IDComponent> idCm;
	private AspectSubscriptionManager subscriptionManager;

	private ObjectMap<String, Array<Entity>> idStore = new ObjectMap<String, Array<Entity>>();

	@Override
	protected void initialize () {
		EntitySubscription subscription = subscriptionManager.get(Aspect.all(IDComponent.class));

		subscription.addSubscriptionListener(new SubscriptionListener() {
			@Override
			public void inserted (ImmutableBag<Entity> entities) {
				for (Entity entity : entities) {
					String id = idCm.get(entity).id;

					Array<Entity> idList = idStore.get(id);

					if (idList == null) {
						idList = new Array<Entity>();
						idStore.put(id, idList);
					}

					idList.add(entity);
				}
			}

			@Override
			public void removed (ImmutableBag<Entity> entities) {
				for (Entity entity : entities) {
					String id = idCm.get(entity).id;

					Array<Entity> idList = idStore.get(id);
					idList.removeValue(entity, true);

					if (idList.size == 0) {
						idStore.remove(id);
					}
				}
			}
		});
	}

	/**
	 * Returns entity for given ID. If multiple entities has the same id only the first one will be returned.
	 * @see #getMultiple(String)
	 */
	public Entity get (String id) {
		return getMultiple(id).get(0);
	}

	/**
	 * Returns all entities with this ID.
	 * @see #get(String)
	 */
	public Array<Entity> getMultiple (String id) {
		Array<Entity> entities = idStore.get(id);
		if (entities == null) throw new IllegalStateException("Could not find any entity with ID: " + id);
		return entities;
	}
}

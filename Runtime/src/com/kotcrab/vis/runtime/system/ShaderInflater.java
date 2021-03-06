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
import com.artemis.annotations.Wire;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.kotcrab.vis.runtime.component.ShaderComponent;
import com.kotcrab.vis.runtime.component.ShaderProtoComponent;

/**
 * Inflates {@link ShaderProtoComponent} into {@link ShaderComponent}
 * @author Kotcrab
 */
@Wire
public class ShaderInflater extends Manager {
	private ComponentMapper<ShaderProtoComponent> protoCm;

	private EntityTransmuter transmuter;

	private AssetManager manager;

	public ShaderInflater (AssetManager manager) {
		this.manager = manager;
	}

	@Override
	protected void initialize () {
		EntityTransmuterFactory factory = new EntityTransmuterFactory(world).remove(ShaderProtoComponent.class);
		transmuter = factory.build();
	}

	@Override
	public void added (Entity e) {
		if (protoCm.has(e) == false) return;

		ShaderProtoComponent protoComponent = protoCm.get(e);

		if (protoComponent.asset != null) {
			ShaderProgram program = manager.get(protoComponent.asset.getPathWithoutExtension(), ShaderProgram.class);

			transmuter.transmute(e);
			e.edit().add(new ShaderComponent(protoComponent.asset, program));
		} else
			transmuter.transmute(e);

	}
}

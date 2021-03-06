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

package com.kotcrab.vis.editor.module.project;

import com.badlogic.gdx.utils.ObjectMap;
import com.kotcrab.vis.editor.module.InjectModule;
import com.kotcrab.vis.editor.module.editor.ExtensionStorageModule;
import com.kotcrab.vis.editor.plugin.ExporterPlugin;

import java.util.UUID;

/**
 * @author Kotcrab
 */
public class ExportersManagerModule extends ProjectModule {
	@InjectModule private ExtensionStorageModule extensionStorage;

	@InjectModule private ExportSettingsModule exportSettings;

	private ObjectMap<UUID, ExporterPlugin> exporters = new ObjectMap<>();

	@Override
	public void init () {
		for (ExporterPlugin exporterPlugin : extensionStorage.getExporterPlugins()) {
			exporters.put(exporterPlugin.getUUID(), exporterPlugin);
			projectContainer.injectModules(exporterPlugin);
			exporterPlugin.init(project);
		}
	}

	public ObjectMap<UUID, ExporterPlugin> getExportersMap () {
		return exporters;
	}

	public void export (boolean quickExport) {
		exporters.get(exportSettings.getCurrentExporterUUID()).export(quickExport);
	}
}

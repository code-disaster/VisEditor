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

package com.kotcrab.vis.editor.module.editor;

import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer;
import com.kotcrab.vis.editor.App;
import com.kotcrab.vis.editor.Log;
import com.kotcrab.vis.editor.module.InjectModule;
import com.kotcrab.vis.editor.module.project.ProjectSettingsIOModule;
import com.kotcrab.vis.editor.serializer.ArraySerializer;
import com.kotcrab.vis.editor.serializer.UUIDSerializer;
import com.kotcrab.vis.editor.ui.toast.DetailsToast;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.util.UUID;

/**
 * Provides common IO for saving editor scope settings. This is typically not used directly, it should be used by using
 * subclassing {@link EditorSettingsModule}. Very similar to {@link ProjectSettingsIOModule} but saves editor
 * specific settings.
 * @author Kotcrab
 * @see ProjectSettingsIOModule
 */
public class EditorSettingsIOModule extends EditorModule {
	@InjectModule private ToastModule toastModule;

	private Kryo kryo;
	private File settingsDirectory;

	@Override
	public void init () {
		kryo = new Kryo();
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		kryo.setDefaultSerializer(TaggedFieldSerializer.class);
		kryo.register(Array.class, new ArraySerializer(), 10);
		kryo.register(UUID.class, new UUIDSerializer(), 11);

		settingsDirectory = new File(App.APP_FOLDER_PATH, "settings");
		settingsDirectory.mkdir();
	}

	public void save (Object configObject, String name) {
		try {
			Output output = new Output(new FileOutputStream(new File(settingsDirectory, name)));
			kryo.writeObject(output, configObject);
			output.close();
		} catch (FileNotFoundException e) {
			Log.exception(e);
		}

	}

	public <T> T load (String name, Class<T> type) {
		File configFile = new File(settingsDirectory, name);

		if (configFile.exists()) {
			try {
				Input input = new Input(new FileInputStream(configFile));
				T config = kryo.readObject(input, type);
				input.close();
				return config;
			} catch (Exception e) {
				toastModule.show(new DetailsToast("Failed to load settings for: '" + type.getSimpleName() + "'", e));
				Log.exception(e);
			}
		}

		try {
			Constructor<T> cos = type.getConstructor();
			return cos.newInstance();
		} catch (ReflectiveOperationException e) {
			Log.exception(e);
		}

		throw new IllegalStateException("Failed to create settings class");
	}
}

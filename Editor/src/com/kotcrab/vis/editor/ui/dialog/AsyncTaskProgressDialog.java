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

package com.kotcrab.vis.editor.ui.dialog;

import com.badlogic.gdx.Gdx;
import com.kotcrab.vis.editor.Editor;
import com.kotcrab.vis.editor.util.AsyncTask;
import com.kotcrab.vis.editor.util.AsyncTaskListener;
import com.kotcrab.vis.ui.util.TableUtils;
import com.kotcrab.vis.ui.util.dialog.DialogUtils;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisProgressBar;
import com.kotcrab.vis.ui.widget.VisWindow;

/**
 * Dialog displayed with {@link AsyncTask} when doing tasks on another thread
 * @author Kotcrab
 */
public class AsyncTaskProgressDialog extends VisWindow {
	public AsyncTaskProgressDialog (String title, AsyncTask task) {
		super(title);
		setModal(true);

		TableUtils.setSpacingDefaults(this);

		final VisLabel statusLabel = new VisLabel("Please wait...");
		final VisProgressBar progressBar = new VisProgressBar(0, 100, 1, false);

		defaults().padLeft(6).padRight(6);

		add(statusLabel).padTop(6).left().row();
		add(progressBar).width(300).padTop(6).padBottom(6);

		task.setListener(new AsyncTaskListener() {
			@Override
			public void progressChanged (int newProgressPercent) {
				Gdx.app.postRunnable(() -> progressBar.setValue(newProgressPercent));
			}

			@Override
			public void messageChanged (String newMsg) {
				Gdx.app.postRunnable(() -> statusLabel.setText(newMsg));
			}

			@Override
			public void finished () {
				fadeOut();
			}

			@Override
			public void failed (String reason) {
				failed(reason, null);
			}

			@Override
			public void failed (String reason, Exception ex) {
				DialogUtils.showErrorDialog(Editor.instance.getStage(), reason == null ? "Unknown error occurred" : reason, ex);
			}
		});

		pack();
		centerWindow();

		task.start();
	}

}

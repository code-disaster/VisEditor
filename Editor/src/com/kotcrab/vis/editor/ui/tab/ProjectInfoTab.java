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

package com.kotcrab.vis.editor.ui.tab;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.editor.module.project.Project;
import com.kotcrab.vis.editor.ui.tabbedpane.MainContentTab;
import com.kotcrab.vis.editor.ui.tabbedpane.TabViewMode;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

/**
 * Tab that displays basic information about project. In the feature it shoud also provide recently used files, etc.
 * @author Kotcrab
 */
public class ProjectInfoTab extends MainContentTab {
	private Table content;

	public ProjectInfoTab (Project project) {
		super(false, false);
		content = new VisTable(true);

		content.add(new VisLabel("To create new scene use Scene -> New Scene...")).row();
		content.add(new VisLabel("To load scene navigate to scene directory in \nAssets Browser bellow and double click it", Align.center)).row();
	}

	@Override
	public String getTabTitle () {
		return "Project Info";
	}

	@Override
	public Table getContentTable () {
		return content;
	}

	@Override
	public TabViewMode getViewMode () {
		return TabViewMode.SPLIT;
	}
}

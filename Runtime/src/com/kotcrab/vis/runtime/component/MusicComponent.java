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

package com.kotcrab.vis.runtime.component;

import com.artemis.Component;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Music.OnCompletionListener;
import com.kotcrab.vis.runtime.util.UsesProtoComponent;

/**
 * Stores entity Music
 * @author Kotcrab
 */
public class MusicComponent extends Component implements UsesProtoComponent {
	public transient Music music;

	private boolean playOnStart;

	private MusicComponent () {
	}

	public MusicComponent (Music music) {
		this.music = music;
	}

	@Override
	public ProtoComponent getProtoComponent () {
		return new MusicProtoComponent(this);
	}

	public boolean isPlayOnStart () {
		return playOnStart;
	}

	public void setPlayOnStart (boolean playOnStart) {
		this.playOnStart = playOnStart;
	}

	public void setOnCompletionListener (OnCompletionListener listener) {
		music.setOnCompletionListener(listener);
	}

	public float getPosition () {
		return music.getPosition();
	}

	public void setMusicPosition (float position) {
		music.setPosition(position);
	}

	public float getVolume () {
		return music.getVolume();
	}

	public void setVolume (float volume) {
		music.setVolume(volume);
	}

	public void setPan (float pan, float volume) {
		music.setPan(pan, volume);
	}

	public boolean isLooping () {
		return music.isLooping();
	}

	public void setLooping (boolean isLooping) {
		music.setLooping(isLooping);
	}

	public boolean isPlaying () {
		return music.isPlaying();
	}

	public void stop () {
		music.stop();
	}

	public void pause () {
		music.pause();
	}

	public void play () {
		music.play();
	}
}

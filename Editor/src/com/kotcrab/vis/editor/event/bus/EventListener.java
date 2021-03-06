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

package com.kotcrab.vis.editor.event.bus;

/**
 * EventListener for {@link EventBus}
 * @author Kotcrab
 */
@Deprecated
public interface EventListener {
	/**
	 * Called when this listener should handle event
	 * @return if true this event will be handle and other listeners won't receive it.
	 * Most of the time you don't want to handle event, return true only in special use cases.
	 */
	boolean onEvent (Event event);
}

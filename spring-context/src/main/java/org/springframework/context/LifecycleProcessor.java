/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context;

/**
 * Strategy interface for processing Lifecycle beans within the ApplicationContext.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0
 */
public interface LifecycleProcessor extends Lifecycle {

	/**
	 * Notification of context refresh for auto-starting components.
	 * @see ConfigurableApplicationContext#refresh()
	 */
	default void onRefresh() {
		start();
	}

	/**
	 * Notification of context restart for auto-stopping and subsequently
	 * auto-starting components.
	 * @since 7.0
	 * @see ConfigurableApplicationContext#restart()
	 */
	default void onRestart() {
		stop();
		start();
	}

	/**
	 * Notification of context pause for auto-stopping components.
	 * @since 7.0
	 * @see ConfigurableApplicationContext#pause()
	 */
	default void onPause() {
		stop();
	}

	/**
	 * Notification of context close phase for auto-stopping components
	 * before destruction.
	 * @see ConfigurableApplicationContext#close()
	 */
	default void onClose() {
		stop();
	}

}

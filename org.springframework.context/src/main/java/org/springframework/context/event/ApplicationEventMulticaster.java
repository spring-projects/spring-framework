 /*
 * Copyright 2002-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * Interface to be implemented by objects that can manage a number
 * of ApplicationListeners, and publish events to them. An example
 * of such an object is an ApplicationEventPublisher, typically
 * the ApplicationContext, which can use an ApplicationEventMulticaster
 * as a helper to publish events to listeners.
 *
 * @author Rod Johnson
 */
public interface ApplicationEventMulticaster {

	/**
	 * Add a listener to be notified of all events.
	 * @param listener the listener to add
	 */
	void addApplicationListener(ApplicationListener listener);

	/**
	 * Remove a listener from the notification list.
	 * @param listener the listener to remove
	 */
	void removeApplicationListener(ApplicationListener listener);

	/**
	 * Remove all listeners registered with this multicaster.
	 * It will perform no action on event notification until more
	 * listeners are registered.
	 */
	void removeAllListeners();

	/**
	 * Multicast the given application event to appropriate listeners.
	 * @param event the event to multicast
	 */
	void multicastEvent(ApplicationEvent event);

}

/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.context.event;

import java.util.function.Predicate;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * Interface to be implemented by objects that can manage a number of
 * {@link ApplicationListener} objects and publish events to them.
 *
 * <p>An {@link org.springframework.context.ApplicationEventPublisher}, typically
 * a Spring {@link org.springframework.context.ApplicationContext}, can use an
 * {@code ApplicationEventMulticaster} as a delegate for actually publishing events.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see ApplicationListener
 */
public interface ApplicationEventMulticaster {

	/**
	 * Add a listener to be notified of all events.
	 * @param listener the listener to add
	 * @see #removeApplicationListener(ApplicationListener)
	 * @see #removeApplicationListeners(Predicate)
	 */
	void addApplicationListener(ApplicationListener<?> listener);

	/**
	 * Add a listener bean to be notified of all events.
	 * @param listenerBeanName the name of the listener bean to add
	 * @see #removeApplicationListenerBean(String)
	 * @see #removeApplicationListenerBeans(Predicate)
	 */
	void addApplicationListenerBean(String listenerBeanName);

	/**
	 * Remove a listener from the notification list.
	 * @param listener the listener to remove
	 * @see #addApplicationListener(ApplicationListener)
	 * @see #removeApplicationListeners(Predicate)
	 */
	void removeApplicationListener(ApplicationListener<?> listener);

	/**
	 * Remove a listener bean from the notification list.
	 * @param listenerBeanName the name of the listener bean to remove
	 * @see #addApplicationListenerBean(String)
	 * @see #removeApplicationListenerBeans(Predicate)
	 */
	void removeApplicationListenerBean(String listenerBeanName);

	/**
	 * Remove all matching listeners from the set of registered
	 * {@code ApplicationListener} instances (which includes adapter classes
	 * such as {@link ApplicationListenerMethodAdapter}, e.g. for annotated
	 * {@link EventListener} methods).
	 * <p>Note: This just applies to instance registrations, not to listeners
	 * registered by bean name.
	 * @param predicate the predicate to identify listener instances to remove,
	 * e.g. checking {@link SmartApplicationListener#getListenerId()}
	 * @since 5.3.5
	 * @see #addApplicationListener(ApplicationListener)
	 * @see #removeApplicationListener(ApplicationListener)
	 */
	void removeApplicationListeners(Predicate<ApplicationListener<?>> predicate);

	/**
	 * Remove all matching listener beans from the set of registered
	 * listener bean names (referring to bean classes which in turn
	 * implement the {@link ApplicationListener} interface directly).
	 * <p>Note: This just applies to bean name registrations, not to
	 * programmatically registered {@code ApplicationListener} instances.
	 * @param predicate the predicate to identify listener bean names to remove
	 * @since 5.3.5
	 * @see #addApplicationListenerBean(String)
	 * @see #removeApplicationListenerBean(String)
	 */
	void removeApplicationListenerBeans(Predicate<String> predicate);

	/**
	 * Remove all listeners registered with this multicaster.
	 * <p>After a remove call, the multicaster will perform no action
	 * on event notification until new listeners are registered.
	 * @see #removeApplicationListeners(Predicate)
	 */
	void removeAllListeners();

	/**
	 * Multicast the given application event to appropriate listeners.
	 * <p>Consider using {@link #multicastEvent(ApplicationEvent, ResolvableType)}
	 * if possible as it provides better support for generics-based events.
	 * <p>If a matching {@code ApplicationListener} does not support asynchronous
	 * execution, it must be run within the calling thread of this multicast call.
	 * @param event the event to multicast
	 * @see ApplicationListener#supportsAsyncExecution()
	 */
	void multicastEvent(ApplicationEvent event);

	/**
	 * Multicast the given application event to appropriate listeners.
	 * <p>If the {@code eventType} is {@code null}, a default type is built
	 * based on the {@code event} instance.
	 * <p>If a matching {@code ApplicationListener} does not support asynchronous
	 * execution, it must be run within the calling thread of this multicast call.
	 * @param event the event to multicast
	 * @param eventType the type of event (can be {@code null})
	 * @since 4.2
	 * @see ApplicationListener#supportsAsyncExecution()
	 */
	void multicastEvent(ApplicationEvent event, @Nullable ResolvableType eventType);

}

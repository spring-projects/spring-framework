/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.OrderComparator;

/**
 * Abstract implementation of the {@link ApplicationEventMulticaster} interface,
 * providing the basic listener registration facility.
 *
 * <p>Doesn't permit multiple instances of the same listener by default,
 * as it keeps listeners in a linked Set. The collection class used to hold
 * ApplicationListener objects can be overridden through the "collectionClass"
 * bean property.
 *
 * <p>Implementing ApplicationEventMulticaster's actual {@link #multicastEvent} method
 * is left to subclasses. {@link SimpleApplicationEventMulticaster} simply multicasts
 * all events to all registered listeners, invoking them in the calling thread.
 * Alternative implementations could be more sophisticated in those respects.
 *
 * @author Juergen Hoeller
 * @since 1.2.3
 * @see #getApplicationListeners(ApplicationEvent)
 * @see SimpleApplicationEventMulticaster
 */
public abstract class AbstractApplicationEventMulticaster implements ApplicationEventMulticaster, BeanFactoryAware {

	private final ListenerRetriever defaultRetriever = new ListenerRetriever(false);

	private final Map<ListenerCacheKey, ListenerRetriever> retrieverCache =
			new ConcurrentHashMap<ListenerCacheKey, ListenerRetriever>(64);

	private BeanFactory beanFactory;


	public void addApplicationListener(ApplicationListener<?> listener) {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListeners.add(listener);
			this.retrieverCache.clear();
		}
	}

	public void addApplicationListenerBean(String listenerBeanName) {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListenerBeans.add(listenerBeanName);
			this.retrieverCache.clear();
		}
	}

	public void removeApplicationListener(ApplicationListener<?> listener) {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListeners.remove(listener);
			this.retrieverCache.clear();
		}
	}

	public void removeApplicationListenerBean(String listenerBeanName) {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListenerBeans.remove(listenerBeanName);
			this.retrieverCache.clear();
		}
	}

	public void removeAllListeners() {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListeners.clear();
			this.defaultRetriever.applicationListenerBeans.clear();
			this.retrieverCache.clear();
		}
	}

	public final void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	private BeanFactory getBeanFactory() {
		if (this.beanFactory == null) {
			throw new IllegalStateException("ApplicationEventMulticaster cannot retrieve listener beans " +
					"because it is not associated with a BeanFactory");
		}
		return this.beanFactory;
	}


	/**
	 * Return a Collection containing all ApplicationListeners.
	 * @return a Collection of ApplicationListeners
	 * @see org.springframework.context.ApplicationListener
	 */
	protected Collection<ApplicationListener<?>> getApplicationListeners() {
		return this.defaultRetriever.getApplicationListeners();
	}

	/**
	 * Return a Collection of ApplicationListeners matching the given
	 * event type. Non-matching listeners get excluded early.
	 * @param event the event to be propagated. Allows for excluding
	 * non-matching listeners early, based on cached matching information.
	 * @return a Collection of ApplicationListeners
	 * @see org.springframework.context.ApplicationListener
	 */
	protected Collection<ApplicationListener<?>> getApplicationListeners(ApplicationEvent event) {
		Class<? extends ApplicationEvent> eventType = event.getClass();
		Class<?> sourceType = event.getSource().getClass();
		ListenerCacheKey cacheKey = new ListenerCacheKey(eventType, sourceType);
		ListenerRetriever retriever = this.retrieverCache.get(cacheKey);
		if (retriever != null) {
			return retriever.getApplicationListeners();
		}
		else {
			retriever = new ListenerRetriever(true);
			LinkedList<ApplicationListener<?>> allListeners = new LinkedList<ApplicationListener<?>>();
			synchronized (this.defaultRetriever) {
				for (ApplicationListener<?> listener : this.defaultRetriever.applicationListeners) {
					if (supportsEvent(listener, eventType, sourceType)) {
						retriever.applicationListeners.add(listener);
						allListeners.add(listener);
					}
				}
				if (!this.defaultRetriever.applicationListenerBeans.isEmpty()) {
					BeanFactory beanFactory = getBeanFactory();
					for (String listenerBeanName : this.defaultRetriever.applicationListenerBeans) {
						ApplicationListener<?> listener = beanFactory.getBean(listenerBeanName, ApplicationListener.class);
						if (!allListeners.contains(listener) && supportsEvent(listener, eventType, sourceType)) {
							retriever.applicationListenerBeans.add(listenerBeanName);
							allListeners.add(listener);
						}
					}
				}
			}
			OrderComparator.sort(allListeners);
			this.retrieverCache.put(cacheKey, retriever);
			return allListeners;
		}
	}

	/**
	 * Determine whether the given listener supports the given event.
	 * <p>The default implementation detects the {@link SmartApplicationListener}
	 * interface. In case of a standard {@link ApplicationListener}, a
	 * {@link GenericApplicationListenerAdapter} will be used to introspect
	 * the generically declared type of the target listener.
	 * @param listener the target listener to check
	 * @param eventType the event type to check against
	 * @param sourceType the source type to check against
	 * @return whether the given listener should be included in the
	 * candidates for the given event type
	 */
	protected boolean supportsEvent(
			ApplicationListener<?> listener, Class<? extends ApplicationEvent> eventType, Class<?> sourceType) {

		SmartApplicationListener smartListener = (listener instanceof SmartApplicationListener ?
				(SmartApplicationListener) listener : new GenericApplicationListenerAdapter(listener));
		return (smartListener.supportsEventType(eventType) && smartListener.supportsSourceType(sourceType));
	}


	/**
	 * Cache key for ListenerRetrievers, based on event type and source type.
	 */
	private static class ListenerCacheKey {

		private final Class<?> eventType;

		private final Class<?> sourceType;

		public ListenerCacheKey(Class<?> eventType, Class<?> sourceType) {
			this.eventType = eventType;
			this.sourceType = sourceType;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			ListenerCacheKey otherKey = (ListenerCacheKey) other;
			return (this.eventType.equals(otherKey.eventType) && this.sourceType.equals(otherKey.sourceType));
		}

		@Override
		public int hashCode() {
			return this.eventType.hashCode() * 29 + this.sourceType.hashCode();
		}
	}


	/**
	 * Helper class that encapsulates a specific set of target listeners,
	 * allowing for efficient retrieval of pre-filtered listeners.
	 * <p>An instance of this helper gets cached per event type and source type.
	 */
	private class ListenerRetriever {

		public final Set<ApplicationListener<?>> applicationListeners;

		public final Set<String> applicationListenerBeans;

		private final boolean preFiltered;

		public ListenerRetriever(boolean preFiltered) {
			this.applicationListeners = new LinkedHashSet<ApplicationListener<?>>();
			this.applicationListenerBeans = new LinkedHashSet<String>();
			this.preFiltered = preFiltered;
		}

		public Collection<ApplicationListener<?>> getApplicationListeners() {
			LinkedList<ApplicationListener<?>> allListeners = new LinkedList<ApplicationListener<?>>();
			for (ApplicationListener<?> listener : this.applicationListeners) {
				allListeners.add(listener);
			}
			if (!this.applicationListenerBeans.isEmpty()) {
				BeanFactory beanFactory = getBeanFactory();
				for (String listenerBeanName : this.applicationListenerBeans) {
					ApplicationListener<?> listener = beanFactory.getBean(listenerBeanName, ApplicationListener.class);
					if (this.preFiltered || !allListeners.contains(listener)) {
						allListeners.add(listener);
					}
				}
			}
			OrderComparator.sort(allListeners);
			return allListeners;
		}
	}

}

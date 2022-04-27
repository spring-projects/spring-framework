/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

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
 * @author Stephane Nicoll
 * @since 1.2.3
 * @see #getApplicationListeners(ApplicationEvent, ResolvableType)
 * @see SimpleApplicationEventMulticaster
 */
public abstract class AbstractApplicationEventMulticaster
		implements ApplicationEventMulticaster, BeanClassLoaderAware, BeanFactoryAware {

	private final DefaultListenerRetriever defaultRetriever = new DefaultListenerRetriever();

	final Map<ListenerCacheKey, CachedListenerRetriever> retrieverCache = new ConcurrentHashMap<>(64);

	@Nullable
	private ClassLoader beanClassLoader;

	@Nullable
	private ConfigurableBeanFactory beanFactory;


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableBeanFactory)) {
			throw new IllegalStateException("Not running in a ConfigurableBeanFactory: " + beanFactory);
		}
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
		if (this.beanClassLoader == null) {
			this.beanClassLoader = this.beanFactory.getBeanClassLoader();
		}
	}

	private ConfigurableBeanFactory getBeanFactory() {
		if (this.beanFactory == null) {
			throw new IllegalStateException("ApplicationEventMulticaster cannot retrieve listener beans " +
					"because it is not associated with a BeanFactory");
		}
		return this.beanFactory;
	}


	@Override
	public void addApplicationListener(ApplicationListener<?> listener) {
		synchronized (this.defaultRetriever) {
			// Explicitly remove target for a proxy, if registered already,
			// in order to avoid double invocations of the same listener.
			Object singletonTarget = AopProxyUtils.getSingletonTarget(listener);
			if (singletonTarget instanceof ApplicationListener) {
				this.defaultRetriever.applicationListeners.remove(singletonTarget);
			}
			this.defaultRetriever.applicationListeners.add(listener);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void addApplicationListenerBean(String listenerBeanName) {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListenerBeans.add(listenerBeanName);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void removeApplicationListener(ApplicationListener<?> listener) {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListeners.remove(listener);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void removeApplicationListenerBean(String listenerBeanName) {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListenerBeans.remove(listenerBeanName);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void removeApplicationListeners(Predicate<ApplicationListener<?>> predicate) {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListeners.removeIf(predicate);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void removeApplicationListenerBeans(Predicate<String> predicate) {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListenerBeans.removeIf(predicate);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void removeAllListeners() {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListeners.clear();
			this.defaultRetriever.applicationListenerBeans.clear();
			this.retrieverCache.clear();
		}
	}


	/**
	 * Return a Collection containing all ApplicationListeners.
	 * @return a Collection of ApplicationListeners
	 * @see org.springframework.context.ApplicationListener
	 */
	protected Collection<ApplicationListener<?>> getApplicationListeners() {
		synchronized (this.defaultRetriever) {
			return this.defaultRetriever.getApplicationListeners();
		}
	}

	/**
	 * Return a Collection of ApplicationListeners matching the given
	 * event type. Non-matching listeners get excluded early.
	 * @param event the event to be propagated. Allows for excluding
	 * non-matching listeners early, based on cached matching information.
	 * @param eventType the event type
	 * @return a Collection of ApplicationListeners
	 * @see org.springframework.context.ApplicationListener
	 */
	protected Collection<ApplicationListener<?>> getApplicationListeners(
			ApplicationEvent event, ResolvableType eventType) {

		Object source = event.getSource();
		Class<?> sourceType = (source != null ? source.getClass() : null);
		ListenerCacheKey cacheKey = new ListenerCacheKey(eventType, sourceType);

		// Potential new retriever to populate
		CachedListenerRetriever newRetriever = null;

		// Quick check for existing entry on ConcurrentHashMap
		CachedListenerRetriever existingRetriever = this.retrieverCache.get(cacheKey);
		if (existingRetriever == null) {
			// Caching a new ListenerRetriever if possible
			if (this.beanClassLoader == null ||
					(ClassUtils.isCacheSafe(event.getClass(), this.beanClassLoader) &&
							(sourceType == null || ClassUtils.isCacheSafe(sourceType, this.beanClassLoader)))) {
				newRetriever = new CachedListenerRetriever();
				existingRetriever = this.retrieverCache.putIfAbsent(cacheKey, newRetriever);
				if (existingRetriever != null) {
					newRetriever = null;  // no need to populate it in retrieveApplicationListeners
				}
			}
		}

		if (existingRetriever != null) {
			Collection<ApplicationListener<?>> result = existingRetriever.getApplicationListeners();
			if (result != null) {
				return result;
			}
			// If result is null, the existing retriever is not fully populated yet by another thread.
			// Proceed like caching wasn't possible for this current local attempt.
		}

		return retrieveApplicationListeners(eventType, sourceType, newRetriever);
	}

	/**
	 * Actually retrieve the application listeners for the given event and source type.
	 * @param eventType the event type
	 * @param sourceType the event source type
	 * @param retriever the ListenerRetriever, if supposed to populate one (for caching purposes)
	 * @return the pre-filtered list of application listeners for the given event and source type
	 */
	private Collection<ApplicationListener<?>> retrieveApplicationListeners(
			ResolvableType eventType, @Nullable Class<?> sourceType, @Nullable CachedListenerRetriever retriever) {

		List<ApplicationListener<?>> allListeners = new ArrayList<>();
		Set<ApplicationListener<?>> filteredListeners = (retriever != null ? new LinkedHashSet<>() : null);
		Set<String> filteredListenerBeans = (retriever != null ? new LinkedHashSet<>() : null);

		Set<ApplicationListener<?>> listeners;
		Set<String> listenerBeans;
		synchronized (this.defaultRetriever) {
			listeners = new LinkedHashSet<>(this.defaultRetriever.applicationListeners);
			listenerBeans = new LinkedHashSet<>(this.defaultRetriever.applicationListenerBeans);
		}

		// Add programmatically registered listeners, including ones coming
		// from ApplicationListenerDetector (singleton beans and inner beans).
		for (ApplicationListener<?> listener : listeners) {
			if (supportsEvent(listener, eventType, sourceType)) {
				if (retriever != null) {
					filteredListeners.add(listener);
				}
				allListeners.add(listener);
			}
		}

		// Add listeners by bean name, potentially overlapping with programmatically
		// registered listeners above - but here potentially with additional metadata.
		if (!listenerBeans.isEmpty()) {
			ConfigurableBeanFactory beanFactory = getBeanFactory();
			for (String listenerBeanName : listenerBeans) {
				try {
					if (supportsEvent(beanFactory, listenerBeanName, eventType)) {
						ApplicationListener<?> listener =
								beanFactory.getBean(listenerBeanName, ApplicationListener.class);
						if (!allListeners.contains(listener) && supportsEvent(listener, eventType, sourceType)) {
							if (retriever != null) {
								if (beanFactory.isSingleton(listenerBeanName)) {
									filteredListeners.add(listener);
								}
								else {
									filteredListenerBeans.add(listenerBeanName);
								}
							}
							allListeners.add(listener);
						}
					}
					else {
						// Remove non-matching listeners that originally came from
						// ApplicationListenerDetector, possibly ruled out by additional
						// BeanDefinition metadata (e.g. factory method generics) above.
						Object listener = beanFactory.getSingleton(listenerBeanName);
						if (retriever != null) {
							filteredListeners.remove(listener);
						}
						allListeners.remove(listener);
					}
				}
				catch (NoSuchBeanDefinitionException ex) {
					// Singleton listener instance (without backing bean definition) disappeared -
					// probably in the middle of the destruction phase
				}
			}
		}

		AnnotationAwareOrderComparator.sort(allListeners);
		if (retriever != null) {
			if (filteredListenerBeans.isEmpty()) {
				retriever.applicationListeners = new LinkedHashSet<>(allListeners);
				retriever.applicationListenerBeans = filteredListenerBeans;
			}
			else {
				retriever.applicationListeners = filteredListeners;
				retriever.applicationListenerBeans = filteredListenerBeans;
			}
		}
		return allListeners;
	}

	/**
	 * Filter a bean-defined listener early through checking its generically declared
	 * event type before trying to instantiate it.
	 * <p>If this method returns {@code true} for a given listener as a first pass,
	 * the listener instance will get retrieved and fully evaluated through a
	 * {@link #supportsEvent(ApplicationListener, ResolvableType, Class)} call afterwards.
	 * @param beanFactory the BeanFactory that contains the listener beans
	 * @param listenerBeanName the name of the bean in the BeanFactory
	 * @param eventType the event type to check
	 * @return whether the given listener should be included in the candidates
	 * for the given event type
	 * @see #supportsEvent(Class, ResolvableType)
	 * @see #supportsEvent(ApplicationListener, ResolvableType, Class)
	 */
	private boolean supportsEvent(
			ConfigurableBeanFactory beanFactory, String listenerBeanName, ResolvableType eventType) {

		Class<?> listenerType = beanFactory.getType(listenerBeanName);
		if (listenerType == null || GenericApplicationListener.class.isAssignableFrom(listenerType) ||
				SmartApplicationListener.class.isAssignableFrom(listenerType)) {
			return true;
		}
		if (!supportsEvent(listenerType, eventType)) {
			return false;
		}
		try {
			BeanDefinition bd = beanFactory.getMergedBeanDefinition(listenerBeanName);
			ResolvableType genericEventType = bd.getResolvableType().as(ApplicationListener.class).getGeneric();
			return (genericEventType == ResolvableType.NONE || genericEventType.isAssignableFrom(eventType));
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Ignore - no need to check resolvable type for manually registered singleton
			return true;
		}
	}

	/**
	 * Filter a listener early through checking its generically declared event
	 * type before trying to instantiate it.
	 * <p>If this method returns {@code true} for a given listener as a first pass,
	 * the listener instance will get retrieved and fully evaluated through a
	 * {@link #supportsEvent(ApplicationListener, ResolvableType, Class)} call afterwards.
	 * @param listenerType the listener's type as determined by the BeanFactory
	 * @param eventType the event type to check
	 * @return whether the given listener should be included in the candidates
	 * for the given event type
	 */
	protected boolean supportsEvent(Class<?> listenerType, ResolvableType eventType) {
		ResolvableType declaredEventType = GenericApplicationListenerAdapter.resolveDeclaredEventType(listenerType);
		return (declaredEventType == null || declaredEventType.isAssignableFrom(eventType));
	}

	/**
	 * Determine whether the given listener supports the given event.
	 * <p>The default implementation detects the {@link SmartApplicationListener}
	 * and {@link GenericApplicationListener} interfaces. In case of a standard
	 * {@link ApplicationListener}, a {@link GenericApplicationListenerAdapter}
	 * will be used to introspect the generically declared type of the target listener.
	 * @param listener the target listener to check
	 * @param eventType the event type to check against
	 * @param sourceType the source type to check against
	 * @return whether the given listener should be included in the candidates
	 * for the given event type
	 */
	protected boolean supportsEvent(
			ApplicationListener<?> listener, ResolvableType eventType, @Nullable Class<?> sourceType) {

		GenericApplicationListener smartListener = (listener instanceof GenericApplicationListener ?
				(GenericApplicationListener) listener : new GenericApplicationListenerAdapter(listener));
		return (smartListener.supportsEventType(eventType) && smartListener.supportsSourceType(sourceType));
	}


	/**
	 * Cache key for ListenerRetrievers, based on event type and source type.
	 */
	private static final class ListenerCacheKey implements Comparable<ListenerCacheKey> {

		private final ResolvableType eventType;

		@Nullable
		private final Class<?> sourceType;

		public ListenerCacheKey(ResolvableType eventType, @Nullable Class<?> sourceType) {
			Assert.notNull(eventType, "Event type must not be null");
			this.eventType = eventType;
			this.sourceType = sourceType;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ListenerCacheKey)) {
				return false;
			}
			ListenerCacheKey otherKey = (ListenerCacheKey) other;
			return (this.eventType.equals(otherKey.eventType) &&
					ObjectUtils.nullSafeEquals(this.sourceType, otherKey.sourceType));
		}

		@Override
		public int hashCode() {
			return this.eventType.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.sourceType);
		}

		@Override
		public String toString() {
			return "ListenerCacheKey [eventType = " + this.eventType + ", sourceType = " + this.sourceType + "]";
		}

		@Override
		public int compareTo(ListenerCacheKey other) {
			int result = this.eventType.toString().compareTo(other.eventType.toString());
			if (result == 0) {
				if (this.sourceType == null) {
					return (other.sourceType == null ? 0 : -1);
				}
				if (other.sourceType == null) {
					return 1;
				}
				result = this.sourceType.getName().compareTo(other.sourceType.getName());
			}
			return result;
		}
	}


	/**
	 * Helper class that encapsulates a specific set of target listeners,
	 * allowing for efficient retrieval of pre-filtered listeners.
	 * <p>An instance of this helper gets cached per event type and source type.
	 */
	private class CachedListenerRetriever {

		@Nullable
		public volatile Set<ApplicationListener<?>> applicationListeners;

		@Nullable
		public volatile Set<String> applicationListenerBeans;

		@Nullable
		public Collection<ApplicationListener<?>> getApplicationListeners() {
			Set<ApplicationListener<?>> applicationListeners = this.applicationListeners;
			Set<String> applicationListenerBeans = this.applicationListenerBeans;
			if (applicationListeners == null || applicationListenerBeans == null) {
				// Not fully populated yet
				return null;
			}

			List<ApplicationListener<?>> allListeners = new ArrayList<>(
					applicationListeners.size() + applicationListenerBeans.size());
			allListeners.addAll(applicationListeners);
			if (!applicationListenerBeans.isEmpty()) {
				BeanFactory beanFactory = getBeanFactory();
				for (String listenerBeanName : applicationListenerBeans) {
					try {
						allListeners.add(beanFactory.getBean(listenerBeanName, ApplicationListener.class));
					}
					catch (NoSuchBeanDefinitionException ex) {
						// Singleton listener instance (without backing bean definition) disappeared -
						// probably in the middle of the destruction phase
					}
				}
			}
			if (!applicationListenerBeans.isEmpty()) {
				AnnotationAwareOrderComparator.sort(allListeners);
			}
			return allListeners;
		}
	}


	/**
	 * Helper class that encapsulates a general set of target listeners.
	 */
	private class DefaultListenerRetriever {

		public final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();

		public final Set<String> applicationListenerBeans = new LinkedHashSet<>();

		public Collection<ApplicationListener<?>> getApplicationListeners() {
			List<ApplicationListener<?>> allListeners = new ArrayList<>(
					this.applicationListeners.size() + this.applicationListenerBeans.size());
			allListeners.addAll(this.applicationListeners);
			if (!this.applicationListenerBeans.isEmpty()) {
				BeanFactory beanFactory = getBeanFactory();
				for (String listenerBeanName : this.applicationListenerBeans) {
					try {
						ApplicationListener<?> listener =
								beanFactory.getBean(listenerBeanName, ApplicationListener.class);
						if (!allListeners.contains(listener)) {
							allListeners.add(listener);
						}
					}
					catch (NoSuchBeanDefinitionException ex) {
						// Singleton listener instance (without backing bean definition) disappeared -
						// probably in the middle of the destruction phase
					}
				}
			}
			AnnotationAwareOrderComparator.sort(allListeners);
			return allListeners;
		}
	}

}

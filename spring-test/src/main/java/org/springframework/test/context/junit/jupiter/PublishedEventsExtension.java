/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.util.Assert;

/**
 * Provides instances of {@link PublishedEvents} as test method parameters.
 *
 * @author Oliver Drotbohm
 */
class PublishedEventsExtension implements ParameterResolver, BeforeAllCallback, AfterEachCallback {

	private ThreadBoundApplicationListenerAdapter listener = new ThreadBoundApplicationListenerAdapter();
	private final Function<ExtensionContext, ApplicationContext> lookup;

	PublishedEventsExtension() {
		this(ctx -> SpringExtension.getApplicationContext(ctx));
	}

	PublishedEventsExtension(Function<ExtensionContext, ApplicationContext> supplier) {
		this.lookup = supplier;
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.jupiter.api.extension.BeforeAllCallback#beforeAll(org.junit.jupiter.api.extension.ExtensionContext)
	 */
	@Override
	public void beforeAll(ExtensionContext extensionContext) {

		ApplicationContext context = this.lookup.apply(extensionContext);
		((ConfigurableApplicationContext) context).addApplicationListener(this.listener);
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.jupiter.api.extension.ParameterResolver#supportsParameter(org.junit.jupiter.api.extension.ParameterContext, org.junit.jupiter.api.extension.ExtensionContext)
	 */
	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return PublishedEvents.class.isAssignableFrom(parameterContext.getParameter().getType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.jupiter.api.extension.ParameterResolver#resolveParameter(org.junit.jupiter.api.extension.ParameterContext, org.junit.jupiter.api.extension.ExtensionContext)
	 */
	@Override
	public PublishedEvents resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {

		DefaultPublishedEvents publishedEvents = new DefaultPublishedEvents();
		this.listener.registerDelegate(publishedEvents);

		return publishedEvents;
	}

	/*
	 * (non-Javadoc)
	 * @see org.junit.jupiter.api.extension.AfterEachCallback#afterEach(org.junit.jupiter.api.extension.ExtensionContext)
	 */
	@Override
	public void afterEach(ExtensionContext context) {
		this.listener.unregisterDelegate();
	}

	/**
	 * {@link ApplicationListener} that allows registering delegate {@link ApplicationListener}s that are held in a
	 * {@link ThreadLocal} and get used on {@link #onApplicationEvent(ApplicationEvent)} if one is registered for the
	 * current thread. This allows multiple event listeners to see the events fired in a certain thread in a concurrent
	 * execution scenario.
	 *
	 * @author Oliver Drotbohm
	 */
	private static class ThreadBoundApplicationListenerAdapter implements ApplicationListener<ApplicationEvent> {

		private final ThreadLocal<ApplicationListener<ApplicationEvent>> delegate = new ThreadLocal<>();

		/**
		 * Registers the given {@link ApplicationListener} to be used for the current thread.
		 *
		 * @param listener must not be {@literal null}.
		 */
		void registerDelegate(ApplicationListener<ApplicationEvent> listener) {

			Assert.notNull(listener, "Delegate ApplicationListener must not be null!");

			this.delegate.set(listener);
		}

		/**
		 * Removes the registration of the currently assigned {@link ApplicationListener}.
		 */
		void unregisterDelegate() {
			this.delegate.remove();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
		 */
		@Override
		public void onApplicationEvent(ApplicationEvent event) {

			ApplicationListener<ApplicationEvent> listener = this.delegate.get();

			if (listener != null) {
				listener.onApplicationEvent(event);
			}
		}
	}

	/**
	 * Default implementation of {@link PublishedEvents}.
	 *
	 * @author Oliver Drotbohm
	 */
	static class DefaultPublishedEvents implements PublishedEvents, ApplicationListener<ApplicationEvent> {

		private List<Object> events = new ArrayList<>();

		/*
		 * (non-Javadoc)
		 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
		 */
		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			this.events.add(unwrapPayloadEvent(event));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.test.context.junit.jupiter.PublishedEvents#ofType(java.lang.Class)
		 */
		@Override
		public <T> TypedPublishedEvents<T> ofType(Class<T> type) {

			return SimpleTypedPublishedEvents.of(this.events.stream()
					.filter(type::isInstance)
					.map(type::cast));
		}

		private static Object unwrapPayloadEvent(Object source) {
			return PayloadApplicationEvent.class.isInstance(source) ? ((PayloadApplicationEvent<?>) source).getPayload() : source;
		}

		private static final class SimpleTypedPublishedEvents<T> implements TypedPublishedEvents<T> {

			private final List<T> events;

			private SimpleTypedPublishedEvents(List<T> events) {

				Assert.notNull(events, "Events must not be null!");

				this.events = events;
			}

			private static <T> SimpleTypedPublishedEvents<T> of(Stream<T> stream) {
				return new SimpleTypedPublishedEvents<>(stream.collect(Collectors.toList()));
			}

			/*
			 * (non-Javadoc)
			 * @see org.springframework.test.context.junit.jupiter.PublishedEvents.TypedPublishedEvents#ofSubType(java.lang.Class)
			 */
			@Override
			public <S extends T> TypedPublishedEvents<S> ofSubType(Class<S> subType) {
				return SimpleTypedPublishedEvents.of(getFilteredEvents(subType::isInstance).map(subType::cast));
			}

			/*
			 * (non-Javadoc)
			 * @see org.springframework.test.context.junit.jupiter.PublishedEvents.TypedPublishedEvents#matching(java.util.function.Predicate)
			 */
			@Override
			public TypedPublishedEvents<T> matching(Predicate<? super T> predicate) {
				return SimpleTypedPublishedEvents.of(getFilteredEvents(predicate));
			}

			/**
			 * Returns a {@link Stream} of events filtered by the given
			 * {@link Predicate}.
			 *
			 * @param predicate must not be {@literal null}.
			 * @return
			 */
			private Stream<T> getFilteredEvents(Predicate<? super T> predicate) {
				return this.events.stream().filter(predicate);
			}

			/*
			 * (non-Javadoc)
			 * @see java.lang.Iterable#iterator()
			 */
			@Override
			public Iterator<T> iterator() {
				return this.events.iterator();
			}
		}
	}
}

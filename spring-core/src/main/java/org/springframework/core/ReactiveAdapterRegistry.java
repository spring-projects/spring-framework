/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.RxReactiveStreams;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import static org.springframework.core.ReactiveTypeDescriptor.multiValue;
import static org.springframework.core.ReactiveTypeDescriptor.noValue;
import static org.springframework.core.ReactiveTypeDescriptor.singleOptionalValue;
import static org.springframework.core.ReactiveTypeDescriptor.singleRequiredValue;

/**
 * A registry of adapters to adapt a Reactive Streams {@link Publisher} to/from
 * various async/reactive types such as {@code CompletableFuture}, RxJava
 * {@code Observable}, and others.
 *
 * <p>By default, depending on classpath availability, adapters are registered
 * for Reactor, RxJava 1, RxJava 2 types, {@link CompletableFuture}, and Java 9+
 * Flow.Publisher.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class ReactiveAdapterRegistry {

	@Nullable
	private static volatile ReactiveAdapterRegistry sharedInstance;

	private final boolean reactorPresent;

	private final List<ReactiveAdapter> adapters = new ArrayList<>(32);


	/**
	 * Create a registry and auto-register default adapters.
	 */
	public ReactiveAdapterRegistry() {

		// Reactor
		boolean reactorRegistered = false;
		try {
			new ReactorRegistrar().registerAdapters(this);
			reactorRegistered = true;
		}
		catch (Throwable ex) {
			// Ignore
		}
		this.reactorPresent = reactorRegistered;

		// RxJava1
		try {
			new RxJava1Registrar().registerAdapters(this);
		}
		catch (Throwable ex) {
			// Ignore
		}

		// RxJava2
		try {
			new RxJava2Registrar().registerAdapters(this);
		}
		catch (Throwable ex) {
			// Ignore
		}

		// Java 9+ Flow.Publisher
		try {
			new ReactorJdkFlowAdapterRegistrar().registerAdapter(this);
		}
		catch (Throwable ex) {
			// Ignore for the time being...
			// We can fall back on "reactive-streams-flow-bridge" (once released)
		}
	}


	/**
	 * Whether the registry has any adapters which would be the case if any of
	 * Reactor, RxJava 2, or RxJava 1 (+ RxJava Reactive Streams bridge) are
	 * present on the classpath.
	 */
	public boolean hasAdapters() {
		return !this.adapters.isEmpty();
	}

	/**
	 * Register a reactive type along with functions to adapt to and from a
	 * Reactive Streams {@link Publisher}. The functions can assume their
	 * input is never be {@code null} nor {@link Optional}.
	 */
	public void registerReactiveType(ReactiveTypeDescriptor descriptor,
			Function<Object, Publisher<?>> toAdapter, Function<Publisher<?>, Object> fromAdapter) {

		if (reactorPresent) {
			this.adapters.add(new ReactorAdapter(descriptor, toAdapter, fromAdapter));
		}
		else {
			this.adapters.add(new ReactiveAdapter(descriptor, toAdapter, fromAdapter));
		}
	}

	/**
	 * Get the adapter for the given reactive type.
	 */
	@Nullable
	public ReactiveAdapter getAdapter(Class<?> reactiveType) {
		return getAdapter(reactiveType, null);
	}

	/**
	 * Get the adapter for the given reactive type. Or if a "source" object is
	 * provided, its actual type is used instead.
	 * @param reactiveType the reactive type
	 * (may be {@code null} if a concrete source object is given)
	 * @param source an instance of the reactive type
	 * (i.e. to adapt from; may be {@code null} if the reactive type is specified)
	 */
	@Nullable
	public ReactiveAdapter getAdapter(@Nullable Class<?> reactiveType, @Nullable Object source) {
		Object sourceToUse = (source instanceof Optional ? ((Optional<?>) source).orElse(null) : source);
		Class<?> clazz = (sourceToUse != null ? sourceToUse.getClass() : reactiveType);
		if (clazz == null) {
			return null;
		}

		return this.adapters.stream()
				.filter(adapter -> adapter.getReactiveType() == clazz)
				.findFirst()
				.orElseGet(() ->
						this.adapters.stream()
								.filter(adapter -> adapter.getReactiveType().isAssignableFrom(clazz))
								.findFirst()
								.orElse(null));
	}


	/**
	 * Return a shared default {@code ReactiveAdapterRegistry} instance, lazily
	 * building it once needed.
	 * <p><b>NOTE:</b> We highly recommend passing a long-lived, pre-configured
	 * {@code ReactiveAdapterRegistry} instance for customization purposes.
	 * This accessor is only meant as a fallback for code paths that want to
	 * fall back on a default instance if one isn't provided.
	 * @return the shared {@code ReactiveAdapterRegistry} instance (never {@code null})
	 * @since 5.0.2
	 */
	public static ReactiveAdapterRegistry getSharedInstance() {
		ReactiveAdapterRegistry ar = sharedInstance;
		if (ar == null) {
			synchronized (ReactiveAdapterRegistry.class) {
				ar = sharedInstance;
				if (ar == null) {
					ar = new ReactiveAdapterRegistry();
					sharedInstance = ar;
				}
			}
		}
		return ar;
	}


	private static class ReactorRegistrar {

		void registerAdapters(ReactiveAdapterRegistry registry) {
			// Flux and Mono ahead of Publisher...

			registry.registerReactiveType(
					singleOptionalValue(Mono.class, Mono::empty),
					source -> (Mono<?>) source,
					Mono::from
			);

			registry.registerReactiveType(multiValue(Flux.class, Flux::empty),
					source -> (Flux<?>) source,
					Flux::from);

			registry.registerReactiveType(multiValue(Publisher.class, Flux::empty),
					source -> (Publisher<?>) source,
					source -> source);

			registry.registerReactiveType(
					singleOptionalValue(CompletableFuture.class, () -> {
						CompletableFuture<?> empty = new CompletableFuture<>();
						empty.complete(null);
						return empty;
					}),
					source -> Mono.fromFuture((CompletableFuture<?>) source),
					source -> Mono.from(source).toFuture()
			);
		}
	}


	private static class RxJava1Registrar {

		void registerAdapters(ReactiveAdapterRegistry registry) {
			registry.registerReactiveType(
					multiValue(rx.Observable.class, rx.Observable::empty),
					source -> RxReactiveStreams.toPublisher((rx.Observable<?>) source),
					RxReactiveStreams::toObservable
			);
			registry.registerReactiveType(
					singleRequiredValue(rx.Single.class),
					source -> RxReactiveStreams.toPublisher((rx.Single<?>) source),
					RxReactiveStreams::toSingle
			);
			registry.registerReactiveType(
					noValue(rx.Completable.class, rx.Completable::complete),
					source -> RxReactiveStreams.toPublisher((rx.Completable) source),
					RxReactiveStreams::toCompletable
			);
		}
	}


	private static class RxJava2Registrar {

		void registerAdapters(ReactiveAdapterRegistry registry) {
			registry.registerReactiveType(
					multiValue(io.reactivex.Flowable.class, io.reactivex.Flowable::empty),
					source -> (io.reactivex.Flowable<?>) source,
					Flowable::fromPublisher
			);
			registry.registerReactiveType(
					multiValue(io.reactivex.Observable.class, io.reactivex.Observable::empty),
					source -> ((io.reactivex.Observable<?>) source).toFlowable(BackpressureStrategy.BUFFER),
					source -> io.reactivex.Flowable.fromPublisher(source).toObservable()
			);
			registry.registerReactiveType(
					singleRequiredValue(io.reactivex.Single.class),
					source -> ((io.reactivex.Single<?>) source).toFlowable(),
					source -> io.reactivex.Flowable.fromPublisher(source).toObservable().singleElement().toSingle()
			);
			registry.registerReactiveType(
					singleOptionalValue(io.reactivex.Maybe.class, io.reactivex.Maybe::empty),
					source -> ((io.reactivex.Maybe<?>) source).toFlowable(),
					source -> io.reactivex.Flowable.fromPublisher(source).toObservable().singleElement()
			);
			registry.registerReactiveType(
					noValue(io.reactivex.Completable.class, io.reactivex.Completable::complete),
					source -> ((io.reactivex.Completable) source).toFlowable(),
					source -> io.reactivex.Flowable.fromPublisher(source).toObservable().ignoreElements()
			);
		}
	}


	private static class ReactorJdkFlowAdapterRegistrar {

		// TODO: remove reflection when build requires JDK 9+
		void registerAdapter(ReactiveAdapterRegistry registry) throws Exception {
			Class<?> type = ClassUtils.forName("java.util.concurrent.Flow.Publisher", getClass().getClassLoader());
			Method toFluxMethod = getMethod("flowPublisherToFlux", type);
			Method toFlowMethod = getMethod("publisherToFlowPublisher", Publisher.class);
			Object emptyFlow = ReflectionUtils.invokeMethod(toFlowMethod, null, Flux.empty());

			registry.registerReactiveType(
					multiValue(type, () -> emptyFlow),
					source -> (Publisher<?>) ReflectionUtils.invokeMethod(toFluxMethod, null, source),
					publisher -> ReflectionUtils.invokeMethod(toFlowMethod, null, publisher)
			);
		}

		private static Method getMethod(String name, Class<?> argumentType) throws NoSuchMethodException {
			return reactor.adapter.JdkFlowAdapter.class.getMethod(name, argumentType);
		}
	}


	/**
	 * Extension of ReactiveAdapter that wraps adapted (raw) Publisher's as
	 * {@link Flux} or {@link Mono} depending on the underlying reactive type's
	 * stream semantics.
	 */
	private static class ReactorAdapter extends ReactiveAdapter {

		ReactorAdapter(ReactiveTypeDescriptor descriptor,
				Function<Object, Publisher<?>> toPublisherFunction,
				Function<Publisher<?>, Object> fromPublisherFunction) {

			super(descriptor, toPublisherFunction, fromPublisherFunction);
		}

		@Override
		public <T> Publisher<T> toPublisher(@Nullable Object source) {
			Publisher<T> publisher = super.toPublisher(source);
			return (isMultiValue() ? Flux.from(publisher) : Mono.from(publisher));
		}
	}

}

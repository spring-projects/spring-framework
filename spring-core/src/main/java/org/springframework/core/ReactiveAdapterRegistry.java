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

package org.springframework.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import kotlinx.coroutines.CompletableDeferredKt;
import kotlinx.coroutines.Deferred;
import org.reactivestreams.Publisher;
import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.RxReactiveStreams;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * A registry of adapters to adapt Reactive Streams {@link Publisher} to/from
 * various async/reactive types such as {@code CompletableFuture}, RxJava
 * {@code Observable}, and others.
 *
 * <p>By default, depending on classpath availability, adapters are registered
 * for Reactor, RxJava 2/3, or RxJava 1 (+ RxJava Reactive Streams bridge),
 * {@link CompletableFuture}, Java 9+ {@code Flow.Publisher}, and Kotlin
 * Coroutines' {@code Deferred} and {@code Flow}.
 *
 * <p><strong>Note:</strong> As of Spring Framework 5.3, support for RxJava 1.x
 * is deprecated in favor of RxJava 2 and 3.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class ReactiveAdapterRegistry {

	@Nullable
	private static volatile ReactiveAdapterRegistry sharedInstance;

	private final boolean reactorPresent;

	private final List<ReactiveAdapter> adapters = new ArrayList<>();


	/**
	 * Create a registry and auto-register default adapters.
	 * @see #getSharedInstance()
	 */
	public ReactiveAdapterRegistry() {
		ClassLoader classLoader = ReactiveAdapterRegistry.class.getClassLoader();

		// Reactor
		boolean reactorRegistered = false;
		if (ClassUtils.isPresent("reactor.core.publisher.Flux", classLoader)) {
			new ReactorRegistrar().registerAdapters(this);
			reactorRegistered = true;
		}
		this.reactorPresent = reactorRegistered;

		// RxJava1 (deprecated)
		if (ClassUtils.isPresent("rx.Observable", classLoader) &&
				ClassUtils.isPresent("rx.RxReactiveStreams", classLoader)) {
			new RxJava1Registrar().registerAdapters(this);
		}

		// RxJava2
		if (ClassUtils.isPresent("io.reactivex.Flowable", classLoader)) {
			new RxJava2Registrar().registerAdapters(this);
		}

		// RxJava3
		if (ClassUtils.isPresent("io.reactivex.rxjava3.core.Flowable", classLoader)) {
			new RxJava3Registrar().registerAdapters(this);
		}

		// Java 9+ Flow.Publisher
		if (ClassUtils.isPresent("java.util.concurrent.Flow.Publisher", classLoader)) {
			new ReactorJdkFlowAdapterRegistrar().registerAdapter(this);
		}
		// If not present, do nothing for the time being...
		// We can fall back on "reactive-streams-flow-bridge" (once released)

		// Coroutines
		if (this.reactorPresent && ClassUtils.isPresent("kotlinx.coroutines.reactor.MonoKt", classLoader)) {
			new CoroutinesRegistrar().registerAdapters(this);
		}
	}


	/**
	 * Whether the registry has any adapters.
	 */
	public boolean hasAdapters() {
		return !this.adapters.isEmpty();
	}

	/**
	 * Register a reactive type along with functions to adapt to and from a
	 * Reactive Streams {@link Publisher}. The function arguments assume that
	 * their input is neither {@code null} nor {@link Optional}.
	 */
	public void registerReactiveType(ReactiveTypeDescriptor descriptor,
			Function<Object, Publisher<?>> toAdapter, Function<Publisher<?>, Object> fromAdapter) {

		if (this.reactorPresent) {
			this.adapters.add(new ReactorAdapter(descriptor, toAdapter, fromAdapter));
		}
		else {
			this.adapters.add(new ReactiveAdapter(descriptor, toAdapter, fromAdapter));
		}
	}

	/**
	 * Get the adapter for the given reactive type.
	 * @return the corresponding adapter, or {@code null} if none available
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
	 * @return the corresponding adapter, or {@code null} if none available
	 */
	@Nullable
	public ReactiveAdapter getAdapter(@Nullable Class<?> reactiveType, @Nullable Object source) {
		if (this.adapters.isEmpty()) {
			return null;
		}

		Object sourceToUse = (source instanceof Optional ? ((Optional<?>) source).orElse(null) : source);
		Class<?> clazz = (sourceToUse != null ? sourceToUse.getClass() : reactiveType);
		if (clazz == null) {
			return null;
		}
		for (ReactiveAdapter adapter : this.adapters) {
			if (adapter.getReactiveType() == clazz) {
				return adapter;
			}
		}
		for (ReactiveAdapter adapter : this.adapters) {
			if (adapter.getReactiveType().isAssignableFrom(clazz)) {
				return adapter;
			}
		}
		return null;
	}


	/**
	 * Return a shared default {@code ReactiveAdapterRegistry} instance,
	 * lazily building it once needed.
	 * <p><b>NOTE:</b> We highly recommend passing a long-lived, pre-configured
	 * {@code ReactiveAdapterRegistry} instance for customization purposes.
	 * This accessor is only meant as a fallback for code paths that want to
	 * fall back on a default instance if one isn't provided.
	 * @return the shared {@code ReactiveAdapterRegistry} instance
	 * @since 5.0.2
	 */
	public static ReactiveAdapterRegistry getSharedInstance() {
		ReactiveAdapterRegistry registry = sharedInstance;
		if (registry == null) {
			synchronized (ReactiveAdapterRegistry.class) {
				registry = sharedInstance;
				if (registry == null) {
					registry = new ReactiveAdapterRegistry();
					sharedInstance = registry;
				}
			}
		}
		return registry;
	}


	private static class ReactorRegistrar {

		void registerAdapters(ReactiveAdapterRegistry registry) {
			// Register Flux and Mono before Publisher...

			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleOptionalValue(Mono.class, Mono::empty),
					source -> (Mono<?>) source,
					Mono::from
			);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(Flux.class, Flux::empty),
					source -> (Flux<?>) source,
					Flux::from);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(Publisher.class, Flux::empty),
					source -> (Publisher<?>) source,
					source -> source);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.nonDeferredAsyncValue(CompletionStage.class, EmptyCompletableFuture::new),
					source -> Mono.fromCompletionStage((CompletionStage<?>) source),
					source -> Mono.from(source).toFuture()
			);
		}
	}


	private static class RxJava1Registrar {

		void registerAdapters(ReactiveAdapterRegistry registry) {
			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(rx.Observable.class, rx.Observable::empty),
					source -> RxReactiveStreams.toPublisher((rx.Observable<?>) source),
					RxReactiveStreams::toObservable
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleRequiredValue(rx.Single.class),
					source -> RxReactiveStreams.toPublisher((rx.Single<?>) source),
					RxReactiveStreams::toSingle
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.noValue(rx.Completable.class, rx.Completable::complete),
					source -> RxReactiveStreams.toPublisher((rx.Completable) source),
					RxReactiveStreams::toCompletable
			);
		}
	}


	private static class RxJava2Registrar {

		void registerAdapters(ReactiveAdapterRegistry registry) {
			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(io.reactivex.Flowable.class, io.reactivex.Flowable::empty),
					source -> (io.reactivex.Flowable<?>) source,
					io.reactivex.Flowable::fromPublisher
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(io.reactivex.Observable.class, io.reactivex.Observable::empty),
					source -> ((io.reactivex.Observable<?>) source).toFlowable(io.reactivex.BackpressureStrategy.BUFFER),
					source -> io.reactivex.Flowable.fromPublisher(source)
							.toObservable()
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleRequiredValue(io.reactivex.Single.class),
					source -> ((io.reactivex.Single<?>) source).toFlowable(),
					source -> io.reactivex.Flowable.fromPublisher(source)
							.toObservable().singleElement().toSingle()
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleOptionalValue(io.reactivex.Maybe.class, io.reactivex.Maybe::empty),
					source -> ((io.reactivex.Maybe<?>) source).toFlowable(),
					source -> io.reactivex.Flowable.fromPublisher(source)
							.toObservable().singleElement()
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.noValue(io.reactivex.Completable.class, io.reactivex.Completable::complete),
					source -> ((io.reactivex.Completable) source).toFlowable(),
					source -> io.reactivex.Flowable.fromPublisher(source)
							.toObservable().ignoreElements()
			);
		}
	}

	private static class RxJava3Registrar {

		void registerAdapters(ReactiveAdapterRegistry registry) {
			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(
							io.reactivex.rxjava3.core.Flowable.class,
							io.reactivex.rxjava3.core.Flowable::empty),
					source -> (io.reactivex.rxjava3.core.Flowable<?>) source,
					io.reactivex.rxjava3.core.Flowable::fromPublisher
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(
							io.reactivex.rxjava3.core.Observable.class,
							io.reactivex.rxjava3.core.Observable::empty),
					source -> ((io.reactivex.rxjava3.core.Observable<?>) source).toFlowable(
							io.reactivex.rxjava3.core.BackpressureStrategy.BUFFER),
					source -> io.reactivex.rxjava3.core.Flowable.fromPublisher(source)
							.toObservable()
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleRequiredValue(io.reactivex.rxjava3.core.Single.class),
					source -> ((io.reactivex.rxjava3.core.Single<?>) source).toFlowable(),
					source -> io.reactivex.rxjava3.core.Flowable.fromPublisher(source)
							.toObservable().singleElement().toSingle()
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleOptionalValue(
							io.reactivex.rxjava3.core.Maybe.class,
							io.reactivex.rxjava3.core.Maybe::empty),
					source -> ((io.reactivex.rxjava3.core.Maybe<?>) source).toFlowable(),
					source -> io.reactivex.rxjava3.core.Flowable.fromPublisher(source)
							.toObservable().singleElement()
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.noValue(
							io.reactivex.rxjava3.core.Completable.class,
							io.reactivex.rxjava3.core.Completable::complete),
					source -> ((io.reactivex.rxjava3.core.Completable) source).toFlowable(),
					source -> io.reactivex.rxjava3.core.Flowable.fromPublisher(source)
							.toObservable().ignoreElements()
			);
		}
	}

	private static class ReactorJdkFlowAdapterRegistrar {

		void registerAdapter(ReactiveAdapterRegistry registry) {
			// TODO: remove reflection when build requires JDK 9+

			try {
				String publisherName = "java.util.concurrent.Flow.Publisher";
				Class<?> publisherClass = ClassUtils.forName(publisherName, getClass().getClassLoader());

				String adapterName = "reactor.adapter.JdkFlowAdapter";
				Class<?> flowAdapterClass = ClassUtils.forName(adapterName,  getClass().getClassLoader());

				Method toFluxMethod = flowAdapterClass.getMethod("flowPublisherToFlux", publisherClass);
				Method toFlowMethod = flowAdapterClass.getMethod("publisherToFlowPublisher", Publisher.class);
				Object emptyFlow = ReflectionUtils.invokeMethod(toFlowMethod, null, Flux.empty());

				registry.registerReactiveType(
						ReactiveTypeDescriptor.multiValue(publisherClass, () -> emptyFlow),
						source -> (Publisher<?>) ReflectionUtils.invokeMethod(toFluxMethod, null, source),
						publisher -> ReflectionUtils.invokeMethod(toFlowMethod, null, publisher)
				);
			}
			catch (Throwable ex) {
				// Ignore
			}
		}
	}


	/**
	 * ReactiveAdapter variant that wraps adapted Publishers as {@link Flux} or
	 * {@link Mono} depending on {@link ReactiveTypeDescriptor#isMultiValue()}.
	 * This is important in places where only the stream and stream element type
	 * information is available like encoders and decoders.
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


	private static class EmptyCompletableFuture<T> extends CompletableFuture<T> {

		EmptyCompletableFuture() {
			complete(null);
		}
	}


	private static class CoroutinesRegistrar {

		@SuppressWarnings("KotlinInternalInJava")
		void registerAdapters(ReactiveAdapterRegistry registry) {
			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleOptionalValue(Deferred.class,
							() -> CompletableDeferredKt.CompletableDeferred(null)),
					source -> CoroutinesUtils.deferredToMono((Deferred<?>) source),
					source -> CoroutinesUtils.monoToDeferred(Mono.from(source)));

			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(kotlinx.coroutines.flow.Flow.class, kotlinx.coroutines.flow.FlowKt::emptyFlow),
					source -> kotlinx.coroutines.reactor.ReactorFlowKt.asFlux((kotlinx.coroutines.flow.Flow<?>) source),
					kotlinx.coroutines.reactive.ReactiveFlowKt::asFlow
			);
		}
	}


	/**
	 * {@code BlockHoundIntegration} for spring-core classes.
	 * <p>Explicitly allow the following:
	 * <ul>
	 * <li>Reading class info via {@link LocalVariableTableParameterNameDiscoverer}.
	 * <li>Locking within {@link ConcurrentReferenceHashMap}.
	 * </ul>
	 * @since 5.2.4
	 */
	public static class SpringCoreBlockHoundIntegration implements BlockHoundIntegration {

		@Override
		public void applyTo(BlockHound.Builder builder) {

			// Avoid hard references potentially anywhere in spring-core (no need for structural dependency)

			builder.allowBlockingCallsInside(
					"org.springframework.core.LocalVariableTableParameterNameDiscoverer", "inspectClass");

			String className = "org.springframework.util.ConcurrentReferenceHashMap$Segment";
			builder.allowBlockingCallsInside(className, "doTask");
			builder.allowBlockingCallsInside(className, "clear");
			builder.allowBlockingCallsInside(className, "restructure");
		}
	}

}

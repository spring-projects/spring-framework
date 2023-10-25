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

package org.springframework.scheduling.annotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.CoroutinesUtils;
import org.springframework.core.KotlinDetector;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.support.DefaultScheduledTaskObservationConvention;
import org.springframework.scheduling.support.ScheduledTaskObservationContext;
import org.springframework.scheduling.support.ScheduledTaskObservationConvention;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import static org.springframework.scheduling.support.ScheduledTaskObservationDocumentation.TASKS_SCHEDULED_EXECUTION;

/**
 * Helper class for @{@link ScheduledAnnotationBeanPostProcessor} to support reactive
 * cases without a dependency on optional classes.
 *
 * @author Simon Baslé
 * @author Brian Clozel
 * @since 6.1
 */
abstract class ScheduledAnnotationReactiveSupport {

	static final boolean reactorPresent = ClassUtils.isPresent(
			"reactor.core.publisher.Flux", ScheduledAnnotationReactiveSupport.class.getClassLoader());

	static final boolean coroutinesReactorPresent = ClassUtils.isPresent(
			"kotlinx.coroutines.reactor.MonoKt", ScheduledAnnotationReactiveSupport.class.getClassLoader());

	private static final Log logger = LogFactory.getLog(ScheduledAnnotationReactiveSupport.class);


	/**
	 * Checks that if the method is reactive, it can be scheduled. Methods are considered
	 * eligible for reactive scheduling if they either return an instance of a type that
	 * can be converted to {@code Publisher} or are a Kotlin suspending function.
	 * If the method doesn't match these criteria, this check returns {@code false}.
	 * <p>For scheduling of Kotlin suspending functions, the Coroutine-Reactor bridge
	 * {@code kotlinx.coroutines.reactor} must be present at runtime (in order to invoke
	 * suspending functions as a {@code Publisher}). Provided that is the case, this
	 * method returns {@code true}. Otherwise, it throws an {@code IllegalStateException}.
	 * @throws IllegalStateException if the method is reactive but Reactor and/or the
	 * Kotlin coroutines bridge are not present at runtime
	 */
	public static boolean isReactive(Method method) {
		if (KotlinDetector.isKotlinPresent() && KotlinDetector.isSuspendingFunction(method)) {
			// Note that suspending functions declared without args have a single Continuation
			// parameter in reflective inspection
			Assert.isTrue(method.getParameterCount() == 1,
					"Kotlin suspending functions may only be annotated with @Scheduled if declared without arguments");
			Assert.isTrue(coroutinesReactorPresent, "Kotlin suspending functions may only be annotated with " +
					"@Scheduled if the Coroutine-Reactor bridge (kotlinx.coroutines.reactor) is present at runtime");
			return true;
		}
		ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();
		if (!registry.hasAdapters()) {
			return false;
		}
		Class<?> returnType = method.getReturnType();
		ReactiveAdapter candidateAdapter = registry.getAdapter(returnType);
		if (candidateAdapter == null) {
			return false;
		}
		Assert.isTrue(method.getParameterCount() == 0,
				"Reactive methods may only be annotated with @Scheduled if declared without arguments");
		Assert.isTrue(candidateAdapter.getDescriptor().isDeferred(),
				"Reactive methods may only be annotated with @Scheduled if the return type supports deferred execution");
		return true;
	}

	/**
	 * Create a {@link Runnable} for the Scheduled infrastructure, allowing for scheduled
	 * subscription to the publisher produced by a reactive method.
	 * <p>Note that the reactive method is invoked once, but the resulting {@code Publisher}
	 * is subscribed to repeatedly, once per each invocation of the {@code Runnable}.
	 * <p>In the case of a fixed-delay configuration, the subscription inside the
	 * {@link Runnable} is turned into a blocking call in order to maintain fixed-delay
	 * semantics (i.e. the task blocks until completion of the Publisher, and the
	 * delay is applied until the next iteration).
	 */
	public static Runnable createSubscriptionRunnable(Method method, Object targetBean, Scheduled scheduled,
			Supplier<ObservationRegistry> observationRegistrySupplier, List<Runnable> subscriptionTrackerRegistry) {

		boolean shouldBlock = (scheduled.fixedDelay() > 0 || StringUtils.hasText(scheduled.fixedDelayString()));
		Publisher<?> publisher = getPublisherFor(method, targetBean);
		Supplier<ScheduledTaskObservationContext> contextSupplier =
				() -> new ScheduledTaskObservationContext(targetBean, method);
		return new SubscribingRunnable(publisher, shouldBlock, scheduled.scheduler(),
				subscriptionTrackerRegistry, observationRegistrySupplier, contextSupplier);
	}

	/**
	 * Turn the invocation of the provided {@code Method} into a {@code Publisher},
	 * either by reflectively invoking it and converting the result to a {@code Publisher}
	 * via {@link ReactiveAdapterRegistry} or by converting a Kotlin suspending function
	 * into a {@code Publisher} via {@link CoroutinesUtils}.
	 * <p>The {@link #isReactive(Method)} check is a precondition to calling this method.
	 * If Reactor is present at runtime, the {@code Publisher} is additionally converted
	 * to a {@code Flux} with a checkpoint String, allowing for better debugging.
	 */
	static Publisher<?> getPublisherFor(Method method, Object bean) {
		if (KotlinDetector.isKotlinPresent() && KotlinDetector.isSuspendingFunction(method)) {
			return CoroutinesUtils.invokeSuspendingFunction(method, bean, (Object[]) method.getParameters());
		}

		ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();
		Class<?> returnType = method.getReturnType();
		ReactiveAdapter adapter = registry.getAdapter(returnType);
		if (adapter == null) {
			throw new IllegalArgumentException("Cannot convert @Scheduled reactive method return type to Publisher");
		}
		if (!adapter.getDescriptor().isDeferred()) {
			throw new IllegalArgumentException("Cannot convert @Scheduled reactive method return type to Publisher: " +
					returnType.getSimpleName() + " is not a deferred reactive type");
		}

		Method invocableMethod = AopUtils.selectInvocableMethod(method, bean.getClass());
		try {
			ReflectionUtils.makeAccessible(invocableMethod);
			Object returnValue = invocableMethod.invoke(bean);

			Publisher<?> publisher = adapter.toPublisher(returnValue);
			// If Reactor is on the classpath, we could benefit from having a checkpoint for debuggability
			if (reactorPresent) {
				return Flux.from(publisher).checkpoint(
						"@Scheduled '"+ method.getName() + "()' in '" + method.getDeclaringClass().getName() + "'");
			}
			else {
				return publisher;
			}
		}
		catch (InvocationTargetException ex) {
			throw new IllegalArgumentException(
					"Cannot obtain a Publisher-convertible value from the @Scheduled reactive method",
					ex.getTargetException());
		}
		catch (IllegalAccessException ex) {
			throw new IllegalArgumentException(
					"Cannot obtain a Publisher-convertible value from the @Scheduled reactive method", ex);
		}
	}


	/**
	 * Utility implementation of {@code Runnable} that subscribes to a {@code Publisher}
	 * or subscribes-then-blocks if {@code shouldBlock} is set to {@code true}.
	 */
	static final class SubscribingRunnable implements SchedulingAwareRunnable {

		private static final ScheduledTaskObservationConvention DEFAULT_CONVENTION =
				new DefaultScheduledTaskObservationConvention();

		private final Publisher<?> publisher;

		final boolean shouldBlock;

		@Nullable
		private final String qualifier;

		private final List<Runnable> subscriptionTrackerRegistry;

		final Supplier<ObservationRegistry> observationRegistrySupplier;

		final Supplier<ScheduledTaskObservationContext> contextSupplier;

		SubscribingRunnable(Publisher<?> publisher, boolean shouldBlock,
				@Nullable String qualifier, List<Runnable> subscriptionTrackerRegistry,
				Supplier<ObservationRegistry> observationRegistrySupplier,
				Supplier<ScheduledTaskObservationContext> contextSupplier) {

			this.publisher = publisher;
			this.shouldBlock = shouldBlock;
			this.qualifier = qualifier;
			this.subscriptionTrackerRegistry = subscriptionTrackerRegistry;
			this.observationRegistrySupplier = observationRegistrySupplier;
			this.contextSupplier = contextSupplier;
		}

		@Override
		@Nullable
		public String getQualifier() {
			return this.qualifier;
		}

		@Override
		public void run() {
			Observation observation = TASKS_SCHEDULED_EXECUTION.observation(null, DEFAULT_CONVENTION,
					this.contextSupplier, this.observationRegistrySupplier.get());
			if (this.shouldBlock) {
				CountDownLatch latch = new CountDownLatch(1);
				TrackingSubscriber subscriber = new TrackingSubscriber(this.subscriptionTrackerRegistry, observation, latch);
				subscribe(subscriber, observation);
				try {
					latch.await();
				}
				catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
			}
			else {
				TrackingSubscriber subscriber = new TrackingSubscriber(this.subscriptionTrackerRegistry, observation);
				subscribe(subscriber, observation);
			}
		}

		private void subscribe(TrackingSubscriber subscriber, Observation observation) {
			this.subscriptionTrackerRegistry.add(subscriber);
			if (reactorPresent) {
				Flux.from(this.publisher)
						.contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation))
						.subscribe(subscriber);
			}
			else {
				this.publisher.subscribe(subscriber);
			}
		}
	}


	/**
	 * A {@code Subscriber} which keeps track of its {@code Subscription} and exposes the
	 * capacity to cancel the subscription as a {@code Runnable}. Can optionally support
	 * blocking if a {@code CountDownLatch} is supplied during construction.
	 */
	private static final class TrackingSubscriber implements Subscriber<Object>, Runnable {

		private final List<Runnable> subscriptionTrackerRegistry;

		private final Observation observation;

		@Nullable
		private final CountDownLatch blockingLatch;

		// Implementation note: since this is created last-minute when subscribing,
		// there shouldn't be a way to cancel the tracker externally from the
		// ScheduledAnnotationBeanProcessor before the #setSubscription(Subscription)
		// method is called.
		@Nullable
		private Subscription subscription;

		TrackingSubscriber(List<Runnable> subscriptionTrackerRegistry, Observation observation) {
			this(subscriptionTrackerRegistry, observation, null);
		}

		TrackingSubscriber(List<Runnable> subscriptionTrackerRegistry, Observation observation, @Nullable CountDownLatch latch) {
			this.subscriptionTrackerRegistry = subscriptionTrackerRegistry;
			this.observation = observation;
			this.blockingLatch = latch;
		}

		@Override
		public void run() {
			if (this.subscription != null) {
				this.subscription.cancel();
				this.observation.stop();
			}
			if (this.blockingLatch != null) {
				this.blockingLatch.countDown();
			}
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			this.subscription = subscription;
			this.observation.start();
			subscription.request(Integer.MAX_VALUE);
		}

		@Override
		public void onNext(Object obj) {
			// no-op
		}

		@Override
		public void onError(Throwable ex) {
			this.subscriptionTrackerRegistry.remove(this);
			logger.warn("Unexpected error occurred in scheduled reactive task", ex);
			this.observation.error(ex);
			this.observation.stop();
			if (this.blockingLatch != null) {
				this.blockingLatch.countDown();
			}
		}

		@Override
		public void onComplete() {
			this.subscriptionTrackerRegistry.remove(this);
			if (this.observation.getContext() instanceof ScheduledTaskObservationContext context) {
				context.setComplete(true);
			}
			this.observation.stop();
			if (this.blockingLatch != null) {
				this.blockingLatch.countDown();
			}
		}
	}

}

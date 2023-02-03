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
import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Helper class for @{@link ScheduledAnnotationBeanPostProcessor} to support reactive cases
 * without a dependency on optional classes.
 * @author Simon Baslé
 * @since 6.0.x //FIXME
 */
abstract class ScheduledAnnotationReactiveSupport {

	static final boolean reactorPresent = ClassUtils.isPresent(
			"reactor.core.publisher.Flux", ScheduledAnnotationReactiveSupport.class.getClassLoader());

	static boolean isReactive(Method method) {
		return Publisher.class.isAssignableFrom(method.getReturnType());
	}

	/**
	 * Encapsulates the logic of {@code @Scheduled} on reactive types, using Reactor.
	 */
	static class ReactiveTask {

		private final Publisher<?> publisher;
		private final Duration initialDelay;
		private final Duration otherDelay;
		private final boolean isFixedRate;
		private final String checkpoint;
		private final Disposable.Swap disposable;

		private final Log logger = LogFactory.getLog(getClass());


		protected ReactiveTask(Method method, Object bean, Duration initialDelay,
				Duration otherDelay, boolean isFixedRate) {

			Assert.isTrue(method.getParameterCount() == 0, "Only no-arg methods may be annotated with @Scheduled");
			Method invocableMethod = AopUtils.selectInvocableMethod(method, bean.getClass());
			try {
				ReflectionUtils.makeAccessible(invocableMethod);
				Object r = invocableMethod.invoke(bean);
				this.publisher = (Publisher<?>) r;
			}
			catch (InvocationTargetException ex) {
				throw new IllegalArgumentException("Cannot obtain a Publisher from the @Scheduled reactive method", ex.getTargetException());
			}
			catch (IllegalAccessException ex) {
				throw new IllegalArgumentException("Cannot obtain a Publisher from the @Scheduled reactive method", ex);
			}

			this.initialDelay = initialDelay;
			this.otherDelay = otherDelay;
			this.isFixedRate = isFixedRate;

			this.disposable = Disposables.swap();
			this.checkpoint = "@Scheduled '"+ method.getDeclaringClass().getName()
					+ "#" + method.getName() + "()' [ScheduledAnnotationReactiveSupport]";
		}

		private Mono<Void> safeExecutionMono() {
			Mono<Void> executionMono;
			if (this.publisher instanceof Mono) {
				executionMono = Mono.from(this.publisher).then();
			}
			else {
				executionMono = Flux.from(this.publisher).then();
			}
			if (this.logger.isWarnEnabled()) {
				executionMono = executionMono.doOnError(ex -> this.logger.warn(
						"Ignored error in publisher from " + this.checkpoint, ex));
			}
			executionMono = executionMono.onErrorComplete();
			return executionMono;
		}

		public void subscribe() {
			if (this.disposable.isDisposed()) {
				return;
			}

			final Mono<Void> executionMono = safeExecutionMono();
			Flux<Void> scheduledFlux;
			if (this.isFixedRate) {
				scheduledFlux = Flux.interval(this.initialDelay, this.otherDelay)
						.flatMap(it -> executionMono);
			}
			else {
				scheduledFlux = Mono.delay(this.otherDelay).then(executionMono).repeat();
				if (!this.initialDelay.isZero()) {
					scheduledFlux = Flux.concat(
							Mono.delay(this.initialDelay).then(executionMono),
							scheduledFlux
					);
				}
			}
			// Subscribe and ensure that errors can be traced back to the @Scheduled via a checkpoint
			if (this.disposable.isDisposed()) {
				return;
			}
			this.disposable.update(scheduledFlux.checkpoint(this.checkpoint)
					.subscribe(it -> {}, ex -> ReactiveTask.this.logger.error("Unexpected error occurred in scheduled reactive task", ex)));
		}

		public void cancel() {
			this.disposable.dispose();
		}
	}
}

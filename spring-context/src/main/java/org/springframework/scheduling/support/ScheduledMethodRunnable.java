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

package org.springframework.scheduling.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Supplier;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.scheduling.config.DefaultScheduledTaskObservationConvention;
import org.springframework.scheduling.config.ScheduledTaskObservationContext;
import org.springframework.scheduling.config.ScheduledTaskObservationConvention;
import org.springframework.scheduling.config.ScheduledTaskObservationDocumentation;
import org.springframework.util.ReflectionUtils;

/**
 * Variant of {@link MethodInvokingRunnable} meant to be used for processing
 * of no-arg scheduled methods. Propagates user exceptions to the caller,
 * assuming that an error strategy for Runnables is in place.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 3.0.6
 * @see org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
 */
public class ScheduledMethodRunnable implements Runnable {

	private static final ScheduledTaskObservationConvention DEFAULT_CONVENTION = new DefaultScheduledTaskObservationConvention();

	private final Object target;

	private final Method method;

	private final Supplier<ObservationRegistry> observationRegistrySupplier;

	/**
	 * Create a {@code ScheduledMethodRunnable} for the given target instance,
	 * calling the specified method.
	 * @param target the target instance to call the method on
	 * @param method the target method to call
	 * @param observationRegistrySupplier a supplier for the observation registry to use
	 * @since 6.1
	 */
	public ScheduledMethodRunnable(Object target, Method method, Supplier<ObservationRegistry> observationRegistrySupplier) {
		this.target = target;
		this.method = method;
		this.observationRegistrySupplier = observationRegistrySupplier;
	}

	/**
	 * Create a {@code ScheduledMethodRunnable} for the given target instance,
	 * calling the specified method.
	 * @param target the target instance to call the method on
	 * @param method the target method to call
	 */
	public ScheduledMethodRunnable(Object target, Method method) {
		this(target, method, () -> ObservationRegistry.NOOP);
	}

	/**
	 * Create a {@code ScheduledMethodRunnable} for the given target instance,
	 * calling the specified method by name.
	 * @param target the target instance to call the method on
	 * @param methodName the name of the target method
	 * @throws NoSuchMethodException if the specified method does not exist
	 */
	public ScheduledMethodRunnable(Object target, String methodName) throws NoSuchMethodException {
		this(target, target.getClass().getMethod(methodName));
	}


	/**
	 * Return the target instance to call the method on.
	 */
	public Object getTarget() {
		return this.target;
	}

	/**
	 * Return the target method to call.
	 */
	public Method getMethod() {
		return this.method;
	}


	@Override
	public void run() {
		ScheduledTaskObservationContext context = new ScheduledTaskObservationContext(this.target, this.method);
		Observation observation = ScheduledTaskObservationDocumentation.TASKS_SCHEDULED_EXECUTION.observation(
				null, DEFAULT_CONVENTION,
				() -> context, this.observationRegistrySupplier.get());
		observation.observe(() -> runInternal(context));
	}

	private void runInternal(ScheduledTaskObservationContext context) {
		try {
			ReflectionUtils.makeAccessible(this.method);
			this.method.invoke(this.target);
			context.setComplete(true);
		}
		catch (InvocationTargetException ex) {
			ReflectionUtils.rethrowRuntimeException(ex.getTargetException());
		}
		catch (IllegalAccessException ex) {
			throw new UndeclaredThrowableException(ex);
		}
	}

	@Override
	public String toString() {
		return this.method.getDeclaringClass().getName() + "." + this.method.getName();
	}

}

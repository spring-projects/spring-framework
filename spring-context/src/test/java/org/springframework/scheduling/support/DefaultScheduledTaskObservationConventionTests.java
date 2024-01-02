/*
 * Copyright 2002-2024 the original author or authors.
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


import java.lang.reflect.Method;

import io.micrometer.common.KeyValue;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultScheduledTaskObservationConvention}.
 */
class DefaultScheduledTaskObservationConventionTests {

	private final Method taskMethod = ClassUtils.getMethod(BeanWithScheduledMethods.class, "process");

	private final Method runMethod = ClassUtils.getMethod(Runnable.class, "run");

	private final ScheduledTaskObservationConvention convention = new DefaultScheduledTaskObservationConvention();


	@Test
	void observationShouldHaveDefaultName() {
		assertThat(convention.getName()).isEqualTo("tasks.scheduled.execution");
	}

	@Test
	void observationShouldHaveContextualName() {
		ScheduledTaskObservationContext context = new ScheduledTaskObservationContext(new BeanWithScheduledMethods(), taskMethod);
		assertThat(convention.getContextualName(context)).isEqualTo("task beanWithScheduledMethods.process");
	}

	@Test
	void observationShouldHaveContextualNameForProxiedClass() {
		Object proxy = ProxyFactory.getProxy(new SingletonTargetSource(new BeanWithScheduledMethods()));
		ScheduledTaskObservationContext context = new ScheduledTaskObservationContext(proxy, taskMethod);
		assertThat(convention.getContextualName(context)).isEqualTo("task beanWithScheduledMethods.process");
	}

	@Test
	void observationShouldHaveTargetType() {
		ScheduledTaskObservationContext context = new ScheduledTaskObservationContext(new BeanWithScheduledMethods(), taskMethod);
		assertThat(convention.getLowCardinalityKeyValues(context))
				.contains(KeyValue.of("code.namespace", getClass().getCanonicalName() + ".BeanWithScheduledMethods"));
	}

	@Test
	void observationShouldHaveMethodName() {
		ScheduledTaskObservationContext context = new ScheduledTaskObservationContext(new BeanWithScheduledMethods(), taskMethod);
		assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("code.function", "process"));
	}

	@Test
	void observationShouldHaveTargetTypeForAnonymousClass() {
		Runnable runnable = () -> { };
		ScheduledTaskObservationContext context = new ScheduledTaskObservationContext(runnable, runMethod);
		assertThat(convention.getLowCardinalityKeyValues(context))
				.contains(KeyValue.of("code.namespace", "ANONYMOUS"));
	}

	@Test
	void observationShouldHaveMethodNameForAnonymousClass() {
		Runnable runnable = () -> { };
		ScheduledTaskObservationContext context = new ScheduledTaskObservationContext(runnable, runMethod);
		assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("code.function", "run"));
	}

	@Test
	void observationShouldHaveSuccessfulOutcome() {
		ScheduledTaskObservationContext context = new ScheduledTaskObservationContext(new BeanWithScheduledMethods(), taskMethod);
		context.setComplete(true);
		assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("outcome", "SUCCESS"),
				KeyValue.of("exception", "none"));
	}

	@Test
	void observationShouldHaveErrorOutcome() {
		ScheduledTaskObservationContext context = new ScheduledTaskObservationContext(new BeanWithScheduledMethods(), taskMethod);
		context.setError(new IllegalStateException("test error"));
		assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("outcome", "ERROR"),
				KeyValue.of("exception", "IllegalStateException"));
	}

	@Test
	void observationShouldHaveUnknownOutcome() {
		ScheduledTaskObservationContext context = new ScheduledTaskObservationContext(new BeanWithScheduledMethods(), taskMethod);
		assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("outcome", "UNKNOWN"),
				KeyValue.of("exception", "none"));
	}


	interface TaskProcessor {

		void process();
	}


	static class BeanWithScheduledMethods implements TaskProcessor {

		@Override
		public void process() {
		}
	}

}

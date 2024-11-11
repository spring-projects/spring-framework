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

package org.springframework.scheduling.annotation;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.observability.DefaultSignalListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.support.ScheduledTaskObservationContext;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Observability tests for {@link ScheduledAnnotationBeanPostProcessor}.
 *
 * @author Brian Clozel
 */
class ScheduledAnnotationBeanPostProcessorObservabilityTests {

	private final StaticApplicationContext context = new StaticApplicationContext();

	private final SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	private final TestObservationRegistry observationRegistry = TestObservationRegistry.create();


	@AfterEach
	void closeContext() {
		context.close();
	}


	@Test
	void shouldRecordSuccessObservationsForTasks() throws Exception {
		registerScheduledBean(FixedDelayBean.class);
		runScheduledTaskAndAwait();
		assertThatTaskObservation().hasLowCardinalityKeyValue("outcome", "SUCCESS")
				.hasLowCardinalityKeyValue("code.function", "fixedDelay")
				.hasLowCardinalityKeyValue("code.namespace", getClass().getCanonicalName() + ".FixedDelayBean")
				.hasLowCardinalityKeyValue("exception", "none");
	}

	@Test
	void shouldRecordFailureObservationsForTasksThrowing() throws Exception {
		registerScheduledBean(FixedDelayErrorBean.class);
		runScheduledTaskAndAwait();
		assertThatTaskObservation().hasLowCardinalityKeyValue("outcome", "ERROR")
				.hasLowCardinalityKeyValue("code.function", "error")
				.hasLowCardinalityKeyValue("code.namespace", getClass().getCanonicalName() + ".FixedDelayErrorBean")
				.hasLowCardinalityKeyValue("exception", "IllegalStateException");
	}

	@Test
	void shouldRecordSuccessObservationsForReactiveTasks() throws Exception {
		registerScheduledBean(FixedDelayReactiveBean.class);
		runScheduledTaskAndAwait();
		assertThatTaskObservation().hasLowCardinalityKeyValue("outcome", "SUCCESS")
				.hasLowCardinalityKeyValue("code.function", "fixedDelay")
				.hasLowCardinalityKeyValue("code.namespace", getClass().getCanonicalName() + ".FixedDelayReactiveBean")
				.hasLowCardinalityKeyValue("exception", "none");
	}

	@Test
	void shouldRecordFailureObservationsForReactiveTasksThrowing() throws Exception {
		registerScheduledBean(FixedDelayReactiveErrorBean.class);
		runScheduledTaskAndAwait();
		assertThatTaskObservation().hasLowCardinalityKeyValue("outcome", "ERROR")
				.hasLowCardinalityKeyValue("code.function", "error")
				.hasLowCardinalityKeyValue("code.namespace", getClass().getCanonicalName() + ".FixedDelayReactiveErrorBean")
				.hasLowCardinalityKeyValue("exception", "IllegalStateException");
	}

	@Test
	void shouldRecordCancelledObservationsForTasks() throws Exception {
		registerScheduledBean(CancelledTaskBean.class);
		ScheduledTask scheduledTask = getScheduledTask();
		this.taskExecutor.execute(scheduledTask.getTask().getRunnable());
		context.getBean(TaskTester.class).await();
		scheduledTask.cancel();
		assertThatTaskObservation().hasLowCardinalityKeyValue("outcome", "UNKNOWN")
				.hasLowCardinalityKeyValue("code.function", "cancelled")
				.hasLowCardinalityKeyValue("code.namespace", getClass().getCanonicalName() + ".CancelledTaskBean")
				.hasLowCardinalityKeyValue("exception", "none");
	}

	@Test
	void shouldRecordCancelledObservationsForReactiveTasks() throws Exception {
		registerScheduledBean(CancelledReactiveTaskBean.class);
		ScheduledTask scheduledTask = getScheduledTask();
		this.taskExecutor.execute(scheduledTask.getTask().getRunnable());
		context.getBean(TaskTester.class).await();
		scheduledTask.cancel();
		assertThatTaskObservation().hasLowCardinalityKeyValue("outcome", "UNKNOWN")
				.hasLowCardinalityKeyValue("code.function", "cancelled")
				.hasLowCardinalityKeyValue("code.namespace", getClass().getCanonicalName() + ".CancelledReactiveTaskBean")
				.hasLowCardinalityKeyValue("exception", "none");
	}

	@Test
	void shouldHaveCurrentObservationInScope() throws Exception {
		registerScheduledBean(CurrentObservationBean.class);
		runScheduledTaskAndAwait();
		assertThatTaskObservation().hasLowCardinalityKeyValue("outcome", "SUCCESS")
				.hasLowCardinalityKeyValue("code.function", "hasCurrentObservation")
				.hasLowCardinalityKeyValue("code.namespace", getClass().getCanonicalName() + ".CurrentObservationBean")
				.hasLowCardinalityKeyValue("exception", "none");
	}

	@Test
	void shouldHaveCurrentObservationInReactiveScope() throws Exception {
		registerScheduledBean(CurrentObservationReactiveBean.class);
		runScheduledTaskAndAwait();
		assertThatTaskObservation().hasLowCardinalityKeyValue("outcome", "SUCCESS")
				.hasLowCardinalityKeyValue("code.function", "hasCurrentObservation")
				.hasLowCardinalityKeyValue("code.namespace", getClass().getCanonicalName() + ".CurrentObservationReactiveBean")
				.hasLowCardinalityKeyValue("exception", "none");
	}


	private void registerScheduledBean(Class<?> beanClass) {
		BeanDefinition processorDefinition = new RootBeanDefinition(ScheduledAnnotationBeanPostProcessor.class);
		BeanDefinition targetDefinition = new RootBeanDefinition(beanClass);
		targetDefinition.getPropertyValues().add("observationRegistry", this.observationRegistry);
		context.registerBeanDefinition("postProcessor", processorDefinition);
		context.registerBeanDefinition("target", targetDefinition);
		context.registerBean("schedulingConfigurer", SchedulingConfigurer.class, () -> taskRegistrar -> taskRegistrar.setObservationRegistry(observationRegistry));
		context.refresh();
	}

	private ScheduledTask getScheduledTask() {
		ScheduledTaskHolder taskHolder = context.getBean("postProcessor", ScheduledTaskHolder.class);
		return taskHolder.getScheduledTasks().iterator().next();
	}

	private void runScheduledTaskAndAwait() throws InterruptedException {
		ScheduledTask scheduledTask = getScheduledTask();
		try {
			scheduledTask.getTask().getRunnable().run();
		}
		catch (Throwable exc) {
			// ignore exceptions thrown by test tasks
		}
		context.getBean(TaskTester.class).await();
	}

	private TestObservationRegistryAssert.TestObservationRegistryAssertReturningObservationContextAssert assertThatTaskObservation() {
		return TestObservationRegistryAssert.assertThat(this.observationRegistry)
				.hasObservationWithNameEqualTo("tasks.scheduled.execution").that();
	}


	abstract static class TaskTester {

		ObservationRegistry observationRegistry;

		CountDownLatch latch = new CountDownLatch(1);

		public void setObservationRegistry(ObservationRegistry observationRegistry) {
			this.observationRegistry = observationRegistry;
		}

		void await() throws InterruptedException {
			this.latch.await(3, TimeUnit.SECONDS);
		}
	}


	static class FixedDelayBean extends TaskTester {

		@Scheduled(fixedDelay = 10_000, initialDelay = 5_000)
		void fixedDelay() {
			this.latch.countDown();
		}
	}


	static class FixedDelayErrorBean extends TaskTester {

		@Scheduled(fixedDelay = 10_000, initialDelay = 5_000)
		void error() {
			this.latch.countDown();
			throw new IllegalStateException("test error");
		}
	}


	static class FixedDelayReactiveBean extends TaskTester {

		@Scheduled(fixedDelay = 10_000, initialDelay = 5_000)
		Mono<Object> fixedDelay() {
			return Mono.empty().doOnTerminate(() -> this.latch.countDown());
		}
	}


	static class FixedDelayReactiveErrorBean extends TaskTester {

		@Scheduled(fixedDelay = 10_000, initialDelay = 5_000)
		Mono<Object> error() {
			return Mono.error(new IllegalStateException("test error"))
					.doOnTerminate(() -> this.latch.countDown());
		}
	}


	static class CancelledTaskBean extends TaskTester {

		@Scheduled(fixedDelay = 10_000, initialDelay = 5_000)
		void cancelled() {
			this.latch.countDown();
			try {
				Thread.sleep(5000);
			}
			catch (InterruptedException exc) {
				// ignore cancelled task
			}
		}
	}


	static class CancelledReactiveTaskBean extends TaskTester {

		@Scheduled(fixedDelay = 10_000, initialDelay = 5_000)
		Flux<Long> cancelled() {
			return Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
					.doOnNext(el -> this.latch.countDown());
		}
	}


	static class CurrentObservationBean extends TaskTester {

		@Scheduled(fixedDelay = 10_000, initialDelay = 5_000)
		void hasCurrentObservation() {
			Observation observation = this.observationRegistry.getCurrentObservation();
			assertThat(observation).isNotNull();
			assertThat(observation.getContext()).isInstanceOf(ScheduledTaskObservationContext.class);
			this.latch.countDown();
		}
	}


	static class CurrentObservationReactiveBean extends TaskTester {

		@Scheduled(fixedDelay = 10_000, initialDelay = 5_000)
		Mono<String> hasCurrentObservation() {
			return Mono.just("test")
					.tap(() -> new DefaultSignalListener<>() {
						@Override
						public void doFirst() {
							Observation observation = observationRegistry.getCurrentObservation();
							assertThat(observation).isNotNull();
							assertThat(observation.getContext()).isInstanceOf(ScheduledTaskObservationContext.class);
						}
					})
					.doOnTerminate(() -> this.latch.countDown());
		}
	}

}

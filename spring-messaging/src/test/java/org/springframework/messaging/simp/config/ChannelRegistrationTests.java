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

package org.springframework.messaging.simp.config;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link ChannelRegistration}.
 *
 * @author Stephane Nicoll
 */
class ChannelRegistrationTests {

	private final Supplier<TaskExecutor> fallback = mock();

	private final Consumer<TaskExecutor> customizer = mock();

	@Test
	void emptyRegistrationUsesFallback() {
		TaskExecutor fallbackTaskExecutor = mock(TaskExecutor.class);
		given(this.fallback.get()).willReturn(fallbackTaskExecutor);
		ChannelRegistration registration = new ChannelRegistration();
		assertThat(registration.hasTaskExecutor()).isFalse();
		TaskExecutor actual = registration.getTaskExecutor(this.fallback, this.customizer);
		assertThat(actual).isSameAs(fallbackTaskExecutor);
		verify(this.fallback).get();
		verify(this.customizer).accept(fallbackTaskExecutor);
	}

	@Test
	void emptyRegistrationDoesNotHaveInterceptors() {
		ChannelRegistration registration = new ChannelRegistration();
		assertThat(registration.hasInterceptors()).isFalse();
		assertThat(registration.getInterceptors()).isEmpty();
	}

	@Test
	void taskRegistrationCreatesDefaultInstance() {
		ChannelRegistration registration = new ChannelRegistration();
		registration.taskExecutor();
		assertThat(registration.hasTaskExecutor()).isTrue();
		TaskExecutor taskExecutor = registration.getTaskExecutor(this.fallback, this.customizer);
		assertThat(taskExecutor).isInstanceOf(ThreadPoolTaskExecutor.class);
		verifyNoInteractions(this.fallback);
		verify(this.customizer).accept(taskExecutor);
	}

	@Test
	void taskRegistrationWithExistingThreadPoolTaskExecutor() {
		ThreadPoolTaskExecutor existingTaskExecutor = mock(ThreadPoolTaskExecutor.class);
		ChannelRegistration registration = new ChannelRegistration();
		registration.taskExecutor(existingTaskExecutor);
		assertThat(registration.hasTaskExecutor()).isTrue();
		TaskExecutor taskExecutor = registration.getTaskExecutor(this.fallback, this.customizer);
		assertThat(taskExecutor).isSameAs(existingTaskExecutor);
		verifyNoInteractions(this.fallback);
		verify(this.customizer).accept(taskExecutor);
	}

	@Test
	void configureExecutor() {
		ChannelRegistration registration = new ChannelRegistration();
		TaskExecutor taskExecutor = mock(TaskExecutor.class);
		registration.executor(taskExecutor);
		assertThat(registration.hasTaskExecutor()).isTrue();
		TaskExecutor taskExecutor1 = registration.getTaskExecutor(this.fallback, this.customizer);
		assertThat(taskExecutor1).isSameAs(taskExecutor);
		verifyNoInteractions(this.fallback, this.customizer);
	}

	@Test
	void configureExecutorTakesPrecedenceOverTaskRegistration() {
		ChannelRegistration registration = new ChannelRegistration();
		TaskExecutor taskExecutor = mock(TaskExecutor.class);
		registration.executor(taskExecutor);
		ThreadPoolTaskExecutor ignored = mock(ThreadPoolTaskExecutor.class);
		registration.taskExecutor(ignored);
		assertThat(registration.hasTaskExecutor()).isTrue();
		assertThat(registration.getTaskExecutor(this.fallback, this.customizer)).isSameAs(taskExecutor);
		verifyNoInteractions(ignored, this.fallback, this.customizer);

	}

	@Test
	void configureInterceptors() {
		ChannelRegistration registration = new ChannelRegistration();
		ChannelInterceptor interceptor1 = mock(ChannelInterceptor.class);
		registration.interceptors(interceptor1);
		ChannelInterceptor interceptor2 = mock(ChannelInterceptor.class);
		registration.interceptors(interceptor2);
		assertThat(registration.getInterceptors()).containsExactly(interceptor1, interceptor2);
	}

}

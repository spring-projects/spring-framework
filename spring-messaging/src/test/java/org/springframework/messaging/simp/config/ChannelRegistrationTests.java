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

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

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

	private final Supplier<Executor> fallback = mock();

	private final Consumer<Executor> customizer = mock();

	@Test
	void emptyRegistrationUsesFallback() {
		Executor fallbackExecutor = mock(Executor.class);
		given(this.fallback.get()).willReturn(fallbackExecutor);
		ChannelRegistration registration = new ChannelRegistration();
		assertThat(registration.hasExecutor()).isFalse();
		Executor actual = registration.getExecutor(this.fallback, this.customizer);
		assertThat(actual).isSameAs(fallbackExecutor);
		verify(this.fallback).get();
		verify(this.customizer).accept(fallbackExecutor);
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
		assertThat(registration.hasExecutor()).isTrue();
		Executor executor = registration.getExecutor(this.fallback, this.customizer);
		assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
		verifyNoInteractions(this.fallback);
		verify(this.customizer).accept(executor);
	}

	@Test
	void taskRegistrationWithExistingThreadPoolTaskExecutorDoesNotInvokeCustomizer() {
		ThreadPoolTaskExecutor existingExecutor = mock(ThreadPoolTaskExecutor.class);
		ChannelRegistration registration = new ChannelRegistration();
		registration.taskExecutor(existingExecutor);
		assertThat(registration.hasExecutor()).isTrue();
		Executor executor = registration.getExecutor(this.fallback, this.customizer);
		assertThat(executor).isSameAs(existingExecutor);
		verifyNoInteractions(this.fallback, this.customizer);
	}

	@Test
	void configureExecutor() {
		ChannelRegistration registration = new ChannelRegistration();
		Executor executor = mock(Executor.class);
		registration.executor(executor);
		assertThat(registration.hasExecutor()).isTrue();
		Executor actualExecutor = registration.getExecutor(this.fallback, this.customizer);
		assertThat(actualExecutor).isSameAs(executor);
		verifyNoInteractions(this.fallback, this.customizer);
	}

	@Test
	void configureExecutorTakesPrecedenceOverTaskRegistration() {
		ChannelRegistration registration = new ChannelRegistration();
		Executor executor = mock(Executor.class);
		registration.executor(executor);
		ThreadPoolTaskExecutor ignored = mock(ThreadPoolTaskExecutor.class);
		registration.taskExecutor(ignored);
		assertThat(registration.hasExecutor()).isTrue();
		assertThat(registration.getExecutor(this.fallback, this.customizer)).isSameAs(executor);
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

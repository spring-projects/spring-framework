/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.generator.config;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link InjectedElementAttributes}.
 *
 * @author Stephane Nicoll
 */
class InjectedElementAttributesTests {

	private static final InjectedElementAttributes unresolved = new InjectedElementAttributes(null);

	private static final InjectedElementAttributes resolved = new InjectedElementAttributes(Collections.singletonList("test"));

	@Test
	void isResolvedWithUnresolvedAttributes() {
		assertThat(unresolved.isResolved()).isFalse();
	}

	@Test
	void isResolvedWithResoledAttributes() {
		assertThat(resolved.isResolved()).isTrue();
	}

	@Test
	void ifResolvedWithUnresolvedAttributesDoesNotInvokeRunnable() {
		Runnable runnable = mock(Runnable.class);
		unresolved.ifResolved(runnable);
		verifyNoInteractions(runnable);
	}

	@Test
	void ifResolvedWithResolvedAttributesInvokesRunnable() {
		Runnable runnable = mock(Runnable.class);
		resolved.ifResolved(runnable);
		verify(runnable).run();
	}

	@Test
	@SuppressWarnings("unchecked")
	void ifResolvedWithUnresolvedAttributesDoesNotInvokeConsumer() {
		BeanDefinitionRegistrar.ThrowableConsumer<InjectedElementAttributes> consumer = mock(BeanDefinitionRegistrar.ThrowableConsumer.class);
		unresolved.ifResolved(consumer);
		verifyNoInteractions(consumer);
	}

	@Test
	@SuppressWarnings("unchecked")
	void ifResolvedWithResolvedAttributesInvokesConsumer() {
		BeanDefinitionRegistrar.ThrowableConsumer<InjectedElementAttributes> consumer = mock(BeanDefinitionRegistrar.ThrowableConsumer.class);
		resolved.ifResolved(consumer);
		verify(consumer).accept(resolved);
	}

	@Test
	void getWithAvailableAttribute() {
		InjectedElementAttributes attributes = new InjectedElementAttributes(Collections.singletonList("test"));
		assertThat((String) attributes.get(0)).isEqualTo("test");
	}

	@Test
	void getWithTypeAndAvailableAttribute() {
		InjectedElementAttributes attributes = new InjectedElementAttributes(Collections.singletonList("test"));
		assertThat(attributes.get(0, String.class)).isEqualTo("test");
	}

}

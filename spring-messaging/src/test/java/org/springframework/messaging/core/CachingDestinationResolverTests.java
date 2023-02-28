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

package org.springframework.messaging.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link CachingDestinationResolverProxy}.
 *
 * @author Agim Emruli
 * @author Juergen Hoeller
 */
public class CachingDestinationResolverTests {

	@Test
	public void cachedDestination() {
		@SuppressWarnings("unchecked")
		DestinationResolver<String> resolver = mock();
		CachingDestinationResolverProxy<String> resolverProxy = new CachingDestinationResolverProxy<>(resolver);

		given(resolver.resolveDestination("abcd")).willReturn("dcba");
		given(resolver.resolveDestination("1234")).willReturn("4321");

		assertThat(resolverProxy.resolveDestination("abcd")).isEqualTo("dcba");
		assertThat(resolverProxy.resolveDestination("1234")).isEqualTo("4321");
		assertThat(resolverProxy.resolveDestination("1234")).isEqualTo("4321");
		assertThat(resolverProxy.resolveDestination("abcd")).isEqualTo("dcba");

		verify(resolver, times(1)).resolveDestination("abcd");
		verify(resolver, times(1)).resolveDestination("1234");
	}

	@Test
	public void noTargetSet() {
		CachingDestinationResolverProxy<String> resolverProxy = new CachingDestinationResolverProxy<>();
		assertThatIllegalArgumentException().isThrownBy(
				resolverProxy::afterPropertiesSet);
	}

	@Test
	public void nullTargetThroughConstructor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new CachingDestinationResolverProxy<String>(null));
	}

}

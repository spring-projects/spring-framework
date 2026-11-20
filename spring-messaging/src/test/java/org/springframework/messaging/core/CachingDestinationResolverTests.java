/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * Tests for {@link CachingDestinationResolverProxy}.
 *
 * @author Agim Emruli
 * @author Juergen Hoeller
 */
class CachingDestinationResolverTests {

	@Test
	void cachedDestination() {
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
	void noTargetSet() {
		CachingDestinationResolverProxy<String> resolverProxy = new CachingDestinationResolverProxy<>();
		assertThatIllegalArgumentException().isThrownBy(
				resolverProxy::afterPropertiesSet);
	}

	@Test
	void nullTargetThroughConstructor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new CachingDestinationResolverProxy<String>(null));
	}

}

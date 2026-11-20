/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;
import org.mockito.internal.configuration.plugins.Plugins;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpringMockResolver}.
 *
 * @author Andy Wilkinson
 * @since 6.2
 * @see SpringMockResolverTests
 */
class SpringMockResolverIntegrationTests {

	@Test
	void customMockResolverIsRegisteredWithMockito() {
		assertThat(Plugins.getMockResolvers()).hasOnlyElementsOfType(SpringMockResolver.class);
	}

}

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.bean.override.mockito.integration.AbstractMockitoBeanAndGenericsIntegrationTests.SomethingImpl;
import org.springframework.test.context.bean.override.mockito.integration.AbstractMockitoBeanAndGenericsIntegrationTests.ThingImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Concrete implementation of {@link AbstractMockitoBeanAndGenericsIntegrationTests}.
 *
 * @author Madhura Bhave
 * @author Sam Brannen
 * @since 6.2
 */
class MockitoBeanAndGenericsIntegrationTests extends AbstractMockitoBeanAndGenericsIntegrationTests<ThingImpl, SomethingImpl> {

	@Test
	void mockitoBeanShouldResolveConcreteType() {
		assertThat(something).isExactlyInstanceOf(SomethingImpl.class);

		when(something.speak()).thenReturn("Hola");
		assertThat(thing.getSomething().speak()).isEqualTo("Hola");
	}

}

/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.bean.override.mockito.integration.AbstractMockitoBeanAndGenericsIntegrationTests.SomethingImpl;
import org.springframework.test.context.bean.override.mockito.integration.AbstractMockitoBeanAndGenericsIntegrationTests.ThingImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.when;

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

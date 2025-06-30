/*
 * Copyright 2002-present the original author or authors.
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

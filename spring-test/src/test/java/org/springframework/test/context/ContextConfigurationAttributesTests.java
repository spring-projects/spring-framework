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

package org.springframework.test.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContextConfigurationAttributes}.
 *
 * @author Sam Brannen
 * @since 7.0.2
 */
class ContextConfigurationAttributesTests {

	@Test  // gh-36000
	void defaultsConstructor() {
		var configAttributes = new ContextConfigurationAttributes(getClass());

		assertThat(configAttributes.getDeclaringClass()).isEqualTo(getClass());
		assertThat(configAttributes.getClasses()).isEmpty();
		assertThat(configAttributes.getLocations()).isEmpty();
		assertThat(configAttributes.getInitializers()).isEmpty();
		assertThat(configAttributes.getContextLoaderClass()).isEqualTo(ContextLoader.class);
		assertThat(configAttributes.isInheritInitializers()).isTrue();
		assertThat(configAttributes.isInheritLocations()).isTrue();
	}

}

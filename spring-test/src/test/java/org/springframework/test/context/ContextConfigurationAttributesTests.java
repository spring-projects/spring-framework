/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

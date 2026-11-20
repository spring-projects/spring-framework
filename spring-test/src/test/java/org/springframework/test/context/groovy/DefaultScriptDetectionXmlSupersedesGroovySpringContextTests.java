/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.groovy;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test class that verifies proper detection of a default
 * XML config file even though a suitable Groovy script exists.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@SpringJUnitConfig
class DefaultScriptDetectionXmlSupersedesGroovySpringContextTests {

	@Autowired
	String foo;


	@Test
	final void foo() {
		assertThat(this.foo).as("The foo field should have been autowired.").isEqualTo("Foo");
	}

}

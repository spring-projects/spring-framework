/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 3.2
 * @see BasicGroovyWacTests
 */
// Config loaded from BasicXmlWacTests-context.xml
@ExtendWith(SpringExtension.class)
@ContextConfiguration
class BasicXmlWacTests extends AbstractBasicWacTests {

	@Test
	void fooBarAutowired() {
		assertThat(foo).isEqualTo("bar");
	}

}

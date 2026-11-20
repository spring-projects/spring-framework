/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.web;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests used to assess claims raised in
 * <a href="https://github.com/spring-projects/spring-framework/issues/14432">gh-14432</a>.
 *
 * @author Sam Brannen
 * @since 3.2
 * @see EnableWebMvcAnnotationConfigTests
 */
@SpringJUnitWebConfig
class EnableWebMvcXmlConfigTests extends AbstractBasicWacTests {

	@Test
	void applicationContextLoads(WebApplicationContext wac) {
		assertThat(wac.getBean("foo", String.class)).isEqualTo("enigma");
	}

}

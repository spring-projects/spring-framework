/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Param}.
 *
 * @author Scott Andrews
 */
class ParamTests {

	private final Param param = new Param();

	@Test
	void name() {
		param.setName("name");
		assertThat(param.getName()).isEqualTo("name");
	}

	@Test
	void value() {
		param.setValue("value");
		assertThat(param.getValue()).isEqualTo("value");
	}

	@Test
	void nullDefaults() {
		assertThat(param.getName()).isNull();
		assertThat(param.getValue()).isNull();
	}

}

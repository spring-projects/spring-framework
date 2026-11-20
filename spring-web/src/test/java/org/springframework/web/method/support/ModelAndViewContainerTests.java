/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.method.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ui.ModelMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link ModelAndViewContainer}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
class ModelAndViewContainerTests {

	private ModelAndViewContainer mavContainer;


	@BeforeEach
	void setup() {
		this.mavContainer = new ModelAndViewContainer();
	}


	@Test
	void getModel() {
		this.mavContainer.addAttribute("name", "value");
		assertThat(this.mavContainer.getModel()).hasSize(1);
		assertThat(this.mavContainer.getModel().get("name")).isEqualTo("value");
	}

	@Test
	void redirectScenarioWithRedirectModel() {
		this.mavContainer.addAttribute("name1", "value1");
		this.mavContainer.setRedirectModel(new ModelMap("name2", "value2"));
		this.mavContainer.setRedirectModelScenario(true);

		assertThat(this.mavContainer.getModel()).hasSize(1);
		assertThat(this.mavContainer.getModel().get("name2")).isEqualTo("value2");
	}

	@Test
	void ignoreDefaultModel() {
		this.mavContainer.addAttribute("name", "value");
		this.mavContainer.setRedirectModelScenario(true);

		assertThat(this.mavContainer.getModel()).isEmpty();
	}

	@Test  // SPR-14045
	void ignoreDefaultModelAndWithoutRedirectModel() {
		this.mavContainer.setRedirectModelScenario(true);
		this.mavContainer.addAttribute("name", "value");

		assertThat(this.mavContainer.getModel()).hasSize(1);
		assertThat(this.mavContainer.getModel().get("name")).isEqualTo("value");
	}


}

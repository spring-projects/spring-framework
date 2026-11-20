/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.view;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.ui.ConcurrentModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Fragment}.
 * @author Rossen Stoyanchev
 */
class FragmentTests {

	@Test
	void mergeAttributes() {
		Fragment fragment = Fragment.create("myView", Map.of("fruit", "apple"));
		fragment.mergeAttributes(new ConcurrentModel("vegetable", "pepper"));

		assertThat(fragment.model()).containsExactly(Map.entry("fruit", "apple"), Map.entry("vegetable", "pepper"));
	}

	@Test
	void mergeAttributesCollision() {
		Fragment fragment = Fragment.create("myView", Map.of("fruit", "apple"));
		fragment.mergeAttributes(new ConcurrentModel("fruit", "orange"));

		assertThat(fragment.model()).containsExactly(Map.entry("fruit", "apple"));
	}

}

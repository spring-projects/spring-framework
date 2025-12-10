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

package org.springframework.web.reactive.result.view;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.ui.ConcurrentModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Fragment}.
 * @author Rossen Stoyanchev
 */
public class FragmentTests {

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

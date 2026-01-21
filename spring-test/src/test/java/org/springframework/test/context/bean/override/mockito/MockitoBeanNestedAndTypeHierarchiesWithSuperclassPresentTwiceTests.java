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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MockitoBean @MockitoBean} which verify that
 * {@code @MockitoBean} fields are not discovered more than once when searching
 * intertwined enclosing class hierarchies and type hierarchies, when a superclass
 * is <em>present</em> twice in the intertwined hierarchies.
 *
 * @author Sam Brannen
 * @since 6.2.7
 * @see MockitoBeanNestedAndTypeHierarchiesWithEnclosingClassPresentTwiceTests
 * @see MockitoBeanWithInterfacePresentTwiceTests
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/34844">gh-34844</a>
 */
class MockitoBeanNestedAndTypeHierarchiesWithSuperclassPresentTwiceTests
		extends AbstractMockitoBeanNestedAndTypeHierarchiesWithSuperclassPresentTwiceTests {

	@Test
	@Override
	void topLevelTest() {
		super.topLevelTest();

		// The following are prerequisites for the reported regression.
		assertThat(NestedTests.class.getSuperclass())
				.isEqualTo(AbstractBaseClassForNestedTests.class);
		assertThat(NestedTests.class.getEnclosingClass())
				.isEqualTo(getClass());
		assertThat(NestedTests.class.getEnclosingClass().getSuperclass())
				.isEqualTo(AbstractBaseClassForNestedTests.class.getEnclosingClass())
				.isEqualTo(getClass().getSuperclass());
	}


	@Nested
	class NestedTests extends AbstractBaseClassForNestedTests {
	}

}

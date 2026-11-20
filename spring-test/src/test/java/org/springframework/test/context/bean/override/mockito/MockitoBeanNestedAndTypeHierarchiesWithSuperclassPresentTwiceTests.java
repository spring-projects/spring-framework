/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

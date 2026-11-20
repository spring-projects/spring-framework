/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.mockito.MockitoAssertions.assertIsMock;

/**
 * Integration tests for {@link MockitoBean @MockitoBean} which verify that
 * {@code @MockitoBean} fields are not discovered more than once when searching
 * intertwined enclosing class hierarchies and type hierarchies, when an enclosing
 * class is <em>present</em> twice in the intertwined hierarchies.
 *
 * @author Sam Brannen
 * @since 6.2.3
 * @see MockitoBeanNestedAndTypeHierarchiesWithSuperclassPresentTwiceTests
 * @see MockitoBeanWithInterfacePresentTwiceTests
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/34324">gh-34324</a>
 */
@ExtendWith(SpringExtension.class)
class MockitoBeanNestedAndTypeHierarchiesWithEnclosingClassPresentTwiceTests {

	@Autowired
	ApplicationContext enclosingContext;

	@MockitoBean
	ExampleService service;


	@Test
	void topLevelTest() {
		assertIsMock(service);
		assertThat(enclosingContext.getBeanNamesForType(ExampleService.class)).hasSize(1);

		// The following are prerequisites for the reported regression.
		assertThat(NestedTests.class.getSuperclass())
				.isEqualTo(AbstractBaseClassForNestedTests.class);
		assertThat(NestedTests.class.getEnclosingClass())
				.isEqualTo(AbstractBaseClassForNestedTests.class.getEnclosingClass())
				.isEqualTo(getClass());
	}


	abstract class AbstractBaseClassForNestedTests {

		@Test
		void nestedTest(ApplicationContext nestedContext) {
			assertIsMock(service);
			assertThat(enclosingContext).isSameAs(nestedContext);
			assertThat(enclosingContext.getBeanNamesForType(ExampleService.class)).hasSize(1);
		}
	}

	@Nested
	class NestedTests extends AbstractBaseClassForNestedTests {
	}

}

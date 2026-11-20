/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.mockito.MockitoAssertions.assertIsMock;

/**
 * Integration tests for {@link MockitoBean @MockitoBean} which verify that type-level
 * {@code @MockitoBean} declarations are not discovered more than once when searching
 * a type hierarchy, when an interface is <em>present</em> twice in the hierarchy.
 *
 * @author Sam Brannen
 * @since 6.2.7
 * @see MockitoBeanNestedAndTypeHierarchiesWithEnclosingClassPresentTwiceTests
 * @see MockitoBeanNestedAndTypeHierarchiesWithSuperclassPresentTwiceTests
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/34844">gh-34844</a>
 */
class MockitoBeanWithInterfacePresentTwiceTests extends AbstractMockitoBeanWithInterfacePresentTwiceTests
		implements MockConfigInterface {

	@Test
	void test(ApplicationContext context) {
		assertIsMock(service);
		assertThat(context.getBeanNamesForType(ExampleService.class)).hasSize(1);

		// The following are prerequisites for the tested scenario.
		assertThat(getClass().getInterfaces()).containsExactly(MockConfigInterface.class);
		assertThat(getClass().getSuperclass().getInterfaces()).containsExactly(MockConfigInterface.class);
	}

}

@MockitoBean(types = ExampleService.class)
interface MockConfigInterface {
}

@ExtendWith(SpringExtension.class)
abstract class AbstractMockitoBeanWithInterfacePresentTwiceTests implements MockConfigInterface {

	@Autowired
	ExampleService service;

}

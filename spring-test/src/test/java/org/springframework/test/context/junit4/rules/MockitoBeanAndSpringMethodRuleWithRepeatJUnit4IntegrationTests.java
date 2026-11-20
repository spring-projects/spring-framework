/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit4.rules;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MockitoBean @MockitoBean}, {@link SpringMethodRule}, and
 * {@link Repeat @Repeat} with JUnit 4.
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/27693">gh-27693</a>
 */
@SuppressWarnings("deprecation")
public class MockitoBeanAndSpringMethodRuleWithRepeatJUnit4IntegrationTests {

	private static int invocations;

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@MockitoBean
	Service service;

	@BeforeClass
	public static void beforeClass() {
		invocations = 0;
	}

	@Test
	@Repeat(2)
	public void repeatedTest() {
		assertThat(service.greeting()).as("mock should have been reset").isNull();

		when(service.greeting()).thenReturn("test");
		assertThat(service.greeting()).isEqualTo("test");

		invocations++;
	}

	@AfterClass
	public static void afterClass() {
		assertThat(invocations).isEqualTo(2);
	}


	interface Service {

		String greeting();
	}

}

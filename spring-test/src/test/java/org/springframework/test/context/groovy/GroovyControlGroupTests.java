/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.groovy;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericGroovyApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple integration test to verify the expected functionality of
 * {@link GenericGroovyApplicationContext}, thereby validating the proper
 * syntax and configuration of {@code "context.groovy"} without using the
 * Spring TestContext Framework.
 *
 * <p>In other words, this test class serves merely as a <em>control group</em>
 * to ensure that there is nothing wrong with the Groovy script used by
 * other tests in this package.
 *
 * @author Sam Brannen
 * @since 4.1
 */
class GroovyControlGroupTests {

	@Test
	void verifyScriptUsingGenericGroovyApplicationContext() {
		try (ConfigurableApplicationContext ctx = new GenericGroovyApplicationContext(getClass(), "context.groovy")) {
			String foo = ctx.getBean("foo", String.class);
			assertThat(foo).isEqualTo("Foo");

			String bar = ctx.getBean("bar", String.class);
			assertThat(bar).isEqualTo("Bar");

			Pet pet = ctx.getBean(Pet.class);
			assertThat(pet).as("pet").isNotNull();
			assertThat(pet.getName()).isEqualTo("Dogbert");

			Employee employee = ctx.getBean(Employee.class);
			assertThat(employee).as("employee").isNotNull();
			assertThat(employee.getName()).isEqualTo("Dilbert");
			assertThat(employee.getCompany()).isEqualTo("???");
		}

	}

}

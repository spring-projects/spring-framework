/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config;

import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ApplicationContext configuration class for various integration tests.
 *
 * <p>The beans defined in this configuration class map directly to the
 * beans defined in {@code CoreContextConfigurationAppCtxTests-context.xml}.
 * Consequently, the application contexts loaded from these two sources
 * should be identical with regard to bean definitions.
 *
 * @author Sam Brannen
 * @since 3.1
 */
@Configuration(proxyBeanMethods = false)
public class PojoAndStringConfig {

	@Bean
	public Employee employee() {
		Employee employee = new Employee();
		employee.setName("John Smith");
		employee.setAge(42);
		employee.setCompany("Acme Widgets, Inc.");
		return employee;
	}

	@Bean
	public Pet pet() {
		return new Pet("Fido");
	}

	@Bean
	public String foo() {
		return "Foo";
	}

	@Bean
	public String bar() {
		return "Bar";
	}

	@Bean
	public String quux() {
		return "Quux";
	}

}

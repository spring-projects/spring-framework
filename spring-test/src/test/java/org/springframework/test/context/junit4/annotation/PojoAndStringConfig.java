/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit4.annotation;

import org.springframework.beans.Employee;
import org.springframework.beans.Pet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ApplicationContext configuration class for various integration tests.
 * 
 * <p>The beans defined in this configuration class map directly to the
 * beans defined in <code>SpringJUnit4ClassRunnerAppCtxTests-context.xml</code>.
 * Consequently, the application contexts loaded from these two sources
 * should be identical with regard to bean definitions.
 * 
 * @author Sam Brannen
 * @since 3.1
 */
@Configuration
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

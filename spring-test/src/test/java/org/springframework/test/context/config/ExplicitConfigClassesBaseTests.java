/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for configuration classes in
 * the Spring TestContext Framework.
 *
 * <p>Configuration will be loaded from {@link DefaultConfigClassesBaseTests.ContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
@SpringJUnitConfig(loader = AnnotationConfigContextLoader.class, classes = DefaultConfigClassesBaseTests.ContextConfiguration.class)
class ExplicitConfigClassesBaseTests {

	@Autowired
	Employee employee;


	@Test
	void verifyEmployeeSetFromBaseContextConfig() {
		assertThat(this.employee).as("The employee should have been autowired.").isNotNull();
		assertThat(this.employee.getName()).isEqualTo("John Smith");
	}

}

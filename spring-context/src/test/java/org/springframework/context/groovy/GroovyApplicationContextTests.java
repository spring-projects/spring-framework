/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.groovy;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.GenericGroovyApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Jeff Brown
 * @author Juergen Hoeller
 */
public class GroovyApplicationContextTests {

	@Test
	public void testLoadingConfigFile() {
		GenericGroovyApplicationContext ctx = new GenericGroovyApplicationContext(
				"org/springframework/context/groovy/applicationContext.groovy");

		Object framework = ctx.getBean("framework");
		assertThat(framework).as("could not find framework bean").isNotNull();
		assertThat(framework).isEqualTo("Grails");
	}

	@Test
	public void testLoadingMultipleConfigFiles() {
		GenericGroovyApplicationContext ctx = new GenericGroovyApplicationContext(
				"org/springframework/context/groovy/applicationContext2.groovy",
				"org/springframework/context/groovy/applicationContext.groovy");

		Object framework = ctx.getBean("framework");
		assertThat(framework).as("could not find framework bean").isNotNull();
		assertThat(framework).isEqualTo("Grails");

		Object company = ctx.getBean("company");
		assertThat(company).as("could not find company bean").isNotNull();
		assertThat(company).isEqualTo("SpringSource");
	}

	@Test
	public void testLoadingMultipleConfigFilesWithRelativeClass() {
		GenericGroovyApplicationContext ctx = new GenericGroovyApplicationContext();
		ctx.load(GroovyApplicationContextTests.class, "applicationContext2.groovy", "applicationContext.groovy");
		ctx.refresh();

		Object framework = ctx.getBean("framework");
		assertThat(framework).as("could not find framework bean").isNotNull();
		assertThat(framework).isEqualTo("Grails");

		Object company = ctx.getBean("company");
		assertThat(company).as("could not find company bean").isNotNull();
		assertThat(company).isEqualTo("SpringSource");
	}

	@Test
	public void testConfigFileParsingError() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class).isThrownBy(() ->
				new GenericGroovyApplicationContext("org/springframework/context/groovy/applicationContext-error.groovy"));
	}

}

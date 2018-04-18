/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.context.groovy;

import org.junit.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.GenericGroovyApplicationContext;

import static org.junit.Assert.*;

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
		assertNotNull("could not find framework bean", framework);
		assertEquals("Grails", framework);
	}

	@Test
	public void testLoadingMultipleConfigFiles() {
		GenericGroovyApplicationContext ctx = new GenericGroovyApplicationContext(
				"org/springframework/context/groovy/applicationContext2.groovy",
				"org/springframework/context/groovy/applicationContext.groovy");

		Object framework = ctx.getBean("framework");
		assertNotNull("could not find framework bean", framework);
		assertEquals("Grails", framework);

		Object company = ctx.getBean("company");
		assertNotNull("could not find company bean", company);
		assertEquals("SpringSource", company);
	}

	@Test
	public void testLoadingMultipleConfigFilesWithRelativeClass() {
		GenericGroovyApplicationContext ctx = new GenericGroovyApplicationContext();
		ctx.load(GroovyApplicationContextTests.class, "applicationContext2.groovy", "applicationContext.groovy");
		ctx.refresh();

		Object framework = ctx.getBean("framework");
		assertNotNull("could not find framework bean", framework);
		assertEquals("Grails", framework);

		Object company = ctx.getBean("company");
		assertNotNull("could not find company bean", company);
		assertEquals("SpringSource", company);
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void testConfigFileParsingError() {
		new GenericGroovyApplicationContext("org/springframework/context/groovy/applicationContext-error.groovy");
	}

}

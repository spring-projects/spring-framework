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

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.GenericGroovyApplicationContext;

import static groovy.test.GroovyAssert.assertEquals;
import static groovy.test.GroovyAssert.assertNotNull;
import static groovy.test.GroovyAssert.assertThrows;

/**
 * @author Jeff Brown
 * @author Sam Brannen
 * @author Dave Syer
 */
class GroovyApplicationContextDynamicBeanPropertyTests {

	@Test
	void testAccessDynamicBeanProperties() {
		var ctx = new GenericGroovyApplicationContext();
		ctx.getReader().loadBeanDefinitions("org/springframework/context/groovy/applicationContext.groovy");
		ctx.refresh();

		var framework = ctx.getProperty("framework");
		assertNotNull("could not find framework bean", framework);
		assertEquals("Grails", framework);
		ctx.close();
	}

	@Test
	void testAccessingNonExistentBeanViaDynamicProperty() {
		var ctx = new GenericGroovyApplicationContext();
		ctx.getReader().loadBeanDefinitions("org/springframework/context/groovy/applicationContext.groovy");
		ctx.refresh();

		var err = assertThrows(NoSuchBeanDefinitionException.class, () -> ctx.getProperty("someNonExistentBean"));

		assertEquals("No bean named 'someNonExistentBean' available", err.getMessage());
		ctx.close();
	}

}

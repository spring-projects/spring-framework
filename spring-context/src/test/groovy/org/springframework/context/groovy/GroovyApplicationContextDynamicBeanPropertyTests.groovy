/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.context.groovy

import org.junit.Test

import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.support.GenericGroovyApplicationContext

import static groovy.test.GroovyAssert.*

/**
 * @author Jeff Brown
 * @author Sam Brannen
 */
class GroovyApplicationContextDynamicBeanPropertyTests {

	@Test
	void testAccessDynamicBeanProperties() {
		def ctx = new GenericGroovyApplicationContext();
		ctx.reader.loadBeanDefinitions("org/springframework/context/groovy/applicationContext.groovy");
		ctx.refresh()

		def framework = ctx.framework
		assertNotNull 'could not find framework bean', framework
		assertEquals 'Grails', framework
	}

	@Test
	void testAccessingNonExistentBeanViaDynamicProperty() {
		def ctx = new GenericGroovyApplicationContext();
		ctx.reader.loadBeanDefinitions("org/springframework/context/groovy/applicationContext.groovy");
		ctx.refresh()

		def err = shouldFail NoSuchBeanDefinitionException, { ctx.someNonExistentBean }

		assertEquals "No bean named 'someNonExistentBean' is defined", err.message
	}
}

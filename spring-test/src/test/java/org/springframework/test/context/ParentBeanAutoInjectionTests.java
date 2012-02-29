/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.context;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.Assert.*;
import static org.springframework.test.context.ParentBeanAutoInjectionTests.*;

/**
 * Verify the bean injection from parent context.
 *
 * @author Tadaya Tsuyukubo
 * @since 3.2
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ParentContextConfiguration(classes = {ParentConfig.class})
@ContextConfiguration(classes = {ChildConfig.class})
public class ParentBeanAutoInjectionTests {

	@Autowired
	@Qualifier("foo")
	public String foo;

	@Autowired
	@Qualifier("bar")
	public String bar;

	/** verify a bean defined in parent app context gets injected into the test class. */
	@Test
	public void testParentDefinedBeanInjection() {

		assertNotNull("bean foo(defined in parent context) must be injected", foo);
		assertEquals("FOO", foo);

		assertNotNull("bean bar(defined in child context) must be injected", bar);
		assertEquals("BAR", bar);
	}

	@Configuration
	public static class ParentConfig {

		@Bean
		public String foo() {
			return "FOO";
		}
	}

	@Configuration
	public static class ChildConfig {

		@Bean
		public String bar() {
			return "BAR";
		}

	}

}

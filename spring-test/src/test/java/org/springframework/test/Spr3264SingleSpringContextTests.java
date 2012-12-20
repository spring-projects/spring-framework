/*
 * Copyright 2007-2011 the original author or authors.
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

package org.springframework.test;

/**
 * JUnit 3.8 based unit test which verifies new functionality requested in <a
 * href="http://opensource.atlassian.com/projects/spring/browse/SPR-3264"
 * target="_blank">SPR-3264</a>.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see Spr3264DependencyInjectionSpringContextTests
 */
@SuppressWarnings("deprecation")
public class Spr3264SingleSpringContextTests extends AbstractSingleSpringContextTests {

	public Spr3264SingleSpringContextTests() {
		super();
	}

	public Spr3264SingleSpringContextTests(String name) {
		super(name);
	}

	/**
	 * <p>
	 * Test which addresses the following issue raised in SPR-3264:
	 * </p>
	 * <p>
	 * AbstractSingleSpringContextTests always expects an application context to
	 * be created even if no files/locations are specified which can lead to NPE
	 * problems or force an appCtx to be instantiated even if not needed.
	 * </p>
	 */
	public void testApplicationContextNotAutoCreated() {
		assertNull("The ApplicationContext should NOT be automatically created if no 'locations' are defined.",
			super.applicationContext);
		assertEquals("Verifying the ApplicationContext load count.", 0, super.getLoadCount());
	}

}

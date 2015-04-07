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

package org.springframework.tests;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link JavaVersion}.
 *
 * @author Phillip Webb
 */
public class JavaVersionTests {

	@Test
	public void runningVersion() {
		assertNotNull(JavaVersion.runningVersion());
		assertThat(System.getProperty("java.version"), startsWith(JavaVersion.runningVersion().toString()));
	}

	@Test
	public void isAtLeast() throws Exception {
		assertTrue(JavaVersion.JAVA_16.isAtLeast(JavaVersion.JAVA_16));
		assertFalse(JavaVersion.JAVA_16.isAtLeast(JavaVersion.JAVA_17));
	}

}

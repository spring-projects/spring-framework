/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.core.io;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.internal.matchers.StringContains.containsString;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

/**
 * Unit tests cornering bug SPR-6888.
 *
 * @author Chris Beams
 */
public class ClassPathResourceTests {
	private static final String PACKAGE_PATH = "org/springframework/core/io";
	private static final String RESOURCE_NAME = "notexist.xml";
	private static final String FQ_RESOURCE_PATH = PACKAGE_PATH + '/' + RESOURCE_NAME;

	@Test
	public void stringConstructorRaisesExceptionWithFullyQualifiedPath() {
		assertExceptionContainsFullyQualifiedPath(new ClassPathResource(FQ_RESOURCE_PATH));
	}

	@Test
	public void classLiteralConstructorRaisesExceptionWithFullyQualifiedPath() {
		assertExceptionContainsFullyQualifiedPath(new ClassPathResource(RESOURCE_NAME, this.getClass()));
	}

	@Test
	public void classLoaderConstructorRaisesExceptionWithFullyQualifiedPath() {
		assertExceptionContainsFullyQualifiedPath(new ClassPathResource(FQ_RESOURCE_PATH, this.getClass().getClassLoader()));
	}

	private void assertExceptionContainsFullyQualifiedPath(ClassPathResource resource) {
		try {
			resource.getInputStream();
			fail("FileNotFoundException expected for resource: " + resource);
		} catch (IOException ex) {
			assertThat(ex, instanceOf(FileNotFoundException.class));
			assertThat(ex.getMessage(), containsString(FQ_RESOURCE_PATH));
		}
	}
}

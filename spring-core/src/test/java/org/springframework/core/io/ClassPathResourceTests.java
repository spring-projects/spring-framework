/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core.io;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link ClassPathResource}.
 *
 * <p>These also originally served as regression tests for the bugs described in
 * SPR-6888 and SPR-9413.
 *
 * @author Chris Beams
 * @author Sam Brannen
 */
class ClassPathResourceTests {

	private static final String PACKAGE_PATH = "org/springframework/core/io";
	private static final String NONEXISTENT_RESOURCE_NAME = "nonexistent.xml";
	private static final String ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE = PACKAGE_PATH + '/' + NONEXISTENT_RESOURCE_NAME;
	private static final String ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE_WITH_LEADING_SLASH = '/' + ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE;


	@Nested
	class GetInputStream {

		@Test
		void withStringConstructorRaisesExceptionForNonexistentResource() {
			assertExceptionContainsAbsolutePath(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE));
		}

		@Test
		void withClassLoaderConstructorRaisesExceptionForNonexistentResource() {
			assertExceptionContainsAbsolutePath(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE, getClass().getClassLoader()));
		}

		@Test
		void withClassLiteralConstructorRaisesExceptionForNonexistentRelativeResource() {
			assertExceptionContainsAbsolutePath(new ClassPathResource(NONEXISTENT_RESOURCE_NAME, getClass()));
		}

		@Test
		void withClassLiteralConstructorRaisesExceptionForNonexistentAbsoluteResource() {
			assertExceptionContainsAbsolutePath(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE, getClass()));
		}

		private static void assertExceptionContainsAbsolutePath(ClassPathResource resource) {
			assertThatExceptionOfType(FileNotFoundException.class)
				.isThrownBy(resource::getInputStream)
				.withMessageContaining(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE);
		}

	}

	@Nested
	class GetDescription {

		@Test
		void withStringConstructor() {
			assertDescription(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE));
		}

		@Test
		void withStringConstructorAndLeadingSlash() {
			assertDescription(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE_WITH_LEADING_SLASH));
		}

		@Test
		void withClassLiteralConstructor() {
			assertDescription(new ClassPathResource(NONEXISTENT_RESOURCE_NAME, getClass()));
		}

		@Test
		void withClassLiteralConstructorAndLeadingSlash() {
			assertDescription(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE_WITH_LEADING_SLASH, getClass()));
		}

		@Test
		void withClassLoaderConstructor() {
			assertDescription(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE, getClass().getClassLoader()));
		}

		@Test
		void withClassLoaderConstructorAndLeadingSlash() {
			assertDescription(new ClassPathResource(ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE_WITH_LEADING_SLASH, getClass().getClassLoader()));
		}

		private static void assertDescription(ClassPathResource resource) {
			assertThat(resource.getDescription()).isEqualTo("class path resource [%s]", ABSOLUTE_PATH_TO_NONEXISTENT_RESOURCE);
		}

	}

	@Nested
	class GetPath {

		@Test
		void dropsLeadingSlashForClassLoaderAccess() {
			assertThat(new ClassPathResource("/test.html").getPath()).isEqualTo("test.html");
			assertThat(((ClassPathResource) new ClassPathResource("").createRelative("/test.html")).getPath()).isEqualTo("test.html");
		}

		@Test
		void convertsToAbsolutePathForClassRelativeAccess() {
			assertThat(new ClassPathResource("/test.html", getClass()).getPath()).isEqualTo("test.html");
			assertThat(new ClassPathResource("", getClass()).getPath()).isEqualTo(PACKAGE_PATH + "/");
			assertThat(((ClassPathResource) new ClassPathResource("", getClass()).createRelative("/test.html")).getPath()).isEqualTo("test.html");
			assertThat(((ClassPathResource) new ClassPathResource("", getClass()).createRelative("test.html")).getPath()).isEqualTo(PACKAGE_PATH + "/test.html");
		}

	}

	@Test
	void directoryNotReadable() throws Exception {
		Resource fileDir = new ClassPathResource("org/springframework/core");
		assertThat(fileDir.getURL()).asString().startsWith("file:");
		assertThat(fileDir.exists()).isTrue();
		assertThat(fileDir.isReadable()).isFalse();

		Resource jarDir = new ClassPathResource("reactor/core");
		assertThat(jarDir.getURL()).asString().startsWith("jar:");
		assertThat(jarDir.exists()).isTrue();
		assertThat(jarDir.isReadable()).isFalse();
	}

}

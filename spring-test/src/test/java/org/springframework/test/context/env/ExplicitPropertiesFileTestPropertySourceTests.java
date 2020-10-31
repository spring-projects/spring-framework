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

package org.springframework.test.context.env;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ClassUtils;

/**
 * Collection of integration tests that verify proper support for
 * {@link TestPropertySource @TestPropertySource} with explicit properties
 * files declared with absolute paths, relative paths, and placeholders in the
 * classpath and in the file system.
 *
 * @author Sam Brannen
 * @since 5.2
 */
@DisplayName("Explicit properties file in @TestPropertySource")
class ExplicitPropertiesFileTestPropertySourceTests {

	static final String CURRENT_TEST_PACKAGE = "current.test.package";


	@BeforeAll
	static void setSystemProperty() {
		String path = ClassUtils.classPackageAsResourcePath(ExplicitPropertiesFileTestPropertySourceTests.class);
		System.setProperty(CURRENT_TEST_PACKAGE, path);
	}

	@AfterAll
	static void clearSystemProperty() {
		System.clearProperty(CURRENT_TEST_PACKAGE);
	}


	@Nested
	@DisplayName("in classpath")
	class ClasspathTests {

		@Nested
		@DisplayName("with absolute path")
		@TestPropertySource("/org/springframework/test/context/env/explicit.properties")
		class AbsolutePathPathTests extends AbstractExplicitPropertiesFileTests {
		}

		@Nested
		@DisplayName("with absolute path with internal relative paths")
		@TestPropertySource("/org/../org/springframework/test/../test/context/env/explicit.properties")
		class AbsolutePathWithInternalRelativePathTests extends AbstractExplicitPropertiesFileTests {
		}

		@Nested
		@DisplayName("with relative path")
		@TestPropertySource("explicit.properties")
		class RelativePathTests extends AbstractExplicitPropertiesFileTests {
		}

		@Nested
		@DisplayName("with relative paths")
		@TestPropertySource("../env/../env/../../context/env/explicit.properties")
		class RelativePathsTests extends AbstractExplicitPropertiesFileTests {
		}

		@Nested
		@DisplayName("with placeholder")
		@TestPropertySource("/${current.test.package}/explicit.properties")
		class PlaceholderTests extends AbstractExplicitPropertiesFileTests {
		}

		@Nested
		@DisplayName("with placeholder and classpath: prefix")
		@TestPropertySource("classpath:${current.test.package}/explicit.properties")
		class PlaceholderAndClasspathPrefixTests extends AbstractExplicitPropertiesFileTests {
		}

	}

	@Nested
	@DisplayName("in file system")
	class FileSystemTests {

		@Nested
		@DisplayName("with full local path")
		@TestPropertySource("file:src/test/resources/org/springframework/test/context/env/explicit.properties")
		class FullLocalPathTests extends AbstractExplicitPropertiesFileTests {
		}

		@Nested
		@DisplayName("with dot-path reference")
		@TestPropertySource("file:./src/test/resources/org/springframework/test/context/env/explicit.properties")
		class DotPathTests extends AbstractExplicitPropertiesFileTests {
		}

		@Nested
		@DisplayName("with relative path")
		@TestPropertySource("file:../spring-test/src/test/resources/org/springframework/test/context/env/explicit.properties")
		class RelativePathTests extends AbstractExplicitPropertiesFileTests {
		}

		@Nested
		@DisplayName("with ${current.test.package} placeholder")
		@TestPropertySource("file:src/test/resources/${current.test.package}/explicit.properties")
		class CustomPlaceholderTests extends AbstractExplicitPropertiesFileTests {
		}

		@Nested
		@DisplayName("with ${user.dir} placeholder")
		@TestPropertySource("file:${user.dir}/src/test/resources/org/springframework/test/context/env/explicit.properties")
		class UserDirPlaceholderTests extends AbstractExplicitPropertiesFileTests {
		}

		@Nested
		@DisplayName("with ${user.dir} and ${current.test.package} placeholders")
		@TestPropertySource("file:${user.dir}/src/test/resources/${current.test.package}/explicit.properties")
		class UserDirAndCustomPlaceholdersTests extends AbstractExplicitPropertiesFileTests {
		}

		@Nested
		@DisplayName("with placeholders followed immediately by relative paths")
		@TestPropertySource("file:${user.dir}/../spring-test/src/test/resources/${current.test.package}/../env/explicit.properties")
		class PlaceholdersFollowedByRelativePathsTests extends AbstractExplicitPropertiesFileTests {
		}

	}

}

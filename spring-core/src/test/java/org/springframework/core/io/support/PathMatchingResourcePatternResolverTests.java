/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.core.io.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link PathMatchingResourcePatternResolver}.
 *
 * <p>If tests fail, uncomment the diagnostics in {@link #assertFilenames}.
 *
 * @author Oliver Hutchison
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 17.11.2004
 */
class PathMatchingResourcePatternResolverTests {

	private static final String[] CLASSES_IN_CORE_IO_SUPPORT = { "EncodedResource.class",
		"LocalizedResourceHelper.class", "PathMatchingResourcePatternResolver.class", "PropertiesLoaderSupport.class",
		"PropertiesLoaderUtils.class", "ResourceArrayPropertyEditor.class", "ResourcePatternResolver.class",
		"ResourcePatternUtils.class", "SpringFactoriesLoader.class" };

	private static final String[] TEST_CLASSES_IN_CORE_IO_SUPPORT = { "PathMatchingResourcePatternResolverTests.class" };

	private static final String[] CLASSES_IN_REACTOR_UTIL_ANNOTATION = { "NonNull.class", "NonNullApi.class", "Nullable.class" };


	private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();


	@Nested
	class InvalidPatterns {

		@Test
		void invalidPrefixWithPatternElementInItThrowsException() throws IOException {
			assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() -> resolver.getResources("xx**:**/*.xy"));
		}

	}


	@Nested
	class FileSystemResources {

		@Test
		void singleResourceOnFileSystem() throws IOException {
			String pattern = "org/springframework/core/io/support/PathMatchingResourcePatternResolverTests.class";
			assertFilenames(pattern, "PathMatchingResourcePatternResolverTests.class");
		}

		@Test
		void classpathStarWithPatternOnFileSystem() throws IOException {
			String pattern = "classpath*:org/springframework/core/io/sup*/*.class";
			String[] expectedFilenames = StringUtils.concatenateStringArrays(CLASSES_IN_CORE_IO_SUPPORT, TEST_CLASSES_IN_CORE_IO_SUPPORT);
			assertFilenames(pattern, expectedFilenames);
		}

		@Test
		void getResourcesOnFileSystemContainingHashtagsInTheirFileNames() throws IOException {
			String pattern = "classpath*:org/springframework/core/io/**/resource#test*.txt";
			assertFilenames(pattern, "resource#test1.txt", "resource#test2.txt");
		}

	}


	@Nested
	class JarResources {

		@Test
		void singleResourceInJar() throws IOException {
			String pattern = "org/reactivestreams/Publisher.class";
			assertFilenames(pattern, "Publisher.class");
		}

		@Test
		void singleResourceInRootOfJar() throws IOException {
			String pattern = "aspectj_1_5_0.dtd";
			assertFilenames(pattern, "aspectj_1_5_0.dtd");
		}

		@Test
		void classpathWithPatternInJar() throws IOException {
			String pattern = "classpath:reactor/util/annotation/*.class";
			assertFilenames(pattern, CLASSES_IN_REACTOR_UTIL_ANNOTATION);
		}

		@Test
		void classpathStarWithPatternInJar() throws IOException {
			String pattern = "classpath*:reactor/util/annotation/*.class";
			assertFilenames(pattern, CLASSES_IN_REACTOR_UTIL_ANNOTATION);
		}

		// Fails in a native image -- https://github.com/oracle/graal/issues/5020
		@Test
		void rootPatternRetrievalInJarFiles() throws IOException {
			assertThat(resolver.getResources("classpath*:*.dtd")).extracting(Resource::getFilename)
				.as("Could not find aspectj_1_5_0.dtd in the root of the aspectjweaver jar")
				.contains("aspectj_1_5_0.dtd");
		}

	}


	private void assertFilenames(String pattern, String... filenames) throws IOException {
		Resource[] resources = resolver.getResources(pattern);
		List<String> actualNames = Arrays.stream(resources)
				.map(Resource::getFilename)
				// Need to decode within GraalVM native image to get %23 converted to #.
				.map(filename -> URLDecoder.decode(filename, UTF_8))
				.sorted()
				.toList();

		// Uncomment the following if you encounter problems with matching against the file system.
		// List<String> expectedNames = Arrays.stream(filenames).sorted().toList();
		// System.out.println("----------------------------------------------------------------------");
		// System.out.println("Expected: " + expectedNames);
		// System.out.println("Actual: " + actualNames);
		// Arrays.stream(resources).forEach(System.out::println);

		assertThat(actualNames).as("subset of files found").contains(filenames);
	}

}

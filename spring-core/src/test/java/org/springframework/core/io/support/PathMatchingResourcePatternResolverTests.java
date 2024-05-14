/*
 * Copyright 2002-2024 the original author or authors.
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
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link PathMatchingResourcePatternResolver}.
 *
 * <p>If tests fail, uncomment the diagnostics in {@link #assertFilenames(String, boolean, String...)}.
 *
 * @author Oliver Hutchison
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 17.11.2004
 */
class PathMatchingResourcePatternResolverTests {

	private static final String[] CLASSES_IN_CORE_IO_SUPPORT = {"EncodedResource.class",
			"LocalizedResourceHelper.class", "PathMatchingResourcePatternResolver.class", "PropertiesLoaderSupport.class",
			"PropertiesLoaderUtils.class", "ResourceArrayPropertyEditor.class", "ResourcePatternResolver.class",
			"ResourcePatternUtils.class", "SpringFactoriesLoader.class"};

	private static final String[] TEST_CLASSES_IN_CORE_IO_SUPPORT = {"PathMatchingResourcePatternResolverTests.class"};

	private static final String[] CLASSES_IN_REACTOR_UTIL_ANNOTATION =
			{"Incubating.class", "NonNull.class", "NonNullApi.class", "Nullable.class"};


	private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();


	@Nested
	class InvalidPatterns {

		@Test
		void invalidPrefixWithPatternElementInItThrowsException() {
			assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() -> resolver.getResources("xx**:**/*.xy"));
		}
	}


	@Nested
	class FileSystemResources {

		@Test
		void singleResourceOnFileSystem() {
			String pattern = "org/springframework/core/io/support/PathMatchingResourcePatternResolverTests.class";
			assertExactFilenames(pattern, "PathMatchingResourcePatternResolverTests.class");
		}

		@Test
		void classpathStarWithPatternOnFileSystem() {
			String pattern = "classpath*:org/springframework/core/io/sup*/*.class";
			String[] expectedFilenames = StringUtils.concatenateStringArrays(CLASSES_IN_CORE_IO_SUPPORT, TEST_CLASSES_IN_CORE_IO_SUPPORT);
			assertFilenames(pattern, expectedFilenames);
		}

		@Test  // gh-31111
		void usingFileProtocolWithWildcardInPatternAndNonexistentRootPath() throws IOException {
			Path testResourcesDir = Paths.get("src/test/resources").toAbsolutePath();
			String pattern = String.format("file:%s/example/bogus/**", testResourcesDir);
			assertThat(resolver.getResources(pattern)).isEmpty();
			// When the log level for the resolver is set to at least INFO, we should see
			// a log entry similar to the following.
			//
			// [main] INFO  o.s.c.i.s.PathMatchingResourcePatternResolver -
			// Skipping search for files matching pattern [**]: directory
			// [/<...>/spring-core/src/test/resources/example/bogus] does not exist
		}

		@Test
		void encodedHashtagInPath() throws IOException {
			Path rootDir = Paths.get("src/test/resources/custom%23root").toAbsolutePath();
			URL root = new URL("file:" + rootDir + "/");
			resolver = new PathMatchingResourcePatternResolver(new DefaultResourceLoader(new URLClassLoader(new URL[] {root})));
			assertExactFilenames("classpath*:scanned/*.txt", "resource#test1.txt", "resource#test2.txt");
		}

		@Nested
		class WithHashtagsInTheirFilenames {

			@Test
			void usingClasspathStarProtocol() {
				String pattern = "classpath*:org/springframework/core/io/**/resource#test*.txt";
				String pathPrefix = ".+org/springframework/core/io/";

				assertExactFilenames(pattern, "resource#test1.txt", "resource#test2.txt");
				assertExactSubPaths(pattern, pathPrefix, "support/resource#test1.txt", "support/resource#test2.txt");
			}

			@Test
			void usingClasspathStarProtocolWithWildcardInPatternAndNotEndingInSlash() throws Exception {
				String pattern = "classpath*:org/springframework/core/io/sup*";
				String pathPrefix = ".+org/springframework/core/io/";

				List<String> actualSubPaths = getSubPathsIgnoringClassFilesEtc(pattern, pathPrefix);

				// We DO find "support" if the pattern does NOT end with a slash.
				assertThat(actualSubPaths).containsExactly("support");
			}

			@Test
			void usingFileProtocolWithWildcardInPatternAndNotEndingInSlash() throws Exception {
				Path testResourcesDir = Paths.get("src/test/resources").toAbsolutePath();
				String pattern = String.format("file:%s/org/springframework/core/io/sup*", testResourcesDir);
				String pathPrefix = ".+org/springframework/core/io/";

				List<String> actualSubPaths = getSubPathsIgnoringClassFilesEtc(pattern, pathPrefix);

				// We DO find "support" if the pattern does NOT end with a slash.
				assertThat(actualSubPaths).containsExactly("support");
			}

			@Test
			void usingClasspathStarProtocolWithWildcardInPatternAndEndingInSlash() throws Exception {
				String pattern = "classpath*:org/springframework/core/io/sup*/";
				String pathPrefix = ".+org/springframework/core/io/";

				List<String> actualSubPaths = getSubPathsIgnoringClassFilesEtc(pattern, pathPrefix);

				URL url = getClass().getClassLoader().getResource("org/springframework/core/io/support/EncodedResource.class");
				if (!url.getProtocol().equals("jar")) {
					// We do NOT find "support" if the pattern ENDS with a slash if org/springframework/core/io/support
					// is in the local file system.
					assertThat(actualSubPaths).isEmpty();
				}
				else {
					// But we do find "support/" if org/springframework/core/io/support is found in a JAR on the classpath.
					assertThat(actualSubPaths).containsExactly("support/");
				}
			}

			@Test
			void usingFileProtocolWithWildcardInPatternAndEndingInSlash() throws Exception {
				Path testResourcesDir = Paths.get("src/test/resources").toAbsolutePath();
				String pattern = String.format("file:%s/org/springframework/core/io/sup*/", testResourcesDir);
				String pathPrefix = ".+org/springframework/core/io/";

				List<String> actualSubPaths = getSubPathsIgnoringClassFilesEtc(pattern, pathPrefix);

				// We do NOT find "support" if the pattern ENDS with a slash.
				assertThat(actualSubPaths).isEmpty();
			}

			@Test
			void usingClasspathStarProtocolWithWildcardInPatternAndEndingWithSuffixPattern() throws Exception {
				String pattern = "classpath*:org/springframework/core/io/sup*/*.txt";
				String pathPrefix = ".+org/springframework/core/io/";

				List<String> actualSubPaths = getSubPathsIgnoringClassFilesEtc(pattern, pathPrefix);

				assertThat(actualSubPaths)
						.containsExactlyInAnyOrder("support/resource#test1.txt", "support/resource#test2.txt");
			}

			private List<String> getSubPathsIgnoringClassFilesEtc(String pattern, String pathPrefix) throws IOException {
				return Arrays.stream(resolver.getResources(pattern))
						.map(resource -> getPath(resource).replaceFirst(pathPrefix, ""))
						.filter(name -> !name.endsWith(".class"))
						.filter(name -> !name.endsWith(".kt"))
						.filter(name -> !name.endsWith(".factories"))
						.distinct()
						.sorted()
						.collect(Collectors.toList());
			}

			@Test
			void usingFileProtocolWithoutWildcardInPatternAndEndingInSlashStarStar() {
				Path testResourcesDir = Paths.get("src/test/resources").toAbsolutePath();
				String pattern = String.format("file:%s/scanned-resources/**", testResourcesDir);
				String pathPrefix = ".+?resources/";

				// We do NOT find "scanned-resources" if the pattern ENDS with "/**" AND does NOT otherwise contain a wildcard.
				assertExactFilenames(pattern, "resource#test1.txt", "resource#test2.txt");
				assertExactSubPaths(pattern, pathPrefix, "scanned-resources/resource#test1.txt",
						"scanned-resources/resource#test2.txt");
			}

			@Test
			void usingFileProtocolWithWildcardInPatternAndEndingInSlashStarStar() {
				Path testResourcesDir = Paths.get("src/test/resources").toAbsolutePath();
				String pattern = String.format("file:%s/scanned*resources/**", testResourcesDir);
				String pathPrefix = ".+?resources/";

				// We DO find "scanned-resources" if the pattern ENDS with "/**" AND DOES otherwise contain a wildcard.
				assertExactFilenames(pattern, "scanned-resources", "resource#test1.txt", "resource#test2.txt");
				assertExactSubPaths(pattern, pathPrefix, "scanned-resources", "scanned-resources/resource#test1.txt",
						"scanned-resources/resource#test2.txt");
			}

			@Test
			void usingFileProtocolAndAssertingUrlAndUriSyntax() throws Exception {
				Path testResourcesDir = Paths.get("src/test/resources").toAbsolutePath();
				String pattern = "file:%s/scanned-resources/**/resource#test1.txt".formatted(testResourcesDir);
				Resource[] resources = resolver.getResources(pattern);
				assertThat(resources).hasSize(1);
				Resource resource = resources[0];
				assertThat(resource.getFilename()).isEqualTo("resource#test1.txt");
				// The following assertions serve as regression tests for the lack of the
				// "authority component" (//) in the returned URI/URL. For example, we are
				// expecting file:/my/path (or file:/C:/My/Path) instead of file:///my/path.
				assertThat(resource.getURL().toString()).matches("^file:\\/[^\\/].+test1\\.txt$");
				assertThat(resource.getURI().toString()).matches("^file:\\/[^\\/].+test1\\.txt$");
			}
		}
	}


	@Nested
	class JarResources {

		@Test
		void singleResourceInJar() {
			String pattern = "org/reactivestreams/Publisher.class";
			assertExactFilenames(pattern, "Publisher.class");
		}

		@Test
		void singleResourceInRootOfJar() {
			String pattern = "aspectj_1_5_0.dtd";
			assertExactFilenames(pattern, "aspectj_1_5_0.dtd");
		}

		@Test
		void classpathWithPatternInJar() {
			String pattern = "classpath:reactor/util/annotation/*.class";
			assertExactFilenames(pattern, CLASSES_IN_REACTOR_UTIL_ANNOTATION);
		}

		@Test
		void classpathStarWithPatternInJar() {
			String pattern = "classpath*:reactor/util/annotation/*.class";
			assertExactFilenames(pattern, CLASSES_IN_REACTOR_UTIL_ANNOTATION);
		}

		// Fails in a native image -- https://github.com/oracle/graal/issues/5020
		@Test
		void rootPatternRetrievalInJarFiles() throws IOException {
			assertThat(resolver.getResources("classpath*:aspectj*.dtd")).extracting(Resource::getFilename)
				.as("Could not find aspectj_1_5_0.dtd in the root of the aspectjweaver jar")
				.containsExactly("aspectj_1_5_0.dtd");
		}
	}


	private void assertFilenames(String pattern, String... filenames) {
		assertFilenames(pattern, false, filenames);
	}

	private void assertExactFilenames(String pattern, String... filenames) {
		assertFilenames(pattern, true, filenames);
	}

	private void assertFilenames(String pattern, boolean exactly, String... filenames) {
		try {
			Resource[] resources = resolver.getResources(pattern);
			List<String> actualNames = Arrays.stream(resources)
					.peek(resource -> assertThat(resource.exists()).as(resource + " exists").isTrue())
					.map(Resource::getFilename)
					.sorted()
					.toList();

			// Uncomment the following if you encounter problems with matching against the file system.
			// List<String> expectedNames = Arrays.stream(filenames).sorted().toList();
			// System.out.println("----------------------------------------------------------------------");
			// System.out.println("Expected: " + expectedNames);
			// System.out.println("Actual: " + actualNames);
			// Arrays.stream(resources).forEach(System.out::println);

			if (exactly) {
				assertThat(actualNames).as("subset of files found").containsExactlyInAnyOrder(filenames);
			}
			else {
				assertThat(actualNames).as("subset of files found").contains(filenames);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private void assertExactSubPaths(String pattern, String pathPrefix, String... subPaths) {
		try {
			Resource[] resources = resolver.getResources(pattern);
			List<String> actualSubPaths = Arrays.stream(resources)
					.map(resource -> getPath(resource).replaceFirst(pathPrefix, ""))
					.sorted()
					.toList();
			assertThat(actualSubPaths).containsExactlyInAnyOrder(subPaths);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private String getPath(Resource resource) {
		// Tests fail if we use resource.getURL().getPath(). They would also fail on macOS when
		// using resource.getURI().getPath() if the resource paths are not Unicode normalized.
		//
		// On the JVM, all tests should pass when using resource.getFile().getPath(); however,
		// we use FileSystemResource#getPath since this test class is sometimes run within a
		// GraalVM native image which cannot support Path#toFile.
		//
		// See: https://github.com/spring-projects/spring-framework/issues/29243
		if (resource instanceof FileSystemResource fileSystemResource) {
			return fileSystemResource.getPath();
		}
		try {
			// Fall back to URL in case the resource came from a JAR
			return resource.getURL().getPath();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}

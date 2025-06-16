/*
 * Copyright 2002-2025 the original author or authors.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.ClassUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;
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
			resolver.setUseCaches(false);
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


	@Nested
	class ClassPathManifestEntries {

		@TempDir
		Path temp;

		@BeforeAll
		static void suppressJarCaches() {
			URLConnection.setDefaultUseCaches("jar", false);
		}

		@AfterAll
		static void restoreJarCaches() {
			URLConnection.setDefaultUseCaches("jar", true);
		}

		@Test
		void javaDashJarFindsClassPathManifestEntries() throws Exception {
			Path lib = this.temp.resolve("lib");
			Files.createDirectories(lib);
			writeAssetJar(lib.resolve("asset.jar"));
			writeApplicationJar(this.temp.resolve("app.jar"));
			String java = ProcessHandle.current().info().command().get();
			Process process = new ProcessBuilder(java, "-jar", "app.jar")
					.directory(this.temp.toFile())
					.start();
			assertThat(process.waitFor()).isZero();
			String result = StreamUtils.copyToString(process.getInputStream(), StandardCharsets.UTF_8);
			assertThat(result.replace("\\", "/")).contains("!!!!").contains("/lib/asset.jar!/assets/file.txt");
		}

		private void writeAssetJar(Path path) throws Exception {
			try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(path.toFile()))) {
				jar.putNextEntry(new ZipEntry("assets/"));
				jar.closeEntry();
				jar.putNextEntry(new ZipEntry("assets/file.txt"));
				StreamUtils.copy("test", StandardCharsets.UTF_8, jar);
				jar.closeEntry();
			}

			assertThat(new FileSystemResource(path).exists()).isTrue();
			assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + path + ResourceUtils.JAR_URL_SEPARATOR).exists()).isTrue();
			assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + path + ResourceUtils.JAR_URL_SEPARATOR + "assets/file.txt").exists()).isTrue();
			assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + path + ResourceUtils.JAR_URL_SEPARATOR + "assets/none.txt").exists()).isFalse();
			assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + "X" + path + ResourceUtils.JAR_URL_SEPARATOR).exists()).isFalse();
			assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + "X" + path + ResourceUtils.JAR_URL_SEPARATOR + "assets/file.txt").exists()).isFalse();
			assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + "X" + path + ResourceUtils.JAR_URL_SEPARATOR + "assets/none.txt").exists()).isFalse();

			Resource resource = new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + path + ResourceUtils.JAR_URL_SEPARATOR + "assets/file.txt");
			try (InputStream is = resource.getInputStream()) {
				assertThat(resource.exists()).isTrue();
				assertThat(resource.createRelative("file.txt").exists()).isTrue();
				assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + path + ResourceUtils.JAR_URL_SEPARATOR).exists()).isTrue();
				is.readAllBytes();
			}
		}

		private void writeApplicationJar(Path path) throws Exception {
			Manifest manifest = new Manifest();
			Attributes mainAttributes = manifest.getMainAttributes();
			mainAttributes.put(Name.CLASS_PATH, buildSpringClassPath() + "lib/asset.jar");
			mainAttributes.put(Name.MAIN_CLASS, ClassPathManifestEntriesTestApplication.class.getName());
			mainAttributes.put(Name.MANIFEST_VERSION, "1.0");
			try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(path.toFile()), manifest)) {
				String appClassResource = ClassUtils.convertClassNameToResourcePath(
						ClassPathManifestEntriesTestApplication.class.getName()) + ClassUtils.CLASS_FILE_SUFFIX;
				String folder = "";
				for (String name : appClassResource.split("/")) {
					if (!name.endsWith(ClassUtils.CLASS_FILE_SUFFIX)) {
						folder += name + "/";
						jar.putNextEntry(new ZipEntry(folder));
						jar.closeEntry();
					}
					else {
						jar.putNextEntry(new ZipEntry(folder + name));
						try (InputStream in = getClass().getResourceAsStream(name)) {
							in.transferTo(jar);
						}
						jar.closeEntry();
					}
				}
			}
			assertThat(new FileSystemResource(path).exists()).isTrue();
			assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + path + ResourceUtils.JAR_URL_SEPARATOR).exists()).isTrue();
		}

		private String buildSpringClassPath() throws Exception {
			return copyClasses(PathMatchingResourcePatternResolver.class, "spring-core") +
					copyClasses(LogFactory.class, "commons-logging");
		}

		private String copyClasses(Class<?> sourceClass, String destinationName) throws URISyntaxException, IOException {
			Path destination = this.temp.resolve(destinationName);
			String resourcePath = ClassUtils.convertClassNameToResourcePath(
					sourceClass.getName()) + ClassUtils.CLASS_FILE_SUFFIX;
			URL resource = getClass().getClassLoader().getResource(resourcePath);
			URL url = new URL(resource.toString().replace(resourcePath, ""));
			URLConnection connection = url.openConnection();
			if (connection instanceof JarURLConnection jarUrlConnection) {
				try (JarFile jarFile = jarUrlConnection.getJarFile()) {
					Enumeration<JarEntry> entries = jarFile.entries();
					while (entries.hasMoreElements()) {
						JarEntry entry = entries.nextElement();
						if (!entry.isDirectory()) {
							Path entryPath = destination.resolve(entry.getName());
							try (InputStream in = jarFile.getInputStream(entry)) {
								Files.createDirectories(entryPath.getParent());
								Files.copy(in, destination.resolve(entry.getName()));
							}
						}
					}
				}
			}
			else {
				File source = new File(url.toURI());
				Files.createDirectories(destination);
				FileSystemUtils.copyRecursively(source, destination.toFile());
			}
			return destinationName + "/ ";
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

/*
 * Copyright 2002-2023 the original author or authors.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.OverridingClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.io.CleanupMode.NEVER;

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
	class EqualsAndHashCode {

		@Test
		void equalsAndHashCode() {
			Resource resource1 = new ClassPathResource("org/springframework/core/io/Resource.class");
			Resource resource2 = new ClassPathResource("org/springframework/core/../core/io/./Resource.class");
			Resource resource3 = new ClassPathResource("org/springframework/core/").createRelative("../core/io/./Resource.class");

			assertThat(resource2).isEqualTo(resource1);
			assertThat(resource3).isEqualTo(resource1);
			assertThat(resource2).hasSameHashCodeAs(resource1);
			assertThat(resource3).hasSameHashCodeAs(resource1);

			// Check whether equal/hashCode works in a HashSet.
			HashSet<Resource> resources = new HashSet<>();
			resources.add(resource1);
			resources.add(resource2);
			assertThat(resources).hasSize(1);
		}

		@Test
		void resourcesWithDifferentInputPathsAreEqual() {
			Resource resource1 = new ClassPathResource("org/springframework/core/io/Resource.class", getClass().getClassLoader());
			ClassPathResource resource2 = new ClassPathResource("org/springframework/core/../core/io/./Resource.class", getClass().getClassLoader());
			assertThat(resource2).isEqualTo(resource1);
		}

		@Test
		void resourcesWithEquivalentAbsolutePathsFromTheSameClassLoaderAreEqual() {
			ClassPathResource resource1 = new ClassPathResource("Resource.class", getClass());
			ClassPathResource resource2 = new ClassPathResource("org/springframework/core/io/Resource.class", getClass().getClassLoader());
			assertThat(resource1.getPath()).isEqualTo(resource2.getPath());
			assertThat(resource1).isEqualTo(resource2);
			assertThat(resource2).isEqualTo(resource1);
		}

		@Test
		void resourcesWithEquivalentAbsolutePathsHaveSameHashCode() {
			ClassPathResource resource1 = new ClassPathResource("Resource.class", getClass());
			ClassPathResource resource2 = new ClassPathResource("org/springframework/core/io/Resource.class", getClass().getClassLoader());
			assertThat(resource1.getPath()).isEqualTo(resource2.getPath());
			assertThat(resource1).hasSameHashCodeAs(resource2);
		}

		@Test
		void resourcesWithEquivalentAbsolutePathsFromDifferentClassLoadersAreNotEqual() {
			class SimpleThrowawayClassLoader extends OverridingClassLoader {
				SimpleThrowawayClassLoader(ClassLoader parent) {
					super(parent);
				}
			}

			ClassPathResource resource1 = new ClassPathResource("Resource.class", getClass());
			ClassPathResource resource2 = new ClassPathResource("org/springframework/core/io/Resource.class",
					new SimpleThrowawayClassLoader(getClass().getClassLoader()));
			assertThat(resource1.getPath()).isEqualTo(resource2.getPath());
			assertThat(resource1).isNotEqualTo(resource2);
			assertThat(resource2).isNotEqualTo(resource1);
		}

		@Test
		void relativeResourcesAreEqual() throws Exception {
			Resource resource = new ClassPathResource("dir/");
			Resource relative = resource.createRelative("subdir");
			assertThat(relative).isEqualTo(new ClassPathResource("dir/subdir"));
		}

	}

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
		Resource fileDir = new ClassPathResource("example/type");
		assertThat(fileDir.getURL()).asString().startsWith("file:");
		assertThat(fileDir.exists()).isTrue();
		assertThat(fileDir.isReadable()).isFalse();

		Resource jarDir = new ClassPathResource("reactor/core");
		assertThat(jarDir.getURL()).asString().startsWith("jar:");
		assertThat(jarDir.exists()).isTrue();
		assertThat(jarDir.isReadable()).isFalse();
	}

	@Test
	// Since the JAR file created in this test cannot be deleted on MS windows,
	// we use `cleanup = NEVER`.
	void emptyFileReadable(@TempDir(cleanup = NEVER) File tempDir) throws IOException {
		File file = new File(tempDir, "empty.txt");
		assertThat(file.createNewFile()).isTrue();
		assertThat(file.isFile()).isTrue();

		try (URLClassLoader fileClassLoader = new URLClassLoader(new URL[]{tempDir.toURI().toURL()})) {
			Resource emptyFile = new ClassPathResource("empty.txt", fileClassLoader);
			assertThat(emptyFile.exists()).isTrue();
			assertThat(emptyFile.isReadable()).isTrue();
			assertThat(emptyFile.contentLength()).isEqualTo(0);
			file.delete();
		}

		File jarFile = new File(tempDir, "test.jar");
		try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(jarFile))) {
			zipOut.putNextEntry(new ZipEntry("empty2.txt"));
			zipOut.closeEntry();
		}
		assertThat(jarFile.isFile()).isTrue();

		try (URLClassLoader jarClassLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()})) {
			Resource emptyJarEntry = new ClassPathResource("empty2.txt", jarClassLoader);
			assertThat(emptyJarEntry.exists()).isTrue();
			assertThat(emptyJarEntry.isReadable()).isTrue();
			assertThat(emptyJarEntry.contentLength()).isEqualTo(0);
		}

		jarFile.deleteOnExit();
		tempDir.deleteOnExit();
	}

}

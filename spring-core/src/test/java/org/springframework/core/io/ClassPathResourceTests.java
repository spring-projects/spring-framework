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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests that serve as regression tests for the bugs described in SPR-6888
 * and SPR-9413.
 *
 * @author Chris Beams
 * @author Sam Brannen
 */
class ClassPathResourceTests {

	private static final String PACKAGE_PATH = "org/springframework/core/io";
	private static final String NONEXISTENT_RESOURCE_NAME = "nonexistent.xml";
	private static final String FQ_RESOURCE_PATH = PACKAGE_PATH + '/' + NONEXISTENT_RESOURCE_NAME;

	/**
	 * Absolute path version of {@link #FQ_RESOURCE_PATH}.
	 */
	private static final String FQ_RESOURCE_PATH_WITH_LEADING_SLASH = '/' + FQ_RESOURCE_PATH;

	private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("^class path resource \\[(.+?)]$");


	@Test
	void stringConstructorRaisesExceptionWithFullyQualifiedPath() {
		assertExceptionContainsFullyQualifiedPath(new ClassPathResource(FQ_RESOURCE_PATH));
	}

	@Test
	void classLiteralConstructorRaisesExceptionWithFullyQualifiedPath() {
		assertExceptionContainsFullyQualifiedPath(new ClassPathResource(NONEXISTENT_RESOURCE_NAME, getClass()));
	}

	@Test
	void classLoaderConstructorRaisesExceptionWithFullyQualifiedPath() {
		assertExceptionContainsFullyQualifiedPath(new ClassPathResource(FQ_RESOURCE_PATH, getClass().getClassLoader()));
	}

	@Test
	void getDescriptionWithStringConstructor() {
		assertDescriptionContainsExpectedPath(new ClassPathResource(FQ_RESOURCE_PATH), FQ_RESOURCE_PATH);
	}

	@Test
	void getDescriptionWithStringConstructorAndLeadingSlash() {
		assertDescriptionContainsExpectedPath(new ClassPathResource(FQ_RESOURCE_PATH_WITH_LEADING_SLASH),
				FQ_RESOURCE_PATH);
	}

	@Test
	void getDescriptionWithClassLiteralConstructor() {
		assertDescriptionContainsExpectedPath(new ClassPathResource(NONEXISTENT_RESOURCE_NAME, getClass()),
				FQ_RESOURCE_PATH);
	}

	@Test
	void getDescriptionWithClassLiteralConstructorAndLeadingSlash() {
		assertDescriptionContainsExpectedPath(
				new ClassPathResource(FQ_RESOURCE_PATH_WITH_LEADING_SLASH, getClass()), FQ_RESOURCE_PATH);
	}

	@Test
	void getDescriptionWithClassLoaderConstructor() {
		assertDescriptionContainsExpectedPath(
				new ClassPathResource(FQ_RESOURCE_PATH, getClass().getClassLoader()), FQ_RESOURCE_PATH);
	}

	@Test
	void getDescriptionWithClassLoaderConstructorAndLeadingSlash() {
		assertDescriptionContainsExpectedPath(
				new ClassPathResource(FQ_RESOURCE_PATH_WITH_LEADING_SLASH, getClass().getClassLoader()), FQ_RESOURCE_PATH);
	}

	@Test
	void dropLeadingSlashForClassLoaderAccess() {
		assertThat(new ClassPathResource("/test.html").getPath()).isEqualTo("test.html");
		assertThat(((ClassPathResource) new ClassPathResource("").createRelative("/test.html")).getPath()).isEqualTo("test.html");
	}

	@Test
	void preserveLeadingSlashForClassRelativeAccess() {
		assertThat(new ClassPathResource("/test.html", getClass()).getPath()).isEqualTo("/test.html");
		assertThat(((ClassPathResource) new ClassPathResource("", getClass()).createRelative("/test.html")).getPath()).isEqualTo("/test.html");
	}

	@Test
	void directoryNotReadable() {
		Resource fileDir = new ClassPathResource("org/springframework/core");
		assertThat(fileDir.exists()).isTrue();
		assertThat(fileDir.isReadable()).isFalse();

		Resource jarDir = new ClassPathResource("reactor/core");
		assertThat(jarDir.exists()).isTrue();
		assertThat(jarDir.isReadable()).isFalse();
	}


	private void assertDescriptionContainsExpectedPath(ClassPathResource resource, String expectedPath) {
		Matcher matcher = DESCRIPTION_PATTERN.matcher(resource.getDescription());
		assertThat(matcher.matches()).isTrue();
		assertThat(matcher.groupCount()).isEqualTo(1);
		String match = matcher.group(1);

		assertThat(match).isEqualTo(expectedPath);
	}

	private void assertExceptionContainsFullyQualifiedPath(ClassPathResource resource) {
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
				resource::getInputStream)
			.withMessageContaining(FQ_RESOURCE_PATH);
	}

}

/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesLoaderUtils}.
 *
 * @author jyx-07
 */
class PropertiesLoaderUtilsTests {

	private static final String PACKAGE_PATH = ClassUtils.classPackageAsResourcePath(PropertiesLoaderUtilsTests.class);


	@Test
	void loadPropertiesFromPropertiesResource() throws Exception {
		Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource(PACKAGE_PATH + "/test.properties"));
		assertThat(props.getProperty("enigma")).isEqualTo("42");
	}

	@Test
	void loadPropertiesFromFlatYamlResourceWithYmlExtension() throws Exception {
		Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource(PACKAGE_PATH + "/test.yml"));
		assertThat(props.getProperty("enigma")).isEqualTo("42");
	}

	@Test
	void loadPropertiesFromYamlResourceWithYamlExtension() throws Exception {
		Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource(PACKAGE_PATH + "/test-simple.yaml"));
		assertThat(props.getProperty("foo")).isEqualTo("bar");
		assertThat(props.getProperty("baz")).isEqualTo("123");
	}

	@Test
	void loadPropertiesFlattensNestedMapsAndLists() throws Exception {
		Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource(PACKAGE_PATH + "/test.yml"));

		assertThat(props.getProperty("environments.dev.url")).isEqualTo("https://dev.bar.com");
		assertThat(props.getProperty("environments.dev.name")).isEqualTo("Developer Setup");
		assertThat(props.getProperty("environments.prod.url")).isEqualTo("https://foo.bar.com");
		assertThat(props.getProperty("environments.prod.name")).isEqualTo("My Cool App");
		assertThat(props.getProperty("servers[0]")).isEqualTo("dev.bar.com");
		assertThat(props.getProperty("servers[1]")).isEqualTo("foo.bar.com");
	}

	@Test
	void loadPropertiesMergesMultipleYamlDocuments() throws Exception {
		Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource(PACKAGE_PATH + "/test-multidoc.yml"));

		assertThat(props.getProperty("name")).isEqualTo("second");
		assertThat(props.getProperty("value")).isEqualTo("2");
	}

	@Test
	void fillPropertiesFromEncodedYamlResource() throws Exception {
		EncodedResource resource = new EncodedResource(new ClassPathResource(PACKAGE_PATH + "/test.yml"));
		Properties props = new Properties();
		PropertiesLoaderUtils.fillProperties(props, resource);

		assertThat(props.getProperty("enigma")).isEqualTo("42");
		assertThat(props.getProperty("servers[0]")).isEqualTo("dev.bar.com");
	}

	@Test
	void loadAllPropertiesFromYamlClasspathResource() throws Exception {
		Properties props = PropertiesLoaderUtils.loadAllProperties(PACKAGE_PATH + "/test.yml");
		assertThat(props.getProperty("enigma")).isEqualTo("42");
	}

}

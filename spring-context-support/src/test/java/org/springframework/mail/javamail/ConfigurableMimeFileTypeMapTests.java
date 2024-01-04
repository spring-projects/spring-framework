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

package org.springframework.mail.javamail;

import java.io.File;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
class ConfigurableMimeFileTypeMapTests {

	@Test
	void againstDefaultConfiguration() {
		ConfigurableMimeFileTypeMap ftm = new ConfigurableMimeFileTypeMap();
		ftm.afterPropertiesSet();

		assertThat(ftm.getContentType("foobar.HTM")).as("Invalid content type for HTM").isEqualTo("text/html");
		assertThat(ftm.getContentType("foobar.html")).as("Invalid content type for html").isEqualTo("text/html");
		assertThat(ftm.getContentType("foobar.c++")).as("Invalid content type for c++").isEqualTo("text/plain");
		assertThat(ftm.getContentType("foobar.svf")).as("Invalid content type for svf").isEqualTo("image/vnd.svf");
		assertThat(ftm.getContentType("foobar.dsf")).as("Invalid content type for dsf").isEqualTo("image/x-mgx-dsf");
		assertThat(ftm.getContentType("foobar.foo")).as("Invalid default content type").isEqualTo("application/octet-stream");
	}

	@Test
	void againstDefaultConfigurationWithFilePath() {
		ConfigurableMimeFileTypeMap ftm = new ConfigurableMimeFileTypeMap();
		assertThat(ftm.getContentType(new File("/tmp/foobar.HTM"))).as("Invalid content type for HTM").isEqualTo("text/html");
	}

	@Test
	void withAdditionalMappings() {
		ConfigurableMimeFileTypeMap ftm = new ConfigurableMimeFileTypeMap();
		ftm.setMappings("foo/bar HTM foo", "foo/cpp c++");
		ftm.afterPropertiesSet();

		assertThat(ftm.getContentType("foobar.HTM")).as("Invalid content type for HTM - override didn't work").isEqualTo("foo/bar");
		assertThat(ftm.getContentType("foobar.c++")).as("Invalid content type for c++ - override didn't work").isEqualTo("foo/cpp");
		assertThat(ftm.getContentType("bar.foo")).as("Invalid content type for foo - new mapping didn't work").isEqualTo("foo/bar");
	}

	@Test
	void withCustomMappingLocation() {
		Resource resource = new ClassPathResource("test.mime.types", getClass());

		ConfigurableMimeFileTypeMap ftm = new ConfigurableMimeFileTypeMap();
		ftm.setMappingLocation(resource);
		ftm.afterPropertiesSet();

		assertThat(ftm.getContentType("foobar.foo")).as("Invalid content type for foo").isEqualTo("text/foo");
		assertThat(ftm.getContentType("foobar.bar")).as("Invalid content type for bar").isEqualTo("text/bar");
		assertThat(ftm.getContentType("foobar.fimg")).as("Invalid content type for fimg").isEqualTo("image/foo");
		assertThat(ftm.getContentType("foobar.bimg")).as("Invalid content type for bimg").isEqualTo("image/bar");
	}

}

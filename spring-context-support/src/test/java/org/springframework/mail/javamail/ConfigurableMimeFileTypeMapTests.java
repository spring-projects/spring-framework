/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

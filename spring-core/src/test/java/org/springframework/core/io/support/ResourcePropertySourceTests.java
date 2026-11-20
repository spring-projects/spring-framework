/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.support;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResourcePropertySource}.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.1
 */
class ResourcePropertySourceTests {

	private static final String PROPERTIES_PATH = "org/springframework/core/io/example.properties";
	private static final String PROPERTIES_LOCATION = "classpath:" + PROPERTIES_PATH;
	private static final String PROPERTIES_RESOURCE_DESCRIPTION = "class path resource [" + PROPERTIES_PATH + "]";

	private static final String XML_PROPERTIES_PATH = "org/springframework/core/io/example.xml";
	private static final String XML_PROPERTIES_LOCATION = "classpath:" + XML_PROPERTIES_PATH;
	private static final String XML_PROPERTIES_RESOURCE_DESCRIPTION = "class path resource [" + XML_PROPERTIES_PATH + "]";

	@Test
	void withLocationAndGeneratedName() throws IOException {
		PropertySource<?> ps = new ResourcePropertySource(PROPERTIES_LOCATION);
		assertThat(ps.getProperty("foo")).isEqualTo("bar");
		assertThat(ps.getName()).isEqualTo(PROPERTIES_RESOURCE_DESCRIPTION);
	}

	@Test
	void xmlWithLocationAndGeneratedName() throws IOException {
		PropertySource<?> ps = new ResourcePropertySource(XML_PROPERTIES_LOCATION);
		assertThat(ps.getProperty("foo")).isEqualTo("bar");
		assertThat(ps.getName()).isEqualTo(XML_PROPERTIES_RESOURCE_DESCRIPTION);
	}

	@Test
	void withLocationAndExplicitName() throws IOException {
		PropertySource<?> ps = new ResourcePropertySource("ps1", PROPERTIES_LOCATION);
		assertThat(ps.getProperty("foo")).isEqualTo("bar");
		assertThat(ps.getName()).isEqualTo("ps1");
	}

	@Test
	void withLocationAndExplicitNameAndExplicitClassLoader() throws IOException {
		PropertySource<?> ps = new ResourcePropertySource("ps1", PROPERTIES_LOCATION, getClass().getClassLoader());
		assertThat(ps.getProperty("foo")).isEqualTo("bar");
		assertThat(ps.getName()).isEqualTo("ps1");
	}

	@Test
	void withLocationAndGeneratedNameAndExplicitClassLoader() throws IOException {
		PropertySource<?> ps = new ResourcePropertySource(PROPERTIES_LOCATION, getClass().getClassLoader());
		assertThat(ps.getProperty("foo")).isEqualTo("bar");
		assertThat(ps.getName()).isEqualTo(PROPERTIES_RESOURCE_DESCRIPTION);
	}

	@Test
	void withResourceAndGeneratedName() throws IOException {
		PropertySource<?> ps = new ResourcePropertySource(new ClassPathResource(PROPERTIES_PATH));
		assertThat(ps.getProperty("foo")).isEqualTo("bar");
		assertThat(ps.getName()).isEqualTo(PROPERTIES_RESOURCE_DESCRIPTION);
	}

	@Test
	void withResourceAndExplicitName() throws IOException {
		PropertySource<?> ps = new ResourcePropertySource("ps1", new ClassPathResource(PROPERTIES_PATH));
		assertThat(ps.getProperty("foo")).isEqualTo("bar");
		assertThat(ps.getName()).isEqualTo("ps1");
	}

	@Test
	void withResourceHavingNoDescription() throws IOException {
		PropertySource<?> ps = new ResourcePropertySource(new ByteArrayResource("foo=bar".getBytes(), ""));
		assertThat(ps.getProperty("foo")).isEqualTo("bar");
		assertThat(ps.getName()).isEqualTo("Byte array resource []");
	}

}

/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.io.support;

import java.io.IOException;

import org.junit.Test;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ResourcePropertySource}.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class ResourcePropertySourceTests {

	private static final String PROPERTIES_PATH = "org/springframework/core/io/example.properties";
	private static final String PROPERTIES_LOCATION = "classpath:" + PROPERTIES_PATH;
	private static final String PROPERTIES_RESOURCE_DESCRIPTION = "class path resource [" + PROPERTIES_PATH + "]";

	@Test
	public void withLocationAndGeneratedName() throws IOException {
		PropertySource<?> ps = new ResourcePropertySource(PROPERTIES_LOCATION);
		assertEquals(ps.getProperty("foo"), "bar");
		assertThat(ps.getName(), is(PROPERTIES_RESOURCE_DESCRIPTION));
	}

	@Test
	public void withLocationAndExplicitName() throws IOException {
		PropertySource<?> ps = new ResourcePropertySource("ps1", PROPERTIES_LOCATION);
		assertEquals(ps.getProperty("foo"), "bar");
		assertThat(ps.getName(), is("ps1"));
	}

	@Test
	public void withLocationAndExplicitNameAndExplicitClassLoader() throws IOException {
		PropertySource<?> ps = new ResourcePropertySource("ps1", PROPERTIES_LOCATION, getClass().getClassLoader());
		assertEquals(ps.getProperty("foo"), "bar");
		assertThat(ps.getName(), is("ps1"));
	}

	@Test
	public void withLocationAndGeneratedNameAndExplicitClassLoader() throws IOException {
		PropertySource<?> ps = new ResourcePropertySource(PROPERTIES_LOCATION, getClass().getClassLoader());
		assertEquals(ps.getProperty("foo"), "bar");
		assertThat(ps.getName(), is(PROPERTIES_RESOURCE_DESCRIPTION));
	}

	@Test
	public void withResourceAndGeneratedName() throws IOException {
		PropertySource<?> ps = new ResourcePropertySource(new ClassPathResource(PROPERTIES_PATH));
		assertEquals(ps.getProperty("foo"), "bar");
		assertThat(ps.getName(), is(PROPERTIES_RESOURCE_DESCRIPTION));
	}

	@Test
	public void withResourceAndExplicitName() throws IOException {
		PropertySource<?> ps = new ResourcePropertySource("ps1", new ClassPathResource(PROPERTIES_PATH));
		assertEquals(ps.getProperty("foo"), "bar");
		assertThat(ps.getName(), is("ps1"));
	}

	@Test
	public void withResourceHavingNoDescriptionAndGeneratedName() throws IOException {
		PropertySource<?> ps = new ResourcePropertySource(new ByteArrayResource("foo=bar".getBytes(), ""));
		assertEquals(ps.getProperty("foo"), "bar");
		assertTrue(ps.getName().startsWith("ByteArrayResource@"));
	}

}

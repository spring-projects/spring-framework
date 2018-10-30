/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.mail.javamail;

import java.io.File;

import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class ConfigurableMimeFileTypeMapTests {

	@Test
	public void againstDefaultConfiguration() throws Exception {
		ConfigurableMimeFileTypeMap ftm = new ConfigurableMimeFileTypeMap();
		ftm.afterPropertiesSet();

		assertEquals("Invalid content type for HTM", "text/html", ftm.getContentType("foobar.HTM"));
		assertEquals("Invalid content type for html", "text/html", ftm.getContentType("foobar.html"));
		assertEquals("Invalid content type for c++", "text/plain", ftm.getContentType("foobar.c++"));
		assertEquals("Invalid content type for svf", "image/vnd.svf", ftm.getContentType("foobar.svf"));
		assertEquals("Invalid content type for dsf", "image/x-mgx-dsf", ftm.getContentType("foobar.dsf"));
		assertEquals("Invalid default content type", "application/octet-stream", ftm.getContentType("foobar.foo"));
	}

	@Test
	public void againstDefaultConfigurationWithFilePath() throws Exception {
		ConfigurableMimeFileTypeMap ftm = new ConfigurableMimeFileTypeMap();
		assertEquals("Invalid content type for HTM", "text/html", ftm.getContentType(new File("/tmp/foobar.HTM")));
	}

	@Test
	public void withAdditionalMappings() throws Exception {
		ConfigurableMimeFileTypeMap ftm = new ConfigurableMimeFileTypeMap();
		ftm.setMappings(new String[] {"foo/bar HTM foo", "foo/cpp c++"});
		ftm.afterPropertiesSet();

		assertEquals("Invalid content type for HTM - override didn't work", "foo/bar", ftm.getContentType("foobar.HTM"));
		assertEquals("Invalid content type for c++ - override didn't work", "foo/cpp", ftm.getContentType("foobar.c++"));
		assertEquals("Invalid content type for foo - new mapping didn't work", "foo/bar", ftm.getContentType("bar.foo"));
	}

	@Test
	public void withCustomMappingLocation() throws Exception {
		Resource resource = new ClassPathResource("test.mime.types", getClass());

		ConfigurableMimeFileTypeMap ftm = new ConfigurableMimeFileTypeMap();
		ftm.setMappingLocation(resource);
		ftm.afterPropertiesSet();

		assertEquals("Invalid content type for foo", "text/foo", ftm.getContentType("foobar.foo"));
		assertEquals("Invalid content type for bar", "text/bar", ftm.getContentType("foobar.bar"));
		assertEquals("Invalid content type for fimg", "image/foo", ftm.getContentType("foobar.fimg"));
		assertEquals("Invalid content type for bimg", "image/bar", ftm.getContentType("foobar.bimg"));
	}

}

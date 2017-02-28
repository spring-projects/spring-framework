/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans.factory.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;

import static org.junit.Assert.*;

/**
 * Tests for {@link YamlMapFactoryBean}.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 */
public class YamlMapFactoryBeanTests {

	private final YamlMapFactoryBean factory = new YamlMapFactoryBean();


	@Test
	public void testSetIgnoreResourceNotFound() throws Exception {
		this.factory.setResolutionMethod(YamlMapFactoryBean.ResolutionMethod.OVERRIDE_AND_IGNORE);
		this.factory.setResources(new FileSystemResource("non-exsitent-file.yml"));
		assertEquals(0, this.factory.getObject().size());
	}

	@Test(expected = IllegalStateException.class)
	public void testSetBarfOnResourceNotFound() throws Exception {
		this.factory.setResources(new FileSystemResource("non-exsitent-file.yml"));
		assertEquals(0, this.factory.getObject().size());
	}

	@Test
	public void testGetObject() throws Exception {
		this.factory.setResources(new ByteArrayResource("foo: bar".getBytes()));
		assertEquals(1, this.factory.getObject().size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testOverrideAndRemoveDefaults() throws Exception {
		this.factory.setResources(new ByteArrayResource("foo:\n  bar: spam".getBytes()),
				new ByteArrayResource("foo:\n  spam: bar".getBytes()));

		assertEquals(1, this.factory.getObject().size());
		assertEquals(2, ((Map<String, Object>) this.factory.getObject().get("foo")).size());
	}

	@Test
	public void testFirstFound() throws Exception {
		this.factory.setResolutionMethod(YamlProcessor.ResolutionMethod.FIRST_FOUND);
		this.factory.setResources(new AbstractResource() {
			@Override
			public String getDescription() {
				return "non-existent";
			}
			@Override
			public InputStream getInputStream() throws IOException {
				throw new IOException("planned");
			}
		}, new ByteArrayResource("foo:\n  spam: bar".getBytes()));

		assertEquals(1, this.factory.getObject().size());
	}

	@Test
	public void testMapWithPeriodsInKey() throws Exception {
		this.factory.setResources(new ByteArrayResource("foo:\n  ? key1.key2\n  : value".getBytes()));
		Map<String, Object> map = this.factory.getObject();

		assertEquals(1, map.size());
		assertTrue(map.containsKey("foo"));
		Object object = map.get("foo");
		assertTrue(object instanceof LinkedHashMap);
		@SuppressWarnings("unchecked")
		Map<String, Object> sub = (Map<String, Object>) object;
		assertTrue(sub.containsKey("key1.key2"));
		assertEquals("value", sub.get("key1.key2"));
	}

	@Test
	public void testMapWithIntegerValue() throws Exception {
		this.factory.setResources(new ByteArrayResource("foo:\n  ? key1.key2\n  : 3".getBytes()));
		Map<String, Object> map = this.factory.getObject();

		assertEquals(1, map.size());
		assertTrue(map.containsKey("foo"));
		Object object = map.get("foo");
		assertTrue(object instanceof LinkedHashMap);
		@SuppressWarnings("unchecked")
		Map<String, Object> sub = (Map<String, Object>) object;
		assertEquals(1, sub.size());
		assertEquals(Integer.valueOf(3), sub.get("key1.key2"));
	}

	@Test
	public void testDuplicateKey() throws Exception {
		this.factory.setResources(new ByteArrayResource("mymap:\n  foo: bar\nmymap:\n  bar: foo".getBytes()));
		Map<String, Object> map = this.factory.getObject();

		assertEquals(1, map.size());
		assertTrue(map.containsKey("mymap"));
		Object object = map.get("mymap");
		assertTrue(object instanceof LinkedHashMap);
		@SuppressWarnings("unchecked")
		Map<String, Object> sub = (Map<String, Object>) object;
		assertEquals(1, sub.size());
		assertEquals("foo", sub.get("bar"));
	}

}

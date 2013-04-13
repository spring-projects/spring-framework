/*
 * Copyright 2012 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.config.YamlProcessor.ResolutionMethod;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * @author Dave Syer
 *
 */
public class YamlMapFactoryBeanTests {
	
	private YamlMapFactoryBean factory = new YamlMapFactoryBean();

	@Test
	public void testSetIgnoreResourceNotFound() throws Exception {
		factory.setResolutionMethod(YamlMapFactoryBean.ResolutionMethod.OVERRIDE_AND_IGNORE);
		factory.setResources(new FileSystemResource[] {new FileSystemResource("non-exsitent-file.yml")});
		assertEquals(0, factory.getObject().size());
	}

	@Test(expected=IllegalStateException.class)
	public void testSetBarfOnResourceNotFound() throws Exception {
		factory.setResources(new FileSystemResource[] {new FileSystemResource("non-exsitent-file.yml")});
		assertEquals(0, factory.getObject().size());
	}

	@Test
	public void testGetObject() throws Exception {
		factory.setResources(new ByteArrayResource[] {new ByteArrayResource("foo: bar".getBytes())});
		assertEquals(1, factory.getObject().size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testOverrideAndremoveDefaults() throws Exception {
		factory.setResources(new ByteArrayResource[] {new ByteArrayResource("foo:\n  bar: spam".getBytes()), new ByteArrayResource("foo:\n  spam: bar".getBytes())});
		assertEquals(1, factory.getObject().size());
		assertEquals(2, ((Map<String, Object>) factory.getObject().get("foo")).size());
	}

	@Test
	public void testFirstFound() throws Exception {
		factory.setResolutionMethod(ResolutionMethod.FIRST_FOUND);
		factory.setResources(new Resource[] {new AbstractResource() {
			public String getDescription() {
				return "non-existent";
			}
			public InputStream getInputStream() throws IOException {
				throw new IOException("planned");
			}
		}, new ByteArrayResource("foo:\n  spam: bar".getBytes())});
		assertEquals(1, factory.getObject().size());
	}

}

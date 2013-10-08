/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.servlet.resource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test fixture for {@link PrefixResourceResolver}
 *
 * @author Brian Clozel
 */
public class PrefixResourceResolverTests {

	private ResourceResolverChain resolver;

	private List<Resource> locations;

	private final String shaPrefix = "1df341f";

	@Before
	public void setUp() {
		List<ResourceResolver> resolvers = new ArrayList<ResourceResolver>();
		resolvers.add(new PrefixResourceResolver(this.shaPrefix));
		resolvers.add(new PathResourceResolver());
		this.resolver = new DefaultResourceResolverChain(resolvers);
		this.locations = new ArrayList<Resource>();
		this.locations.add(new ClassPathResource("test/", getClass()));
	}

	@Test
	public void testResolveResource() {
		String resourceId = "foo.css";
		Resource expected = new ClassPathResource("test/foo.css", getClass());
		Resource actual = this.resolver.resolveResource(null, "/" + this.shaPrefix + "/" + resourceId, this.locations);
		assertEquals(expected, actual);
	}

	@Test
	public void testResolveUrlPath() {
		String resourceId = "/foo.css";
		String url = "/" + this.shaPrefix + resourceId;
		assertEquals(url, resolver.resolveUrlPath(resourceId, locations));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFailPrefixResolverConstructor() {
		PrefixResourceResolver resolver = new PrefixResourceResolver("");
	}
}

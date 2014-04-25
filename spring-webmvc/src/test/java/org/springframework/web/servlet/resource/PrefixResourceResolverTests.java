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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link PrefixResourceResolver}
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
public class PrefixResourceResolverTests {

	private final List<? extends Resource> locations = Arrays.asList(new ClassPathResource("test/", getClass()));

	private final String shaPrefix = "1df341f";

	private ResourceResolverChain chain;


	@Before
	public void setUp() {
		List<ResourceResolver> resolvers = new ArrayList<ResourceResolver>();
		resolvers.add(new PrefixResourceResolver(this.shaPrefix));
		resolvers.add(new PathResourceResolver());
		this.chain = new DefaultResourceResolverChain(resolvers);
	}

	@Test
	public void resolveResource() {
		String resourceId = "foo.css";
		Resource expected = new ClassPathResource("test/foo.css", getClass());
		Resource actual = this.chain.resolveResource(null, this.shaPrefix + "/" + resourceId, this.locations);
		assertEquals(expected, actual);
	}

	@Test
	public void resolveUrlPath() {
		String resourceId = "/foo.css";
		String url = this.shaPrefix + resourceId;
		assertEquals(url, chain.resolveUrlPath(resourceId, locations));
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructWithEmptyPrefix() {
		new PrefixResourceResolver("   ");
	}

}

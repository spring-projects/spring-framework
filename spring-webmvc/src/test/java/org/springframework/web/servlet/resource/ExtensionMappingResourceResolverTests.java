/*
 * Copyright 2002-2013 the original author or authors.
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
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.junit.Assert.*;


/**
 * 
 * @author Jeremy Grelle
 */
public class ExtensionMappingResourceResolverTests {

	private ResourceResolverChain resolver;
	
	private List<Resource> locations;
	
	@Before
	public void setUp() {
		List<ResourceResolver> resolvers = new ArrayList<ResourceResolver>();
		resolvers.add(new ExtensionMappingResourceResolver());
		resolver = new DefaultResourceResolverChain(resolvers,  new ArrayList<ResourceTransformer>());
		locations = new ArrayList<Resource>();
		locations.add(new ClassPathResource("test/", getClass()));
		locations.add(new ClassPathResource("testalternatepath/", getClass()));
	}
	
	@Test
	public void resolveLessResource() throws Exception {
		String resourceId = "zoo.css"; 
		Resource resource = new ClassPathResource("test/"+resourceId+".less", getClass());
		Resource resolved = resolver.resolveAndTransform(null, resourceId, locations);
		assertEquals(resource, resolved);
	}
	
	@Test
	public void resolveLessUrl() {
		String resourceId = "zoo.css";
		String url = "zoo.css";
		assertEquals(url, resolver.resolveUrl(resourceId, locations));
	}
}

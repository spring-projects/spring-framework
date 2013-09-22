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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.*;


/**
 * 
 * @author Jeremy Grelle
 */
public class FingerprintingResourceResolverTests {

	private ResourceResolverChain chain;
	
	private FingerprintingResourceResolver resolver = new FingerprintingResourceResolver();
	
	private List<Resource> locations;
	
	@Before
	public void setUp() {
		List<ResourceResolver> resolvers = new ArrayList<ResourceResolver>();
		resolvers.add(resolver);
		chain = new DefaultResourceResolverChain(resolvers, new ArrayList<ResourceTransformer>());
		locations = new ArrayList<Resource>();
		locations.add(new ClassPathResource("test/", getClass()));
		locations.add(new ClassPathResource("testalternatepath/", getClass()));
	}
	
	@Test
	public void resolveStaticFingerprintedResource() throws Exception {
		String file = "foo-e36d2e05253c6c7085a91522ce43a0b4.css"; 
		Resource resource = new ClassPathResource("test/"+file, getClass());
		Resource resolved = chain.resolveAndTransform(null, file, locations);
		assertEquals(resource, resolved);
	}
	
	@Test
	public void resolveDynamicFingerprintedResource() throws Exception {
		Resource resource = new ClassPathResource("test/bar.css", getClass());
		String hash = DigestUtils.md5DigestAsHex(FileCopyUtils.copyToByteArray(resource.getInputStream()));
		String path = "/bar-"+hash+".css";
		
		Resource resolved = chain.resolveAndTransform(null, path, locations);
		
		assertEquals(resource, resolved);		
	}
	
	@Test
	public void resolveWithMultipleExtensions() throws Exception {
		Resource resource = new ClassPathResource("test/bar.min.css", getClass());
		String hash = DigestUtils.md5DigestAsHex(FileCopyUtils.copyToByteArray(resource.getInputStream()));
		String path = "/bar.min-"+hash+".css";
		
		Resource resolved = chain.resolveAndTransform(null, path, locations);
		
		assertEquals(resource, resolved);
	}
	
	@Test
	public void resolveWithMultipleHyphens() throws Exception {
		Resource resource = new ClassPathResource("test/foo-bar/foo-bar.css", getClass());
		String hash = DigestUtils.md5DigestAsHex(FileCopyUtils.copyToByteArray(resource.getInputStream()));
		String path = "/foo-bar/foo-bar-"+hash+".css";
		
		Resource resolved = chain.resolveAndTransform(null, path, locations);
		
		assertEquals(resource, resolved);
	}
	
	@Test
	public void extractHash() throws Exception {
		String hash = "7fbe76cdac6093784895bb4989203e5a";
		String path = "font-awesome/css/font-awesome.min-"+hash+".css";
		
		Method extractHash = ReflectionUtils.findMethod(FingerprintingResourceResolver.class, "extractHash", String.class);
		ReflectionUtils.makeAccessible(extractHash);
		String result = (String) ReflectionUtils.invokeMethod(extractHash, resolver, path);
		assertEquals(hash, result);
	}
}

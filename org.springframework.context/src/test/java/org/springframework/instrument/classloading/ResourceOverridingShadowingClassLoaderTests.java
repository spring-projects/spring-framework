/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.instrument.classloading;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Enumeration;

import org.junit.Test;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @since 2.0
 */
public class ResourceOverridingShadowingClassLoaderTests {
	
	private static final String EXISTING_RESOURCE = "org/springframework/instrument/classloading/testResource.xml";
	
	private ClassLoader thisClassLoader = getClass().getClassLoader();
	
	private ResourceOverridingShadowingClassLoader overridingLoader = new ResourceOverridingShadowingClassLoader(thisClassLoader);
	
	
	@Test
	public void testFindsExistingResourceWithGetResourceAndNoOverrides() {
		assertNotNull(thisClassLoader.getResource(EXISTING_RESOURCE));
		assertNotNull(overridingLoader.getResource(EXISTING_RESOURCE));
	}
	
	@Test
	public void testDoesNotFindExistingResourceWithGetResourceAndNullOverride() {
		assertNotNull(thisClassLoader.getResource(EXISTING_RESOURCE));
		overridingLoader.override(EXISTING_RESOURCE, null);
		assertNull(overridingLoader.getResource(EXISTING_RESOURCE));
	}
	
	@Test
	public void testFindsExistingResourceWithGetResourceAsStreamAndNoOverrides() {
		assertNotNull(thisClassLoader.getResourceAsStream(EXISTING_RESOURCE));
		assertNotNull(overridingLoader.getResourceAsStream(EXISTING_RESOURCE));
	}
	
	@Test
	public void testDoesNotFindExistingResourceWithGetResourceAsStreamAndNullOverride() {
		assertNotNull(thisClassLoader.getResourceAsStream(EXISTING_RESOURCE));
		overridingLoader.override(EXISTING_RESOURCE, null);
		assertNull(overridingLoader.getResourceAsStream(EXISTING_RESOURCE));
	}
	
	@Test
	public void testFindsExistingResourceWithGetResourcesAndNoOverrides() throws IOException {
		assertNotNull(thisClassLoader.getResources(EXISTING_RESOURCE));
		assertNotNull(overridingLoader.getResources(EXISTING_RESOURCE));
		assertEquals(1, countElements(overridingLoader.getResources(EXISTING_RESOURCE)));
	}
	
	@Test
	public void testDoesNotFindExistingResourceWithGetResourcesAndNullOverride() throws IOException {
		assertNotNull(thisClassLoader.getResources(EXISTING_RESOURCE));
		overridingLoader.override(EXISTING_RESOURCE, null);
		assertEquals(0, countElements(overridingLoader.getResources(EXISTING_RESOURCE)));
	}

	private int countElements(Enumeration<?> e) {
		int elts = 0;
		while (e.hasMoreElements()) {
			e.nextElement();
			++elts;
		}
		return elts;
	}
}

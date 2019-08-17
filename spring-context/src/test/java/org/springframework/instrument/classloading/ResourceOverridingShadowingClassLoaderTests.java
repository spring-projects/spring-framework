/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.instrument.classloading;

import java.io.IOException;
import java.util.Enumeration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(thisClassLoader.getResource(EXISTING_RESOURCE)).isNotNull();
		assertThat(overridingLoader.getResource(EXISTING_RESOURCE)).isNotNull();
	}

	@Test
	public void testDoesNotFindExistingResourceWithGetResourceAndNullOverride() {
		assertThat(thisClassLoader.getResource(EXISTING_RESOURCE)).isNotNull();
		overridingLoader.override(EXISTING_RESOURCE, null);
		assertThat(overridingLoader.getResource(EXISTING_RESOURCE)).isNull();
	}

	@Test
	public void testFindsExistingResourceWithGetResourceAsStreamAndNoOverrides() {
		assertThat(thisClassLoader.getResourceAsStream(EXISTING_RESOURCE)).isNotNull();
		assertThat(overridingLoader.getResourceAsStream(EXISTING_RESOURCE)).isNotNull();
	}

	@Test
	public void testDoesNotFindExistingResourceWithGetResourceAsStreamAndNullOverride() {
		assertThat(thisClassLoader.getResourceAsStream(EXISTING_RESOURCE)).isNotNull();
		overridingLoader.override(EXISTING_RESOURCE, null);
		assertThat(overridingLoader.getResourceAsStream(EXISTING_RESOURCE)).isNull();
	}

	@Test
	public void testFindsExistingResourceWithGetResourcesAndNoOverrides() throws IOException {
		assertThat(thisClassLoader.getResources(EXISTING_RESOURCE)).isNotNull();
		assertThat(overridingLoader.getResources(EXISTING_RESOURCE)).isNotNull();
		assertThat(countElements(overridingLoader.getResources(EXISTING_RESOURCE))).isEqualTo(1);
	}

	@Test
	public void testDoesNotFindExistingResourceWithGetResourcesAndNullOverride() throws IOException {
		assertThat(thisClassLoader.getResources(EXISTING_RESOURCE)).isNotNull();
		overridingLoader.override(EXISTING_RESOURCE, null);
		assertThat(countElements(overridingLoader.getResources(EXISTING_RESOURCE))).isEqualTo(0);
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

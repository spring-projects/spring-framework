/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class ResourceOverridingShadowingClassLoaderTests {

	private static final String EXISTING_RESOURCE = "org/springframework/instrument/classloading/testResource.xml";

	private ClassLoader thisClassLoader = getClass().getClassLoader();

	private ResourceOverridingShadowingClassLoader overridingLoader = new ResourceOverridingShadowingClassLoader(thisClassLoader);


	@Test
	void findsExistingResourceWithGetResourceAndNoOverrides() {
		assertThat(thisClassLoader.getResource(EXISTING_RESOURCE)).isNotNull();
		assertThat(overridingLoader.getResource(EXISTING_RESOURCE)).isNotNull();
	}

	@Test
	void doesNotFindExistingResourceWithGetResourceAndNullOverride() {
		assertThat(thisClassLoader.getResource(EXISTING_RESOURCE)).isNotNull();
		overridingLoader.override(EXISTING_RESOURCE, null);
		assertThat(overridingLoader.getResource(EXISTING_RESOURCE)).isNull();
	}

	@Test
	void findsExistingResourceWithGetResourceAsStreamAndNoOverrides() {
		assertThat(thisClassLoader.getResourceAsStream(EXISTING_RESOURCE)).isNotNull();
		assertThat(overridingLoader.getResourceAsStream(EXISTING_RESOURCE)).isNotNull();
	}

	@Test
	void doesNotFindExistingResourceWithGetResourceAsStreamAndNullOverride() {
		assertThat(thisClassLoader.getResourceAsStream(EXISTING_RESOURCE)).isNotNull();
		overridingLoader.override(EXISTING_RESOURCE, null);
		assertThat(overridingLoader.getResourceAsStream(EXISTING_RESOURCE)).isNull();
	}

	@Test
	void findsExistingResourceWithGetResourcesAndNoOverrides() throws IOException {
		assertThat(thisClassLoader.getResources(EXISTING_RESOURCE)).isNotNull();
		assertThat(overridingLoader.getResources(EXISTING_RESOURCE)).isNotNull();
		assertThat(countElements(overridingLoader.getResources(EXISTING_RESOURCE))).isEqualTo(1);
	}

	@Test
	void doesNotFindExistingResourceWithGetResourcesAndNullOverride() throws IOException {
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

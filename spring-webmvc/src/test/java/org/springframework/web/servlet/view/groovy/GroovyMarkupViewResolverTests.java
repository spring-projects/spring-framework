/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.view.groovy;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GroovyMarkupViewResolver}.
 *
 * @author Brian Clozel
 */
class GroovyMarkupViewResolverTests {

	@Test
	void viewClass() {
		GroovyMarkupViewResolver resolver = new GroovyMarkupViewResolver();
		assertThat(resolver.requiredViewClass()).isEqualTo(GroovyMarkupView.class);
		DirectFieldAccessor viewAccessor = new DirectFieldAccessor(resolver);
		Class<?> viewClass = (Class<?>) viewAccessor.getPropertyValue("viewClass");
		assertThat(viewClass).isEqualTo(GroovyMarkupView.class);
	}

	@Test
	void cacheKey() {
		GroovyMarkupViewResolver resolver = new GroovyMarkupViewResolver();
		String cacheKey = (String) resolver.getCacheKey("test", Locale.US);
		assertThat(cacheKey).isNotNull();
		assertThat(cacheKey).isEqualTo("test_en_US");
	}

}

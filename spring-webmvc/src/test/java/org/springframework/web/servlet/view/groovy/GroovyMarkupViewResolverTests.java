/*
 * Copyright 2002-2024 the original author or authors.
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

/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.web.servlet.handler;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Test fixture for {@link MappedInterceptor} tests.
 *
 * @author Rossen Stoyanchev
 */
public class MappedInterceptorTests {

	private LocaleChangeInterceptor interceptor;

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	@Before
	public void setup() {
		this.interceptor = new LocaleChangeInterceptor();
	}

	@Test
	public void noPatterns() {
		MappedInterceptor mappedInterceptor = new MappedInterceptor(null, null, this.interceptor);
		assertTrue(mappedInterceptor.matches("/foo", pathMatcher));
	}

	@Test
	public void includePatternOnly() {
		MappedInterceptor mappedInterceptor = new MappedInterceptor(new String[] { "/foo/*" }, this.interceptor);

		assertTrue(mappedInterceptor.matches("/foo/bar", pathMatcher));
		assertFalse(mappedInterceptor.matches("/bar/foo", pathMatcher));
	}

	@Test
	public void excludePatternOnly() {
		MappedInterceptor mappedInterceptor = new MappedInterceptor(null, new String[] { "/admin/**" }, this.interceptor);

		assertTrue(mappedInterceptor.matches("/foo", pathMatcher));
		assertFalse(mappedInterceptor.matches("/admin/foo", pathMatcher));
	}

	@Test
	public void includeAndExcludePatterns() {
		MappedInterceptor mappedInterceptor = new MappedInterceptor(
				new String[] { "/**" }, new String[] { "/admin/**" }, this.interceptor);

		assertTrue(mappedInterceptor.matches("/foo", pathMatcher));
		assertFalse(mappedInterceptor.matches("/admin/foo", pathMatcher));
	}

}

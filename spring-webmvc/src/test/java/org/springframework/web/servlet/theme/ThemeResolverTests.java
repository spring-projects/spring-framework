/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.servlet.theme;

import org.junit.jupiter.api.Test;

import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @since 19.06.2003
 */
@SuppressWarnings("deprecation")
public class ThemeResolverTests {

	private static final String TEST_THEME_NAME = "test.theme";
	private static final String DEFAULT_TEST_THEME_NAME = "default.theme";

	private void internalTest(ThemeResolver themeResolver, boolean shouldSet, String defaultName) {
		// create mocks
		MockServletContext context = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(context);
		MockHttpServletResponse response = new MockHttpServletResponse();
		// check original theme
		String themeName = themeResolver.resolveThemeName(request);
		assertThat(defaultName).isEqualTo(themeName);
		// set new theme name
		try {
			themeResolver.setThemeName(request, response, TEST_THEME_NAME);
			assertThat(shouldSet).as("able to set theme name").isTrue();
			// check new theme namelocale
			themeName = themeResolver.resolveThemeName(request);
			assertThat(themeName).isEqualTo(TEST_THEME_NAME);
			themeResolver.setThemeName(request, response, null);
			themeName = themeResolver.resolveThemeName(request);
			assertThat(defaultName).isEqualTo(themeName);
		}
		catch (UnsupportedOperationException ex) {
			assertThat(shouldSet).as("able to set theme name").isFalse();
		}
	}

	@Test
	public void fixedThemeResolver() {
		internalTest(new FixedThemeResolver(), false, AbstractThemeResolver.ORIGINAL_DEFAULT_THEME_NAME);
	}

	@Test
	public void cookieThemeResolver() {
		internalTest(new CookieThemeResolver(), true, AbstractThemeResolver.ORIGINAL_DEFAULT_THEME_NAME);
	}

	@Test
	public void sessionThemeResolver() {
		internalTest(new SessionThemeResolver(), true,AbstractThemeResolver.ORIGINAL_DEFAULT_THEME_NAME);
	}

	@Test
	public void sessionThemeResolverWithDefault() {
		SessionThemeResolver tr = new SessionThemeResolver();
		tr.setDefaultThemeName(DEFAULT_TEST_THEME_NAME);
		internalTest(tr, true, DEFAULT_TEST_THEME_NAME);
	}

}

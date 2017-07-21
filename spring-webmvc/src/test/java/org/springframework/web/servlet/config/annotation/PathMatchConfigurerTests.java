/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.web.util.pattern.ParsingPathMatcher;

/**
 * Unit tests for {@link PathMatchConfigurer}
 * @author Brian Clozel
 */
public class PathMatchConfigurerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	// SPR-15303
	@Test
	public void illegalConfigurationParsingPathMatcher() {
		PathMatchConfigurer configurer = new PathMatchConfigurer();
		configurer.setPathMatcher(new ParsingPathMatcher());
		configurer.setUseSuffixPatternMatch(true);
		configurer.setUseTrailingSlashMatch(true);

		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage(Matchers.containsString("useSuffixPatternMatch"));
		this.thrown.expectMessage(Matchers.containsString("useTrailingSlashMatch"));

		configurer.getPathMatcher();
	}
}

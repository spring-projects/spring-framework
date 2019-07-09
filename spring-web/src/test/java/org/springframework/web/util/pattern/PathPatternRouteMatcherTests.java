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

package org.springframework.web.util.pattern;

import org.junit.Test;

import org.springframework.util.RouteMatcher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link PathPatternRouteMatcher}.
 *
 * @author Brian Clozel
 * @since 5.2
 */
public class PathPatternRouteMatcherTests {

	@Test
	public void matchRoute() {
		PathPatternRouteMatcher routeMatcher = new PathPatternRouteMatcher(new PathPatternParser());
		RouteMatcher.Route route = routeMatcher.parseRoute("/projects/spring-framework");
		assertThat(routeMatcher.match("/projects/{name}", route)).isTrue();
	}

	@Test
	public void matchRouteWithCustomSeparator() {
		PathPatternParser pathPatternParser = new PathPatternParser();
		pathPatternParser.setSeparator('.');
		PathPatternRouteMatcher routeMatcher = new PathPatternRouteMatcher(pathPatternParser);
		RouteMatcher.Route route = routeMatcher.parseRoute("projects.spring-framework");
		assertThat(routeMatcher.match("projects.{name}", route)).isTrue();
	}

}

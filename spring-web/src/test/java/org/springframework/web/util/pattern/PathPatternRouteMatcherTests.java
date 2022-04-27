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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.server.PathContainer;
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
		PathPatternRouteMatcher routeMatcher = new PathPatternRouteMatcher();
		RouteMatcher.Route route = routeMatcher.parseRoute("projects.spring-framework");
		assertThat(routeMatcher.match("projects.{name}", route)).isTrue();
	}

	@Test
	public void matchRouteWithCustomSeparator() {
		PathPatternParser parser = new PathPatternParser();
		parser.setPathOptions(PathContainer.Options.create('/', false));
		PathPatternRouteMatcher routeMatcher = new PathPatternRouteMatcher(parser);
		RouteMatcher.Route route = routeMatcher.parseRoute("/projects/spring-framework");
		assertThat(routeMatcher.match("/projects/{name}", route)).isTrue();
	}

	@Test // gh-23310
	public void noDecodingAndNoParamParsing() {
		PathPatternRouteMatcher routeMatcher = new PathPatternRouteMatcher();
		RouteMatcher.Route route = routeMatcher.parseRoute("projects.spring%20framework;p=1");
		assertThat(routeMatcher.match("projects.spring%20framework;p=1", route)).isTrue();
	}

	@Test // gh-23310
	public void separatorOnlyDecoded() {
		PathPatternRouteMatcher routeMatcher = new PathPatternRouteMatcher();
		RouteMatcher.Route route = routeMatcher.parseRoute("projects.spring%2Eframework");
		Map<String, String> vars = routeMatcher.matchAndExtract("projects.{project}", route);
		assertThat(vars).containsEntry("project", "spring.framework");
	}

}

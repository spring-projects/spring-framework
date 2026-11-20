/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class PathPatternRouteMatcherTests {

	@Test
	void matchRoute() {
		PathPatternRouteMatcher routeMatcher = new PathPatternRouteMatcher();
		RouteMatcher.Route route = routeMatcher.parseRoute("projects.spring-framework");
		assertThat(routeMatcher.match("projects.{name}", route)).isTrue();
	}

	@Test
	void matchRouteWithCustomSeparator() {
		PathPatternParser parser = new PathPatternParser();
		parser.setPathOptions(PathContainer.Options.create('/', false));
		PathPatternRouteMatcher routeMatcher = new PathPatternRouteMatcher(parser);
		RouteMatcher.Route route = routeMatcher.parseRoute("/projects/spring-framework");
		assertThat(routeMatcher.match("/projects/{name}", route)).isTrue();
	}

	@Test // gh-23310
	void noDecodingAndNoParamParsing() {
		PathPatternRouteMatcher routeMatcher = new PathPatternRouteMatcher();
		RouteMatcher.Route route = routeMatcher.parseRoute("projects.spring%20framework;p=1");
		assertThat(routeMatcher.match("projects.spring%20framework;p=1", route)).isTrue();
	}

	@Test // gh-23310
	void separatorOnlyDecoded() {
		PathPatternRouteMatcher routeMatcher = new PathPatternRouteMatcher();
		RouteMatcher.Route route = routeMatcher.parseRoute("projects.spring%2Eframework");
		Map<String, String> vars = routeMatcher.matchAndExtract("projects.{project}", route);
		assertThat(vars).containsEntry("project", "spring.framework");
	}

}

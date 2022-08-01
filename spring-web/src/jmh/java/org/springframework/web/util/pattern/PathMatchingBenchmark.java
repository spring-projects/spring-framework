/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import org.springframework.http.server.PathContainer;
import org.springframework.util.AntPathMatcher;

/**
 * Benchmarks for matching requests paths against path patterns in a web context.
 * We're considering here the {@link org.springframework.util.AntPathMatcher} and
 * {@link PathPatternParser} implementations with typical sets of patterns.
 * @author Brian Clozel
 */
@BenchmarkMode(Mode.Throughput)
public class PathMatchingBenchmark {

	@State(Scope.Benchmark)
	public static class AllRoutesPatternParser extends PatternParserData {

		@Setup(Level.Trial)
		public void registerPatterns() {
			parseRoutes(RouteGenerator.allRoutes());
		}
	}

	@Benchmark
	public void matchAllRoutesWithPathPatternParser(AllRoutesPatternParser data, Blackhole bh) {
		for (PathContainer path : data.requestPaths) {
			for (PathPattern pattern : data.patterns) {
				bh.consume(pattern.matches(path));
			}
		}
	}

	@Benchmark
	public void matchAndSortAllRoutesWithPathPatternParser(AllRoutesPatternParser data, Blackhole bh) {
		for (PathContainer path : data.requestPaths) {
			List<PathPattern> matches = new ArrayList<>();
			for (PathPattern pattern : data.patterns) {
				if (pattern.matches(path)) {
					matches.add(pattern);
				}
			}
			Collections.sort(matches);
			bh.consume(matches);
		}
	}

	@State(Scope.Benchmark)
	public static class StaticRoutesPatternParser extends PatternParserData {

		@Setup(Level.Trial)
		public void registerPatterns() {
			parseRoutes(RouteGenerator.staticRoutes());
		}
	}

	@Benchmark
	public void matchStaticRoutesWithPathPatternParser(StaticRoutesPatternParser data, Blackhole bh) {
		for (PathContainer path : data.requestPaths) {
			for (PathPattern pattern : data.patterns) {
				bh.consume(pattern.matches(path));
			}
		}
	}

	@State(Scope.Benchmark)
	public static class AllRoutesAntPathMatcher extends AntPathMatcherData {

		@Setup(Level.Trial)
		public void registerPatterns() {
			parseRoutes(RouteGenerator.allRoutes());
		}
	}

	@Benchmark
	public void matchAllRoutesWithAntPathMatcher(AllRoutesAntPathMatcher data, Blackhole bh) {
		for (String path : data.requestPaths) {
			for (String pattern : data.patterns) {
				bh.consume(data.matcher.match(pattern, path));
			}
		}
	}

	@Benchmark
	public void matchAndSortAllRoutesWithAntPathMatcher(AllRoutesAntPathMatcher data, Blackhole bh) {
		for (String path : data.requestPaths) {
			List<String> matches = new ArrayList<>();
			for (String pattern : data.patterns) {
				if (data.matcher.match(pattern, path)) {
					matches.add(pattern);
				}
			}
			matches.sort(data.matcher.getPatternComparator(path));
			bh.consume(matches);
		}
	}

	@State(Scope.Benchmark)
	public static class StaticRoutesAntPathMatcher extends AntPathMatcherData {

		@Setup(Level.Trial)
		public void registerPatterns() {
			parseRoutes(RouteGenerator.staticRoutes());
		}
	}

	@Benchmark
	public void matchStaticRoutesWithAntPathMatcher(StaticRoutesAntPathMatcher data, Blackhole bh) {
		for (String path : data.requestPaths) {
			for (String pattern : data.patterns) {
				bh.consume(data.matcher.match(pattern, path));
			}
		}
	}


	static class PatternParserData {

		List<PathPattern> patterns = new ArrayList<>();

		List<PathContainer> requestPaths = new ArrayList<>();

		void parseRoutes(List<Route> routes) {
			PathPatternParser parser = new PathPatternParser();
			routes.forEach(route -> {
				this.patterns.add(parser.parse(route.pattern));
				route.matchingPaths.forEach(path -> this.requestPaths.add(PathContainer.parsePath(path)));
			});
		}

	}

	static class AntPathMatcherData {

		AntPathMatcher matcher = new AntPathMatcher();

		List<String> patterns = new ArrayList<>();

		List<String> requestPaths = new ArrayList<>();

		void parseRoutes(List<Route> routes) {
			routes.forEach(route -> {
				this.patterns.add(route.pattern);
				this.requestPaths.addAll(route.matchingPaths);
			});
		}

	}

	/**
	 * Route in the web application.
	 * Each route has a path pattern and can generate sets of matching request paths for that pattern.
	 */
	static class Route {

		private final String pattern;

		private final List<String> matchingPaths;

		public Route(String pattern, String... matchingPaths) {
			this.pattern = pattern;
			if (matchingPaths.length > 0) {
				this.matchingPaths = Arrays.asList(matchingPaths);
			}
			else {
				this.matchingPaths = Collections.singletonList(pattern);
			}
		}

		public String pattern() {
			return this.pattern;
		}

		public Iterable<String> matchingPaths() {
			return this.matchingPaths;
		}
	}

	static class RouteGenerator {

		static List<Route> staticRoutes() {
			return Arrays.asList(
					new Route("/"),
					new Route("/why-spring"),
					new Route("/microservices"),
					new Route("/reactive"),
					new Route("/event-driven"),
					new Route("/cloud"),
					new Route("/web-applications"),
					new Route("/serverless"),
					new Route("/batch"),
					new Route("/community/overview"),
					new Route("/community/team"),
					new Route("/community/events"),
					new Route("/community/support"),
					new Route("/some/other/section"),
					new Route("/blog.atom")
			);
		}

		static List<Route> captureRoutes() {
			return Arrays.asList(
					new Route("/guides"),
					new Route("/guides/gs/{repositoryName}",
							"/guides/gs/rest-service", "/guides/gs/scheduling-tasks",
							"/guides/gs/consuming-rest", "/guides/gs/relational-data-access"),
					new Route("/projects"),
					new Route("/projects/{name}",
							"/projects/spring-boot", "/projects/spring-framework",
							"/projects/spring-data", "/projects/spring-security", "/projects/spring-cloud"),
					new Route("/blog/category/{category}.atom",
							"/blog/category/releases.atom", "/blog/category/engineering.atom",
							"/blog/category/news.atom"),
					new Route("/tools/{name}", "/tools/eclipse", "/tools/vscode"),
					new Route("/team/{username}",
							"/team/jhoeller", "/team/bclozel", "/team/snicoll", "/team/sdeleuze", "/team/rstoyanchev"),
					new Route("/api/projects/{projectId}",
							"/api/projects/spring-boot", "/api/projects/spring-framework",
							"/api/projects/reactor", "/api/projects/spring-data",
							"/api/projects/spring-restdocs", "/api/projects/spring-batch"),
					new Route("/api/projects/{projectId}/releases/{version}",
							"/api/projects/spring-boot/releases/2.3.0", "/api/projects/spring-framework/releases/5.3.0",
							"/api/projects/spring-boot/releases/2.2.0", "/api/projects/spring-framework/releases/5.2.0")
			);
		}

		static List<Route> regexRoute() {
			return Arrays.asList(
					new Route("/blog/{year:\\\\d+}/{month:\\\\d+}/{day:\\\\d+}/{slug}",
							"/blog/2020/01/01/spring-boot-released", "/blog/2020/02/10/this-week-in-spring",
							"/blog/2020/03/12/spring-one-conference-2020", "/blog/2020/05/17/spring-io-barcelona-2020",
							"/blog/2020/05/17/spring-io-barcelona-2020", "/blog/2020/06/06/spring-cloud-release"),
					new Route("/user/{name:[a-z]+}",
							"/user/emily", "/user/example", "/user/spring")
			);
		}

		static List<Route> allRoutes() {
			List<Route> routes = new ArrayList<>();
			routes.addAll(staticRoutes());
			routes.addAll(captureRoutes());
			routes.addAll(regexRoute());
			routes.add(new Route("/static/**", "/static/image.png", "/static/style.css"));
			routes.add(new Route("/**", "/notfound", "/favicon.ico"));
			return routes;
		}

	}
}

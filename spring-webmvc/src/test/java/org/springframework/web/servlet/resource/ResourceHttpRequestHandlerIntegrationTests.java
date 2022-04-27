/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import javax.servlet.ServletException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletConfig;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Integration tests for static resource handling.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceHttpRequestHandlerIntegrationTests {

	private final MockServletContext servletContext = new MockServletContext();

	private final MockServletConfig servletConfig = new MockServletConfig(this.servletContext);


	public static Stream<Arguments> argumentSource() {
		return Stream.of(
				arguments(true, "/cp"),
				arguments(true, "/fs"),
				arguments(true, "/url"),
				arguments(false, "/cp"),
				arguments(false, "/fs"),
				arguments(false, "/url")
		);
	}


	@ParameterizedTest
	@MethodSource("argumentSource")
	void cssFile(boolean usePathPatterns, String pathPrefix) throws Exception {
		MockHttpServletRequest request = initRequest(pathPrefix + "/test/foo.css");
		MockHttpServletResponse response = new MockHttpServletResponse();

		DispatcherServlet servlet = initDispatcherServlet(usePathPatterns, WebConfig.class);
		servlet.service(request, response);

		String description = "usePathPattern=" + usePathPatterns + ", prefix=" + pathPrefix;
		assertThat(response.getStatus()).as(description).isEqualTo(200);
		assertThat(response.getContentType()).as(description).isEqualTo("text/css");
		assertThat(response.getContentAsString()).as(description).isEqualTo("h1 { color:red; }");
	}

	@ParameterizedTest
	@MethodSource("argumentSource")
	void classpathLocationWithEncodedPath(boolean usePathPatterns, String pathPrefix) throws Exception {
		MockHttpServletRequest request = initRequest(pathPrefix + "/test/foo with spaces.css");
		MockHttpServletResponse response = new MockHttpServletResponse();

		DispatcherServlet servlet = initDispatcherServlet(usePathPatterns, WebConfig.class);
		servlet.service(request, response);

		String description = "usePathPattern=" + usePathPatterns + ", prefix=" + pathPrefix;
		assertThat(response.getStatus()).as(description).isEqualTo(200);
		assertThat(response.getContentType()).as(description).isEqualTo("text/css");
		assertThat(response.getContentAsString()).as(description).isEqualTo("h1 { color:red; }");
	}

	private DispatcherServlet initDispatcherServlet(boolean usePathPatterns, Class<?>... configClasses)
			throws ServletException {

		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(configClasses);
		if (usePathPatterns) {
			context.register(PathPatternParserConfig.class);
		}
		context.setServletConfig(this.servletConfig);
		context.refresh();

		DispatcherServlet servlet = new DispatcherServlet();
		servlet.setApplicationContext(context);
		servlet.init(this.servletConfig);
		return servlet;
	}

	private MockHttpServletRequest initRequest(String path) {
		path = UriUtils.encodePath(path, StandardCharsets.UTF_8);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
		request.setCharacterEncoding(StandardCharsets.UTF_8.name());
		return request;
	}


	@EnableWebMvc
	static class WebConfig implements WebMvcConfigurer {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			ClassPathResource classPathLocation = new ClassPathResource("", getClass());
			String path = getPath(classPathLocation);

			registerClasspathLocation("/cp/**", classPathLocation, registry);
			registerFileSystemLocation("/fs/**", path, registry);
			registerUrlLocation("/url/**", "file:" + path, registry);
		}

		protected void registerClasspathLocation(String pattern, ClassPathResource resource, ResourceHandlerRegistry registry) {
			registry.addResourceHandler(pattern).addResourceLocations(resource);
		}

		protected void registerFileSystemLocation(String pattern, String path, ResourceHandlerRegistry registry) {
			FileSystemResource fileSystemLocation = new FileSystemResource(path);
			registry.addResourceHandler(pattern).addResourceLocations(fileSystemLocation);
		}

		protected void registerUrlLocation(String pattern, String path, ResourceHandlerRegistry registry) {
			try {
				UrlResource urlLocation = new UrlResource(path);
				registry.addResourceHandler(pattern).addResourceLocations(urlLocation);
			}
			catch (MalformedURLException ex) {
				throw new IllegalStateException(ex);
			}
		}

		private String getPath(ClassPathResource resource) {
			try {
				return resource.getFile().getCanonicalPath().replace('\\', '/').replace("classes/java", "resources") + "/";
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}


	static class PathPatternParserConfig implements WebMvcConfigurer {

		@Override
		public void configurePathMatch(PathMatchConfigurer configurer) {
			configurer.setPatternParser(new PathPatternParser());
		}
	}

}

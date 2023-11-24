/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.List;
import java.util.stream.Stream;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletConfig;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;
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
				// PathPattern
				arguments(true, true, "/cp"),
				arguments(true, true, "/fs"),
				arguments(true, true, "/url"),

				arguments(true, false, "/cp"),
				arguments(true, false, "/fs"),
				arguments(true, false, "/url"),

				// PathMatcher
				arguments(false, true, "/cp"),
				arguments(false, true, "/fs"),
				arguments(false, true, "/url"),

				arguments(false, false, "/cp"),
				arguments(false, false, "/fs"),
				arguments(false, false, "/url")
		);
	}


	@ParameterizedTest
	@MethodSource("argumentSource")
	void cssFile(boolean usePathPatterns, boolean decodingUrlPathHelper, String pathPrefix) throws Exception {

		MockHttpServletRequest request = initRequest(pathPrefix + "/test/foo.css");
		MockHttpServletResponse response = new MockHttpServletResponse();

		DispatcherServlet servlet = initDispatcherServlet(usePathPatterns, decodingUrlPathHelper, WebConfig.class);
		servlet.service(request, response);

		String description = "usePathPattern=" + usePathPatterns + ", prefix=" + pathPrefix;
		assertThat(response.getStatus()).as(description).isEqualTo(200);
		assertThat(response.getContentType()).as(description).isEqualTo("text/css");
		assertThat(response.getContentAsString()).as(description).isEqualTo("h1 { color:red; }");
	}

	@ParameterizedTest
	@MethodSource("argumentSource") // gh-26775
	void classpathLocationWithEncodedPath(
			boolean usePathPatterns, boolean decodingUrlPathHelper, String pathPrefix) throws Exception {

		MockHttpServletRequest request = initRequest(pathPrefix + "/test/foo with spaces.css");
		MockHttpServletResponse response = new MockHttpServletResponse();

		DispatcherServlet servlet = initDispatcherServlet(usePathPatterns, decodingUrlPathHelper, WebConfig.class);
		servlet.service(request, response);

		String description = "usePathPattern=" + usePathPatterns + ", prefix=" + pathPrefix;
		assertThat(response.getStatus()).as(description).isEqualTo(200);
		assertThat(response.getContentType()).as(description).isEqualTo("text/css");
		assertThat(response.getContentAsString()).as(description).isEqualTo("h1 { color:red; }");
	}

	@Test
	void testNoResourceFoundException() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletConfig(this.servletConfig);
		context.register(WebConfig.class);
		context.register(GlobalExceptionHandler.class);
		context.refresh();

		DispatcherServlet servlet = new DispatcherServlet();
		servlet.setApplicationContext(context);
		servlet.init(this.servletConfig);

		MockHttpServletRequest request = initRequest("/cp/non-existing");
		MockHttpServletResponse response = new MockHttpServletResponse();

		servlet.service(request, response);

		assertThat(response.getStatus()).isEqualTo(404);
		assertThat(response.getContentType()).isEqualTo("application/problem+json");
		assertThat(response.getContentAsString()).isEqualTo("""
				{"type":"about:blank",\
				"title":"Not Found",\
				"status":404,\
				"detail":"No static resource non-existing.",\
				"instance":"/cp/non-existing"}\
				""");
	}

	private DispatcherServlet initDispatcherServlet(
			boolean usePathPatterns, boolean decodingUrlPathHelper, Class<?>... configClasses) throws ServletException {

		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(configClasses);
		if (usePathPatterns) {
			context.register(PathPatternParserConfig.class);
		}
		context.register(decodingUrlPathHelper ?
				DecodingUrlPathHelperConfig.class : NonDecodingUrlPathHelperConfig.class);
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

			registry.addResourceHandler("/cp/**").addResourceLocations(classPathLocation);
			registry.addResourceHandler("/fs/**").addResourceLocations(new FileSystemResource(path));
			registry.addResourceHandler("/url/**").addResourceLocations(urlResource(path));
		}

		private String getPath(ClassPathResource resource) {
			try {
				return resource.getFile().getCanonicalPath()
						.replace('\\', '/')
						.replace("classes/java", "resources") + "/";
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

		private UrlResource urlResource(String path) {
			UrlResource urlResource;
			try {
				urlResource = new UrlResource("file:" + path);
			}
			catch (MalformedURLException ex) {
				throw new IllegalStateException(ex);
			}
			return urlResource;
		}

		@Override
		public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			converters.add(new MappingJackson2HttpMessageConverter());
		}
	}


	static class PathPatternParserConfig implements WebMvcConfigurer {

		@Override
		public void configurePathMatch(PathMatchConfigurer configurer) {
			configurer.setPatternParser(new PathPatternParser());
		}
	}


	static class DecodingUrlPathHelperConfig implements WebMvcConfigurer {

		@Override
		public void configurePathMatch(PathMatchConfigurer configurer) {
			UrlPathHelper helper = new UrlPathHelper();
			helper.setUrlDecode(true);
			configurer.setUrlPathHelper(helper);
		}
	}


	static class NonDecodingUrlPathHelperConfig implements WebMvcConfigurer {

		@Override
		public void configurePathMatch(PathMatchConfigurer configurer) {
			UrlPathHelper helper = new UrlPathHelper();
			helper.setUrlDecode(false);
			configurer.setUrlPathHelper(helper);
		}
	}


	@ControllerAdvice
	private static class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
	}

}

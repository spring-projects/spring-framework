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

package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ResourceHttpRequestHandler}.
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class ResourceHttpRequestHandlerTests {

	private ResourceHttpRequestHandler handler;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@BeforeEach
	public void setup() throws Exception {
		List<Resource> paths = new ArrayList<>(2);
		paths.add(new ClassPathResource("test/", getClass()));
		paths.add(new ClassPathResource("testalternatepath/", getClass()));
		paths.add(new ClassPathResource("META-INF/resources/webjars/"));

		TestServletContext servletContext = new TestServletContext();

		this.handler = new ResourceHttpRequestHandler();
		this.handler.setLocations(paths);
		this.handler.setCacheSeconds(3600);
		this.handler.setServletContext(servletContext);
		this.handler.afterPropertiesSet();

		this.request = new MockHttpServletRequest(servletContext, "GET", "");
		this.response = new MockHttpServletResponse();
	}


	@Test
	public void getResource() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/css");
		assertThat(this.response.getContentLength()).isEqualTo(17);
		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
		assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
	}

	@Test
	public void getResourceHttpHeader() throws Exception {
		this.request.setMethod("HEAD");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getContentType()).isEqualTo("text/css");
		assertThat(this.response.getContentLength()).isEqualTo(17);
		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
		assertThat(this.response.getContentAsByteArray().length).isEqualTo(0);
	}

	@Test
	public void getResourceHttpOptions() throws Exception {
		this.request.setMethod("OPTIONS");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getHeader("Allow")).isEqualTo("GET,HEAD,OPTIONS");
	}

	@Test
	public void getResourceNoCache() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.setCacheSeconds(0);
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("no-store");
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void getVersionedResource() throws Exception {
		VersionResourceResolver versionResolver = new VersionResourceResolver()
				.addFixedVersionStrategy("versionString", "/**");
		this.handler.setResourceResolvers(Arrays.asList(versionResolver, new PathResourceResolver()));
		this.handler.afterPropertiesSet();

		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "versionString/foo.css");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getHeader("ETag")).isEqualTo("\"versionString\"");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void getResourceHttp10BehaviorCache() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.setCacheSeconds(3600);
		this.handler.setUseExpiresHeader(true);
		this.handler.setUseCacheControlHeader(true);
		this.handler.setAlwaysMustRevalidate(true);
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600, must-revalidate");
		assertThat(this.response.getDateHeader("Expires") >= System.currentTimeMillis() - 1000 + (3600 * 1000)).isTrue();
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void getResourceHttp10BehaviorNoCache() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.setCacheSeconds(0);
		this.handler.setUseExpiresHeader(true);
		this.handler.setUseCacheControlNoStore(false);
		this.handler.setUseCacheControlHeader(true);
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getHeader("Pragma")).isEqualTo("no-cache");
		assertThat(this.response.getHeaderValues("Cache-Control")).hasSize(1);
		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("no-cache");
		assertThat(this.response.getDateHeader("Expires") <= System.currentTimeMillis()).isTrue();
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void getResourceWithHtmlMediaType() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.html");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/html");
		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.html") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void getResourceFromAlternatePath() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "baz.css");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/css");
		assertThat(this.response.getContentLength()).isEqualTo(17);
		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("testalternatepath/baz.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
		assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
	}

	@Test
	public void getResourceFromSubDirectory() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/foo.js");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/javascript");
		assertThat(this.response.getContentAsString()).isEqualTo("function foo() { console.log(\"hello world\"); }");
	}

	@Test
	public void getResourceFromSubDirectoryOfAlternatePath() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/baz.js");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/javascript");
		assertThat(this.response.getContentAsString()).isEqualTo("function foo() { console.log(\"hello world\"); }");
	}

	@Test  // SPR-13658
	public void getResourceWithRegisteredMediaType() throws Exception {
		ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
		factory.addMediaType("bar", new MediaType("foo", "bar"));
		factory.afterPropertiesSet();
		ContentNegotiationManager manager = factory.getObject();

		List<Resource> paths = Collections.singletonList(new ClassPathResource("test/", getClass()));
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setServletContext(new MockServletContext());
		handler.setLocations(paths);
		handler.setContentNegotiationManager(manager);
		handler.afterPropertiesSet();

		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.bar");
		handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("foo/bar");
		assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
	}

	@Test  // SPR-14577
	public void getMediaTypeWithFavorPathExtensionOff() throws Exception {
		ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
		factory.setFavorPathExtension(false);
		factory.afterPropertiesSet();
		ContentNegotiationManager manager = factory.getObject();

		List<Resource> paths = Collections.singletonList(new ClassPathResource("test/", getClass()));
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setServletContext(new MockServletContext());
		handler.setLocations(paths);
		handler.setContentNegotiationManager(manager);
		handler.afterPropertiesSet();

		this.request.addHeader("Accept", "application/json,text/plain,*/*");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.html");
		handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/html");
	}

	@Test  // SPR-14368
	public void getResourceWithMediaTypeResolvedThroughServletContext() throws Exception {

		MockServletContext servletContext = new MockServletContext() {
			@Override
			public String getMimeType(String filePath) {
				return "foo/bar";
			}
		};

		List<Resource> paths = Collections.singletonList(new ClassPathResource("test/", getClass()));
		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setServletContext(servletContext);
		handler.setLocations(paths);
		handler.afterPropertiesSet();

		MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "");
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		handler.handleRequest(request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("foo/bar");
		assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
	}

	@Test
	public void testInvalidPath() throws Exception {

		// Use mock ResourceResolver: i.e. we're only testing upfront validations...

		Resource resource = mock(Resource.class);
		given(resource.getFilename()).willThrow(new AssertionError("Resource should not be resolved"));
		given(resource.getInputStream()).willThrow(new AssertionError("Resource should not be resolved"));
		ResourceResolver resolver = mock(ResourceResolver.class);
		given(resolver.resolveResource(any(), any(), any(), any())).willReturn(resource);

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setLocations(Collections.singletonList(new ClassPathResource("test/", getClass())));
		handler.setResourceResolvers(Collections.singletonList(resolver));
		handler.setServletContext(new TestServletContext());
		handler.afterPropertiesSet();

		testInvalidPath("../testsecret/secret.txt", handler);
		testInvalidPath("test/../../testsecret/secret.txt", handler);
		testInvalidPath(":/../../testsecret/secret.txt", handler);

		Resource location = new UrlResource(getClass().getResource("./test/"));
		this.handler.setLocations(Collections.singletonList(location));
		Resource secretResource = new UrlResource(getClass().getResource("testsecret/secret.txt"));
		String secretPath = secretResource.getURL().getPath();

		testInvalidPath("file:" + secretPath, handler);
		testInvalidPath("/file:" + secretPath, handler);
		testInvalidPath("url:" + secretPath, handler);
		testInvalidPath("/url:" + secretPath, handler);
		testInvalidPath("/../.." + secretPath, handler);
		testInvalidPath("/%2E%2E/testsecret/secret.txt", handler);
		testInvalidPath("/%2E%2E/testsecret/secret.txt", handler);
		testInvalidPath("%2F%2F%2E%2E%2F%2F%2E%2E" + secretPath, handler);
	}

	private void testInvalidPath(String requestPath, ResourceHttpRequestHandler handler) throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, requestPath);
		this.response = new MockHttpServletResponse();
		handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
	}

	@Test
	public void resolvePathWithTraversal() throws Exception {
		for (HttpMethod method : HttpMethod.values()) {
			this.request = new MockHttpServletRequest("GET", "");
			this.response = new MockHttpServletResponse();
			testResolvePathWithTraversal(method);
		}
	}

	private void testResolvePathWithTraversal(HttpMethod httpMethod) throws Exception {
		this.request.setMethod(httpMethod.name());

		Resource location = new ClassPathResource("test/", getClass());
		this.handler.setLocations(Collections.singletonList(location));

		testResolvePathWithTraversal(location, "../testsecret/secret.txt");
		testResolvePathWithTraversal(location, "test/../../testsecret/secret.txt");
		testResolvePathWithTraversal(location, ":/../../testsecret/secret.txt");

		location = new UrlResource(getClass().getResource("./test/"));
		this.handler.setLocations(Collections.singletonList(location));
		Resource secretResource = new UrlResource(getClass().getResource("testsecret/secret.txt"));
		String secretPath = secretResource.getURL().getPath();

		testResolvePathWithTraversal(location, "file:" + secretPath);
		testResolvePathWithTraversal(location, "/file:" + secretPath);
		testResolvePathWithTraversal(location, "url:" + secretPath);
		testResolvePathWithTraversal(location, "/url:" + secretPath);
		testResolvePathWithTraversal(location, "/" + secretPath);
		testResolvePathWithTraversal(location, "////../.." + secretPath);
		testResolvePathWithTraversal(location, "/%2E%2E/testsecret/secret.txt");
		testResolvePathWithTraversal(location, "%2F%2F%2E%2E%2F%2Ftestsecret/secret.txt");
		testResolvePathWithTraversal(location, "/  " + secretPath);
	}

	private void testResolvePathWithTraversal(Resource location, String requestPath) throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, requestPath);
		this.response = new MockHttpServletResponse();
		this.handler.handleRequest(this.request, this.response);
		if (!location.createRelative(requestPath).exists() && !requestPath.contains(":")) {
			fail(requestPath + " doesn't actually exist as a relative path");
		}
		assertThat(this.response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
	}

	@Test
	public void ignoreInvalidEscapeSequence() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/%foo%/bar.txt");
		this.response = new MockHttpServletResponse();
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(404);
	}

	@Test
	public void processPath() {
		// Unchanged
		assertThat(this.handler.processPath("/foo/bar")).isSameAs("/foo/bar");
		assertThat(this.handler.processPath("foo/bar")).isSameAs("foo/bar");

		// leading whitespace control characters (00-1F)
		assertThat(this.handler.processPath("  /foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath((char) 1 + "/foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath((char) 31 + "/foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath("  foo/bar")).isEqualTo("foo/bar");
		assertThat(this.handler.processPath((char) 31 + "foo/bar")).isEqualTo("foo/bar");

		// leading control character 0x7F (DEL)
		assertThat(this.handler.processPath((char) 127 + "/foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath((char) 127 + "/foo/bar")).isEqualTo("/foo/bar");

		// leading control and '/' characters
		assertThat(this.handler.processPath("  /  foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath("  /  /  foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath("  // /// ////  foo/bar")).isEqualTo("/foo/bar");
		assertThat(this.handler.processPath((char) 1 + " / " + (char) 127 + " // foo/bar")).isEqualTo("/foo/bar");

		// root or empty path
		assertThat(this.handler.processPath("   ")).isEqualTo("");
		assertThat(this.handler.processPath("/")).isEqualTo("/");
		assertThat(this.handler.processPath("///")).isEqualTo("/");
		assertThat(this.handler.processPath("/ /   / ")).isEqualTo("/");
		assertThat(this.handler.processPath("\\/ \\/   \\/ ")).isEqualTo("/");

		// duplicate slash or backslash
		assertThat(this.handler.processPath("//foo/ /bar//baz//")).isEqualTo("/foo/ /bar/baz/");
		assertThat(this.handler.processPath("\\\\foo\\ \\bar\\\\baz\\\\")).isEqualTo("/foo/ /bar/baz/");
		assertThat(this.handler.processPath("foo\\\\/\\////bar")).isEqualTo("foo/bar");

	}

	@Test
	public void initAllowedLocations() {
		PathResourceResolver resolver = (PathResourceResolver) this.handler.getResourceResolvers().get(0);
		Resource[] locations = resolver.getAllowedLocations();

		assertThat(locations.length).isEqualTo(3);
		assertThat(((ClassPathResource) locations[0]).getPath()).isEqualTo("test/");
		assertThat(((ClassPathResource) locations[1]).getPath()).isEqualTo("testalternatepath/");
		assertThat(((ClassPathResource) locations[2]).getPath()).isEqualTo("META-INF/resources/webjars/");
	}

	@Test
	public void initAllowedLocationsWithExplicitConfiguration() throws Exception {
		ClassPathResource location1 = new ClassPathResource("test/", getClass());
		ClassPathResource location2 = new ClassPathResource("testalternatepath/", getClass());

		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(location1);

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setResourceResolvers(Collections.singletonList(pathResolver));
		handler.setServletContext(new MockServletContext());
		handler.setLocations(Arrays.asList(location1, location2));
		handler.afterPropertiesSet();

		Resource[] locations = pathResolver.getAllowedLocations();
		assertThat(locations.length).isEqualTo(1);
		assertThat(((ClassPathResource) locations[0]).getPath()).isEqualTo("test/");
	}

	@Test
	public void notModified() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css"));
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_MODIFIED);
	}

	@Test
	public void modified() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css") / 1000 * 1000 - 1);
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
	}

	@Test
	public void directory() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/");
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(404);
	}

	@Test
	public void directoryInJarFile() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "underscorejs/");
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(404);
	}

	@Test
	public void missingResourcePath() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "");
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(404);
	}

	@Test
	public void noPathWithinHandlerMappingAttribute() throws Exception {
		assertThatIllegalStateException().isThrownBy(() ->
				this.handler.handleRequest(this.request, this.response));
	}

	@Test
	public void unsupportedHttpMethod() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.request.setMethod("POST");
		assertThatExceptionOfType(HttpRequestMethodNotSupportedException.class).isThrownBy(() ->
				this.handler.handleRequest(this.request, this.response));
	}

	@Test
	public void resourceNotFound() throws Exception {
		for (HttpMethod method : HttpMethod.values()) {
			this.request = new MockHttpServletRequest("GET", "");
			this.response = new MockHttpServletResponse();
			resourceNotFound(method);
		}
	}

	private void resourceNotFound(HttpMethod httpMethod) throws Exception {
		this.request.setMethod(httpMethod.name());
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "not-there.css");
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
	}

	@Test
	public void partialContentByteRange() throws Exception {
		this.request.addHeader("Range", "bytes=0-1");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(2);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-1/10");
		assertThat(this.response.getContentAsString()).isEqualTo("So");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void partialContentByteRangeNoEnd() throws Exception {
		this.request.addHeader("Range", "bytes=9-");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(1);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
		assertThat(this.response.getContentAsString()).isEqualTo(".");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void partialContentByteRangeLargeEnd() throws Exception {
		this.request.addHeader("Range", "bytes=9-10000");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(1);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
		assertThat(this.response.getContentAsString()).isEqualTo(".");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void partialContentSuffixRange() throws Exception {
		this.request.addHeader("Range", "bytes=-1");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(1);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
		assertThat(this.response.getContentAsString()).isEqualTo(".");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void partialContentSuffixRangeLargeSuffix() throws Exception {
		this.request.addHeader("Range", "bytes=-11");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(10);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-9/10");
		assertThat(this.response.getContentAsString()).isEqualTo("Some text.");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void partialContentInvalidRangeHeader() throws Exception {
		this.request.addHeader("Range", "bytes= foo bar");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(416);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes */10");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges").size()).isEqualTo(1);
	}

	@Test
	public void partialContentMultipleByteRanges() throws Exception {
		this.request.addHeader("Range", "bytes=0-1, 4-5, 8-9");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType().startsWith("multipart/byteranges; boundary=")).isTrue();

		String boundary = "--" + this.response.getContentType().substring(31);

		String content = this.response.getContentAsString();
		String[] ranges = StringUtils.tokenizeToStringArray(content, "\r\n", false, true);

		assertThat(ranges[0]).isEqualTo(boundary);
		assertThat(ranges[1]).isEqualTo("Content-Type: text/plain");
		assertThat(ranges[2]).isEqualTo("Content-Range: bytes 0-1/10");
		assertThat(ranges[3]).isEqualTo("So");

		assertThat(ranges[4]).isEqualTo(boundary);
		assertThat(ranges[5]).isEqualTo("Content-Type: text/plain");
		assertThat(ranges[6]).isEqualTo("Content-Range: bytes 4-5/10");
		assertThat(ranges[7]).isEqualTo(" t");

		assertThat(ranges[8]).isEqualTo(boundary);
		assertThat(ranges[9]).isEqualTo("Content-Type: text/plain");
		assertThat(ranges[10]).isEqualTo("Content-Range: bytes 8-9/10");
		assertThat(ranges[11]).isEqualTo("t.");
	}

	@Test  // SPR-14005
	public void doOverwriteExistingCacheControlHeaders() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.response.setHeader("Cache-Control", "no-store");

		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
	}


	private long resourceLastModified(String resourceName) throws IOException {
		return new ClassPathResource(resourceName, getClass()).getFile().lastModified();
	}


	private static class TestServletContext extends MockServletContext {

		@Override
		public String getMimeType(String filePath) {
			if (filePath.endsWith(".css")) {
				return "text/css";
			}
			else if (filePath.endsWith(".js")) {
				return "text/javascript";
			}
			else {
				return super.getMimeType(filePath);
			}
		}
	}

}

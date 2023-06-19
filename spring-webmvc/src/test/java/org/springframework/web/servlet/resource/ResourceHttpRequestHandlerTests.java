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
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
 * @author Sam Brannen
 */
@ExtendWith(GzipSupport.class)
class ResourceHttpRequestHandlerTests {

	private final ClassPathResource testResource = new ClassPathResource("test/", getClass());
	private final ClassPathResource testAlternatePathResource = new ClassPathResource("testalternatepath/", getClass());
	private final ClassPathResource webjarsResource = new ClassPathResource("META-INF/resources/webjars/");

	private ResourceHttpRequestHandler handler;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@BeforeEach
	void setup() throws Exception {
		List<Resource> locations = List.of(
				this.testResource,
				this.testAlternatePathResource,
				this.webjarsResource);

		TestServletContext servletContext = new TestServletContext();

		this.handler = new ResourceHttpRequestHandler();
		this.handler.setLocations(locations);
		this.handler.setCacheSeconds(3600);
		this.handler.setServletContext(servletContext);
		this.handler.afterPropertiesSet();

		this.request = new MockHttpServletRequest(servletContext, "GET", "");
		this.response = new MockHttpServletResponse();
	}


	@Test
	void getResource() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/css");
		assertThat(this.response.getContentLength()).isEqualTo(17);
		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
		assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
	}

	@Test
	void getResourceHttpHeader() throws Exception {
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
		assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
		assertThat(this.response.getContentAsByteArray()).isEmpty();
	}

	@Test
	void getResourceHttpOptions() throws Exception {
		this.request.setMethod("OPTIONS");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(200);
		assertThat(this.response.getHeader("Allow")).isEqualTo("GET,HEAD,OPTIONS");
	}

	@Test
	void getResourceNoCache() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.setCacheSeconds(0);
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("no-store");
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
	}

	@Test
	void getVersionedResource() throws Exception {
		VersionResourceResolver versionResolver = new VersionResourceResolver()
				.addFixedVersionStrategy("versionString", "/**");
		this.handler.setResourceResolvers(List.of(versionResolver, new PathResourceResolver()));
		this.handler.afterPropertiesSet();

		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "versionString/foo.css");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getHeader("ETag")).isEqualTo("W/\"versionString\"");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
	}

	@Test
	@SuppressWarnings("deprecation")
	void getResourceHttp10BehaviorCache() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.setCacheSeconds(3600);
		this.handler.setUseExpiresHeader(true);
		this.handler.setUseCacheControlHeader(true);
		this.handler.setAlwaysMustRevalidate(true);
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600, must-revalidate");
		assertThat(this.response.getDateHeader("Expires")).isGreaterThanOrEqualTo(
				System.currentTimeMillis() - 1000 + (3600 * 1000));
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
	}

	@Test
	@SuppressWarnings("deprecation")
	void getResourceHttp10BehaviorNoCache() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.setCacheSeconds(0);
		this.handler.setUseExpiresHeader(true);
		this.handler.setUseCacheControlNoStore(false);
		this.handler.setUseCacheControlHeader(true);
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getHeader("Pragma")).isEqualTo("no-cache");
		assertThat(this.response.getHeaderValues("Cache-Control")).hasSize(1);
		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("no-cache");
		assertThat(this.response.getDateHeader("Expires")).isLessThanOrEqualTo(System.currentTimeMillis());
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
	}

	@Test
	void getResourceWithHtmlMediaType() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.html");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/html");
		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("test/foo.html") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
	}

	@Test
	void getResourceFromAlternatePath() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "baz.css");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/css");
		assertThat(this.response.getContentLength()).isEqualTo(17);
		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
		assertThat(this.response.containsHeader("Last-Modified")).isTrue();
		assertThat(this.response.getDateHeader("Last-Modified") / 1000).isEqualTo(resourceLastModified("testalternatepath/baz.css") / 1000);
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
		assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
	}

	@Test
	void getResourceFromSubDirectory() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/foo.js");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/javascript");
		assertThat(this.response.getContentAsString()).isEqualTo("function foo() { console.log(\"hello world\"); }");
	}

	@Test
	void getResourceFromSubDirectoryOfAlternatePath() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/baz.js");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/javascript");
		assertThat(this.response.getContentAsString()).isEqualTo("function foo() { console.log(\"hello world\"); }");
	}

	@Test  // SPR-13658
	@SuppressWarnings("deprecation")
	void getResourceWithRegisteredMediaType() throws Exception {
		ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
		factory.addMediaType("bar", new MediaType("foo", "bar"));
		factory.afterPropertiesSet();
		ContentNegotiationManager manager = factory.getObject();

		List<Resource> paths = List.of(new ClassPathResource("test/", getClass()));
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
	@SuppressWarnings("deprecation")
	void getMediaTypeWithFavorPathExtensionOff() throws Exception {
		ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
		factory.setFavorPathExtension(false);
		factory.afterPropertiesSet();
		ContentNegotiationManager manager = factory.getObject();

		List<Resource> paths = List.of(new ClassPathResource("test/", getClass()));
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
	void getResourceWithMediaTypeResolvedThroughServletContext() throws Exception {
		MockServletContext servletContext = new MockServletContext() {
			@Override
			public String getMimeType(String filePath) {
				return "foo/bar";
			}
		};

		List<Resource> paths = List.of(new ClassPathResource("test/", getClass()));
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

	@Test  // gh-27538, gh-27624
	void filterNonExistingLocations() throws Exception {
		List<Resource> inputLocations = List.of(
				new ClassPathResource("test/", getClass()),
				new ClassPathResource("testalternatepath/", getClass()),
				new ClassPathResource("nosuchpath/", getClass()));

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setServletContext(new MockServletContext());
		handler.setLocations(inputLocations);
		handler.setOptimizeLocations(true);
		handler.afterPropertiesSet();

		List<Resource> actual = handler.getLocations();
		assertThat(actual).hasSize(2);
		assertThat(actual.get(0).getURL().toString()).endsWith("test/");
		assertThat(actual.get(1).getURL().toString()).endsWith("testalternatepath/");
	}

	@Test
	void testInvalidPath() throws Exception {
		// Use mock ResourceResolver: i.e. we're only testing upfront validations...

		Resource resource = mock();
		given(resource.getFilename()).willThrow(new AssertionError("Resource should not be resolved"));
		given(resource.getInputStream()).willThrow(new AssertionError("Resource should not be resolved"));
		ResourceResolver resolver = mock();
		given(resolver.resolveResource(any(), any(), any(), any())).willReturn(resource);

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setLocations(List.of(new ClassPathResource("test/", getClass())));
		handler.setResourceResolvers(List.of(resolver));
		handler.setServletContext(new TestServletContext());
		handler.afterPropertiesSet();

		testInvalidPath("../testsecret/secret.txt", handler);
		testInvalidPath("test/../../testsecret/secret.txt", handler);
		testInvalidPath(":/../../testsecret/secret.txt", handler);

		Resource location = new UrlResource(getClass().getResource("./test/"));
		this.handler.setLocations(List.of(location));
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

	private void testInvalidPath(String requestPath, ResourceHttpRequestHandler handler) {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, requestPath);
		this.response = new MockHttpServletResponse();
		assertThatThrownBy(() -> handler.handleRequest(this.request, this.response))
				.isInstanceOf(NoResourceFoundException.class);
	}

	@Test
	void resolvePathWithTraversal() throws Exception {
		for (HttpMethod method : HttpMethod.values()) {
			this.request = new MockHttpServletRequest("GET", "");
			this.response = new MockHttpServletResponse();
			testResolvePathWithTraversal(method);
		}
	}

	private void testResolvePathWithTraversal(HttpMethod httpMethod) throws Exception {
		this.request.setMethod(httpMethod.name());

		Resource location = new ClassPathResource("test/", getClass());
		this.handler.setLocations(List.of(location));

		testResolvePathWithTraversal(location, "../testsecret/secret.txt");
		testResolvePathWithTraversal(location, "test/../../testsecret/secret.txt");
		testResolvePathWithTraversal(location, ":/../../testsecret/secret.txt");

		location = new UrlResource(getClass().getResource("./test/"));
		this.handler.setLocations(List.of(location));
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

	private void testResolvePathWithTraversal(Resource location, String requestPath) {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, requestPath);
		this.response = new MockHttpServletResponse();
		assertNotFound();
	}

	@Test
	void ignoreInvalidEscapeSequence() {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/%foo%/bar.txt");
		this.response = new MockHttpServletResponse();
		assertNotFound();
	}

	@Test
	void processPath() {
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
		assertThat(this.handler.processPath("   ")).isEmpty();
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
	void initAllowedLocations() {
		PathResourceResolver resolver = (PathResourceResolver) this.handler.getResourceResolvers().get(0);
		Resource[] locations = resolver.getAllowedLocations();

		assertThat(locations).containsExactly(this.testResource, this.testAlternatePathResource, this.webjarsResource);
	}

	@Test
	void initAllowedLocationsWithExplicitConfiguration() throws Exception {
		ClassPathResource location1 = new ClassPathResource("test/", getClass());
		ClassPathResource location2 = new ClassPathResource("testalternatepath/", getClass());

		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(location1);

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setResourceResolvers(List.of(pathResolver));
		handler.setServletContext(new MockServletContext());
		handler.setLocations(List.of(location1, location2));
		handler.afterPropertiesSet();

		assertThat(pathResolver.getAllowedLocations()).containsExactly(location1);
	}

	@Test
	void notModified() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css"));
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_MODIFIED);
	}

	@Test
	void modified() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.request.addHeader("If-Modified-Since", resourceLastModified("test/foo.css") / 1000 * 1000 - 1);
		this.handler.handleRequest(this.request, this.response);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
	}

	@Test
	void directory() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "js/");
		assertNotFound();
	}

	@Test
	void directoryInJarFile() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "underscorejs/");
		assertNotFound();
	}

	@Test
	void missingResourcePath() {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "");
		assertNotFound();
	}

	@Test
	void noPathWithinHandlerMappingAttribute() {
		assertThatIllegalStateException().isThrownBy(() ->
				this.handler.handleRequest(this.request, this.response));
	}

	@Test
	void unsupportedHttpMethod() {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.request.setMethod("POST");
		assertThatExceptionOfType(HttpRequestMethodNotSupportedException.class).isThrownBy(() ->
				this.handler.handleRequest(this.request, this.response));
	}

	@Test
	void testResourceNotFound() {
		for (HttpMethod method : HttpMethod.values()) {
			this.request = new MockHttpServletRequest("GET", "");
			this.response = new MockHttpServletResponse();
			testResourceNotFound(method);
		}
	}

	private void testResourceNotFound(HttpMethod httpMethod) {
		this.request.setMethod(httpMethod.name());
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "not-there.css");
		assertNotFound();
	}

	@Test
	void partialContentByteRange() throws Exception {
		this.request.addHeader("Range", "bytes=0-1");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(2);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-1/10");
		assertThat(this.response.getContentAsString()).isEqualTo("So");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
	}

	@Test
	void partialContentByteRangeNoEnd() throws Exception {
		this.request.addHeader("Range", "bytes=9-");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(1);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
		assertThat(this.response.getContentAsString()).isEqualTo(".");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
	}

	@Test
	void partialContentByteRangeLargeEnd() throws Exception {
		this.request.addHeader("Range", "bytes=9-10000");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(1);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
		assertThat(this.response.getContentAsString()).isEqualTo(".");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
	}

	@Test
	void partialContentSuffixRange() throws Exception {
		this.request.addHeader("Range", "bytes=-1");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(1);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 9-9/10");
		assertThat(this.response.getContentAsString()).isEqualTo(".");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
	}

	@Test
	void partialContentSuffixRangeLargeSuffix() throws Exception {
		this.request.addHeader("Range", "bytes=-11");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(10);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-9/10");
		assertThat(this.response.getContentAsString()).isEqualTo("Some text.");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
	}

	@Test
	void partialContentInvalidRangeHeader() throws Exception {
		this.request.addHeader("Range", "bytes= foo bar");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(416);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes */10");
		assertThat(this.response.getHeader("Accept-Ranges")).isEqualTo("bytes");
		assertThat(this.response.getHeaders("Accept-Ranges")).hasSize(1);
	}

	@Test
	void partialContentMultipleByteRanges() throws Exception {
		this.request.addHeader("Range", "bytes=0-1, 4-5, 8-9");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).startsWith("multipart/byteranges; boundary=");

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

	@Test  // gh-25976
	void partialContentByteRangeWithEncodedResource(GzipSupport.GzippedFiles gzippedFiles) throws Exception {
		String path = "js/foo.js";
		gzippedFiles.create(path);

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setResourceResolvers(List.of(new EncodedResourceResolver(), new PathResourceResolver()));
		handler.setLocations(List.of(new ClassPathResource("test/", getClass())));
		handler.setServletContext(new MockServletContext());
		handler.afterPropertiesSet();

		this.request.addHeader("Accept-Encoding", "gzip");
		this.request.addHeader("Range", "bytes=0-1");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, path);
		handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getHeaderNames()).containsExactlyInAnyOrder(
				"Content-Type", "Content-Length", "Content-Range", "Accept-Ranges",
				"Last-Modified", "Content-Encoding", "Vary");

		assertThat(this.response.getContentType()).isEqualTo("text/javascript");
		assertThat(this.response.getContentLength()).isEqualTo(2);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-1/66");
		assertThat(this.response.getHeaderValues("Accept-Ranges")).containsExactly("bytes");
		assertThat(this.response.getHeaderValues("Content-Encoding")).containsExactly("gzip");
		assertThat(this.response.getHeaderValues("Vary")).containsExactly("Accept-Encoding");
	}

	@Test  // gh-25976
	void partialContentWithHttpHead() throws Exception {
		this.request.setMethod("HEAD");
		this.request.addHeader("Range", "bytes=0-1");
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.txt");
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getStatus()).isEqualTo(206);
		assertThat(this.response.getContentType()).isEqualTo("text/plain");
		assertThat(this.response.getContentLength()).isEqualTo(2);
		assertThat(this.response.getHeader("Content-Range")).isEqualTo("bytes 0-1/10");
		assertThat(this.response.getHeaderValues("Accept-Ranges")).containsExactly("bytes");
	}

	@Test  // SPR-14005
	void doOverwriteExistingCacheControlHeaders() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.response.setHeader("Cache-Control", "no-store");

		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getHeader("Cache-Control")).isEqualTo("max-age=3600");
	}

	@Test
	void ignoreLastModified() throws Exception {
		this.request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "foo.css");
		this.handler.setUseLastModified(false);
		this.handler.handleRequest(this.request, this.response);

		assertThat(this.response.getContentType()).isEqualTo("text/css");
		assertThat(this.response.getContentLength()).isEqualTo(17);
		assertThat(this.response.containsHeader("Last-Modified")).isFalse();
		assertThat(this.response.getContentAsString()).isEqualTo("h1 { color:red; }");
	}

	@Test
	void servletContextRootValidation() {
		StaticWebApplicationContext context = new StaticWebApplicationContext() {
			@Override
			public Resource getResource(String location) {
				return new FileSystemResource("/");
			}
		};

		ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
		handler.setLocationValues(List.of("/"));
		handler.setApplicationContext(context);

		assertThatIllegalStateException().isThrownBy(handler::afterPropertiesSet)
				.withMessage("The String-based location \"/\" should be relative to the web application root but " +
						"resolved to a Resource of type: class org.springframework.core.io.FileSystemResource. " +
						"If this is intentional, please pass it as a pre-configured Resource via setLocations.");
	}


	private void assertNotFound() {
		assertThatThrownBy(() -> this.handler.handleRequest(this.request, this.response))
				.isInstanceOf(NoResourceFoundException.class);
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

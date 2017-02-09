/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.util.patterns.PathPatternParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for {@link CssLinkResourceTransformer}.
 * @author Rossen Stoyanchev
 */
public class CssLinkResourceTransformerTests {

	private ResourceTransformerChain transformerChain;


	@Before
	public void setUp() {
		ClassPathResource allowedLocation = new ClassPathResource("test/", getClass());
		ResourceWebHandler resourceHandler = new ResourceWebHandler();

		ResourceUrlProvider resourceUrlProvider = new ResourceUrlProvider();
		resourceUrlProvider.setHandlerMap(Collections.singletonMap("/static/**", resourceHandler));

		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));
		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(allowedLocation);
		List<ResourceResolver> resolvers = Arrays.asList(versionResolver, pathResolver);

		CssLinkResourceTransformer cssLinkResourceTransformer = new CssLinkResourceTransformer();
		cssLinkResourceTransformer.setResourceUrlProvider(resourceUrlProvider);
		List<ResourceTransformer> transformers = Collections.singletonList(cssLinkResourceTransformer);

		resourceHandler.setResourceResolvers(resolvers);
		resourceHandler.setResourceTransformers(transformers);
		resourceHandler.setLocations(Collections.singletonList(allowedLocation));
		ResourceResolverChain resolverChain = new DefaultResourceResolverChain(resolvers);
		this.transformerChain = new DefaultResourceTransformerChain(resolverChain, transformers);
	}


	@Test
	public void transform() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/static/main.css");
		Resource css = new ClassPathResource("test/main.css", getClass());

		String expected = "\n" +
				"@import url(\"/static/bar-11e16cf79faee7ac698c805cf28248d2.css\");\n" +
				"@import url('/static/bar-11e16cf79faee7ac698c805cf28248d2.css');\n" +
				"@import url(/static/bar-11e16cf79faee7ac698c805cf28248d2.css);\n\n" +
				"@import \"/static/foo-e36d2e05253c6c7085a91522ce43a0b4.css\";\n" +
				"@import '/static/foo-e36d2e05253c6c7085a91522ce43a0b4.css';\n\n" +
				"body { background: url(\"/static/images/image-f448cd1d5dba82b774f3202c878230b3.png\") }\n";

		StepVerifier.create(this.transformerChain.transform(exchange, css).cast(TransformedResource.class))
				.consumeNextWith(resource -> {
					String result = new String(resource.getByteArray(), StandardCharsets.UTF_8);
					result = StringUtils.deleteAny(result, "\r");
					assertEquals(expected, result);
				})
				.expectComplete().verify();
	}

	@Test
	public void transformNoLinks() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/static/foo.css");
		Resource expected = new ClassPathResource("test/foo.css", getClass());
		StepVerifier.create(this.transformerChain.transform(exchange, expected))
				.consumeNextWith(resource -> assertSame(expected, resource))
				.expectComplete().verify();
	}

	@Test
	public void transformExtLinksNotAllowed() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/static/external.css");
		ResourceResolverChain resolverChain = Mockito.mock(DefaultResourceResolverChain.class);
		ResourceTransformerChain transformerChain = new DefaultResourceTransformerChain(resolverChain,
				Collections.singletonList(new CssLinkResourceTransformer()));

		Resource externalCss = new ClassPathResource("test/external.css", getClass());
		StepVerifier.create(transformerChain.transform(exchange, externalCss).cast(TransformedResource.class))
				.consumeNextWith(resource -> {
					String expected = "@import url(\"http://example.org/fonts/css\");\n" +
							"body { background: url(\"file:///home/spring/image.png\") }\n" +
							"figure { background: url(\"//example.org/style.css\")}";
					String result = new String(resource.getByteArray(), StandardCharsets.UTF_8);
					result = StringUtils.deleteAny(result, "\r");
					assertEquals(expected, result);
				}).expectComplete().verify();

		Mockito.verify(resolverChain, Mockito.never())
				.resolveUrlPath("http://example.org/fonts/css", Collections.singletonList(externalCss));
		Mockito.verify(resolverChain, Mockito.never())
				.resolveUrlPath("file:///home/spring/image.png", Collections.singletonList(externalCss));
		Mockito.verify(resolverChain, Mockito.never())
				.resolveUrlPath("//example.org/style.css", Collections.singletonList(externalCss));
	}

	@Test
	public void transformWithNonCssResource() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/static/images/image.png");
		Resource expected = new ClassPathResource("test/images/image.png", getClass());
		StepVerifier.create(this.transformerChain.transform(exchange, expected))
				.expectNext(expected)
				.expectComplete().verify();
	}

	@Test
	public void transformWithGzippedResource() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/static/main.css");
		Resource original = new ClassPathResource("test/main.css", getClass());
		createTempCopy("main.css", "main.css.gz");
		GzipResourceResolver.GzippedResource expected = new GzipResourceResolver.GzippedResource(original);
		StepVerifier.create(this.transformerChain.transform(exchange, expected))
				.expectNext(expected)
				.expectComplete().verify();
	}

	private void createTempCopy(String filePath, String copyFilePath) throws IOException {
		Resource location = new ClassPathResource("test/", CssLinkResourceTransformerTests.class);
		Path original = Paths.get(location.getFile().getAbsolutePath(), filePath);
		Path copy = Paths.get(location.getFile().getAbsolutePath(), copyFilePath);
		Files.deleteIfExists(copy);
		Files.copy(original, copy);
		copy.toFile().deleteOnExit();
	}

	private ServerWebExchange createExchange(HttpMethod method, String url) {
		MockServerHttpRequest request = MockServerHttpRequest.method(method, url).build();
		ServerHttpResponse response = new MockServerHttpResponse();
		return new DefaultServerWebExchange(request, response);
	}

}

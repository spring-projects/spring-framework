/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for
 * {@link AppCacheManifestTransformer}.
 *
 * @author Brian Clozel
 */
public class AppCacheManifestTransformerTests {

	private AppCacheManifestTransformer transformer;

	private ResourceTransformerChain chain;

	private HttpServletRequest request;

	@Before
	public void setup() {
		this.transformer = new AppCacheManifestTransformer();
		this.chain = mock(ResourceTransformerChain.class);
		this.request = mock(HttpServletRequest.class);
	}

	@Test
	public void noTransformIfExtensionNoMatch() throws Exception {
		Resource resource = mock(Resource.class);
		given(resource.getFilename()).willReturn("foobar.file");
		given(this.chain.transform(this.request, resource)).willReturn(resource);

		Resource result = this.transformer.transform(this.request, resource, this.chain);
		assertEquals(resource, result);
	}

	@Test
	public void syntaxErrorInManifest() throws Exception {
		Resource resource = new ClassPathResource("test/error.manifest", getClass());
		given(this.chain.transform(this.request, resource)).willReturn(resource);

		Resource result = this.transformer.transform(this.request, resource, this.chain);
		assertEquals(resource, result);
	}

	@Test
	public void transformManifest() throws Exception {

		VersionResourceResolver versionResolver = new VersionResourceResolver();
		versionResolver.setStrategyMap(Collections.singletonMap("/**", new ContentVersionStrategy()));

		PathResourceResolver pathResolver = new PathResourceResolver();
		pathResolver.setAllowedLocations(new ClassPathResource("test/", getClass()));

		List<ResourceResolver> resolvers = Arrays.asList(versionResolver, pathResolver);
		ResourceResolverChain resolverChain = new DefaultResourceResolverChain(resolvers);

		List<ResourceTransformer> transformers = new ArrayList<>();
		transformers.add(new CssLinkResourceTransformer());
		this.chain = new DefaultResourceTransformerChain(resolverChain, transformers);

		Resource resource = new ClassPathResource("test/appcache.manifest", getClass());
		Resource result = this.transformer.transform(this.request, resource, this.chain);
		byte[] bytes = FileCopyUtils.copyToByteArray(result.getInputStream());
		String content = new String(bytes, "UTF-8");

		assertThat("should rewrite resource links", content,
				Matchers.containsString("foo-e36d2e05253c6c7085a91522ce43a0b4.css"));
		assertThat("should rewrite resource links", content,
				Matchers.containsString("bar-11e16cf79faee7ac698c805cf28248d2.css"));
		assertThat("should rewrite resource links", content,
				Matchers.containsString("js/bar-bd508c62235b832d960298ca6c0b7645.js"));

		assertThat("should not rewrite external resources", content,
				Matchers.containsString("//example.org/style.css"));
		assertThat("should not rewrite external resources", content,
				Matchers.containsString("http://example.org/image.png"));

		assertThat("should generate fingerprint", content,
				Matchers.containsString("# Hash: 4bf0338bcbeb0a5b3a4ec9ed8864107d"));
	}

}

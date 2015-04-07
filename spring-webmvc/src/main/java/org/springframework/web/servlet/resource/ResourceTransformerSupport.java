/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.web.servlet.resource;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;

/**
 * A base class for a {@code ResourceTransformer} with an optional helper method
 * for resolving public links within a transformed resource.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public abstract class ResourceTransformerSupport implements ResourceTransformer {

	private ResourceUrlProvider resourceUrlProvider;


	/**
	 * Configure a {@link ResourceUrlProvider} to use when resolving the public
	 * URL of links in a transformed resource (e.g. import links in a CSS file).
	 * This is required only for links expressed as full paths, i.e. including
	 * context and servlet path, and not for relative links.
	 *
	 * <p>By default this property is not set. In that case if a
	 * {@code ResourceUrlProvider} is needed an attempt is made to find the
	 * {@code ResourceUrlProvider} exposed through the
	 * {@link org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor
	 * ResourceUrlProviderExposingInterceptor} (configured by default in the MVC
	 * Java config and XML namespace). Therefore explicitly configuring this
	 * property should not be needed in most cases.
	 * @param resourceUrlProvider the URL provider to use
	 */
	public void setResourceUrlProvider(ResourceUrlProvider resourceUrlProvider) {
		this.resourceUrlProvider = resourceUrlProvider;
	}

	/**
	 * @return the configured {@code ResourceUrlProvider}.
	 */
	public ResourceUrlProvider getResourceUrlProvider() {
		return this.resourceUrlProvider;
	}


	/**
	 * A transformer can use this method when a resource being transformed
	 * contains links to other resources. Such links need to be replaced with the
	 * public facing link as determined by the resource resolver chain (e.g. the
	 * public URL may have a version inserted).
	 *
	 * @param resourcePath the path to a resource that needs to be re-written
	 * @param request the current request
	 * @param resource the resource being transformed
	 * @param transformerChain the transformer chain
	 * @return the resolved URL or null
	 */
	protected String resolveUrlPath(String resourcePath, HttpServletRequest request,
			Resource resource, ResourceTransformerChain transformerChain) {

		if (resourcePath.startsWith("/")) {
			// full resource path
			ResourceUrlProvider urlProvider = findResourceUrlProvider(request);
			return (urlProvider != null ? urlProvider.getForRequestUrl(request, resourcePath) : null);
		}
		else {
			// try resolving as relative path
			return transformerChain.getResolverChain().resolveUrlPath(resourcePath, Arrays.asList(resource));
		}
	}

	private ResourceUrlProvider findResourceUrlProvider(HttpServletRequest request) {
		if (this.resourceUrlProvider != null) {
			return this.resourceUrlProvider;
		}
		return (ResourceUrlProvider) request.getAttribute(
				ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR);
	}

}

/*
 * Copyright 2002-present the original author or authors.
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

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.io.Resource;

/**
 * Base class for {@link org.springframework.web.servlet.resource.ResourceResolver}
 * implementations. Provides consistent logging.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public abstract class AbstractResourceResolver implements ResourceResolver {

	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public @Nullable Resource resolveResource(@Nullable HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		return resolveResourceInternal(request, requestPath, locations, chain);
	}

	@Override
	public @Nullable String resolveUrlPath(String resourceUrlPath, List<? extends Resource> locations,
			ResourceResolverChain chain) {

		return resolveUrlPathInternal(resourceUrlPath, locations, chain);
	}


	protected abstract @Nullable Resource resolveResourceInternal(@Nullable HttpServletRequest request,
			String requestPath, List<? extends Resource> locations, ResourceResolverChain chain);

	protected abstract @Nullable String resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain);

}

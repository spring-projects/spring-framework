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

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;

/**
 * An abstraction for transforming the content of a resource.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 4.1
 */
@FunctionalInterface
public interface ResourceTransformer {

	/**
	 * Transform the given resource.
	 * @param request the current request
	 * @param resource the resource to transform
	 * @param transformerChain the chain of remaining transformers to delegate to
	 * @return the transformed resource (never {@code null})
	 * @throws IOException if the transformation fails
	 */
	Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
			throws IOException;

}

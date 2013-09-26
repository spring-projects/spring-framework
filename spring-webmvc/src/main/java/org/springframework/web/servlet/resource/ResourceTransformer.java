/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.resource;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;


/**
 * A strategy for transforming a resource.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface ResourceTransformer {

	/**
	 * Whether this transformer can transform the given resource.
	 *
	 * @param request the context request
	 * @param resource the candidate resource to transform
	 */
	boolean willTransform(HttpServletRequest request, Resource resource);

	/**
	 * Transform the given resource and return a new resource.
	 *
	 * @param resource the resource to transform
	 * @return the transformed resource, never {@code null}
	 *
	 * @throws IOException if the transformation fails
	 */
	Resource transform(Resource resource) throws IOException;

}

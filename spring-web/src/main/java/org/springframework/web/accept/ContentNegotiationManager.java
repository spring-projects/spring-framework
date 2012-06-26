/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.accept;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * This class is used to determine the requested {@linkplain MediaType media types}
 * in a request by delegating to a list of {@link ContentNegotiationStrategy} instances.
 *
 * <p>It may also be used to determine the extensions associated with a MediaType by
 * delegating to a list of {@link MediaTypeFileExtensionResolver} instances.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ContentNegotiationManager implements ContentNegotiationStrategy, MediaTypeFileExtensionResolver {

	private final List<ContentNegotiationStrategy> contentNegotiationStrategies =
			new ArrayList<ContentNegotiationStrategy>();

	private final Set<MediaTypeFileExtensionResolver> fileExtensionResolvers =
			new LinkedHashSet<MediaTypeFileExtensionResolver>();

	/**
	 * Create an instance with the given ContentNegotiationStrategy instances.
	 * <p>Each instance is checked to see if it is also an implementation of
	 * MediaTypeFileExtensionResolver, and if so it is registered as such.
	 */
	public ContentNegotiationManager(ContentNegotiationStrategy... strategies) {
		Assert.notEmpty(strategies, "At least one ContentNegotiationStrategy is expected");
		this.contentNegotiationStrategies.addAll(Arrays.asList(strategies));
		for (ContentNegotiationStrategy strategy : this.contentNegotiationStrategies) {
			if (strategy instanceof MediaTypeFileExtensionResolver) {
				this.fileExtensionResolvers.add((MediaTypeFileExtensionResolver) strategy);
			}
		}
	}

	/**
	 * Create an instance with a {@link HeaderContentNegotiationStrategy}.
	 */
	public ContentNegotiationManager() {
		this(new HeaderContentNegotiationStrategy());
	}

	/**
	 * Add MediaTypeFileExtensionResolver instances.
	 */
	public void addFileExtensionResolvers(MediaTypeFileExtensionResolver... resolvers) {
		this.fileExtensionResolvers.addAll(Arrays.asList(resolvers));
	}

	/**
	 * Delegate to all configured ContentNegotiationStrategy instances until one
	 * returns a non-empty list.
	 * @param request the current request
	 * @return the requested media types or an empty list, never {@code null}
	 * @throws HttpMediaTypeNotAcceptableException if the requested media types cannot be parsed
	 */
	public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest) throws HttpMediaTypeNotAcceptableException {
		for (ContentNegotiationStrategy strategy : this.contentNegotiationStrategies) {
			List<MediaType> mediaTypes = strategy.resolveMediaTypes(webRequest);
			if (!mediaTypes.isEmpty()) {
				return mediaTypes;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Delegate to all configured MediaTypeFileExtensionResolver instances and aggregate
	 * the list of all file extensions found.
	 */
	public List<String> resolveFileExtensions(MediaType mediaType) {
		Set<String> result = new LinkedHashSet<String>();
		for (MediaTypeFileExtensionResolver resolver : this.fileExtensionResolvers) {
			result.addAll(resolver.resolveFileExtensions(mediaType));
		}
		return new ArrayList<String>(result);
	}

	/**
	 * Delegate to all configured MediaTypeFileExtensionResolver instances and aggregate
	 * the list of all known file extensions.
	 */
	public List<String> getAllFileExtensions() {
		Set<String> result = new LinkedHashSet<String>();
		for (MediaTypeFileExtensionResolver resolver : this.fileExtensionResolvers) {
			result.addAll(resolver.getAllFileExtensions());
		}
		return new ArrayList<String>(result);
	}

}

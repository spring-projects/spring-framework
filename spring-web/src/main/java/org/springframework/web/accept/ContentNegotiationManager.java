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

package org.springframework.web.accept;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Central class to determine requested {@linkplain MediaType media types}
 * for a request. This is done by delegating to a list of configured
 * {@code ContentNegotiationStrategy} instances.
 *
 * <p>Also provides methods to look up file extensions for a media type.
 * This is done by delegating to the list of configured
 * {@code MediaTypeFileExtensionResolver} instances.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
public class ContentNegotiationManager implements ContentNegotiationStrategy, MediaTypeFileExtensionResolver {

	private final List<ContentNegotiationStrategy> strategies = new ArrayList<>();

	private final Set<MediaTypeFileExtensionResolver> resolvers = new LinkedHashSet<>();


	/**
	 * Create an instance with the given list of
	 * {@code ContentNegotiationStrategy} strategies each of which may also be
	 * an instance of {@code MediaTypeFileExtensionResolver}.
	 * @param strategies the strategies to use
	 */
	public ContentNegotiationManager(ContentNegotiationStrategy... strategies) {
		this(Arrays.asList(strategies));
	}

	/**
	 * A collection-based alternative to
	 * {@link #ContentNegotiationManager(ContentNegotiationStrategy...)}.
	 * @param strategies the strategies to use
	 * @since 3.2.2
	 */
	public ContentNegotiationManager(Collection<ContentNegotiationStrategy> strategies) {
		Assert.notEmpty(strategies, "At least one ContentNegotiationStrategy is expected");
		this.strategies.addAll(strategies);
		for (ContentNegotiationStrategy strategy : this.strategies) {
			if (strategy instanceof MediaTypeFileExtensionResolver mediaTypeFileExtensionResolver) {
				this.resolvers.add(mediaTypeFileExtensionResolver);
			}
		}
	}

	/**
	 * Create a default instance with a {@link HeaderContentNegotiationStrategy}.
	 */
	public ContentNegotiationManager() {
		this(new HeaderContentNegotiationStrategy());
	}


	/**
	 * Return the configured content negotiation strategies.
	 * @since 3.2.16
	 */
	public List<ContentNegotiationStrategy> getStrategies() {
		return this.strategies;
	}

	/**
	 * Find a {@code ContentNegotiationStrategy} of the given type.
	 * @param strategyType the strategy type
	 * @return the first matching strategy, or {@code null} if none
	 * @since 4.3
	 */
	@SuppressWarnings("unchecked")
	public <T extends ContentNegotiationStrategy> @Nullable T getStrategy(Class<T> strategyType) {
		for (ContentNegotiationStrategy strategy : getStrategies()) {
			if (strategyType.isInstance(strategy)) {
				return (T) strategy;
			}
		}
		return null;
	}

	/**
	 * Register more {@code MediaTypeFileExtensionResolver} instances in addition
	 * to those detected at construction.
	 * @param resolvers the resolvers to add
	 */
	public void addFileExtensionResolvers(MediaTypeFileExtensionResolver... resolvers) {
		Collections.addAll(this.resolvers, resolvers);
	}

	@Override
	public List<MediaType> resolveMediaTypes(NativeWebRequest request) throws HttpMediaTypeNotAcceptableException {
		for (ContentNegotiationStrategy strategy : this.strategies) {
			List<MediaType> mediaTypes = strategy.resolveMediaTypes(request);
			if (mediaTypes.equals(MEDIA_TYPE_ALL_LIST)) {
				continue;
			}
			return mediaTypes;
		}
		return MEDIA_TYPE_ALL_LIST;
	}

	@Override
	public List<String> resolveFileExtensions(MediaType mediaType) {
		return doResolveExtensions(resolver -> resolver.resolveFileExtensions(mediaType));
	}

	@Override
	public List<String> getAllFileExtensions() {
		return doResolveExtensions(MediaTypeFileExtensionResolver::getAllFileExtensions);
	}

	private List<String> doResolveExtensions(Function<MediaTypeFileExtensionResolver, List<String>> extractor) {
		List<String> result = null;
		for (MediaTypeFileExtensionResolver resolver : this.resolvers) {
			List<String> extensions = extractor.apply(resolver);
			if (CollectionUtils.isEmpty(extensions)) {
				continue;
			}
			result = (result != null ? result : new ArrayList<>(4));
			for (String extension : extensions) {
				if (!result.contains(extension)) {
					result.add(extension);
				}
			}
		}
		return (result != null ? result : Collections.emptyList());
	}

	/**
	 * Return all registered lookup key to media type mappings by iterating
	 * {@link MediaTypeFileExtensionResolver}s.
	 * @since 5.2.4
	 */
	public Map<String, MediaType> getMediaTypeMappings() {
		Map<String, MediaType> result = null;
		for (MediaTypeFileExtensionResolver resolver : this.resolvers) {
			if (resolver instanceof MappingMediaTypeFileExtensionResolver mappingResolver) {
				Map<String, MediaType> map = mappingResolver.getMediaTypes();
				if (CollectionUtils.isEmpty(map)) {
					continue;
				}
				result = (result != null ? result : new HashMap<>(4));
				result.putAll(map);
			}
		}
		return (result != null ? result : Collections.emptyMap());
	}

}

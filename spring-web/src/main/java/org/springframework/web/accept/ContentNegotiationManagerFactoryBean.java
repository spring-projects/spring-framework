/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Factory to create a {@code ContentNegotiationManager} and configure it with
 * {@link ContentNegotiationStrategy} instances.
 *
 * <p>This factory offers properties that in turn result in configuring the
 * underlying strategies. The table below shows the property names, their
 * default settings, as well as the strategies that they help to configure:
 *
 * <table>
 * <tr>
 * <th>Property Setter</th>
 * <th>Default Value</th>
 * <th>Underlying Strategy</th>
 * <th>Enabled Or Not</th>
 * </tr>
 * <tr>
 * <td>{@link #setFavorParameter favorParameter}</td>
 * <td>false</td>
 * <td>{@link ParameterContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * <tr>
 * <td>{@link #setIgnoreAcceptHeader ignoreAcceptHeader}</td>
 * <td>false</td>
 * <td>{@link HeaderContentNegotiationStrategy}</td>
 * <td>Enabled</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentType defaultContentType}</td>
 * <td>null</td>
 * <td>{@link FixedContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentTypeStrategy defaultContentTypeStrategy}</td>
 * <td>null</td>
 * <td>{@link ContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * </table>
 *
 * <p>Alternatively you can avoid use of the above convenience builder
 * methods and set the exact strategies to use via
 * {@link #setStrategies(List)}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.2
 */
public class ContentNegotiationManagerFactoryBean implements FactoryBean<ContentNegotiationManager>, InitializingBean {

	private @Nullable List<ContentNegotiationStrategy> strategies;


	private boolean favorParameter = false;

	private String parameterName = "format";

	private final Map<String, MediaType> mediaTypes = new HashMap<>();

	private @Nullable Boolean useRegisteredExtensionsOnly;

	private boolean ignoreAcceptHeader = false;

	private @Nullable ContentNegotiationStrategy defaultNegotiationStrategy;

	private @Nullable ContentNegotiationManager contentNegotiationManager;


	/**
	 * Set the exact list of strategies to use.
	 * <p><strong>Note:</strong> use of this method is mutually exclusive with
	 * use of all other setters in this class which customize a default, fixed
	 * set of strategies. See class level doc for more details.
	 * @param strategies the strategies to use
	 * @since 5.0
	 */
	public void setStrategies(@Nullable List<ContentNegotiationStrategy> strategies) {
		this.strategies = (strategies != null ? new ArrayList<>(strategies) : null);
	}

	/**
	 * Whether a request parameter ("format" by default) should be used to
	 * determine the requested media type. For this option to work you must
	 * register {@link #setMediaTypes media type mappings}.
	 * <p>By default this is set to {@code false}.
	 * @see #setParameterName
	 */
	public void setFavorParameter(boolean favorParameter) {
		this.favorParameter = favorParameter;
	}

	/**
	 * Set the query parameter name to use when {@link #setFavorParameter} is on.
	 * <p>The default parameter name is {@code "format"}.
	 */
	public void setParameterName(String parameterName) {
		Assert.notNull(parameterName, "parameterName is required");
		this.parameterName = parameterName;
	}

	/**
	 * Add a mapping from a key to a MediaType where the key are normalized to
	 * lowercase and may have been extracted from a path extension, a filename
	 * extension, or passed as a query parameter.
	 * <p>The {@link #setFavorParameter(boolean) parameter strategy} requires
	 * such mappings in order to work.
	 * <p><strong>Note:</strong> Mappings registered here may be accessed via
	 * {@link ContentNegotiationManager#getMediaTypeMappings()} and may be used
	 * not only in the parameter and path extension strategies. For example,
	 * with the Spring MVC config, for example, {@code @EnableWebMvc} or
	 * {@code <mvc:annotation-driven>}, the media type mappings are also plugged
	 * in to:
	 * <ul>
	 * <li>Determine the media type of static resources served with
	 * {@code ResourceHttpRequestHandler}.
	 * <li>Determine the media type of views rendered with
	 * {@code ContentNegotiatingViewResolver}.
	 * <li>List safe extensions for RFD attack detection (check the Spring
	 * Framework reference docs for details).
	 * </ul>
	 * @param mediaTypes media type mappings
	 * @see #addMediaType(String, MediaType)
	 * @see #addMediaTypes(Map)
	 */
	public void setMediaTypes(Properties mediaTypes) {
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			mediaTypes.forEach((key, value) ->
					addMediaType((String) key, MediaType.valueOf((String) value)));
		}
	}

	/**
	 * An alternative to {@link #setMediaTypes} for programmatic registrations.
	 */
	public void addMediaType(String key, MediaType mediaType) {
		this.mediaTypes.put(key.toLowerCase(Locale.ROOT), mediaType);
	}

	/**
	 * An alternative to {@link #setMediaTypes} for programmatic registrations.
	 */
	public void addMediaTypes(@Nullable Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			mediaTypes.forEach(this::addMediaType);
		}
	}

	/**
	 * When {@link #setFavorParameter(boolean)} is set, this property determines
	 * whether to use only registered {@code MediaType} mappings or to allow
	 * dynamic resolution, for example, via {@link MediaTypeFactory}.
	 * <p>By default, this is not set in which case dynamic resolution is on.
	 */
	public void setUseRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
		this.useRegisteredExtensionsOnly = useRegisteredExtensionsOnly;
	}

	private boolean useRegisteredExtensionsOnly() {
		return (this.useRegisteredExtensionsOnly != null && this.useRegisteredExtensionsOnly);
	}

	/**
	 * Whether to disable checking the 'Accept' request header.
	 * <p>By default this value is set to {@code false}.
	 */
	public void setIgnoreAcceptHeader(boolean ignoreAcceptHeader) {
		this.ignoreAcceptHeader = ignoreAcceptHeader;
	}

	/**
	 * Set the default content type to use when no content type is requested.
	 * <p>By default this is not set.
	 * @see #setDefaultContentTypeStrategy
	 */
	public void setDefaultContentType(MediaType contentType) {
		this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(contentType);
	}

	/**
	 * Set the default content types to use when no content type is requested.
	 * <p>By default this is not set.
	 * @since 5.0
	 * @see #setDefaultContentTypeStrategy
	 */
	public void setDefaultContentTypes(List<MediaType> contentTypes) {
		this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(contentTypes);
	}

	/**
	 * Set a custom {@link ContentNegotiationStrategy} to use to determine
	 * the content type to use when no content type is requested.
	 * <p>By default this is not set.
	 * @since 4.1.2
	 * @see #setDefaultContentType
	 */
	public void setDefaultContentTypeStrategy(ContentNegotiationStrategy strategy) {
		this.defaultNegotiationStrategy = strategy;
	}


	@Override
	public void afterPropertiesSet() {
		build();
	}

	/**
	 * Create and initialize a {@link ContentNegotiationManager} instance.
	 * @since 5.0
	 */
	public ContentNegotiationManager build() {
		List<ContentNegotiationStrategy> strategies = new ArrayList<>();

		if (this.strategies != null) {
			strategies.addAll(this.strategies);
		}
		else {
			if (this.favorParameter) {
				ParameterContentNegotiationStrategy strategy = new ParameterContentNegotiationStrategy(this.mediaTypes);
				strategy.setParameterName(this.parameterName);
				if (this.useRegisteredExtensionsOnly != null) {
					strategy.setUseRegisteredExtensionsOnly(this.useRegisteredExtensionsOnly);
				}
				else {
					strategy.setUseRegisteredExtensionsOnly(true);  // backwards compatibility
				}
				strategies.add(strategy);
			}
			if (!this.ignoreAcceptHeader) {
				strategies.add(new HeaderContentNegotiationStrategy());
			}
			if (this.defaultNegotiationStrategy != null) {
				strategies.add(this.defaultNegotiationStrategy);
			}
		}

		this.contentNegotiationManager = new ContentNegotiationManager(strategies);

		// Ensure media type mappings are available via ContentNegotiationManager#getMediaTypeMappings()
		// independent of path extension or parameter strategies.

		if (!CollectionUtils.isEmpty(this.mediaTypes) && !this.favorParameter) {
			this.contentNegotiationManager.addFileExtensionResolvers(
					new MappingMediaTypeFileExtensionResolver(this.mediaTypes));
		}

		return this.contentNegotiationManager;
	}


	@Override
	public @Nullable ContentNegotiationManager getObject() {
		return this.contentNegotiationManager;
	}

	@Override
	public Class<?> getObjectType() {
		return ContentNegotiationManager.class;
	}

}

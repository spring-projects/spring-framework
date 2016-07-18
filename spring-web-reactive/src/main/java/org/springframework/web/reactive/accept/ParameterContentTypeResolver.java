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

package org.springframework.web.reactive.accept;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@link RequestedContentTypeResolver} that extracts the media type lookup
 * key from a known query parameter named "format" by default.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ParameterContentTypeResolver extends AbstractMappingContentTypeResolver {

	private static final Log logger = LogFactory.getLog(ParameterContentTypeResolver.class);

	private String parameterName = "format";


	/**
	 * Create an instance with the given map of file extensions and media types.
	 */
	public ParameterContentTypeResolver(Map<String, MediaType> mediaTypes) {
		super(mediaTypes);
	}


	/**
	 * Set the name of the parameter to use to determine requested media types.
	 * <p>By default this is set to {@code "format"}.
	 */
	public void setParameterName(String parameterName) {
		Assert.notNull(parameterName, "parameterName is required");
		this.parameterName = parameterName;
	}

	public String getParameterName() {
		return this.parameterName;
	}


	@Override
	protected String extractKey(ServerWebExchange exchange) {
		return exchange.getRequest().getQueryParams().getFirst(getParameterName());
	}

	@Override
	protected void handleMatch(String mediaTypeKey, MediaType mediaType) {
		if (logger.isDebugEnabled()) {
			logger.debug("Requested media type is '" + mediaType +
					"' based on '" + getParameterName() + "'='" + mediaTypeKey + "'.");
		}
	}

	@Override
	protected MediaType handleNoMatch(String key) throws NotAcceptableStatusException {
		throw new NotAcceptableStatusException(getMediaTypes());
	}

}

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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * A ContentNegotiationStrategy that uses a request parameter to determine what
 * media types are requested. The default parameter name is {@code format}.
 * Its value is used to look up the media type in the map given to the constructor.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ParameterContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {

	private static final Log logger = LogFactory.getLog(ParameterContentNegotiationStrategy.class);

	private String parameterName = "format";

	/**
	 * Create an instance with the given extension-to-MediaType lookup.
	 * @throws IllegalArgumentException if a media type string cannot be parsed
	 */
	public ParameterContentNegotiationStrategy(Map<String, MediaType> mediaTypes) {
		super(mediaTypes);
		Assert.notEmpty(mediaTypes, "Cannot look up media types without any mappings");
	}

	/**
	 * Set the parameter name that can be used to determine the requested media type.
	 * <p>The default parameter name is {@code format}.
	 */
	public void setParameterName(String parameterName) {
		this.parameterName = parameterName;
	}

	@Override
	protected String getMediaTypeKey(NativeWebRequest webRequest) {
		return webRequest.getParameter(this.parameterName);
	}

	@Override
	protected void handleMatch(String mediaTypeKey, MediaType mediaType) {
		if (logger.isDebugEnabled()) {
			logger.debug("Requested media type is '" + mediaType + "' (based on parameter '" +
					this.parameterName + "'='" + mediaTypeKey + "')");
		}
	}

}

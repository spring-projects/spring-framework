/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Strategy that resolves the requested content type from a query parameter.
 * The default query parameter name is {@literal "format"}.
 *
 * <p>You can register static mappings between keys (i.e. the expected value of
 * the query parameter) and MediaType's via {@link #addMapping(String, MediaType)}.
 * As of 5.0 this strategy also supports dynamic lookups of keys via
 * {@link org.springframework.http.MediaTypeFactory#getMediaType}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ParameterContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {

	private String parameterName = "format";


	/**
	 * Create an instance with the given map of file extensions and media types.
	 */
	public ParameterContentNegotiationStrategy(Map<String, MediaType> mediaTypes) {
		super(mediaTypes);
	}


	/**
	 * Set the name of the parameter to use to determine requested media types.
	 * <p>By default this is set to {@code "format"}.
	 */
	public void setParameterName(String parameterName) {
		Assert.notNull(parameterName, "'parameterName' is required");
		this.parameterName = parameterName;
	}

	public String getParameterName() {
		return this.parameterName;
	}


	@Override
	protected @Nullable String getMediaTypeKey(NativeWebRequest request) {
		return request.getParameter(getParameterName());
	}

}

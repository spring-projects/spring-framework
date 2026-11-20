/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.support;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.multipart.MultipartHttpMessageConverter;

/**
 * Extension of {@link MultipartHttpMessageConverter},
 * adding support for XML, JSON, Smile, CBOR, Protobuf and Yaml based parts when
 * related libraries are present in the classpath.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.2
 * @deprecated since 7.1 in favor of {@link MultipartHttpMessageConverter}.
 */
@Deprecated(since = "7.1", forRemoval = true)
public class AllEncompassingFormHttpMessageConverter extends MultipartHttpMessageConverter {


	/**
	 * Create a new {@link AllEncompassingFormHttpMessageConverter} instance
	 * that will auto-detect part converters.
	 */
	public AllEncompassingFormHttpMessageConverter() {
		super(HttpMessageConverters.forClient().registerDefaults().build());
	}

	/**
	 * Create a new {@link AllEncompassingFormHttpMessageConverter} instance
	 * using the given message converters.
	 * @param converters the message converters to use for part conversion
	 * @since 7.0
	 */
	public AllEncompassingFormHttpMessageConverter(Iterable<HttpMessageConverter<?>> converters) {
		super(converters);
	}

}

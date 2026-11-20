/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.support;

import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;

/**
 * Extension of {@link org.springframework.http.converter.FormHttpMessageConverter},
 * adding support for XML, JSON, Smile, CBOR, Protobuf and Yaml based parts when
 * related libraries are present in the classpath.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.2
 */
public class AllEncompassingFormHttpMessageConverter extends FormHttpMessageConverter {


	/**
	 * Create a new {@link AllEncompassingFormHttpMessageConverter} instance
	 * that will auto-detect part converters.
	 */
	@SuppressWarnings("removal")
	public AllEncompassingFormHttpMessageConverter() {
		HttpMessageConverters.forClient().registerDefaults().build().forEach(this::addPartConverter);
	}

	/**
	 * Create a new {@link AllEncompassingFormHttpMessageConverter} instance
	 * using the given message converters.
	 * @param converters the message converters to use for part conversion
	 * @since 7.0
	 */
	public AllEncompassingFormHttpMessageConverter(Iterable<HttpMessageConverter<?>> converters) {
		converters.forEach(this::addPartConverter);
	}

}

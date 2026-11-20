/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.smile;

import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.smile.SmileMapper;

import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter}
 * that can read and write Smile data format ("binary JSON") using
 * <a href="https://github.com/FasterXML/jackson-dataformats-binary/tree/3.x/smile">
 * the dedicated Jackson 3.x extension</a>.
 *
 * <p>By default, this converter supports {@code "application/x-jackson-smile"}
 * media type. This can be overridden by setting the
 * {@link #setSupportedMediaTypes supportedMediaTypes} property.
 *
 * <p>The following hints entries are supported:
 * <ul>
 *     <li>A JSON view with a <code>"com.fasterxml.jackson.annotation.JsonView"</code>
 *         key and the class name of the JSON view as value.</li>
 *     <li>A filter provider with a <code>"tools.jackson.databind.ser.FilterProvider"</code>
 *         key and the filter provider class name as value.</li>
 * </ul>
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
public class JacksonSmileHttpMessageConverter extends AbstractJacksonHttpMessageConverter<SmileMapper> {

	private static final MediaType DEFAULT_SMILE_MIME_TYPES = new MediaType("application", "x-jackson-smile");

	/**
	 * Construct a new instance with a {@link SmileMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 */
	public JacksonSmileHttpMessageConverter() {
		super(SmileMapper.builder(), DEFAULT_SMILE_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link SmileMapper} customized
	 * with the {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 * @see SmileMapper#builder()
	 */
	public JacksonSmileHttpMessageConverter(SmileMapper.Builder builder) {
		super(builder, DEFAULT_SMILE_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link SmileMapper}.
	 * @see SmileMapper#builder()
	 */
	public JacksonSmileHttpMessageConverter(SmileMapper mapper) {
		super(mapper, DEFAULT_SMILE_MIME_TYPES);
	}

}

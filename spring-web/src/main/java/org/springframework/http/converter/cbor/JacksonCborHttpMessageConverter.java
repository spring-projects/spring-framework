/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.cbor;

import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.cbor.CBORMapper;

import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter
 * HttpMessageConverter} that can read and write the <a href="https://cbor.io/">CBOR</a>
 * data format using <a href="https://github.com/FasterXML/jackson-dataformats-binary/tree/3.x/cbor">
 * the dedicated Jackson 3.x extension</a>.
 *
 * <p>By default, this converter supports the {@link MediaType#APPLICATION_CBOR_VALUE}
 * media type. This can be overridden by setting the {@link #setSupportedMediaTypes
 * supportedMediaTypes} property.
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
public class JacksonCborHttpMessageConverter extends AbstractJacksonHttpMessageConverter<CBORMapper> {

	/**
	 * Construct a new instance with a {@link CBORMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 */
	public JacksonCborHttpMessageConverter() {
		super(CBORMapper.builder(), MediaType.APPLICATION_CBOR);
	}

	/**
	 * Construct a new instance with the provided {@link CBORMapper.Builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s
	 * found by {@link MapperBuilder#findModules(ClassLoader)}.
	 * @see CBORMapper#builder()
	 */
	public JacksonCborHttpMessageConverter(CBORMapper.Builder builder) {
		super(builder, MediaType.APPLICATION_CBOR);
	}

	/**
	 * Construct a new instance with the provided {@link CBORMapper}.
	 * @see CBORMapper#builder()
	 */
	public JacksonCborHttpMessageConverter(CBORMapper mapper) {
		super(mapper, MediaType.APPLICATION_CBOR);
	}

}

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.yaml;

import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.yaml.YAMLMapper;

import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter
 * HttpMessageConverter} that can read and write the <a href="https://yaml.io/">YAML</a>
 * data format using <a href="https://github.com/FasterXML/jackson-dataformats-text/tree/3.x/yaml">
 * the dedicated Jackson 3.x extension</a>.
 *
 * <p>By default, this converter supports the {@link MediaType#APPLICATION_YAML_VALUE}
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
public class JacksonYamlHttpMessageConverter extends AbstractJacksonHttpMessageConverter<YAMLMapper> {

	/**
	 * Construct a new instance with a {@link YAMLMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 */
	public JacksonYamlHttpMessageConverter() {
		super(YAMLMapper.builder(), MediaType.APPLICATION_YAML);
	}

	/**
	 * Construct a new instance with the provided {@link YAMLMapper}.
	 * @see YAMLMapper#builder()
	 */
	public JacksonYamlHttpMessageConverter(YAMLMapper.Builder builder) {
		super(builder, MediaType.APPLICATION_YAML);
	}

	/**
	 * Construct a new instance with the provided {@link YAMLMapper.Builder} customized
	 * with the {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 * @see YAMLMapper#builder()
	 */
	public JacksonYamlHttpMessageConverter(YAMLMapper mapper) {
		super(mapper, MediaType.APPLICATION_YAML);
	}

}

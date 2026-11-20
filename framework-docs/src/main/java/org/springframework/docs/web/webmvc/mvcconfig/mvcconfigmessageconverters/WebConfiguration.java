/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigmessageconverters;

import java.text.SimpleDateFormat;

import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SuppressWarnings("removal")
// tag::snippet[]
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

	@Override
	public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
		JsonMapper jsonMapper = JsonMapper.builder()
				.findAndAddModules()
				.enable(SerializationFeature.INDENT_OUTPUT)
				.defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
				.build();
		XmlMapper xmlMapper = XmlMapper.builder()
				.findAndAddModules()
				.defaultUseWrapper(false)
				.build();
		builder.withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper))
				.withXmlConverter(new JacksonXmlHttpMessageConverter(xmlMapper));
	}
}
// end::snippet[]

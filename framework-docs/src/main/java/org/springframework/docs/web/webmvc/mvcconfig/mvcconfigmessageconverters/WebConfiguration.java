/*
 * Copyright 2002-present the original author or authors.
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
		builder.jsonMessageConverter(new JacksonJsonHttpMessageConverter(jsonMapper))
				.xmlMessageConverter(new JacksonXmlHttpMessageConverter(xmlMapper));
	}
}
// end::snippet[]

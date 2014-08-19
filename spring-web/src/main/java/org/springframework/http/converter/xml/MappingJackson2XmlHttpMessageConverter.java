/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.http.converter.xml;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter}
 * that can read and write XML using <a href="https://github.com/FasterXML/jackson-dataformat-xml">
 * Jackson 2.x extension component for reading and writing XML encoded data</a>.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class MappingJackson2XmlHttpMessageConverter extends AbstractJackson2HttpMessageConverter {

	public MappingJackson2XmlHttpMessageConverter() {
		super(new XmlMapper(), new MediaType("application", "xml", DEFAULT_CHARSET));
	}

}

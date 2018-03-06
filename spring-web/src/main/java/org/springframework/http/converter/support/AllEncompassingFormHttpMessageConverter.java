/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.converter.support;

import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.ClassUtils;

/**
 * Extension of {@link org.springframework.http.converter.FormHttpMessageConverter},
 * adding support for XML and JSON-based parts.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
public class AllEncompassingFormHttpMessageConverter extends FormHttpMessageConverter {

	private static final boolean JAXB_2_PRESENT =
			ClassUtils.isPresent("javax.xml.bind.Binder",
					AllEncompassingFormHttpMessageConverter.class.getClassLoader());

	private static final boolean JACKSON_2_PRESENT =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					AllEncompassingFormHttpMessageConverter.class.getClassLoader()) &&
			ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
					AllEncompassingFormHttpMessageConverter.class.getClassLoader());

	private static final boolean JACKSON_2_XML_PRESENT =
			ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper",
					AllEncompassingFormHttpMessageConverter.class.getClassLoader());

	private static final boolean JACKSON_2_SMILE_PRESENT =
			ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory",
					AllEncompassingFormHttpMessageConverter.class.getClassLoader());

	private static final boolean GSON_PRESENT =
			ClassUtils.isPresent("com.google.gson.Gson",
					AllEncompassingFormHttpMessageConverter.class.getClassLoader());

	private static final boolean JSONB_PRESENT =
			ClassUtils.isPresent("javax.json.bind.Jsonb",
					AllEncompassingFormHttpMessageConverter.class.getClassLoader());


	public AllEncompassingFormHttpMessageConverter() {
		addPartConverter(new SourceHttpMessageConverter<>());

		if (JAXB_2_PRESENT && !JACKSON_2_XML_PRESENT) {
			addPartConverter(new Jaxb2RootElementHttpMessageConverter());
		}

		if (JACKSON_2_PRESENT) {
			addPartConverter(new MappingJackson2HttpMessageConverter());
		}
		else if (GSON_PRESENT) {
			addPartConverter(new GsonHttpMessageConverter());
		}
		else if (JSONB_PRESENT) {
			addPartConverter(new JsonbHttpMessageConverter());
		}

		if (JACKSON_2_XML_PRESENT) {
			addPartConverter(new MappingJackson2XmlHttpMessageConverter());
		}

		if (JACKSON_2_SMILE_PRESENT) {
			addPartConverter(new MappingJackson2SmileHttpMessageConverter());
		}
	}

}

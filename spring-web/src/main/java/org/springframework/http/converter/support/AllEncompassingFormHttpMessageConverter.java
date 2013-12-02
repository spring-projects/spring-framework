/*
 * Copyright 2002-2013 the original author or authors.
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.ClassUtils;

/**
 * Extension of {@link org.springframework.http.converter.FormHttpMessageConverter},
 * adding support for XML and JSON-based parts.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class AllEncompassingFormHttpMessageConverter extends FormHttpMessageConverter {

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder", AllEncompassingFormHttpMessageConverter.class.getClassLoader());

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", AllEncompassingFormHttpMessageConverter.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", AllEncompassingFormHttpMessageConverter.class.getClassLoader());

	private static final boolean jacksonPresent =
			ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", AllEncompassingFormHttpMessageConverter.class.getClassLoader()) &&
					ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator", AllEncompassingFormHttpMessageConverter.class.getClassLoader());


	public AllEncompassingFormHttpMessageConverter() {
		addPartConverter(new SourceHttpMessageConverter());
		if (jaxb2Present) {
			addPartConverter(new Jaxb2RootElementHttpMessageConverter());
		}
		if (jackson2Present) {
			addPartConverter(new MappingJackson2HttpMessageConverter());
		}
		else if (jacksonPresent) {
			addPartConverter(new MappingJacksonHttpMessageConverter());
		}
	}

}

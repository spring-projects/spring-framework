/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.http.converter.support;

import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.cbor.JacksonCborHttpMessageConverter;
import org.springframework.http.converter.cbor.KotlinSerializationCborHttpMessageConverter;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.protobuf.KotlinSerializationProtobufHttpMessageConverter;
import org.springframework.http.converter.smile.JacksonSmileHttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter;
import org.springframework.http.converter.yaml.MappingJackson2YamlHttpMessageConverter;
import org.springframework.util.ClassUtils;

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

	private static final boolean jaxb2Present;

	private static final boolean jacksonPresent;

	private static final boolean jackson2Present;

	private static final boolean jacksonXmlPresent;

	private static final boolean jackson2XmlPresent;

	private static final boolean jacksonSmilePresent;

	private static final boolean jackson2SmilePresent;

	private static final boolean jacksonCborPresent;

	private static final boolean jackson2CborPresent;

	private static final boolean jacksonYamlPresent;

	private static final boolean jackson2YamlPresent;

	private static final boolean gsonPresent;

	private static final boolean jsonbPresent;

	private static final boolean kotlinSerializationCborPresent;

	private static final boolean kotlinSerializationJsonPresent;

	private static final boolean kotlinSerializationProtobufPresent;

	static {
		ClassLoader classLoader = AllEncompassingFormHttpMessageConverter.class.getClassLoader();
		jaxb2Present = ClassUtils.isPresent("jakarta.xml.bind.Binder", classLoader);
		jacksonPresent = ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", classLoader);
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
						ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		jacksonXmlPresent = jacksonPresent && ClassUtils.isPresent("tools.jackson.dataformat.xml.XmlMapper", classLoader);
		jackson2XmlPresent = jackson2Present && ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
		jacksonSmilePresent = jacksonPresent && ClassUtils.isPresent("tools.jackson.dataformat.smile.SmileMapper", classLoader);
		jackson2SmilePresent = jackson2Present && ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
		jacksonCborPresent = jacksonPresent && ClassUtils.isPresent("tools.jackson.dataformat.cbor.CBORMapper", classLoader);
		jackson2CborPresent = jackson2Present && ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);
		jacksonYamlPresent = jacksonPresent && ClassUtils.isPresent("tools.jackson.dataformat.yaml.YAMLMapper", classLoader);
		jackson2YamlPresent = jackson2Present && ClassUtils.isPresent("com.fasterxml.jackson.dataformat.yaml.YAMLFactory", classLoader);
		gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
		jsonbPresent = ClassUtils.isPresent("jakarta.json.bind.Jsonb", classLoader);
		kotlinSerializationCborPresent = ClassUtils.isPresent("kotlinx.serialization.cbor.Cbor", classLoader);
		kotlinSerializationJsonPresent = ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
		kotlinSerializationProtobufPresent = ClassUtils.isPresent("kotlinx.serialization.protobuf.ProtoBuf", classLoader);
	}


	@SuppressWarnings("removal")
	public AllEncompassingFormHttpMessageConverter() {

		if (jaxb2Present && !jacksonXmlPresent && !jackson2XmlPresent) {
			addPartConverter(new Jaxb2RootElementHttpMessageConverter());
		}

		if (jacksonPresent) {
			addPartConverter(new JacksonJsonHttpMessageConverter());
		}
		else if (jackson2Present) {
			addPartConverter(new MappingJackson2HttpMessageConverter());
		}
		else if (gsonPresent) {
			addPartConverter(new GsonHttpMessageConverter());
		}
		else if (jsonbPresent) {
			addPartConverter(new JsonbHttpMessageConverter());
		}
		else if (kotlinSerializationJsonPresent) {
			addPartConverter(new KotlinSerializationJsonHttpMessageConverter());
		}

		if (jacksonXmlPresent) {
			addPartConverter(new JacksonXmlHttpMessageConverter());
		}
		else if (jackson2XmlPresent) {
			addPartConverter(new MappingJackson2XmlHttpMessageConverter());
		}

		if (jacksonSmilePresent) {
			addPartConverter(new JacksonSmileHttpMessageConverter());
		}
		else if (jackson2SmilePresent) {
			addPartConverter(new MappingJackson2SmileHttpMessageConverter());
		}

		if (jacksonCborPresent) {
			addPartConverter(new JacksonCborHttpMessageConverter());
		}
		else if (jackson2CborPresent) {
			addPartConverter(new MappingJackson2CborHttpMessageConverter());
		}
		else if (kotlinSerializationCborPresent) {
			addPartConverter(new KotlinSerializationCborHttpMessageConverter());
		}

		if (jacksonYamlPresent) {
			addPartConverter(new JacksonYamlHttpMessageConverter());
		}
		else if (jackson2YamlPresent) {
			addPartConverter(new MappingJackson2YamlHttpMessageConverter());
		}

		if (kotlinSerializationProtobufPresent) {
			addPartConverter(new KotlinSerializationProtobufHttpMessageConverter());
		}
	}

}

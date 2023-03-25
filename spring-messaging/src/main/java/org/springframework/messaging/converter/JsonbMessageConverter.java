/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.messaging.converter;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.springframework.util.Assert;

/**
 * Implementation of {@link MessageConverter} that can read and write JSON
 * using the <a href="https://javaee.github.io/jsonb-spec/">JSON Binding API</a>.
 *
 * @author Juergen Hoeller
 * @since 5.3
 * @see javax.json.bind.Jsonb
 * @see javax.json.bind.JsonbBuilder
 * @see #setJsonb
 */
public class JsonbMessageConverter extends AbstractJsonMessageConverter {

	private Jsonb jsonb;


	/**
	 * Construct a new {@code JsonbMessageConverter} with default configuration.
	 */
	public JsonbMessageConverter() {
		this.jsonb = JsonbBuilder.create();
	}

	/**
	 * Construct a new {@code JsonbMessageConverter} with the given configuration.
	 * @param config the {@code JsonbConfig} for the underlying delegate
	 */
	public JsonbMessageConverter(JsonbConfig config) {
		this.jsonb = JsonbBuilder.create(config);
	}

	/**
	 * Construct a new {@code JsonbMessageConverter} with the given delegate.
	 * @param jsonb the Jsonb instance to use
	 */
	public JsonbMessageConverter(Jsonb jsonb) {
		Assert.notNull(jsonb, "A Jsonb instance is required");
		this.jsonb = jsonb;
	}


	/**
	 * Set the {@code Jsonb} instance to use.
	 * If not set, a default {@code Jsonb} instance will be created.
	 * <p>Setting a custom-configured {@code Jsonb} is one way to take further
	 * control of the JSON serialization process.
	 * @see #JsonbMessageConverter(Jsonb)
	 * @see #JsonbMessageConverter(JsonbConfig)
	 * @see JsonbBuilder
	 */
	public void setJsonb(Jsonb jsonb) {
		Assert.notNull(jsonb, "A Jsonb instance is required");
		this.jsonb = jsonb;
	}

	/**
	 * Return the configured {@code Jsonb} instance for this converter.
	 */
	public Jsonb getJsonb() {
		return this.jsonb;
	}


	@Override
	protected Object fromJson(Reader reader, Type resolvedType) {
		return getJsonb().fromJson(reader, resolvedType);
	}

	@Override
	protected Object fromJson(String payload, Type resolvedType) {
		return getJsonb().fromJson(payload, resolvedType);
	}

	@Override
	protected void toJson(Object payload, Type resolvedType, Writer writer) {
		if (resolvedType instanceof ParameterizedType) {
			getJsonb().toJson(payload, resolvedType, writer);
		}
		else {
			getJsonb().toJson(payload, writer);
		}
	}

	@Override
	protected String toJson(Object payload, Type resolvedType) {
		if (resolvedType instanceof ParameterizedType) {
			return getJsonb().toJson(payload, resolvedType);
		}
		else {
			return getJsonb().toJson(payload);
		}
	}

}

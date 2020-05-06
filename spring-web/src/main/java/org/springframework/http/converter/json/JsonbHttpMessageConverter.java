/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.converter.json;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter}
 * that can read and write JSON using the
 * <a href="http://json-b.net/">JSON Binding API</a>.
 *
 * <p>This converter can be used to bind to typed beans or untyped {@code HashMap}s.
 * By default, it supports {@code application/json} and {@code application/*+json} with
 * {@code UTF-8} character set.
 *
 * @author Juergen Hoeller
 * @since 5.0
 * @see javax.json.bind.Jsonb
 * @see javax.json.bind.JsonbBuilder
 * @see #setJsonb
 */
public class JsonbHttpMessageConverter extends AbstractJsonHttpMessageConverter {

	private Jsonb jsonb;


	/**
	 * Construct a new {@code JsonbHttpMessageConverter} with default configuration.
	 */
	public JsonbHttpMessageConverter() {
		this(JsonbBuilder.create());
	}

	/**
	 * Construct a new {@code JsonbHttpMessageConverter} with the given configuration.
	 * @param config the {@code JsonbConfig} for the underlying delegate
	 */
	public JsonbHttpMessageConverter(JsonbConfig config) {
		this.jsonb = JsonbBuilder.create(config);
	}

	/**
	 * Construct a new {@code JsonbHttpMessageConverter} with the given delegate.
	 * @param jsonb the Jsonb instance to use
	 */
	public JsonbHttpMessageConverter(Jsonb jsonb) {
		Assert.notNull(jsonb, "A Jsonb instance is required");
		this.jsonb = jsonb;
	}


	/**
	 * Set the {@code Jsonb} instance to use.
	 * If not set, a default {@code Jsonb} instance will be created.
	 * <p>Setting a custom-configured {@code Jsonb} is one way to take further
	 * control of the JSON serialization process.
	 * @see #JsonbHttpMessageConverter(Jsonb)
	 * @see #JsonbHttpMessageConverter(JsonbConfig)
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
	protected Object readInternal(Type resolvedType, Reader reader) throws Exception {
		return getJsonb().fromJson(reader, resolvedType);
	}

	@Override
	protected void writeInternal(Object o, @Nullable Type type, Writer writer) throws Exception {
		if (type instanceof ParameterizedType) {
			getJsonb().toJson(o, type, writer);
		}
		else {
			getJsonb().toJson(o, writer);
		}
	}

}

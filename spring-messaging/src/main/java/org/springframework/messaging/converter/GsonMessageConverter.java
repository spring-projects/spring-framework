/*
 * Copyright 2002-2020 the original author or authors.
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

import com.google.gson.Gson;

import org.springframework.util.Assert;

/**
 * Implementation of {@link MessageConverter} that can read and write JSON
 * using <a href="https://code.google.com/p/google-gson/">Google Gson</a>.
 *
 * @author Juergen Hoeller
 * @since 5.3
 * @see com.google.gson.Gson
 * @see com.google.gson.GsonBuilder
 * @see #setGson
 */
public class GsonMessageConverter extends AbstractJsonMessageConverter {

	private Gson gson;


	/**
	 * Construct a new {@code GsonMessageConverter} with default configuration.
	 */
	public GsonMessageConverter() {
		this.gson = new Gson();
	}

	/**
	 * Construct a new {@code GsonMessageConverter} with the given delegate.
	 * @param gson the Gson instance to use
	 */
	public GsonMessageConverter(Gson gson) {
		Assert.notNull(gson, "A Gson instance is required");
		this.gson = gson;
	}


	/**
	 * Set the {@code Gson} instance to use.
	 * If not set, a default {@link Gson#Gson() Gson} instance will be used.
	 * <p>Setting a custom-configured {@code Gson} is one way to take further
	 * control of the JSON serialization process.
	 * @see #GsonMessageConverter(Gson)
	 */
	public void setGson(Gson gson) {
		Assert.notNull(gson, "A Gson instance is required");
		this.gson = gson;
	}

	/**
	 * Return the configured {@code Gson} instance for this converter.
	 */
	public Gson getGson() {
		return this.gson;
	}


	@Override
	protected Object fromJson(Reader reader, Type resolvedType) {
		return getGson().fromJson(reader, resolvedType);
	}

	@Override
	protected Object fromJson(String payload, Type resolvedType) {
		return getGson().fromJson(payload, resolvedType);
	}

	@Override
	protected void toJson(Object payload, Type resolvedType, Writer writer) {
		if (resolvedType instanceof ParameterizedType) {
			getGson().toJson(payload, resolvedType, writer);
		}
		else {
			getGson().toJson(payload, writer);
		}
	}

	@Override
	protected String toJson(Object payload, Type resolvedType) {
		if (resolvedType instanceof ParameterizedType) {
			return getGson().toJson(payload, resolvedType);
		}
		else {
			return getGson().toJson(payload);
		}
	}

}

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

package org.springframework.http.converter.json;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import com.google.gson.Gson;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter}
 * that can read and write JSON using the
 * <a href="https://code.google.com/p/google-gson/">Google Gson</a> library.
 *
 * <p>This converter can be used to bind to typed beans or untyped {@code HashMap}s.
 * By default, it supports {@code application/json} and {@code application/*+json} with
 * {@code UTF-8} character set.
 *
 * <p>Tested against Gson 2.6; compatible with Gson 2.0 and higher.
 *
 * @author Roy Clarkson
 * @author Juergen Hoeller
 * @since 4.1
 * @see com.google.gson.Gson
 * @see com.google.gson.GsonBuilder
 * @see #setGson
 */
public class GsonHttpMessageConverter extends AbstractJsonHttpMessageConverter {

	private Gson gson;


	/**
	 * Construct a new {@code GsonHttpMessageConverter} with default configuration.
	 */
	public GsonHttpMessageConverter() {
		this.gson = new Gson();
	}

	/**
	 * Construct a new {@code GsonHttpMessageConverter} with the given delegate.
	 * @param gson the Gson instance to use
	 * @since 5.0
	 */
	public GsonHttpMessageConverter(Gson gson) {
		Assert.notNull(gson, "A Gson instance is required");
		this.gson = gson;
	}


	/**
	 * Set the {@code Gson} instance to use.
	 * If not set, a default {@link Gson#Gson() Gson} instance will be used.
	 * <p>Setting a custom-configured {@code Gson} is one way to take further
	 * control of the JSON serialization process.
	 * @see #GsonHttpMessageConverter(Gson)
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
	protected Object readInternal(Type resolvedType, Reader reader) throws Exception {
		return getGson().fromJson(reader, resolvedType);
	}

	@Override
	protected void writeInternal(Object o, @Nullable Type type, Writer writer) throws Exception {
		if (type != null) {
			getGson().toJson(o, type, writer);
		}
		else {
			getGson().toJson(o, writer);
		}
	}

}

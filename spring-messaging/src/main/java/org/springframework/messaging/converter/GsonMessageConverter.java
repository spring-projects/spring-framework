/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.converter;

import java.io.IOException;
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
	protected void toJson(Object payload, Type resolvedType, Writer writer) throws IOException {
		if (resolvedType instanceof ParameterizedType) {
			getGson().toJson(payload, resolvedType, writer);
		}
		else {
			getGson().toJson(payload, writer);
		}
		writer.flush();
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

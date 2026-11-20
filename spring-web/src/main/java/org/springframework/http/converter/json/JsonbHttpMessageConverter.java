/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.json;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter}
 * that can read and write JSON using the
 * <a href="https://javaee.github.io/jsonb-spec/">JSON Binding API</a>.
 *
 * <p>This converter can be used to bind to typed beans or untyped {@code HashMap}s.
 * By default, it supports {@code application/json} and {@code application/*+json} with
 * {@code UTF-8} character set.
 *
 * @author Juergen Hoeller
 * @since 5.0
 * @see jakarta.json.bind.Jsonb
 * @see jakarta.json.bind.JsonbBuilder
 * @see #JsonbHttpMessageConverter(Jsonb)
 * @see #JsonbHttpMessageConverter(JsonbConfig)
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
	protected void writeInternal(Object object, @Nullable Type type, Writer writer) throws Exception {
		if (type instanceof ParameterizedType) {
			getJsonb().toJson(object, type, writer);
		}
		else {
			getJsonb().toJson(object, writer);
		}
	}

	@Override
	public boolean canWriteRepeatedly(Object o, @Nullable MediaType contentType) {
		return true;
	}

	@Override
	@SuppressWarnings("removal")
	protected boolean supportsRepeatableWrites(Object o) {
		return true;
	}

}

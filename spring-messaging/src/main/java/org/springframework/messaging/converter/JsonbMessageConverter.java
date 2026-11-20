/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.converter;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

import org.springframework.util.Assert;

/**
 * Implementation of {@link MessageConverter} that can read and write JSON
 * using the <a href="https://javaee.github.io/jsonb-spec/">JSON Binding API</a>.
 *
 * @author Juergen Hoeller
 * @since 5.3
 * @see jakarta.json.bind.Jsonb
 * @see jakarta.json.bind.JsonbBuilder
 * @see #JsonbMessageConverter(Jsonb)
 * @see #JsonbMessageConverter(JsonbConfig)
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

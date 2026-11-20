/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.json;

import java.lang.reflect.Type;
import java.util.Base64;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


/**
 * A simple utility class for obtaining a Google Gson 2.x {@link GsonBuilder}
 * which Base64-encodes {@code byte[]} properties when reading and writing JSON.
 *
 * @author Juergen Hoeller
 * @author Roy Clarkson
 * @since 4.1
 * @see GsonFactoryBean#setBase64EncodeByteArrays
 */
public abstract class GsonBuilderUtils {

	/**
	 * Obtain a {@link GsonBuilder} which Base64-encodes {@code byte[]}
	 * properties when reading and writing JSON.
	 * <p>A custom {@link com.google.gson.TypeAdapter} will be registered via
	 * {@link GsonBuilder#registerTypeHierarchyAdapter(Class, Object)} which
	 * serializes a {@code byte[]} property to and from a Base64-encoded String
	 * instead of a JSON array.
	 */
	public static GsonBuilder gsonBuilderWithBase64EncodedByteArrays() {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeHierarchyAdapter(byte[].class, new Base64TypeAdapter());
		return builder;
	}


	private static class Base64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

		@Override
		public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(Base64.getEncoder().encodeToString(src));
		}

		@Override
		public byte[] deserialize(JsonElement json, Type type, JsonDeserializationContext cxt) {
			return Base64.getDecoder().decode(json.getAsString());
		}
	}

}

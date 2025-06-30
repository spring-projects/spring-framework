/*
 * Copyright 2002-present the original author or authors.
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

/*
 * Copyright 2002-2014 the original author or authors.
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

import java.lang.reflect.Type;
import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;

/**
 * Custom Gson {@link TypeAdapter} for serialization or deserialization of
 * {@code byte[]}. By default Gson converts byte arrays to JSON arrays instead
 * of a Base64 encoded string. Use this type adapter with
 * {@link org.springframework.http.converter.json.GsonHttpMessageConverter
 * GsonHttpMessageConverter} to read and write Base64 encoded byte arrays.
 *
 * @author Roy Clarkson
 * @since 4.1
 * @see GsonBuilder#registerTypeHierarchyAdapter(Class, Object)
 */
final class GsonBase64ByteArrayJsonTypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private final Base64 base64 = new Base64();


	@Override
	public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
		String encoded = new String(this.base64.encode(src), DEFAULT_CHARSET);
		return new JsonPrimitive(encoded);
	}

	@Override
	public byte[] deserialize(JsonElement json, Type type, JsonDeserializationContext cxt) throws JsonParseException {
		return this.base64.decode(json.getAsString().getBytes(DEFAULT_CHARSET));
	}

}

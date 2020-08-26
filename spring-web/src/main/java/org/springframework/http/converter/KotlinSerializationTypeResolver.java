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

package org.springframework.http.converter;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializersKt;

/**
 * Caching resolver for kotlinx.serialization serializers.
 *
 * @author Andreas Ahlenstorf
 */
public class KotlinSerializationTypeResolver {

	private static final Map<Type, KSerializer<Object>> RESOLVER_CACHE = new LinkedHashMap<>();

	/**
	 * Tries to find a serializer that can marshall or unmarshall instances of the given type using
	 * kotlinx.serialization. If no serializer can be found, an exception is thrown.
	 * <p>
	 * Serializers are resolved using reflection. Therefore, resolved serializers are cached and cached results are
	 * returned on successive calls.
	 *
	 * @param type to find a serializer for.
	 * @return resolved serializer for the given type.
	 * @throws RuntimeException if no serializer supporting the given type can be found.
	 */
	public synchronized KSerializer<Object> resolve(Type type) {
		KSerializer<Object> cachedSerializer = RESOLVER_CACHE.get(type);
		if (cachedSerializer != null) {
			return cachedSerializer;
		}
		KSerializer<Object> resolvedSerializer = SerializersKt.serializer(type);
		RESOLVER_CACHE.put(type, resolvedSerializer);
		return resolvedSerializer;
	}
}

/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.converter.json;

/**
 * Holds an Object to be serialized via Jackson together with a serialization
 * view to be applied.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 *
 * @see com.fasterxml.jackson.annotation.JsonView
 */
public class MappingJacksonValue {

	private final Object value;

	private final Class<?> serializationView;


	/**
	 * Create a new instance.
	 * @param value the Object to be serialized
	 * @param serializationView the view to be applied
	 */
	public MappingJacksonValue(Object value, Class<?> serializationView) {
		this.value = value;
		this.serializationView = serializationView;
	}


	/**
	 * Return the value to be serialized.
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * Return the serialization view to use.
	 */
	public Class<?> getSerializationView() {
		return this.serializationView;
	}

}

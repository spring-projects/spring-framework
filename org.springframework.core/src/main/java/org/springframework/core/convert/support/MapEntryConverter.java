/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.core.convert.support;

/**
 * A helper for convertering map keys and values.
 *
 * @author Keith Donald
 * @since 3.0
 */
class MapEntryConverter {

	public static final MapEntryConverter NO_OP_INSTANCE = new MapEntryConverter(null, null);


	private final ConversionExecutor keyConverter;

	private final ConversionExecutor valueConverter;


	public MapEntryConverter(ConversionExecutor keyConverter, ConversionExecutor valueConverter) {
		this.keyConverter = keyConverter;
		this.valueConverter = valueConverter;
	}


	public Object convertKey(Object key) {
		if (this.keyConverter != null) {
			return this.keyConverter.execute(key);
		}
		else {
			return key;
		}
	}

	public Object convertValue(Object value) {
		if (this.valueConverter != null) {
			return this.valueConverter.execute(value);
		}
		else {
			return value;
		}
	}

}

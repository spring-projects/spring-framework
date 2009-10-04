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

import java.util.Collection;
import java.util.Map;

/**
 * Default implementation of a conversion service. Will automatically register <i>from string</i>
 * converters for a number of standard Java types like Class, Number, Boolean and so on.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public class DefaultConversionService extends GenericConversionService {

	/**
	 * Create a new default conversion service, installing the default converters.
	 */
	public DefaultConversionService() {
		addGenericConverter(Object[].class, Object[].class, new ArrayToArrayConverter(this));
		addGenericConverter(Object[].class, Collection.class, new ArrayToCollectionConverter(this));
		addGenericConverter(Object[].class, Map.class, new ArrayToMapConverter(this));
		addGenericConverter(Object[].class, Object.class, new ArrayToObjectConverter(this));
		addGenericConverter(Collection.class, Collection.class, new CollectionToCollectionConverter(this));
		addGenericConverter(Collection.class, Object[].class, new CollectionToArrayConverter(this));
		addGenericConverter(Collection.class, Map.class, new CollectionToMapConverter(this));
		addGenericConverter(Collection.class, Object.class, new CollectionToObjectConverter(this));
		addGenericConverter(Map.class, Map.class, new MapToMapConverter(this));
		addGenericConverter(Map.class, Object[].class, new MapToArrayConverter(this));
		addGenericConverter(Map.class, Collection.class, new MapToCollectionConverter(this));
		addGenericConverter(Map.class, Object.class, new MapToObjectConverter(this));
		addGenericConverter(Object.class, Object[].class, new ObjectToArrayConverter(this));
		addGenericConverter(Object.class, Collection.class, new ObjectToCollectionConverter(this));
		addGenericConverter(Object.class, Map.class, new ObjectToMapConverter(this));
		addConverter(new StringToBooleanConverter());
		addConverter(new StringToCharacterConverter());
		addConverter(new StringToLocaleConverter());
		addConverter(new NumberToCharacterConverter());
		addConverter(new ObjectToStringConverter());
		addConverterFactory(new StringToNumberConverterFactory());
		addConverterFactory(new StringToEnumConverterFactory());
		addConverterFactory(new NumberToNumberConverterFactory());
		addConverterFactory(new CharacterToNumberFactory());
	}

}

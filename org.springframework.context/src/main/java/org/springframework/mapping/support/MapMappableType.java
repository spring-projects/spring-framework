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
package org.springframework.mapping.support;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;

final class MapMappableType implements MappableType<Map<? extends Object, ? extends Object>> {

	public boolean isInstance(Object object) {
		return object instanceof Map<?, ?>;
	}

	public EvaluationContext getEvaluationContext(Map<? extends Object, ? extends Object> object,
			ConversionService conversionService) {
		StandardEvaluationContext context = new StandardEvaluationContext(object);
		context.setTypeConverter(new StandardTypeConverter(conversionService));
		context.addPropertyAccessor(new MapAccessor());
		return context;
	}

	public Set<String> getFieldNames(Map<? extends Object, ? extends Object> object) {
		Set<String> fieldNames = new LinkedHashSet<String>(object.size(), 1);
		for (Object key : object.keySet()) {
			if (key != null && key instanceof String) {
				fieldNames.add((String) key);
			}
		}
		return fieldNames;
	}

	public Map<String, Object> getNestedFields(String fieldName, Map<? extends Object, ? extends Object> object) {
		Map<String, Object> fields = new LinkedHashMap<String, Object>(object.size(), 1);
		for (Map.Entry<? extends Object, ? extends Object> entry : object.entrySet()) {
			Object key = entry.getKey();
			if (key != null && key instanceof String) {
				String name = (String) key;
				if (name.startsWith(fieldName + ".")) {
					fields.put(name.substring(fieldName.length() + 1), entry.getValue());					
				}
			}
		}
		return fields;
	}

}
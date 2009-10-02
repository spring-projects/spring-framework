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

import java.beans.PropertyDescriptor;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;

class BeanMappableType implements MappableType<Object> {

	public Set<String> getFields(Object object) {
		Set<String> fields = new LinkedHashSet<String>();
		PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(object.getClass());
		for (PropertyDescriptor descriptor : descriptors) {
			String propertyName = descriptor.getName();
			if (propertyName.equals("class")) {
				continue;
			}
			fields.add(descriptor.getName());
		}
		return fields;
	}

	public EvaluationContext getEvaluationContext(Object instance, ConversionService conversionService) {
		StandardEvaluationContext context = new StandardEvaluationContext(instance);
		context.setTypeConverter(new StandardTypeConverter(conversionService));
		return context;
	}

}
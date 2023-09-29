/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jdbc.core.test;

import it.fineco.services.common.jdbc.annotation.Column;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

/**
 * @author Giuseppe Milicia
 */
public class AddPropertyBeanPropertyRowMapper<T> extends BeanPropertyRowMapper<T> {

	@Override
	protected void initialize(Class<T> mappedClass) {
		super.initialize(mappedClass);

		for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(mappedClass)) {
			Method writeMethod = pd.getWriteMethod();
			if (writeMethod != null) {
				AddPropertyColumn columnAnnotation = writeMethod.getAnnotation(AddPropertyColumn.class);
				super.addProperty(columnAnnotation.name(), pd);
			}
		}
	}

}
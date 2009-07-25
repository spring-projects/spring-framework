/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.model.ui.support;

import org.springframework.context.MessageSource;
import org.springframework.core.convert.TypeConverter;
import org.springframework.model.ui.FieldModel;
import org.springframework.model.ui.config.Condition;
import org.springframework.model.ui.format.Formatter;

/**
 * A context that allows a FieldModel to access its external configuration.
 * @author Keith Donald
 * @since 3.0
 */
public interface FieldModelContext {

	MessageSource getMessageSource();

	TypeConverter getTypeConverter();

	Condition getEditableCondition();

	Condition getEnabledCondition();

	Condition getVisibleCondition();

	@SuppressWarnings("unchecked")
	Formatter getFormatter();

	@SuppressWarnings("unchecked")
	Formatter getElementFormatter();

	@SuppressWarnings("unchecked")
	Formatter getKeyFormatter();

	String getLabel();

	FieldModel getNested(String fieldName);

	FieldModel getListElement(int index);

	FieldModel getMapValue(Object key);

}
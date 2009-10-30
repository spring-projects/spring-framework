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

import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.style.StylerUtils;
import org.springframework.expression.Expression;

/**
 * A mapping between several source fields and a target field.
 * @author Keith Donald
 */
final class MultiFieldToFieldMapping implements SpelMapping {

	private final String sourceField;

	private final Expression targetField;

	@SuppressWarnings("unchecked")
	private final Converter targetFieldValueAssembler;

	private final Expression condition;

	public MultiFieldToFieldMapping(String sourceField, Expression targetField, Converter<? extends Map<?,?>, ?> assembler,
			Expression condition) {
		this.sourceField = sourceField;
		this.targetField = targetField;
		this.targetFieldValueAssembler = assembler;
		this.condition = condition;
	}

	public String getSourceField() {
		return this.sourceField + ".*";
	}

	public String getTargetField() {
		return this.targetField.getExpressionString();
	}

	public boolean mapsField(String field) {
		return field.startsWith(this.sourceField + ".");
	}

	@SuppressWarnings("unchecked")
	public void map(SpelMappingContext context) {
		if (!context.conditionHolds(this.condition)) {
			return;
		}
		try {
			Map<String, Object> nestedFields = context.getSourceNestedFields(this.sourceField);
			Object value = this.targetFieldValueAssembler.convert(nestedFields);
			context.setTargetFieldValue(this.targetField, value);
		} catch (Exception e) {
			context.addMappingFailure(e);
		}
	}

	public int hashCode() {
		return getSourceField().hashCode() + getTargetField().hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof MultiFieldToFieldMapping)) {
			return false;
		}
		MultiFieldToFieldMapping m = (MultiFieldToFieldMapping) o;
		return getSourceField().equals(m.getSourceField()) && getTargetField().equals(m.getTargetField());
	}

	public String toString() {
		return "[MultiFieldToFieldMapping<" + StylerUtils.style(getSourceField()) + " -> " + getTargetField() + ">]";
	}

}
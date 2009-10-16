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

import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.Expression;

/**
 * A mapping between a source field and a target field.
 * @author Keith Donald
 */
final class FieldToFieldMapping implements SpelMapping {

	private final Expression sourceField;

	private final Expression targetField;

	@SuppressWarnings("unchecked")
	private final Converter converter;

	public FieldToFieldMapping(Expression sourceField, Expression targetField, Converter<?, ?> converter) {
		this.sourceField = sourceField;
		this.targetField = targetField;
		this.converter = converter;
	}

	public String getSourceField() {
		return this.sourceField.getExpressionString();
	}

	public String getTargetField() {
		return this.targetField.getExpressionString();
	}

	@SuppressWarnings("unchecked")
	public void map(SpelMappingContext context) {
		try {
			Object value = context.getSourceFieldValue(this.sourceField);
			if (this.converter != null) {
				value = this.converter.convert(value);
			}
			context.setTargetFieldValue(this.targetField, value);
		} catch (Exception e) {
			context.addMappingFailure(e);
		}
	}

	public int hashCode() {
		return getSourceField().hashCode() + getTargetField().hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof FieldToFieldMapping)) {
			return false;
		}
		FieldToFieldMapping m = (FieldToFieldMapping) o;
		return getSourceField().equals(m.getSourceField()) && getTargetField().equals(m.getTargetField());
	}

	public String toString() {
		return "[FieldToFieldMapping<" + getSourceField() + " -> " + getTargetField() + ">]";
	}

}
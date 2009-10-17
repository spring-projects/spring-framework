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

import org.springframework.expression.Expression;
import org.springframework.mapping.Mapper;

/**
 * A mapping between a source field and several target fields.
 * @author Keith Donald
 */
final class FieldToMultiFieldMapping implements SpelMapping {

	private final Expression sourceField;

	@SuppressWarnings("unchecked")
	private final Mapper targetFieldMapper;

	private final Expression condition;

	public FieldToMultiFieldMapping(Expression sourceField, Mapper<?, ?> targetFieldMapper, Expression condition) {
		this.sourceField = sourceField;
		this.targetFieldMapper = targetFieldMapper;
		this.condition = condition;
	}

	public String getSourceField() {
		return this.sourceField.getExpressionString();
	}

	public boolean mapsField(String field) {
		return getSourceField().equals(field);
	}

	@SuppressWarnings("unchecked")
	public void map(SpelMappingContext context) {
		if (!context.conditionHolds(this.condition)) {
			return;
		}		
		try {
			Object value = context.getSourceFieldValue(this.sourceField);
			this.targetFieldMapper.map(value, context.getTarget());
		} catch (Exception e) {
			context.addMappingFailure(e);
		}
	}

	public int hashCode() {
		return getSourceField().hashCode() + this.targetFieldMapper.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof FieldToMultiFieldMapping)) {
			return false;
		}
		FieldToMultiFieldMapping m = (FieldToMultiFieldMapping) o;
		return getSourceField().equals(m.getSourceField()) && this.targetFieldMapper.equals(m.targetFieldMapper);
	}

	public String toString() {
		return "[FieldToFieldMapping<" + getSourceField() + " -> " + this.targetFieldMapper + ">]";
	}

}
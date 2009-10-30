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

import org.springframework.core.style.StylerUtils;
import org.springframework.expression.Expression;
import org.springframework.mapping.Mapper;

/**
 * A mapping between several source fields and one or more target fields.
 * @author Keith Donald
 */
final class FlexibleFieldMapping implements SpelMapping {

	private String[] fields;

	@SuppressWarnings("unchecked")
	private final Mapper flexibleFieldMapper;

	private Expression condition;
	
	public FlexibleFieldMapping(String[] fields, Mapper<?, ?> flexibleFieldMapper, Expression condition) {
		this.fields = fields;
		this.flexibleFieldMapper = flexibleFieldMapper;
		this.condition = condition;
	}

	public String[] getSourceFields() {
		return fields;
	}

	public boolean mapsField(String field) {
		for (String f : this.fields) {
			if (f.equals(field)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public void map(SpelMappingContext context) {
		if (!context.conditionHolds(this.condition)) {
			return;
		}		
		try {
			this.flexibleFieldMapper.map(context.getSource(), context.getTarget());
		} catch (Exception e) {
			context.addMappingFailure(e);
		}
	}

	public int hashCode() {
		return this.flexibleFieldMapper.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof FlexibleFieldMapping)) {
			return false;
		}
		FlexibleFieldMapping m = (FlexibleFieldMapping) o;
		return this.flexibleFieldMapper.equals(m.flexibleFieldMapper);
	}

	public String toString() {
		return "[FlexibleFieldMapping<" + StylerUtils.style(this.fields) + " -> " + this.flexibleFieldMapper + ">]";
	}

}
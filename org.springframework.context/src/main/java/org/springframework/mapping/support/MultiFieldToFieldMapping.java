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

import org.springframework.mapping.Mapper;

/**
 * A mapping between several source fields and a target field.
 * @author Keith Donald
 */
final class MultiFieldToFieldMapping implements SpelMapping {

	@SuppressWarnings("unchecked")
	private final Mapper multiFieldMapper;

	public MultiFieldToFieldMapping(Mapper<?, ?> multiFieldMapper) {
		this.multiFieldMapper = multiFieldMapper;
	}

	@SuppressWarnings("unchecked")
	public void map(SpelMappingContext context) {
		try {
			this.multiFieldMapper.map(context.getSource(), context.getTarget());
		} catch (Exception e) {
			context.addMappingFailure(e);
		}
	}

	public int hashCode() {
		return this.multiFieldMapper.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof MultiFieldToFieldMapping)) {
			return false;
		}
		MultiFieldToFieldMapping m = (MultiFieldToFieldMapping) o;
		return this.multiFieldMapper.equals(m.multiFieldMapper);
	}

	public String toString() {
		return "[MultiFieldToFieldMapping<" + this.multiFieldMapper + ">]";
	}

}
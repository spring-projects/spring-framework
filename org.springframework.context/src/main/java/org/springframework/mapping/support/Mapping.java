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

import java.util.Collection;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.support.ConverterFactoryGenericConverter;
import org.springframework.core.convert.support.ConverterGenericConverter;
import org.springframework.core.convert.support.GenericConverter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.mapping.MappingFailure;

/**
 * An individual mapping definition between two fields.
 * @author Keith Donald
 */
class Mapping implements MappingConfiguration {

	private Expression source;

	private Expression target;

	private GenericConverter converter;

	public Mapping(Expression source, Expression target) {
		this.source = source;
		this.target = target;
	}

	public String getSourceExpressionString() {
		return this.source.getExpressionString();
	}

	public String getTargetExpressionString() {
		return this.target.getExpressionString();
	}

	public MappingConfiguration setConverter(Converter<?, ?> converter) {
		return setGenericConverter(new ConverterGenericConverter(converter));
	}

	public MappingConfiguration setConverterFactory(ConverterFactory<?, ?> converter) {
		return setGenericConverter(new ConverterFactoryGenericConverter(converter));
	}

	public MappingConfiguration setGenericConverter(GenericConverter converter) {
		this.converter = converter;
		return this;
	}

	public void map(EvaluationContext sourceContext, EvaluationContext targetContext,
			Collection<MappingFailure> failures) {
		try {
			Object value = this.source.getValue(sourceContext);
			if (this.converter != null) {
				value = this.converter.convert(value, this.source.getValueTypeDescriptor(sourceContext), this.target
						.getValueTypeDescriptor(targetContext));
			}
			this.target.setValue(targetContext, value);
		} catch (Exception e) {
			failures.add(new MappingFailure(e));
		}
	}

	public int hashCode() {
		return getSourceExpressionString().hashCode() + getTargetExpressionString().hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof Mapping)) {
			return false;
		}
		Mapping m = (Mapping) o;
		return getSourceExpressionString().equals(m.getSourceExpressionString())
				&& getTargetExpressionString().equals(m.getTargetExpressionString());
	}

	public String toString() {
		return "[Mapping<" + getSourceExpressionString() + " -> " + getTargetExpressionString() + ">]";
	}

}
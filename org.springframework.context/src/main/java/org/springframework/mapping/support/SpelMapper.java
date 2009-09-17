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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.mapping.Mapper;
import org.springframework.mapping.MappingException;

/**
 * A generic object mapper implementation based on the Spring Expression Language (SpEL).
 * @author Keith Donald
 */
public class SpelMapper implements Mapper<Object, Object> {

	private static final MappableType MAP_MAPPABLE_TYPE = new MapMappableType();

	private static final MappableType BEAN_MAPPABLE_TYPE = new BeanMappableType();

	private static final ExpressionParser expressionParser = new SpelExpressionParser();

	private Set<Mapping> mappings = new LinkedHashSet<Mapping>();

	private boolean autoMappingEnabled = true;

	public void setAutoMappingEnabled(boolean autoMappingEnabled) {
		this.autoMappingEnabled = autoMappingEnabled;
	}

	public MappingConfiguration addMapping(String sourceExpression, String targetExpression) {
		Expression sourceExp;
		try {
			sourceExp = expressionParser.parseExpression(sourceExpression);
		} catch (ParseException e) {
			throw new IllegalArgumentException("The mapping source '" + sourceExpression
					+ "' is not a parseable value expression", e);
		}
		Expression targetExp;
		try {
			targetExp = expressionParser.parseExpression(targetExpression);
		} catch (ParseException e) {
			throw new IllegalArgumentException("The mapping target '" + sourceExpression
					+ "' is not a parseable property expression", e);
		}
		Mapping mapping = new Mapping(sourceExp, targetExp);
		if (mappings == null) {
			mappings = new LinkedHashSet<Mapping>();
		}
		mappings.add(mapping);
		return mapping;
	}

	public void map(Object source, Object target) throws MappingException {
		EvaluationContext sourceContext = getMappingContext(source);
		EvaluationContext targetContext = getMappingContext(target);
		for (Mapping mapping : mappings) {
			mapping.map(sourceContext, targetContext);
		}
		Set<Mapping> autoMappings = getAutoMappings(source);
		for (Mapping mapping : autoMappings) {
			mapping.map(sourceContext, targetContext);
		}
	}

	protected EvaluationContext getMappingContext(Object object) {
		if (object instanceof Map) {
			return MAP_MAPPABLE_TYPE.getMappingContext(object);
		} else {
			return BEAN_MAPPABLE_TYPE.getMappingContext(object);
		}
	}

	protected Set<String> getMappableFields(Object object) {
		if (object instanceof Map) {
			return MAP_MAPPABLE_TYPE.getMappableFields(object);
		} else {
			return BEAN_MAPPABLE_TYPE.getMappableFields(object);
		}
	}

	private Set<Mapping> getAutoMappings(Object source) {
		if (autoMappingEnabled) {
			Set<Mapping> autoMappings = new LinkedHashSet<Mapping>();
			Set<String> fields = getMappableFields(source);
			for (String field : fields) {
				if (!explicitlyMapped(field)) {
					Expression exp;
					try {
						exp = expressionParser.parseExpression(field);
					} catch (ParseException e) {
						throw new IllegalArgumentException("The mapping source '" + source
								+ "' is not a parseable value expression", e);
					}
					Mapping mapping = new Mapping(exp, exp);
					autoMappings.add(mapping);
				}
			}
			return autoMappings;
		} else {
			return Collections.emptySet();
		}
	}

	private boolean explicitlyMapped(String field) {
		for (Mapping mapping : mappings) {
			if (mapping.source.getExpressionString().equals(field)) {
				return true;
			}
		}
		return false;
	}

	private static class Mapping implements MappingConfiguration {

		private Expression source;

		private Expression target;

		private Converter converter;

		public Mapping(Expression source, Expression target) {
			this.source = source;
			this.target = target;
		}

		public MappingConfiguration setConverter(Converter converter) {
			this.converter = converter;
			return this;
		}

		public void map(EvaluationContext sourceContext, EvaluationContext targetContext) throws MappingException {
			try {
				Object value = source.getValue(sourceContext);
				if (converter != null) {
					value = converter.convert(value);
				}
				target.setValue(targetContext, value);
			} catch (Exception e) {
				throw new MappingException("Could not perform mapping", e);
			}
		}

		public int hashCode() {
			return source.getExpressionString().hashCode() + target.getExpressionString().hashCode();
		}

		public boolean equals(Object o) {
			if (!(o instanceof Mapping)) {
				return false;
			}
			Mapping m = (Mapping) o;
			return source.getExpressionString().equals(m.source.getExpressionString())
					&& target.getExpressionString().equals(m.source.getExpressionString());
		}

		public String toString() {
			return source.getExpressionString() + " -> " + target.getExpressionString();
		}

	}

	static class MapMappableType implements MappableType {

		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

		public Set<String> getMappableFields(Object instance) {
			Map map = (Map) instance;
			LinkedHashSet<String> fields = new LinkedHashSet<String>(map.size(), 1);
			for (Object key : map.keySet()) {
				fields.add(key.toString());
			}
			return fields;
		}

		public EvaluationContext getMappingContext(Object instance) {
			StandardEvaluationContext context = new StandardEvaluationContext(instance);
			context.addPropertyAccessor(new MapAccessor());
			return context;
		}

	}

	static class BeanMappableType implements MappableType {

		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

		public Set<String> getMappableFields(Object instance) {
			// TODO
			return null;
		}

		public EvaluationContext getMappingContext(Object instance) {
			return new StandardEvaluationContext(instance);
		}

	}

}

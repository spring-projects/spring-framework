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
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParserConfiguration;
import org.springframework.mapping.Mapper;
import org.springframework.util.Assert;

/**
 * A general-purpose object mapper implementation based on the Spring Expression Language (SpEL).
 * @author Keith Donald 
 */
final class SpelMapper implements Mapper<Object, Object> {

	private static final Log logger = LogFactory.getLog(SpelMapper.class);

	private final SpelExpressionParser sourceExpressionParser = new SpelExpressionParser();

	private final SpelExpressionParser targetExpressionParser = new SpelExpressionParser(
			SpelExpressionParserConfiguration.CreateObjectIfAttemptToReferenceNull
					| SpelExpressionParserConfiguration.GrowListsOnIndexBeyondSize);

	private final Set<SpelMapping> mappings = new LinkedHashSet<SpelMapping>();

	private MappableTypeFactory mappableTypeFactory = new DefaultMappableTypeFactory();

	private boolean autoMappingEnabled = true;

	private MappingConversionService conversionService = new MappingConversionService();

	public SpelMapper() {

	}

	public SpelMapper(Class sourceType, Class targetType) {
		// TODO - addMapping assertions based on specified sourceType and targetType
	}

	public void setAutoMappingEnabled(boolean autoMappingEnabled) {
		this.autoMappingEnabled = autoMappingEnabled;
	}

	public void setMappableTypeFactory(MappableTypeFactory mappableTypeFactory) {
		this.mappableTypeFactory = mappableTypeFactory;
	}

	public void setExcludedFields(String[] fields) {
		// TODO
	}
	
	public void addMapping(String sourceFieldExpression, String targetFieldExpression, Converter<?, ?> converter,
			String condition) {
		Expression sourceField = parseSourceField(sourceFieldExpression);
		Expression targetField = parseTargetField(targetFieldExpression);
		FieldToFieldMapping mapping = new FieldToFieldMapping(sourceField, targetField, converter,
				parseCondition(condition));
		this.mappings.add(mapping);
	}

	public void addMapping(String field, Mapper<?, ?> mapper, String condition) {
		this.mappings.add(new FieldToMultiFieldMapping(parseSourceField(field), mapper, parseCondition(condition)));
	}

	public void addMapping(String[] fields, Mapper<?, ?> mapper, String condition) {
		this.mappings.add(new MultiFieldToFieldMapping(fields, mapper, parseCondition(condition)));
	}

	public void addNestedMapper(Mapper<?, ?> nestedMapper, MappingTargetFactory targetFactory) {
		Class<?>[] typeInfo = getRequiredTypeInfo(nestedMapper);
		addNestedMapper(typeInfo[0], typeInfo[1], nestedMapper, targetFactory);
	}

	public void addNestedMapper(Class<?> sourceType, Class<?> targetType, Mapper<?, ?> nestedMapper,
			MappingTargetFactory targetFactory) {
		this.conversionService.addGenericConverter(sourceType, targetType, new MappingConverter(nestedMapper,
				targetFactory));
	}

	public ConverterRegistry getConverterRegistry() {
		return conversionService;
	}

	public Object map(Object source, Object target) {
		Assert.notNull(source, "The source to map from cannot be null");
		Assert.notNull(target, "The target to map to cannot be null");
		try {
			MappingContextHolder.push(source);
			EvaluationContext sourceContext = getEvaluationContext(source);
			EvaluationContext targetContext = getEvaluationContext(target);
			SpelMappingContext context = new SpelMappingContext(sourceContext, targetContext);
			for (SpelMapping mapping : this.mappings) {
				if (logger.isDebugEnabled()) {
					logger.debug(MappingContextHolder.getLevel() + mapping);
				}
				mapping.map(context);
			}
			Set<FieldToFieldMapping> autoMappings = getAutoMappings(sourceContext, targetContext);
			for (SpelMapping mapping : autoMappings) {
				if (logger.isDebugEnabled()) {
					logger.debug(MappingContextHolder.getLevel() + mapping + " (auto)");
				}
				mapping.map(context);
			}
			context.handleFailures();
			return target;
		} finally {
			MappingContextHolder.pop();
		}
	}

	// internal helpers

	private Expression parseSourceField(String sourceFieldExpression) {
		try {
			return sourceExpressionParser.parseExpression(sourceFieldExpression);
		} catch (ParseException e) {
			throw new IllegalArgumentException("The mapping source '" + sourceFieldExpression
					+ "' is not a parseable value expression", e);
		}
	}

	private Expression parseCondition(String condition) {
		if (condition == null) {
			return null;
		}
		try {
			return sourceExpressionParser.parseExpression(condition);
		} catch (ParseException e) {
			throw new IllegalArgumentException("The mapping condition '" + condition
					+ "' is not a parseable value expression", e);
		}
	}

	private Expression parseTargetField(String targetFieldExpression) {
		try {
			return targetExpressionParser.parseExpression(targetFieldExpression);
		} catch (ParseException e) {
			throw new IllegalArgumentException("The mapping target '" + targetFieldExpression
					+ "' is not a parseable property expression", e);
		}
	}

	private Class<?>[] getRequiredTypeInfo(Mapper<?, ?> mapper) {
		return GenericTypeResolver.resolveTypeArguments(mapper.getClass(), Mapper.class);
	}

	private EvaluationContext getEvaluationContext(Object object) {
		return mappableTypeFactory.getMappableType(object).getEvaluationContext(object, this.conversionService);
	}

	private Set<FieldToFieldMapping> getAutoMappings(EvaluationContext sourceContext, EvaluationContext targetContext) {
		if (this.autoMappingEnabled) {
			Set<FieldToFieldMapping> autoMappings = new LinkedHashSet<FieldToFieldMapping>();
			Set<String> sourceFields = getMappableFields(sourceContext.getRootObject().getValue());
			for (String field : sourceFields) {
				if (!explicitlyMapped(field)) {
					Expression sourceExpression;
					Expression targetExpression;
					try {
						sourceExpression = sourceExpressionParser.parseExpression(field);
					} catch (ParseException e) {
						throw new IllegalArgumentException("The mapping source '" + field
								+ "' is not a parseable value expression", e);
					}
					try {
						targetExpression = targetExpressionParser.parseExpression(field);
					} catch (ParseException e) {
						throw new IllegalArgumentException("The mapping target '" + field
								+ "' is not a parseable value expression", e);
					}
					try {
						if (targetExpression.isWritable(targetContext)) {
							autoMappings.add(new FieldToFieldMapping(sourceExpression, targetExpression, null, null));
						}
					} catch (EvaluationException e) {

					}
				}
			}
			return autoMappings;
		} else {
			return Collections.emptySet();
		}
	}

	private Set<String> getMappableFields(Object object) {
		return mappableTypeFactory.getMappableType(object).getFields(object);
	}

	private boolean explicitlyMapped(String field) {
		for (SpelMapping mapping : this.mappings) {
			if (mapping.mapsField(field)) {
				return true;
			}
		}
		return false;
	}

}

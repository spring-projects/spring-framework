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
import java.util.LinkedList;
import java.util.List;
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
import org.springframework.mapping.MappingException;
import org.springframework.mapping.MappingFailure;
import org.springframework.util.Assert;

/**
 * A general-purpose object mapper implementation based on the Spring Expression Language (SpEL).
 * @author Keith Donald 
 * @see #setAutoMappingEnabled(boolean)
 * @see #setMappableTypeFactory(MappableTypeFactory)
 * @see #addMapping(String)
 * @see #addMapping(String, String)
 * @see #getConverterRegistry()
 */
public class SpelMapper implements Mapper<Object, Object> {

	private static final Log logger = LogFactory.getLog(SpelMapper.class);

	private static final SpelExpressionParser sourceExpressionParser = new SpelExpressionParser();

	private static final SpelExpressionParser targetExpressionParser = new SpelExpressionParser(
			SpelExpressionParserConfiguration.CreateObjectIfAttemptToReferenceNull
					| SpelExpressionParserConfiguration.GrowListsOnIndexBeyondSize);

	private final Set<SpelMapping> mappings = new LinkedHashSet<SpelMapping>();

	private MappableTypeFactory mappableTypeFactory = new DefaultMappableTypeFactory();

	private boolean autoMappingEnabled = true;

	private MappingConversionService conversionService = new MappingConversionService();

	/**
	 * Sets whether "auto mapping" is enabled.
	 * When enabled, source and target fields with the same name will automatically be mapped unless an explicit mapping override has been registered.
	 * Set to false to require explicit registration of all source-to-target mapping rules.
	 * Default is enabled (true).
	 * @param autoMappingEnabled auto mapping status
	 */
	public void setAutoMappingEnabled(boolean autoMappingEnabled) {
		this.autoMappingEnabled = autoMappingEnabled;
	}

	/**
	 * Sets the factory for {@link MappableType mappable types} supported by this mapper.
	 * Default is {@link DefaultMappableTypeFactory}.
	 * @param mappableTypeFactory the mappableTypeFactory
	 */
	public void setMappableTypeFactory(MappableTypeFactory mappableTypeFactory) {
		this.mappableTypeFactory = mappableTypeFactory;
	}

	/**
	 * Register a field mapping.
	 * The source and target field expressions will be the same value.
	 * For example, calling <code>addMapping("order")</code> will register a mapping that maps between the <code>order</code> field on the source and the <code>order</code> field on the target.
	 * This is a convenience method for calling {@link #addMapping(String, String)} with the same source and target value..
	 * @param fieldExpression the field mapping expression
	 * @return this, for configuring additional field mapping options fluently
	 */
	public MappingConfiguration addMapping(String fieldExpression) {
		return addMapping(fieldExpression, fieldExpression);
	}

	/**
	 * Register a mapping between a source and target field.
	 * For example, calling <code>addMapping("order", "primaryOrder")</code> will register a mapping that maps between the <code>order</code> field on the source and the <code>primaryOrder</code> field on the target.
	 * @param sourceFieldExpression the source field mapping expression
	 * @param targetFieldExpression the target field mapping expression 
	 * @return this, for configuring additional field mapping options fluently
	 */
	public MappingConfiguration addMapping(String sourceFieldExpression, String targetFieldExpression) {
		Expression sourceExp;
		try {
			sourceExp = sourceExpressionParser.parseExpression(sourceFieldExpression);
		} catch (ParseException e) {
			throw new IllegalArgumentException("The mapping source '" + sourceFieldExpression
					+ "' is not a parseable value expression", e);
		}
		Expression targetExp;
		try {
			targetExp = targetExpressionParser.parseExpression(targetFieldExpression);
		} catch (ParseException e) {
			throw new IllegalArgumentException("The mapping target '" + targetFieldExpression
					+ "' is not a parseable property expression", e);
		}
		SpelMapping mapping = new SpelMapping(sourceExp, targetExp);
		this.mappings.add(mapping);
		return mapping;
	}

	/**
	 * Adds a Mapper that will map the fields of a nested sourceType/targetType pair.
	 * The source and target field types are determined by introspecting the parameterized types on the implementation's Mapper generic interface.
	 * The target instance that is mapped is constructed by a {@link DefaultMappingTargetFactory}.
	 * This method is a convenience method for {@link #addNestedMapper(Class, Class, Mapper)}.
	 * @param nestedMapper the nested mapper
	 */
	public void addNestedMapper(Mapper<?, ?> nestedMapper) {
		Class<?>[] typeInfo = getRequiredTypeInfo(nestedMapper);
		addNestedMapper(typeInfo[0], typeInfo[1], nestedMapper);
	}

	/**
	 * Adds a Mapper that will map the fields of a nested sourceType/targetType pair.
	 * The source and target field types are determined by introspecting the parameterized types on the implementation's Mapper generic interface.
	 * The target instance that is mapped is constructed by the provided {@link MappingTargetFactory}.
	 * This method is a convenience method for {@link #addNestedMapper(Class, Class, Mapper, MappingTargetFactory)}.
	 * @param nestedMapper the nested mapper
	 * @param targetFactory the nested mapper's target factory
	 */
	public void addNestedMapper(Mapper<?, ?> nestedMapper, MappingTargetFactory targetFactory) {
		Class<?>[] typeInfo = getRequiredTypeInfo(nestedMapper);
		addNestedMapper(typeInfo[0], typeInfo[1], nestedMapper, targetFactory);
	}

	/**
	 * Adds a Mapper that will map the fields of a nested sourceType/targetType pair.
	 * The target instance that is mapped is constructed by a {@link DefaultMappingTargetFactory}.
	 * @param sourceType the source nested object property type
	 * @param targetType the target nested object property type
	 * @param nestedMapper the nested mapper
	 */
	public void addNestedMapper(Class<?> sourceType, Class<?> targetType, Mapper<?, ?> nestedMapper) {
		this.conversionService.addGenericConverter(sourceType, targetType, new MappingConverter(nestedMapper));
	}

	/**
	 * Adds a Mapper that will map the fields of a nested sourceType/targetType pair.
	 * @param sourceType the source nested object property type
	 * @param targetType the target nested object property type
	 * @param nestedMapper the nested mapper
	 * @param targetFactory the nested mapper's target factory
	 */
	public void addNestedMapper(Class<?> sourceType, Class<?> targetType, Mapper<?, ?> nestedMapper,
			MappingTargetFactory targetFactory) {
		this.conversionService.addGenericConverter(sourceType, targetType, new MappingConverter(nestedMapper));
	}

	/**
	 * Return this mapper's internal converter registry.
	 * Allows for registration of simple type Converters in addition to MapperConverters that map entire nested object structures using a Mapper.
	 * To register the latter, consider using one of the {@link #addNestedMapper(Mapper) addNestedMapper} variants.
	 * @see Converter
	 * @see MappingConverter
	 */
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
			List<MappingFailure> failures = new LinkedList<MappingFailure>();
			for (SpelMapping mapping : this.mappings) {
				doMap(mapping, sourceContext, targetContext, failures);
			}
			Set<SpelMapping> autoMappings = getAutoMappings(sourceContext, targetContext);
			for (SpelMapping mapping : autoMappings) {
				doMap(mapping, sourceContext, targetContext, failures);
			}
			if (!failures.isEmpty()) {
				throw new MappingException(failures);
			}
			return target;
		} finally {
			MappingContextHolder.pop();
		}
	}

	// internal helpers

	private Class<?>[] getRequiredTypeInfo(Mapper<?, ?> mapper) {
		return GenericTypeResolver.resolveTypeArguments(mapper.getClass(), Mapper.class);
	}

	private EvaluationContext getEvaluationContext(Object object) {
		return mappableTypeFactory.getMappableType(object).getEvaluationContext(object, this.conversionService);
	}

	private void doMap(SpelMapping mapping, EvaluationContext sourceContext, EvaluationContext targetContext,
			List<MappingFailure> failures) {
		if (logger.isDebugEnabled()) {
			logger.debug(MappingContextHolder.getLevel() + mapping);
		}
		mapping.map(sourceContext, targetContext, failures);
	}

	private Set<SpelMapping> getAutoMappings(EvaluationContext sourceContext, EvaluationContext targetContext) {
		if (this.autoMappingEnabled) {
			Set<SpelMapping> autoMappings = new LinkedHashSet<SpelMapping>();
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
							autoMappings.add(new SpelMapping(sourceExpression, targetExpression));
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
			if (mapping.getSourceExpressionString().startsWith(field)) {
				return true;
			}
		}
		return false;
	}

}

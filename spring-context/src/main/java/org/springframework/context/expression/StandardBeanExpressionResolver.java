/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.expression;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanExpressionException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.SpringProperties;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Standard implementation of the
 * {@link org.springframework.beans.factory.config.BeanExpressionResolver}
 * interface, parsing and evaluating Spring EL using Spring's expression module.
 *
 * <p>All beans in the containing {@code BeanFactory} are made available as
 * predefined variables with their common bean name, including standard context
 * beans such as "environment", "systemProperties" and "systemEnvironment".
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see BeanExpressionContext#getBeanFactory()
 * @see org.springframework.expression.ExpressionParser
 * @see org.springframework.expression.spel.standard.SpelExpressionParser
 * @see org.springframework.expression.spel.support.StandardEvaluationContext
 */
public class StandardBeanExpressionResolver implements BeanExpressionResolver {

	/**
	 * System property to configure the maximum length for SpEL expressions: {@value}.
	 * <p>Can also be configured via the {@link SpringProperties} mechanism.
	 * @since 6.1.3
	 * @see SpelParserConfiguration#getMaximumExpressionLength()
	 */
	public static final String MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME = "spring.context.expression.maxLength";

	/** Default expression prefix: "#{". */
	public static final String DEFAULT_EXPRESSION_PREFIX = "#{";

	/** Default expression suffix: "}". */
	public static final String DEFAULT_EXPRESSION_SUFFIX = "}";


	private String expressionPrefix = DEFAULT_EXPRESSION_PREFIX;

	private String expressionSuffix = DEFAULT_EXPRESSION_SUFFIX;

	private ExpressionParser expressionParser;

	private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>(256);

	private final Map<BeanExpressionContext, StandardEvaluationContext> evaluationCache = new ConcurrentHashMap<>(8);

	private final ParserContext beanExpressionParserContext = new ParserContext() {
		@Override
		public boolean isTemplate() {
			return true;
		}
		@Override
		public String getExpressionPrefix() {
			return expressionPrefix;
		}
		@Override
		public String getExpressionSuffix() {
			return expressionSuffix;
		}
	};


	/**
	 * Create a new {@code StandardBeanExpressionResolver} with default settings.
	 * <p>As of Spring Framework 6.1.3, the maximum SpEL expression length can be
	 * configured via the {@link #MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME} property.
	 */
	public StandardBeanExpressionResolver() {
		this(null);
	}

	/**
	 * Create a new {@code StandardBeanExpressionResolver} with the given bean class loader,
	 * using it as the basis for expression compilation.
	 * <p>As of Spring Framework 6.1.3, the maximum SpEL expression length can be
	 * configured via the {@link #MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME} property.
	 * @param beanClassLoader the factory's bean class loader
	 */
	public StandardBeanExpressionResolver(@Nullable ClassLoader beanClassLoader) {
		SpelParserConfiguration parserConfig = new SpelParserConfiguration(
				null, beanClassLoader, false, false, Integer.MAX_VALUE, retrieveMaxExpressionLength());
		this.expressionParser = new SpelExpressionParser(parserConfig);
	}


	/**
	 * Set the prefix that an expression string starts with.
	 * The default is "#{".
	 * @see #DEFAULT_EXPRESSION_PREFIX
	 */
	public void setExpressionPrefix(String expressionPrefix) {
		Assert.hasText(expressionPrefix, "Expression prefix must not be empty");
		this.expressionPrefix = expressionPrefix;
	}

	/**
	 * Set the suffix that an expression string ends with.
	 * The default is "}".
	 * @see #DEFAULT_EXPRESSION_SUFFIX
	 */
	public void setExpressionSuffix(String expressionSuffix) {
		Assert.hasText(expressionSuffix, "Expression suffix must not be empty");
		this.expressionSuffix = expressionSuffix;
	}

	/**
	 * Specify the EL parser to use for expression parsing.
	 * <p>Default is a {@link org.springframework.expression.spel.standard.SpelExpressionParser},
	 * compatible with standard Unified EL style expression syntax.
	 */
	public void setExpressionParser(ExpressionParser expressionParser) {
		Assert.notNull(expressionParser, "ExpressionParser must not be null");
		this.expressionParser = expressionParser;
	}


	@Override
	@Nullable
	public Object evaluate(@Nullable String value, BeanExpressionContext beanExpressionContext) throws BeansException {
		if (!StringUtils.hasLength(value)) {
			return value;
		}
		try {
			Expression expr = this.expressionCache.computeIfAbsent(value, expression ->
					this.expressionParser.parseExpression(expression, this.beanExpressionParserContext));
			EvaluationContext evalContext = this.evaluationCache.computeIfAbsent(beanExpressionContext, bec -> {
					ConfigurableBeanFactory beanFactory = bec.getBeanFactory();
					StandardEvaluationContext sec = new StandardEvaluationContext(bec);
					sec.addPropertyAccessor(new BeanExpressionContextAccessor());
					sec.addPropertyAccessor(new BeanFactoryAccessor());
					sec.addPropertyAccessor(new MapAccessor());
					sec.addPropertyAccessor(new EnvironmentAccessor());
					sec.setBeanResolver(new BeanFactoryResolver(beanFactory));
					sec.setTypeLocator(new StandardTypeLocator(beanFactory.getBeanClassLoader()));
					sec.setTypeConverter(new StandardTypeConverter(() -> {
						ConversionService cs = beanFactory.getConversionService();
						return (cs != null ? cs : DefaultConversionService.getSharedInstance());
					}));
					customizeEvaluationContext(sec);
					return sec;
				});
			return expr.getValue(evalContext);
		}
		catch (Throwable ex) {
			throw new BeanExpressionException("Expression parsing failed", ex);
		}
	}

	/**
	 * Template method for customizing the expression evaluation context.
	 * <p>The default implementation is empty.
	 */
	protected void customizeEvaluationContext(StandardEvaluationContext evalContext) {
	}

	private static int retrieveMaxExpressionLength() {
		String value = SpringProperties.getProperty(MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME);
		if (!StringUtils.hasText(value)) {
			return SpelParserConfiguration.DEFAULT_MAX_EXPRESSION_LENGTH;
		}

		try {
			int maxLength = Integer.parseInt(value.trim());
			Assert.isTrue(maxLength > 0, () -> "Value [" + maxLength + "] for system property ["
					+ MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME + "] must be positive");
			return maxLength;
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Failed to parse value for system property [" +
					MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME + "]: " + ex.getMessage(), ex);
		}
	}

}

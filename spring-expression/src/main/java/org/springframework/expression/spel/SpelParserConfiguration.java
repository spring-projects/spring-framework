/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.expression.spel;

import java.util.Locale;

import org.springframework.core.SpringProperties;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Configuration object for the SpEL expression parser.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Andy Clement
 * @author Sam Brannen
 * @since 3.0
 * @see org.springframework.expression.spel.standard.SpelExpressionParser#SpelExpressionParser(SpelParserConfiguration)
 */
public class SpelParserConfiguration {

	/**
	 * Default maximum length permitted for a SpEL expression: {@value}.
	 * @since 5.2.24
	 */
	public static final int DEFAULT_MAX_EXPRESSION_LENGTH = 10_000;

	/**
	 * Default maximum number of operations permitted during SpEL expression evaluation: {@value}.
	 * @since 6.2.19
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 */
	public static final int DEFAULT_MAX_OPERATIONS = 10_000;

	/**
	 * System property to configure the default compiler mode for SpEL expression parsers: {@value}.
	 * <p><strong>NOTE</strong>: Instead of relying on a global default, applications
	 * and frameworks should ideally set an explicit custom value via the
	 * {@link #SpelParserConfiguration(SpelCompilerMode, ClassLoader, boolean, boolean, int, int, int)}
	 * constructor which provides complete configuration control and the ability
	 * to override global defaults per use case.
	 * <p>Can also be configured via the {@link SpringProperties} mechanism.
	 */
	public static final String SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME = "spring.expression.compiler.mode";

	/**
	 * System property to configure the default maximum number of operations permitted
	 * during SpEL expression evaluation: {@value}.
	 * <p><strong>NOTE</strong>: Instead of relying on a global default, applications
	 * and frameworks should ideally set an explicit custom value via the
	 * {@link #SpelParserConfiguration(SpelCompilerMode, ClassLoader, boolean, boolean, int, int, int)}
	 * constructor which provides complete configuration control and the ability
	 * to override global defaults per use case.
	 * <p>Can also be configured via the {@link SpringProperties} mechanism.
	 * @since 6.2.19
	 * @see #DEFAULT_MAX_OPERATIONS
	 */
	public static final String SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME = "spring.expression.maxOperations";


	private static final SpelCompilerMode defaultCompilerMode;

	static {
		String compilerMode = SpringProperties.getProperty(SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME);
		defaultCompilerMode = (compilerMode != null ?
				SpelCompilerMode.valueOf(compilerMode.toUpperCase(Locale.ROOT)) : SpelCompilerMode.OFF);
	}


	private final SpelCompilerMode compilerMode;

	@Nullable
	private final ClassLoader compilerClassLoader;

	private final boolean autoGrowNullReferences;

	private final boolean autoGrowCollections;

	private final int maximumAutoGrowSize;

	private final int maximumExpressionLength;

	private final int maximumOperations;


	/**
	 * Create a new {@code SpelParserConfiguration} instance with default settings.
	 * <p><strong>NOTE</strong>: Favor the
	 * {@link #SpelParserConfiguration(SpelCompilerMode, ClassLoader, boolean, boolean, int, int, int)}
	 * constructor for complete configuration control and the ability to override
	 * global defaults per use case.
	 * @see #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 */
	public SpelParserConfiguration() {
		this(null, null, false, false, Integer.MAX_VALUE);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * <p><strong>NOTE</strong>: Favor the
	 * {@link #SpelParserConfiguration(SpelCompilerMode, ClassLoader, boolean, boolean, int, int, int)}
	 * constructor for complete configuration control and the ability to override
	 * global defaults per use case.
	 * @param compilerMode the compiler mode that parsers using this configuration
	 * should use; or {@code null} to use the default mode
	 * @param compilerClassLoader the {@code ClassLoader} to use as the basis for
	 * expression compilation; or {@code null} to use the default {@code ClassLoader}
	 * @see #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 */
	public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader) {
		this(compilerMode, compilerClassLoader, false, false, Integer.MAX_VALUE);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * <p><strong>NOTE</strong>: Favor the
	 * {@link #SpelParserConfiguration(SpelCompilerMode, ClassLoader, boolean, boolean, int, int, int)}
	 * constructor for complete configuration control and the ability to override
	 * global defaults per use case.
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @see #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 */
	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections) {
		this(null, null, autoGrowNullReferences, autoGrowCollections, Integer.MAX_VALUE);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * <p><strong>NOTE</strong>: Favor the
	 * {@link #SpelParserConfiguration(SpelCompilerMode, ClassLoader, boolean, boolean, int, int, int)}
	 * constructor for complete configuration control and the ability to override
	 * global defaults per use case.
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size to which a collection can auto grow
	 * @see #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 */
	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {
		this(null, null, autoGrowNullReferences, autoGrowCollections, maximumAutoGrowSize);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * <p><strong>NOTE</strong>: Favor the
	 * {@link #SpelParserConfiguration(SpelCompilerMode, ClassLoader, boolean, boolean, int, int, int)}
	 * constructor for complete configuration control and the ability to override
	 * global defaults per use case.
	 * @param compilerMode the compiler mode that parsers using this configuration
	 * should use; or {@code null} to use the default mode
	 * @param compilerClassLoader the {@code ClassLoader} to use as the basis for
	 * expression compilation; or {@code null} to use the default {@code ClassLoader}
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size to which a collection can auto grow
	 * @see #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 */
	public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
			boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {

		this(compilerMode, compilerClassLoader, autoGrowNullReferences, autoGrowCollections,
				maximumAutoGrowSize, DEFAULT_MAX_EXPRESSION_LENGTH);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * <p><strong>NOTE</strong>: Favor the
	 * {@link #SpelParserConfiguration(SpelCompilerMode, ClassLoader, boolean, boolean, int, int, int)}
	 * constructor for complete configuration control and the ability to override
	 * global defaults per use case.
	 * @param compilerMode the compiler mode that parsers using this configuration
	 * should use; or {@code null} to use the default mode
	 * @param compilerClassLoader the {@code ClassLoader} to use as the basis for
	 * expression compilation; or {@code null} to use the default {@code ClassLoader}
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size to which a collection can auto grow
	 * @param maximumExpressionLength the maximum length of a SpEL expression;
	 * must be a positive number
	 * @since 5.2.25
	 * @see #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 */
	public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
			boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize, int maximumExpressionLength) {

		this((compilerMode != null ? compilerMode : defaultCompilerMode), compilerClassLoader, autoGrowNullReferences,
				autoGrowCollections, maximumAutoGrowSize, maximumExpressionLength, retrieveMaxOperations());
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * @param compilerMode the compiler mode that parsers using this configuration
	 * should use; must not be {@code null}
	 * @param compilerClassLoader the {@code ClassLoader} to use as the basis for
	 * expression compilation; or {@code null} to use the default {@code ClassLoader}
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size to which a collection can auto grow
	 * @param maximumExpressionLength the maximum length of a SpEL expression;
	 * must be a positive number
	 * @param maximumOperations the maximum number of operations permitted during
	 * SpEL expression evaluation; must be a positive number
	 * @since 6.2.19
	 */
	public SpelParserConfiguration(SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
			boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize, int maximumExpressionLength,
			int maximumOperations) {

		Assert.notNull(compilerMode, "'compilerMode' must not be null");
		Assert.isTrue(maximumExpressionLength > 0, "'maximumExpressionLength' must be a positive number");
		Assert.isTrue(maximumOperations > 0, "'maximumOperations' must be a positive number");

		this.compilerMode = compilerMode;
		this.compilerClassLoader = compilerClassLoader;
		this.autoGrowNullReferences = autoGrowNullReferences;
		this.autoGrowCollections = autoGrowCollections;
		this.maximumAutoGrowSize = maximumAutoGrowSize;
		this.maximumExpressionLength = maximumExpressionLength;
		this.maximumOperations = maximumOperations;
	}


	/**
	 * Return the compiler mode for parsers using this configuration object.
	 */
	public SpelCompilerMode getCompilerMode() {
		return this.compilerMode;
	}

	/**
	 * Return the {@code ClassLoader} to use as the basis for expression compilation.
	 */
	@Nullable
	public ClassLoader getCompilerClassLoader() {
		return this.compilerClassLoader;
	}

	/**
	 * Return {@code true} if {@code null} references should be automatically grown.
	 */
	public boolean isAutoGrowNullReferences() {
		return this.autoGrowNullReferences;
	}

	/**
	 * Return {@code true} if collections should be automatically grown.
	 */
	public boolean isAutoGrowCollections() {
		return this.autoGrowCollections;
	}

	/**
	 * Return the maximum size to which a collection can auto grow.
	 */
	public int getMaximumAutoGrowSize() {
		return this.maximumAutoGrowSize;
	}

	/**
	 * Return the maximum number of characters that a SpEL expression can contain.
	 * @since 5.2.25
	 */
	public int getMaximumExpressionLength() {
		return this.maximumExpressionLength;
	}

	/**
	 * Return the maximum number of operations permitted during SpEL expression
	 * evaluation.
	 * @since 6.2.19
	 */
	public int getMaximumOperations() {
		return this.maximumOperations;
	}


	private static int retrieveMaxOperations() {
		String value = SpringProperties.getProperty(SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME);
		if (!StringUtils.hasText(value)) {
			return DEFAULT_MAX_OPERATIONS;
		}

		try {
			int maxOperations = Integer.parseInt(value.trim());
			Assert.isTrue(maxOperations > 0, () -> "Value [" + maxOperations + "] for system property [" +
					SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME + "] must be positive");
			return maxOperations;
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Failed to parse value for system property [" +
					SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME + "]: " + ex.getMessage(), ex);
		}
	}

}

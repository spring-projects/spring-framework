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

import org.jspecify.annotations.Nullable;

import org.springframework.core.SpringProperties;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Configuration object for the SpEL expression parser.
 *
 * <p>Rather than using one of the numerous constructors in this class, it is
 * strongly recommended that you use the {@linkplain #builder() builder API} to
 * configure and create a {@code SpelParserConfiguration} instance, since the
 * builder only requires configuration of the properties that need to deviate from
 * their sensible defaults &mdash; or use {@link #withDefaults()} if none of those
 * defaults need to be overridden. Note that the constructors in this class have
 * been deprecated in favor of the builder as of Spring Framework 7.1.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Andy Clement
 * @author Sam Brannen
 * @since 3.0
 * @see org.springframework.expression.spel.standard.SpelExpressionParser#SpelExpressionParser(SpelParserConfiguration)
 * @see #withDefaults()
 * @see #builder()
 */
public class SpelParserConfiguration {

	/**
	 * Default maximum size to which a collection or array can automatically grow: {@value}.
	 * <p>Aligned with the default auto-grow limit used for Spring's data binding
	 * support (see {@code DataBinder.DEFAULT_AUTO_GROW_COLLECTION_LIMIT}), for
	 * consistency between SpEL and data binding.
	 * @since 7.1
	 */
	public static final int DEFAULT_MAX_AUTO_GROW_SIZE = 256;

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
	 * Default maximum number of bits permitted in the result of a
	 * {@link java.math.BigDecimal} or {@link java.math.BigInteger} power operation
	 * within a SpEL expression: {@value}.
	 * <p>Approximately equivalent to a decimal number with 300,000 digits.
	 * @since 7.0.9
	 * @see #SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME
	 */
	public static final int DEFAULT_MAX_BIG_POWER_BITS = 1_000_000;

	/**
	 * Default maximum nesting depth permitted within a SpEL expression: {@value}.
	 * <p>This limit guards against deeply nested constructs (for example, nested
	 * inline lists or maps, parenthesized expressions, ternary or Elvis expressions,
	 * or chained unary operators) that could otherwise drive SpEL's recursive-descent
	 * parser to exhaust the current thread's call stack.
	 * <p><strong>NOTE</strong>: This limit improves diagnostics for the common case
	 * by converting what would otherwise be an opaque {@link StackOverflowError}
	 * into a descriptive {@link SpelParseException}, but it is <em>not</em> a
	 * guaranteed defense against {@code StackOverflowError} under every possible
	 * JVM thread stack size configuration. The amount of stack space consumed per
	 * level of nesting depends on the JVM, its current JIT compilation state, and
	 * the platform; consequently, this default may not suffice on threads configured
	 * with a substantially reduced stack size (for example, via a reduced {@code -Xss}
	 * setting, as is sometimes done in high-concurrency deployments to support large
	 * thread pools). Applications and frameworks that evaluate SpEL expressions from
	 * an untrusted source should not rely on this limit alone; see the
	 * <a href="https://docs.spring.io/spring-framework/reference/core/expressions/evaluation.html#expressions-evaluation-context-security"
	 * >Security Considerations</a> section of the Spring Framework reference
	 * documentation for further guidance on evaluating untrusted SpEL expressions.
	 * @since 7.1
	 */
	public static final int DEFAULT_MAX_EXPRESSION_NESTING_DEPTH = 1_000;

	/**
	 * System property to configure the default compiler mode for SpEL expression parsers: {@value}.
	 * <p><strong>NOTE</strong>: Instead of relying on a global default, applications
	 * and frameworks should ideally set an explicit custom value via
	 * {@link Builder#compilerMode(SpelCompilerMode)}, which provides complete
	 * configuration control and the ability to override global defaults per
	 * use case.
	 * <p>Can also be configured via the {@link SpringProperties} mechanism.
	 */
	public static final String SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME = "spring.expression.compiler.mode";

	/**
	 * System property to configure the default maximum number of operations permitted
	 * during SpEL expression evaluation: {@value}.
	 * <p><strong>NOTE</strong>: Instead of relying on a global default, applications
	 * and frameworks should ideally set an explicit custom value via
	 * {@link Builder#maximumOperations(int)}, which provides complete
	 * configuration control and the ability to override global defaults per
	 * use case.
	 * <p>Can also be configured via the {@link SpringProperties} mechanism.
	 * @since 6.2.19
	 * @see #DEFAULT_MAX_OPERATIONS
	 */
	public static final String SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME = "spring.expression.maxOperations";

	/**
	 * System property to configure the default maximum number of bits permitted in the
	 * result of a {@link java.math.BigDecimal} or {@link java.math.BigInteger} power
	 * operation within a SpEL expression: {@value}.
	 * <p><strong>NOTE</strong>: Instead of relying on a global default, applications
	 * and frameworks should ideally set an explicit custom value via
	 * {@link Builder#maximumBigPowerBits(int)}, which provides complete
	 * configuration control and the ability to override global defaults per
	 * use case.
	 * <p>Can also be configured via the {@link SpringProperties} mechanism.
	 * @since 7.0.9
	 * @see #DEFAULT_MAX_BIG_POWER_BITS
	 */
	public static final String SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME =
			"spring.expression.maxBigPowerBits";


	private static final SpelCompilerMode defaultCompilerMode;

	static {
		String compilerMode = SpringProperties.getProperty(SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME);
		defaultCompilerMode = (compilerMode != null ?
				SpelCompilerMode.valueOf(compilerMode.toUpperCase(Locale.ROOT)) : SpelCompilerMode.OFF);
	}


	private final SpelCompilerMode compilerMode;

	private final @Nullable ClassLoader compilerClassLoader;

	private final boolean autoGrowNullReferences;

	private final boolean autoGrowCollections;

	private final int maximumAutoGrowSize;

	private final int maximumExpressionLength;

	private final int maximumOperations;

	private final int maximumBigPowerBits;

	private final int maximumNestingDepth;


	/**
	 * Create a new {@code SpelParserConfiguration} instance with the same defaults
	 * applied by {@link #builder()}.
	 * <p>This is shorthand for {@code SpelParserConfiguration.builder().build()},
	 * for use whenever none of the defaults need to be overridden.
	 * @since 7.0.10
	 * @see #builder()
	 */
	public static SpelParserConfiguration withDefaults() {
		return builder().build();
	}

	/**
	 * Create a new {@link Builder} for configuring a {@code SpelParserConfiguration}.
	 * <p>The builder only requires configuration of the properties that need
	 * to deviate from their sensible defaults. See {@link Builder} for details
	 * on those defaults.
	 * @since 7.0.10
	 * @see #withDefaults()
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance with default settings.
	 * <p><strong>NOTE</strong>: Favor {@link #builder()} for complete
	 * configuration control and the ability to override global defaults
	 * per use case.
	 * @see #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME
	 * @see #DEFAULT_MAX_AUTO_GROW_SIZE
	 * @deprecated as of Spring Framework 7.1, in favor of {@link #withDefaults()}
	 */
	@Deprecated(since = "7.1")
	public SpelParserConfiguration() {
		this(null, null);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * <p><strong>NOTE</strong>: Favor {@link #builder()} for complete
	 * configuration control and the ability to override global defaults
	 * per use case.
	 * @param compilerMode the compiler mode that parsers using this configuration
	 * should use; or {@code null} to use the default mode
	 * @param compilerClassLoader the {@code ClassLoader} to use as the basis for
	 * expression compilation; or {@code null} to use the default {@code ClassLoader}
	 * @see #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME
	 * @see #DEFAULT_MAX_AUTO_GROW_SIZE
	 * @deprecated as of Spring Framework 7.1, in favor of the {@linkplain #builder() builder API}
	 */
	@Deprecated(since = "7.1")
	public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader) {
		this(compilerMode, compilerClassLoader, false, false, DEFAULT_MAX_AUTO_GROW_SIZE);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * <p><strong>NOTE</strong>: Favor {@link #builder()} for complete
	 * configuration control and the ability to override global defaults
	 * per use case.
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @see #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME
	 * @see #DEFAULT_MAX_AUTO_GROW_SIZE
	 * @deprecated as of Spring Framework 7.1, in favor of the {@linkplain #builder() builder API}
	 */
	@Deprecated(since = "7.1")
	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections) {
		this(autoGrowNullReferences, autoGrowCollections, DEFAULT_MAX_AUTO_GROW_SIZE);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * <p><strong>NOTE</strong>: Favor {@link #builder()} for complete
	 * configuration control and the ability to override global defaults
	 * per use case.
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size to which a collection can auto grow;
	 * must not be negative, and a value of {@code 0} effectively disables growing
	 * a collection beyond its current size
	 * @see #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME
	 * @deprecated as of Spring Framework 7.1, in favor of the {@linkplain #builder() builder API}
	 */
	@Deprecated(since = "7.1")
	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {
		this(null, null, autoGrowNullReferences, autoGrowCollections, maximumAutoGrowSize);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * <p><strong>NOTE</strong>: Favor {@link #builder()} for complete
	 * configuration control and the ability to override global defaults
	 * per use case.
	 * @param compilerMode the compiler mode that parsers using this configuration
	 * should use; or {@code null} to use the default mode
	 * @param compilerClassLoader the {@code ClassLoader} to use as the basis for
	 * expression compilation; or {@code null} to use the default {@code ClassLoader}
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size to which a collection can auto grow;
	 * must not be negative, and a value of {@code 0} effectively disables growing
	 * a collection beyond its current size
	 * @see #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME
	 * @deprecated as of Spring Framework 7.1, in favor of the {@linkplain #builder() builder API}
	 */
	@Deprecated(since = "7.1")
	public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
			boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {

		this(compilerMode, compilerClassLoader, autoGrowNullReferences, autoGrowCollections,
				maximumAutoGrowSize, DEFAULT_MAX_EXPRESSION_LENGTH);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * <p><strong>NOTE</strong>: Favor {@link #builder()} for complete
	 * configuration control and the ability to override global defaults
	 * per use case.
	 * @param compilerMode the compiler mode that parsers using this configuration
	 * should use; or {@code null} to use the default mode
	 * @param compilerClassLoader the {@code ClassLoader} to use as the basis for
	 * expression compilation; or {@code null} to use the default {@code ClassLoader}
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size to which a collection can auto grow;
	 * must not be negative, and a value of {@code 0} effectively disables growing
	 * a collection beyond its current size
	 * @param maximumExpressionLength the maximum length of a SpEL expression;
	 * must be a positive number
	 * @since 5.2.25
	 * @see #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME
	 * @see #DEFAULT_MAX_EXPRESSION_NESTING_DEPTH
	 * @deprecated as of Spring Framework 7.1, in favor of the {@linkplain #builder() builder API}
	 */
	@Deprecated(since = "7.1")
	public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
			boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize, int maximumExpressionLength) {

		this((compilerMode != null ? compilerMode : defaultCompilerMode), compilerClassLoader, autoGrowNullReferences,
				autoGrowCollections, maximumAutoGrowSize, maximumExpressionLength, retrieveMaxOperations());
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * <p><strong>NOTE</strong>: Favor {@link #builder()} for complete
	 * configuration control and the ability to override global defaults
	 * per use case.
	 * @param compilerMode the compiler mode that parsers using this configuration
	 * should use; must not be {@code null}
	 * @param compilerClassLoader the {@code ClassLoader} to use as the basis for
	 * expression compilation; or {@code null} to use the default {@code ClassLoader}
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size to which a collection can auto grow;
	 * must not be negative, and a value of {@code 0} effectively disables growing
	 * a collection beyond its current size
	 * @param maximumExpressionLength the maximum length of a SpEL expression;
	 * must be a positive number
	 * @param maximumOperations the maximum number of operations permitted during
	 * SpEL expression evaluation; must be a positive number
	 * @since 6.2.19
	 * @see #SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME
	 * @see #DEFAULT_MAX_EXPRESSION_NESTING_DEPTH
	 * @deprecated as of Spring Framework 7.1, in favor of the {@linkplain #builder() builder API}
	 */
	@Deprecated(since = "7.1")
	public SpelParserConfiguration(SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
			boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize, int maximumExpressionLength,
			int maximumOperations) {

		this(compilerMode, compilerClassLoader, autoGrowNullReferences, autoGrowCollections,
				maximumAutoGrowSize, maximumExpressionLength, maximumOperations, retrieveMaxBigPowerBits());
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * <p><strong>NOTE</strong>: Favor {@link #builder()} for complete
	 * configuration control and the ability to override global defaults
	 * per use case.
	 * @param compilerMode the compiler mode that parsers using this configuration
	 * should use; must not be {@code null}
	 * @param compilerClassLoader the {@code ClassLoader} to use as the basis for
	 * expression compilation; or {@code null} to use the default {@code ClassLoader}
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size to which a collection can auto grow;
	 * must not be negative, and a value of {@code 0} effectively disables growing
	 * a collection beyond its current size
	 * @param maximumExpressionLength the maximum length of a SpEL expression;
	 * must be a positive number
	 * @param maximumOperations the maximum number of operations permitted during
	 * SpEL expression evaluation; must be a positive number
	 * @param maximumBigPowerBits the maximum number of bits permitted in the
	 * result of a {@link java.math.BigDecimal} or {@link java.math.BigInteger} power
	 * operation; must be a positive number; use {@link Integer#MAX_VALUE} for no limit
	 * @since 7.0.9
	 * @see #DEFAULT_MAX_EXPRESSION_NESTING_DEPTH
	 * @deprecated as of Spring Framework 7.1, in favor of the {@linkplain #builder() builder API}
	 */
	@Deprecated(since = "7.1")
	public SpelParserConfiguration(SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
			boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize, int maximumExpressionLength,
			int maximumOperations, int maximumBigPowerBits) {

		this(compilerMode, compilerClassLoader, autoGrowNullReferences, autoGrowCollections, maximumAutoGrowSize,
				maximumExpressionLength, maximumOperations, maximumBigPowerBits, DEFAULT_MAX_EXPRESSION_NESTING_DEPTH);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * <p><strong>NOTE</strong>: Favor {@link #builder()} for complete
	 * configuration control and the ability to override global defaults
	 * per use case.
	 * @param compilerMode the compiler mode that parsers using this configuration
	 * should use; must not be {@code null}
	 * @param compilerClassLoader the {@code ClassLoader} to use as the basis for
	 * expression compilation; or {@code null} to use the default {@code ClassLoader}
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size to which a collection can auto grow;
	 * must not be negative, and a value of {@code 0} effectively disables growing
	 * a collection beyond its current size
	 * @param maximumExpressionLength the maximum length of a SpEL expression;
	 * must be a positive number
	 * @param maximumOperations the maximum number of operations permitted during
	 * SpEL expression evaluation; must be a positive number
	 * @param maximumBigPowerBits the maximum number of bits permitted in the
	 * result of a {@link java.math.BigDecimal} or {@link java.math.BigInteger} power
	 * operation; must be a positive number; use {@link Integer#MAX_VALUE} for no limit
	 * @param maximumNestingDepth the maximum nesting depth permitted within a SpEL
	 * expression; must be a positive number
	 * @since 7.1
	 * @deprecated as of Spring Framework 7.1, in favor of the {@linkplain #builder() builder API}
	 */
	@Deprecated(since = "7.1")
	public SpelParserConfiguration(SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
			boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize, int maximumExpressionLength,
			int maximumOperations, int maximumBigPowerBits, int maximumNestingDepth) {

		Assert.notNull(compilerMode, "'compilerMode' must not be null");
		Assert.isTrue(maximumAutoGrowSize >= 0, "'maximumAutoGrowSize' must not be negative");
		Assert.isTrue(maximumExpressionLength > 0, "'maximumExpressionLength' must be a positive number");
		Assert.isTrue(maximumOperations > 0, "'maximumOperations' must be a positive number");
		Assert.isTrue(maximumBigPowerBits > 0, "'maximumBigPowerBits' must be a positive number");
		Assert.isTrue(maximumNestingDepth > 0, "'maximumNestingDepth' must be a positive number");

		this.compilerMode = compilerMode;
		this.compilerClassLoader = compilerClassLoader;
		this.autoGrowNullReferences = autoGrowNullReferences;
		this.autoGrowCollections = autoGrowCollections;
		this.maximumAutoGrowSize = maximumAutoGrowSize;
		this.maximumExpressionLength = maximumExpressionLength;
		this.maximumOperations = maximumOperations;
		this.maximumBigPowerBits = maximumBigPowerBits;
		this.maximumNestingDepth = maximumNestingDepth;
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
	public @Nullable ClassLoader getCompilerClassLoader() {
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
	 * @see #DEFAULT_MAX_AUTO_GROW_SIZE
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

	/**
	 * Return the maximum number of bits permitted in the result of a
	 * {@link java.math.BigDecimal} or {@link java.math.BigInteger} power operation.
	 * @since 7.0.9
	 */
	public int getMaximumBigPowerBits() {
		return this.maximumBigPowerBits;
	}

	/**
	 * Return the maximum nesting depth permitted within a SpEL expression.
	 * @since 7.1
	 * @see #DEFAULT_MAX_EXPRESSION_NESTING_DEPTH
	 */
	public int getMaximumNestingDepth() {
		return this.maximumNestingDepth;
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

	private static int retrieveMaxBigPowerBits() {
		String value = SpringProperties.getProperty(SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME);
		if (!StringUtils.hasText(value)) {
			return DEFAULT_MAX_BIG_POWER_BITS;
		}
		try {
			int maxBits = Integer.parseInt(value.trim());
			Assert.isTrue(maxBits > 0, () -> "Value [" + maxBits + "] for system property [" +
					SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME + "] must be positive");
			return maxBits;
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Failed to parse value for system property [" +
					SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME + "]: " + ex.getMessage(), ex);
		}
	}


	/**
	 * Fluent builder API for {@link SpelParserConfiguration}.
	 * <p>Each property defaults to the same value as the corresponding
	 * property in a {@code SpelParserConfiguration} created via the
	 * {@linkplain SpelParserConfiguration#SpelParserConfiguration() no-arg constructor}
	 * &mdash; including honoring the system properties or Spring properties named
	 * {@value #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME},
	 * {@value #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME}, and
	 * {@value #SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME}.
	 * <p>Only configure the properties that need to deviate from those defaults.
	 * @since 7.0.10
	 * @see SpelParserConfiguration#builder()
	 */
	public static final class Builder {

		private SpelCompilerMode compilerMode = defaultCompilerMode;

		private @Nullable ClassLoader compilerClassLoader;

		private boolean autoGrowNullReferences = false;

		private boolean autoGrowCollections = false;

		private int maximumAutoGrowSize = DEFAULT_MAX_AUTO_GROW_SIZE;

		private int maximumExpressionLength = DEFAULT_MAX_EXPRESSION_LENGTH;

		private @Nullable Integer maximumOperations;

		private @Nullable Integer maximumBigPowerBits;

		private int maximumNestingDepth = DEFAULT_MAX_EXPRESSION_NESTING_DEPTH;

		private Builder() {
		}

		/**
		 * Set the compiler mode that parsers using this configuration should use.
		 * <p>By default, set to the value configured via {@link SpringProperties}
		 * for a system property or Spring property named
		 * {@value #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME}, or
		 * {@link SpelCompilerMode#OFF} if that property is not set.
		 * @param compilerMode the compiler mode to use; must not be {@code null}
		 */
		public Builder compilerMode(SpelCompilerMode compilerMode) {
			Assert.notNull(compilerMode, "'compilerMode' must not be null");
			this.compilerMode = compilerMode;
			return this;
		}

		/**
		 * Set the {@code ClassLoader} to use as the basis for expression compilation.
		 * <p>By default, set to {@code null}, indicating that the default
		 * {@code ClassLoader} should be used.
		 * @param compilerClassLoader the {@code ClassLoader} to use
		 */
		public Builder compilerClassLoader(@Nullable ClassLoader compilerClassLoader) {
			this.compilerClassLoader = compilerClassLoader;
			return this;
		}

		/**
		 * Enable automatic growth of {@code null} references encountered while
		 * traversing a property path.
		 * <p>By default, this is disabled.
		 */
		public Builder autoGrowNullReferences() {
			this.autoGrowNullReferences = true;
			return this;
		}

		/**
		 * Enable automatic growth of collections and arrays encountered while
		 * traversing a property path.
		 * <p>By default, this is disabled.
		 * @see #maximumAutoGrowSize(int)
		 */
		public Builder autoGrowCollections() {
			this.autoGrowCollections = true;
			return this;
		}

		/**
		 * Set the maximum size to which a collection or array can automatically grow.
		 * <p>By default, set to {@link #DEFAULT_MAX_AUTO_GROW_SIZE}.
		 * @param maximumAutoGrowSize the maximum auto-grow size; must not be
		 * negative, and a value of {@code 0} effectively disables growing a
		 * collection or array beyond its current size
		 * @see #autoGrowCollections()
		 */
		public Builder maximumAutoGrowSize(int maximumAutoGrowSize) {
			Assert.isTrue(maximumAutoGrowSize >= 0, "'maximumAutoGrowSize' must not be negative");
			this.maximumAutoGrowSize = maximumAutoGrowSize;
			return this;
		}

		/**
		 * Set the maximum length permitted for a SpEL expression.
		 * <p>By default, set to {@link #DEFAULT_MAX_EXPRESSION_LENGTH}.
		 * @param maximumExpressionLength the maximum expression length; must be
		 * a positive number
		 */
		public Builder maximumExpressionLength(int maximumExpressionLength) {
			Assert.isTrue(maximumExpressionLength > 0, "'maximumExpressionLength' must be a positive number");
			this.maximumExpressionLength = maximumExpressionLength;
			return this;
		}

		/**
		 * Set the maximum number of operations permitted during SpEL expression evaluation.
		 * <p>By default, set to the value configured via {@link SpringProperties}
		 * for a system property or Spring property named
		 * {@value #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME}, or
		 * {@link #DEFAULT_MAX_OPERATIONS} if that property is not set.
		 * @param maximumOperations the maximum number of operations; must be a
		 * positive number
		 */
		public Builder maximumOperations(int maximumOperations) {
			Assert.isTrue(maximumOperations > 0, "'maximumOperations' must be a positive number");
			this.maximumOperations = maximumOperations;
			return this;
		}

		/**
		 * Set the maximum number of bits permitted in the result of a
		 * {@link java.math.BigDecimal} or {@link java.math.BigInteger} power
		 * operation within a SpEL expression.
		 * <p>By default, set to the value configured via {@link SpringProperties}
		 * for a system property or Spring property named
		 * {@value #SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME}, or
		 * {@link #DEFAULT_MAX_BIG_POWER_BITS} if that property is not set.
		 * @param maximumBigPowerBits the maximum number of bits; must be a
		 * positive number; use {@link Integer#MAX_VALUE} for no limit
		 */
		public Builder maximumBigPowerBits(int maximumBigPowerBits) {
			Assert.isTrue(maximumBigPowerBits > 0, "'maximumBigPowerBits' must be a positive number");
			this.maximumBigPowerBits = maximumBigPowerBits;
			return this;
		}

		/**
		 * Set the maximum nesting depth permitted within a SpEL expression.
		 * <p>By default, set to {@link #DEFAULT_MAX_EXPRESSION_NESTING_DEPTH}.
		 * @param maximumNestingDepth the maximum nesting depth; must be a
		 * positive number
		 * @since 7.1
		 */
		public Builder maximumNestingDepth(int maximumNestingDepth) {
			Assert.isTrue(maximumNestingDepth > 0, "'maximumNestingDepth' must be a positive number");
			this.maximumNestingDepth = maximumNestingDepth;
			return this;
		}

		/**
		 * Build the {@link SpelParserConfiguration} configured via this builder.
		 */
		@SuppressWarnings("deprecation")
		public SpelParserConfiguration build() {
			int maximumOperations = (this.maximumOperations != null ?
					this.maximumOperations : retrieveMaxOperations());
			int maximumBigPowerBits = (this.maximumBigPowerBits != null ?
					this.maximumBigPowerBits : retrieveMaxBigPowerBits());
			return new SpelParserConfiguration(this.compilerMode, this.compilerClassLoader,
					this.autoGrowNullReferences, this.autoGrowCollections, this.maximumAutoGrowSize,
					this.maximumExpressionLength, maximumOperations, maximumBigPowerBits, this.maximumNestingDepth);
		}

	}

}

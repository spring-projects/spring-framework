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
 * defaults need to be overridden. Note that the constructors in this class are
 * planned to be deprecated in favor of the builder as of Spring Framework 7.1.
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
	 */
	public SpelParserConfiguration() {
		this(null, null, false, false, Integer.MAX_VALUE);
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
	 */
	public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader) {
		this(compilerMode, compilerClassLoader, false, false, Integer.MAX_VALUE);
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
	 */
	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections) {
		this(null, null, autoGrowNullReferences, autoGrowCollections, Integer.MAX_VALUE);
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * <p><strong>NOTE</strong>: Favor {@link #builder()} for complete
	 * configuration control and the ability to override global defaults
	 * per use case.
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size to which a collection can auto grow
	 * @see #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME
	 */
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
	 * @param maximumAutoGrowSize the maximum size to which a collection can auto grow
	 * @see #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME
	 */
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
	 * @param maximumAutoGrowSize the maximum size to which a collection can auto grow
	 * @param maximumExpressionLength the maximum length of a SpEL expression;
	 * must be a positive number
	 * @since 5.2.25
	 * @see #SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
	 * @see #SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME
	 */
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
	 * @param maximumAutoGrowSize the maximum size to which a collection can auto grow
	 * @param maximumExpressionLength the maximum length of a SpEL expression;
	 * must be a positive number
	 * @param maximumOperations the maximum number of operations permitted during
	 * SpEL expression evaluation; must be a positive number
	 * @since 6.2.19
	 * @see #SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME
	 */
	public SpelParserConfiguration(SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
			boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize, int maximumExpressionLength,
			int maximumOperations) {

		this(compilerMode, compilerClassLoader, autoGrowNullReferences, autoGrowCollections,
				maximumAutoGrowSize, maximumExpressionLength, maximumOperations, retrieveMaxBigPowerBits());
	}

	/**
	 * Create a new {@code SpelParserConfiguration} instance.
	 * <p><strong>NOTE</strong>: Favor {@link #builder()} for a more readable
	 * way to override global defaults per use case.
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
	 * @param maximumBigPowerBits the maximum number of bits permitted in the
	 * result of a {@link java.math.BigDecimal} or {@link java.math.BigInteger} power
	 * operation; must be a positive number; use {@link Integer#MAX_VALUE} for no limit
	 * @since 7.0.9
	 */
	public SpelParserConfiguration(SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
			boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize, int maximumExpressionLength,
			int maximumOperations, int maximumBigPowerBits) {

		Assert.notNull(compilerMode, "'compilerMode' must not be null");
		Assert.isTrue(maximumExpressionLength > 0, "'maximumExpressionLength' must be a positive number");
		Assert.isTrue(maximumOperations > 0, "'maximumOperations' must be a positive number");
		Assert.isTrue(maximumBigPowerBits > 0, "'maximumBigPowerBits' must be a positive number");

		this.compilerMode = compilerMode;
		this.compilerClassLoader = compilerClassLoader;
		this.autoGrowNullReferences = autoGrowNullReferences;
		this.autoGrowCollections = autoGrowCollections;
		this.maximumAutoGrowSize = maximumAutoGrowSize;
		this.maximumExpressionLength = maximumExpressionLength;
		this.maximumOperations = maximumOperations;
		this.maximumBigPowerBits = maximumBigPowerBits;
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
	 * {@value #SPRING_EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME} &mdash; with
	 * one exception: {@link #maximumAutoGrowSize(int) maximumAutoGrowSize}
	 * defaults to {@code 256} rather than {@link Integer#MAX_VALUE}, for
	 * consistency with the default auto-grow limit used for Spring's data
	 * binding support (see {@code DataBinder.DEFAULT_AUTO_GROW_COLLECTION_LIMIT}).
	 * <p>Only configure the properties that need to deviate from those defaults.
	 * @since 7.0.10
	 * @see SpelParserConfiguration#builder()
	 */
	public static final class Builder {

		// Aligned with DataBinder.DEFAULT_AUTO_GROW_COLLECTION_LIMIT, unlike the
		// constructors in the enclosing class, which default to Integer.MAX_VALUE
		// for backward compatibility.
		private static final int DEFAULT_MAX_AUTO_GROW_SIZE = 256;

		private SpelCompilerMode compilerMode = defaultCompilerMode;

		private @Nullable ClassLoader compilerClassLoader;

		private boolean autoGrowNullReferences = false;

		private boolean autoGrowCollections = false;

		private int maximumAutoGrowSize = DEFAULT_MAX_AUTO_GROW_SIZE;

		private int maximumExpressionLength = DEFAULT_MAX_EXPRESSION_LENGTH;

		private @Nullable Integer maximumOperations;

		private @Nullable Integer maximumBigPowerBits;

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
		 * <p>By default, set to {@code 256}, for consistency with the default
		 * auto-grow limit used for Spring's data binding support (see
		 * {@code DataBinder.DEFAULT_AUTO_GROW_COLLECTION_LIMIT}) &mdash; unlike
		 * the constructors in {@link SpelParserConfiguration}, which default to
		 * {@link Integer#MAX_VALUE} (effectively unbounded) for backward
		 * compatibility.
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
		 * Build the {@link SpelParserConfiguration} configured via this builder.
		 */
		public SpelParserConfiguration build() {
			int maximumOperations = (this.maximumOperations != null ?
					this.maximumOperations : retrieveMaxOperations());
			int maximumBigPowerBits = (this.maximumBigPowerBits != null ?
					this.maximumBigPowerBits : retrieveMaxBigPowerBits());
			return new SpelParserConfiguration(this.compilerMode, this.compilerClassLoader,
					this.autoGrowNullReferences, this.autoGrowCollections, this.maximumAutoGrowSize,
					this.maximumExpressionLength, maximumOperations, maximumBigPowerBits);
		}

	}

}

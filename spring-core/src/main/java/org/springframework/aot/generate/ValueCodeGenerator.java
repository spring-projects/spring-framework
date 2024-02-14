/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aot.generate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Code generator for a single value. Delegates code generation to a list of
 * configurable {@link Delegate} implementations.
 *
 * @author Stephane Nicoll
 * @since 6.1.2
 */
public final class ValueCodeGenerator {
	static final String DEFAULT_IDENTIFIER_PREFIX = "$valueCodeGeneratorObject";

	private static final ValueCodeGenerator INSTANCE = new ValueCodeGenerator(DEFAULT_IDENTIFIER_PREFIX, ValueCodeGeneratorDelegates.INSTANCES, null);

	private static final CodeBlock NULL_VALUE_CODE_BLOCK = CodeBlock.of("null");

	private final List<Delegate> delegates;

	@Nullable
	private final GeneratedMethods generatedMethods;

	private final ThreadLocal<Integer> generatedCodeDepth = ThreadLocal.withInitial(() -> 0);
	private final ThreadLocal<Set<Object>> toGenerateObjectSet = ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

	private final String identifierPrefix;

	private ValueCodeGenerator(String identifierPrefix, List<Delegate> delegates, @Nullable GeneratedMethods generatedMethods) {
		this.identifierPrefix = identifierPrefix;
		this.delegates = delegates;
		this.generatedMethods = generatedMethods;
	}

	/**
	 * Return an instance that provides support for {@linkplain
	 * ValueCodeGeneratorDelegates#INSTANCES common value types}.
	 * @return an instance with support for common value types
	 */
	public static ValueCodeGenerator withDefaults() {
		return INSTANCE;
	}

	/**
	 * Create an instance with the specified {@link Delegate} implementations.
	 * @param delegates the delegates to use
	 * @return an instance with the specified delegates
	 */
	public static ValueCodeGenerator with(Delegate... delegates) {
		return with(Arrays.asList(delegates));
	}

	/**
	 * Create an instance with the specified {@link Delegate} implementations.
	 * @param delegates the delegates to use
	 * @return an instance with the specified delegates
	 */
	public static ValueCodeGenerator with(List<Delegate> delegates) {
		Assert.notEmpty(delegates, "Delegates must not be empty");
		return new ValueCodeGenerator(DEFAULT_IDENTIFIER_PREFIX, new ArrayList<>(delegates), null);
	}

	public ValueCodeGenerator add(List<Delegate> additionalDelegates) {
		Assert.notEmpty(additionalDelegates, "AdditionalDelegates must not be empty");
		List<Delegate> allDelegates = new ArrayList<>(this.delegates);
		allDelegates.addAll(additionalDelegates);
		return new ValueCodeGenerator(DEFAULT_IDENTIFIER_PREFIX, allDelegates, this.generatedMethods);
	}

	/**
	 * Return a new {@link ValueCodeGenerator} that uses the given {@link String} as the
	 * prefix for all of its identifiers.
	 * @param identifierPrefix the prefix to use for each identifier. Cannot be null or empty.
	 * @return a new {@link ValueCodeGenerator} with the same delegates and generated methods
	 * as this {@link ValueCodeGenerator}.
	 */
	public ValueCodeGenerator withIdentifierPrefix(String identifierPrefix) {
		Assert.notNull(identifierPrefix, "Identifier prefix must not be null");
		Assert.state(!identifierPrefix.isEmpty(), "Identifier prefix must not be empty");
		return new ValueCodeGenerator(identifierPrefix, this.delegates, this.generatedMethods);
	}

	/**
	 * Return a {@link ValueCodeGenerator} that is scoped for the specified
	 * {@link GeneratedMethods}. This allows code generation to generate
	 * additional methods if necessary, or perform some optimization in
	 * case of visibility issues.
	 * @param generatedMethods the generated methods to use
	 * @return an instance scoped to the specified generated methods
	 */
	public ValueCodeGenerator scoped(GeneratedMethods generatedMethods) {
		return new ValueCodeGenerator(DEFAULT_IDENTIFIER_PREFIX, this.delegates, generatedMethods);
	}

	/**
	 * Generate the code that represents the specified {@code value}.
	 * @param value the value to generate
	 * @return the code that represents the specified value
	 */
	public CodeBlock generateCode(@Nullable Object value) {
		if (value == null) {
			return NULL_VALUE_CODE_BLOCK;
		}
		if (this.toGenerateObjectSet.get().contains(value)) {
			throw new ValueCodeGenerationException("Unable to generate code for (" + value + ") because it contains a cyclic reference.",
					value, null);
		}
		try {
			this.toGenerateObjectSet.get().add(value);
			this.generatedCodeDepth.set(this.generatedCodeDepth.get() + 1);
			for (Delegate delegate : this.delegates) {
				CodeBlock code = delegate.generateCode(this, value);
				if (code != null) {
					return code;
				}
			}
			throw new UnsupportedTypeValueCodeGenerationException(value);
		}
		catch (Exception ex) {
			throw new ValueCodeGenerationException(value, ex);
		}
		finally {
			this.generatedCodeDepth.set(this.generatedCodeDepth.get() - 1);
			this.toGenerateObjectSet.get().remove(value);
		}
	}

	/**
	 * Returns a String that can be used as a new identifier in
	 * a {@link CodeBlock} expression.
	 * For a given {@link Delegate} call, it will always return the same value.
	 * That is, each {@link Delegate} can define at most one new variable.
	 * @return an identifier that can be used in a {@link CodeBlock} that
	 * is guaranteed to not be used in an upper {@link Delegate} call.
	 */
	public String getIdentifierForCurrentDepth() {
		return this.identifierPrefix + this.generatedCodeDepth.get();
	}

	/**
	 * Return the {@link GeneratedMethods} that represents the scope
	 * in which code generated by this instance will be added, or
	 * {@code null} if no specific scope is set.
	 * @return the generated methods to use for code generation
	 */
	@Nullable
	public GeneratedMethods getGeneratedMethods() {
		return this.generatedMethods;
	}

	/**
	 * Strategy interface that can be used to implement code generation for a
	 * particular value type.
	 */
	public interface Delegate {

		/**
		 * Generate the code for the specified non-null {@code value}. If this
		 * instance does not support the value, it should return {@code null} to
		 * indicate so.
		 * @param valueCodeGenerator the code generator to use for embedded values
		 * @param value the value to generate
		 * @return the code that represents the specified value or {@code null} if
		 * the specified value is not supported.
		 */
		@Nullable
		CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value);

	}

}

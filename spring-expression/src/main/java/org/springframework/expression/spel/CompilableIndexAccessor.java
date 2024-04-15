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

package org.springframework.expression.spel;

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.expression.IndexAccessor;

/**
 * A compilable {@link IndexAccessor} is able to generate bytecode that represents
 * the operation for reading the index, facilitating compilation to bytecode of
 * expressions that use the accessor.
 *
 * @author Sam Brannen
 * @since 6.2
 */
public interface CompilableIndexAccessor extends IndexAccessor, Opcodes {

	/**
	 * Determine if this {@code IndexAccessor} is currently suitable for compilation.
	 * <p>May only be known once the index has been read.
	 * @see #read(org.springframework.expression.EvaluationContext, Object, Object)
	 */
	boolean isCompilable();

	/**
	 * Get the type of the indexed value.
	 * <p>For example, given the expression {@code book.authors[0]}, the indexed
	 * value type represents the result of {@code authors[0]} which may be an
	 * {@code Author} object, a {@code String} representing the author's name, etc.
	 * <p>May only be known once the index has been read.
	 * @see #read(org.springframework.expression.EvaluationContext, Object, Object)
	 */
	Class<?> getIndexedValueType();

	/**
	 * Generate bytecode that performs the operation for reading the index.
	 * <p>Bytecode should be generated into the supplied {@link MethodVisitor}
	 * using context information from the {@link CodeFlow} where necessary.
	 * <p>The supplied {@code indexNode} should be used to generate the
	 * appropriate bytecode to load the index onto the stack. For example, given
	 * the expression {@code book.authors[0]}, invoking
	 * {@code codeFlow.generateCodeForArgument(methodVisitor, indexNode, int.class)}
	 * will ensure that the index ({@code 0}) is available on the stack as a
	 * primitive {@code int}.
	 * <p>Will only be invoked if {@link #isCompilable()} returns {@code true}.
	 * @param indexNode the {@link SpelNode} that represents the index being
	 * accessed
	 * @param methodVisitor the ASM {@link MethodVisitor} into which code should
	 * be generated
	 * @param codeFlow the current state of the expression compiler
	 */
	void generateCode(SpelNode indexNode, MethodVisitor methodVisitor, CodeFlow codeFlow);

}

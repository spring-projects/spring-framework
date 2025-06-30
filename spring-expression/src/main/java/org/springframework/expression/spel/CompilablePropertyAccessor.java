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

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.expression.PropertyAccessor;

/**
 * A compilable {@link PropertyAccessor} is able to generate bytecode that represents
 * the access operation, facilitating compilation to bytecode of expressions
 * that use the accessor.
 *
 * @author Andy Clement
 * @since 4.1
 */
public interface CompilablePropertyAccessor extends PropertyAccessor, Opcodes {

	/**
	 * Return {@code true} if this property accessor is currently suitable for compilation.
	 */
	boolean isCompilable();

	/**
	 * Return the type of the accessed property - may only be known once an access has occurred.
	 */
	Class<?> getPropertyType();

	/**
	 * Generate the bytecode that performs the access operation into the specified
	 * {@link MethodVisitor} using context information from the {@link CodeFlow}
	 * where necessary.
	 * @param propertyName the name of the property
	 * @param methodVisitor the ASM method visitor into which code should be generated
	 * @param codeFlow the current state of the expression compiler
	 */
	void generateCode(String propertyName, MethodVisitor methodVisitor, CodeFlow codeFlow);

}

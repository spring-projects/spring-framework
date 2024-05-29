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
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;

/**
 * Represents a node in the abstract syntax tree (AST) for a parsed Spring
 * Expression Language (SpEL) expression.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @since 3.0
 */
public interface SpelNode {

	/**
	 * Evaluate the expression node in the context of the supplied expression state
	 * and return the value.
	 * @param expressionState the current expression state (includes the context)
	 * @return the value of this node evaluated against the specified state
	 * @throws EvaluationException if any problem occurs evaluating the expression
	 */
	@Nullable
	Object getValue(ExpressionState expressionState) throws EvaluationException;

	/**
	 * Evaluate the expression node in the context of the supplied expression state
	 * and return the typed value.
	 * @param expressionState the current expression state (includes the context)
	 * @return the typed value of this node evaluated against the specified state
	 * @throws EvaluationException if any problem occurs evaluating the expression
	 */
	TypedValue getTypedValue(ExpressionState expressionState) throws EvaluationException;

	/**
	 * Determine if this expression node will support a setValue() call.
	 * @param expressionState the current expression state (includes the context)
	 * @return true if the expression node will allow setValue()
	 * @throws EvaluationException if something went wrong trying to determine
	 * if the node supports writing
	 */
	boolean isWritable(ExpressionState expressionState) throws EvaluationException;

	/**
	 * Evaluate the expression to a node and then set the new value on that node.
	 * For example, if the expression evaluates to a property reference, then the
	 * property will be set to the new value.
	 * @param expressionState the current expression state (includes the context)
	 * @param newValue the new value
	 * @throws EvaluationException if any problem occurs evaluating the expression or
	 * setting the new value
	 */
	void setValue(ExpressionState expressionState, @Nullable Object newValue) throws EvaluationException;

	/**
	 * Return the string form of this AST node.
	 * @return the string form
	 */
	String toStringAST();

	/**
	 * Return the number of children under this node.
	 * @return the child count
	 * @see #getChild(int)
	 */
	int getChildCount();

	/**
	 * Return the n<sup>th</sup> child under this node.
	 * @return the child node
	 * @see #getChildCount()
	 */
	SpelNode getChild(int index);

	/**
	 * Determine the class of the object passed in, unless it is already a class object.
	 * @param obj the object that the caller wants the class of
	 * @return the class of the object if it is not already a class object,
	 * or {@code null} if the object is {@code null}
	 */
	@Nullable
	Class<?> getObjectClass(@Nullable Object obj);

	/**
	 * Return the start position of this AST node in the expression string.
	 * @return the start position
	 */
	int getStartPosition();

	/**
	 * Return the end position of this AST node in the expression string.
	 * @return the end position
	 */
	int getEndPosition();

	/**
	 * Determine if this node can be compiled to bytecode.
	 * <p>The reasoning in each node may be different but will typically involve
	 * checking whether the exit type descriptor of the node is known and any
	 * relevant child nodes are compilable.
	 * <p>The default implementation returns {@code false}.
	 * <p>If you override this method, you must also override
	 * {@link #generateCode(MethodVisitor, CodeFlow)}.
	 * @return {@code true} if this node can be compiled to bytecode
	 * @since 6.2
	 * @see #generateCode(MethodVisitor, CodeFlow)
	 */
	default boolean isCompilable() {
		return false;
	}

	/**
	 * Generate the bytecode for this node into the supplied {@link MethodVisitor}.
	 * <p>Context information about the current expression being compiled is
	 * available in the supplied {@link CodeFlow} object &mdash; for example,
	 * information about the type of the object currently on the stack.
	 * <p>This method will not be invoked unless {@link #isCompilable()} returns
	 * {@code true}.
	 * <p>The default implementation throws an {@link IllegalStateException}
	 * since {@link #isCompilable()} returns {@code false} by default.
	 * <p>If you override this method, you must also override {@link #isCompilable()}.
	 * @param methodVisitor the ASM {@code MethodVisitor} into which code should
	 * be generated
	 * @param codeFlow a context object with information about what is on the stack
	 * @since 6.2
	 * @see #isCompilable()
	 */
	default void generateCode(MethodVisitor methodVisitor, CodeFlow codeFlow) {
		throw new IllegalStateException(getClass().getName() + " does not support bytecode generation");
	}

}

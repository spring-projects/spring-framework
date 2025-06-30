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

package org.springframework.expression.spel.standard;

import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.expression.spel.CompiledExpression;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

/**
 * A {@code SpelExpression} represents a parsed (valid) expression that is ready to be
 * evaluated in a specified context. An expression can be evaluated standalone or in a
 * specified context. During expression evaluation the context may be asked to resolve
 * references to types, beans, properties, and methods.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class SpelExpression implements Expression {

	// Number of times to interpret an expression before compiling it
	private static final int INTERPRETED_COUNT_THRESHOLD = 100;

	// Number of times to try compiling an expression before giving up
	private static final int FAILED_ATTEMPTS_THRESHOLD = 100;


	private final String expression;

	private final SpelNodeImpl ast;

	private final SpelParserConfiguration configuration;

	// The default context is used if no override is supplied by the user
	private @Nullable EvaluationContext evaluationContext;

	// Holds the compiled form of the expression (if it has been compiled)
	private volatile @Nullable CompiledExpression compiledAst;

	// Count of many times as the expression been interpreted - can trigger compilation
	// when certain limit reached
	private final AtomicInteger interpretedCount = new AtomicInteger();

	// The number of times compilation was attempted and failed - enables us to eventually
	// give up trying to compile it when it just doesn't seem to be possible.
	private final AtomicInteger failedAttempts = new AtomicInteger();


	/**
	 * Construct an expression, only used by the parser.
	 */
	public SpelExpression(String expression, SpelNodeImpl ast, SpelParserConfiguration configuration) {
		this.expression = expression;
		this.ast = ast;
		this.configuration = configuration;
	}


	/**
	 * Set the evaluation context that will be used if none is specified on an evaluation call.
	 * @param evaluationContext the evaluation context to use
	 */
	public void setEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	/**
	 * Return the default evaluation context that will be used if none is supplied on an evaluation call.
	 * @return the default evaluation context
	 */
	public EvaluationContext getEvaluationContext() {
		if (this.evaluationContext == null) {
			this.evaluationContext = new StandardEvaluationContext();
		}
		return this.evaluationContext;
	}


	// implementing Expression

	@Override
	public String getExpressionString() {
		return this.expression;
	}

	@Override
	public @Nullable Object getValue() throws EvaluationException {
		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				EvaluationContext context = getEvaluationContext();
				return compiledAst.getValue(context.getRootObject().getValue(), context);
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(getEvaluationContext(), this.configuration);
		Object result = this.ast.getValue(expressionState);
		checkCompile(expressionState);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T getValue(@Nullable Class<T> expectedResultType) throws EvaluationException {
		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				EvaluationContext context = getEvaluationContext();
				Object result = compiledAst.getValue(context.getRootObject().getValue(), context);
				if (expectedResultType == null) {
					return (T) result;
				}
				else {
					return ExpressionUtils.convertTypedValue(
							getEvaluationContext(), new TypedValue(result), expectedResultType);
				}
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(getEvaluationContext(), this.configuration);
		TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
		checkCompile(expressionState);
		return ExpressionUtils.convertTypedValue(
				expressionState.getEvaluationContext(), typedResultValue, expectedResultType);
	}

	@Override
	public @Nullable Object getValue(@Nullable Object rootObject) throws EvaluationException {
		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				return compiledAst.getValue(rootObject, getEvaluationContext());
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState =
				new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration);
		Object result = this.ast.getValue(expressionState);
		checkCompile(expressionState);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T getValue(@Nullable Object rootObject, @Nullable Class<T> expectedResultType) throws EvaluationException {
		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				Object result = compiledAst.getValue(rootObject, getEvaluationContext());
				if (expectedResultType == null) {
					return (T)result;
				}
				else {
					return ExpressionUtils.convertTypedValue(
							getEvaluationContext(), new TypedValue(result), expectedResultType);
				}
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState =
				new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration);
		TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
		checkCompile(expressionState);
		return ExpressionUtils.convertTypedValue(
				expressionState.getEvaluationContext(), typedResultValue, expectedResultType);
	}

	@Override
	public @Nullable Object getValue(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext must not be null");

		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				return compiledAst.getValue(context.getRootObject().getValue(), context);
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(context, this.configuration);
		Object result = this.ast.getValue(expressionState);
		checkCompile(expressionState);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T getValue(EvaluationContext context, @Nullable Class<T> expectedResultType) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext must not be null");

		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				Object result = compiledAst.getValue(context.getRootObject().getValue(), context);
				if (expectedResultType != null) {
					return ExpressionUtils.convertTypedValue(context, new TypedValue(result), expectedResultType);
				}
				else {
					return (T) result;
				}
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(context, this.configuration);
		TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
		checkCompile(expressionState);
		return ExpressionUtils.convertTypedValue(context, typedResultValue, expectedResultType);
	}

	@Override
	public @Nullable Object getValue(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext must not be null");

		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				return compiledAst.getValue(rootObject, context);
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
		Object result = this.ast.getValue(expressionState);
		checkCompile(expressionState);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T getValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Class<T> expectedResultType)
			throws EvaluationException {

		Assert.notNull(context, "EvaluationContext must not be null");

		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			try {
				Object result = compiledAst.getValue(rootObject, context);
				if (expectedResultType != null) {
					return ExpressionUtils.convertTypedValue(context, new TypedValue(result), expectedResultType);
				}
				else {
					return (T) result;
				}
			}
			catch (Throwable ex) {
				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
		TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
		checkCompile(expressionState);
		return ExpressionUtils.convertTypedValue(context, typedResultValue, expectedResultType);
	}

	@Override
	public @Nullable Class<?> getValueType() throws EvaluationException {
		return getValueType(getEvaluationContext());
	}

	@Override
	public @Nullable Class<?> getValueType(@Nullable Object rootObject) throws EvaluationException {
		return getValueType(getEvaluationContext(), rootObject);
	}

	@Override
	public @Nullable Class<?> getValueType(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext must not be null");
		ExpressionState expressionState = new ExpressionState(context, this.configuration);
		TypeDescriptor typeDescriptor = this.ast.getValueInternal(expressionState).getTypeDescriptor();
		return (typeDescriptor != null ? typeDescriptor.getType() : null);
	}

	@Override
	public @Nullable Class<?> getValueType(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
		TypeDescriptor typeDescriptor = this.ast.getValueInternal(expressionState).getTypeDescriptor();
		return (typeDescriptor != null ? typeDescriptor.getType() : null);
	}

	@Override
	public @Nullable TypeDescriptor getValueTypeDescriptor() throws EvaluationException {
		return getValueTypeDescriptor(getEvaluationContext());
	}

	@Override
	public @Nullable TypeDescriptor getValueTypeDescriptor(@Nullable Object rootObject) throws EvaluationException {
		ExpressionState expressionState =
				new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration);
		return this.ast.getValueInternal(expressionState).getTypeDescriptor();
	}

	@Override
	public @Nullable TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext must not be null");
		ExpressionState expressionState = new ExpressionState(context, this.configuration);
		return this.ast.getValueInternal(expressionState).getTypeDescriptor();
	}

	@Override
	public @Nullable TypeDescriptor getValueTypeDescriptor(EvaluationContext context, @Nullable Object rootObject)
			throws EvaluationException {

		Assert.notNull(context, "EvaluationContext must not be null");
		ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
		return this.ast.getValueInternal(expressionState).getTypeDescriptor();
	}

	@Override
	public boolean isWritable(@Nullable Object rootObject) throws EvaluationException {
		return this.ast.isWritable(
				new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration));
	}

	@Override
	public boolean isWritable(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext must not be null");
		return this.ast.isWritable(new ExpressionState(context, this.configuration));
	}

	@Override
	public boolean isWritable(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext must not be null");
		return this.ast.isWritable(new ExpressionState(context, toTypedValue(rootObject), this.configuration));
	}

	@Override
	public void setValue(@Nullable Object rootObject, @Nullable Object value) throws EvaluationException {
		this.ast.setValue(
				new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration), value);
	}

	@Override
	public void setValue(EvaluationContext context, @Nullable Object value) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext must not be null");
		this.ast.setValue(new ExpressionState(context, this.configuration), value);
	}

	@Override
	public void setValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Object value)
			throws EvaluationException {

		Assert.notNull(context, "EvaluationContext must not be null");
		this.ast.setValue(new ExpressionState(context, toTypedValue(rootObject), this.configuration), value);
	}


	/**
	 * Compile the expression if it has been evaluated more than the threshold number
	 * of times to trigger compilation.
	 * @param expressionState the expression state used to determine compilation mode
	 */
	private void checkCompile(ExpressionState expressionState) {
		this.interpretedCount.incrementAndGet();
		SpelCompilerMode compilerMode = expressionState.getConfiguration().getCompilerMode();
		if (compilerMode != SpelCompilerMode.OFF) {
			if (compilerMode == SpelCompilerMode.IMMEDIATE) {
				if (this.interpretedCount.get() > 1) {
					compileExpression();
				}
			}
			else {
				// compilerMode = SpelCompilerMode.MIXED
				if (this.interpretedCount.get() > INTERPRETED_COUNT_THRESHOLD) {
					compileExpression();
				}
			}
		}
	}

	/**
	 * Perform expression compilation. This will only succeed once exit descriptors for
	 * all nodes have been determined. If the compilation fails and has failed more than
	 * 100 times the expression is no longer considered suitable for compilation.
	 * @return whether this expression has been successfully compiled
	 */
	public boolean compileExpression() {
		CompiledExpression compiledAst = this.compiledAst;
		if (compiledAst != null) {
			// Previously compiled
			return true;
		}
		if (this.failedAttempts.get() > FAILED_ATTEMPTS_THRESHOLD) {
			// Don't try again
			return false;
		}

		synchronized (this) {
			if (this.compiledAst != null) {
				// Compiled by another thread before this thread got into the sync block
				return true;
			}
			try {
				SpelCompiler compiler = SpelCompiler.getCompiler(this.configuration.getCompilerClassLoader());
				compiledAst = compiler.compile(this.ast);
				if (compiledAst != null) {
					// Successfully compiled
					this.compiledAst = compiledAst;
					return true;
				}
				else {
					// Failed to compile
					this.failedAttempts.incrementAndGet();
					return false;
				}
			}
			catch (Exception ex) {
				// Failed to compile
				this.failedAttempts.incrementAndGet();

				// If running in mixed mode, revert to interpreted
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.compiledAst = null;
					this.interpretedCount.set(0);
					return false;
				}
				else {
					// Running in SpelCompilerMode.immediate mode - propagate exception to caller
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_COMPILING_EXPRESSION);
				}
			}
		}
	}

	/**
	 * Cause an expression to revert to being interpreted if it has been using a compiled
	 * form. It also resets the compilation attempt failure count (an expression is normally no
	 * longer considered compilable if it cannot be compiled after 100 attempts).
	 */
	public void revertToInterpreted() {
		this.compiledAst = null;
		this.interpretedCount.set(0);
		this.failedAttempts.set(0);
	}

	/**
	 * Return the Abstract Syntax Tree for the expression.
	 */
	public SpelNode getAST() {
		return this.ast;
	}

	/**
	 * Produce a string representation of the Abstract Syntax Tree for the expression.
	 * This should ideally look like the input expression, but properly formatted since any
	 * unnecessary whitespace will have been discarded during the parse of the expression.
	 * @return the string representation of the AST
	 */
	public String toStringAST() {
		return this.ast.toStringAST();
	}

	private TypedValue toTypedValue(@Nullable Object object) {
		return (object != null ? new TypedValue(object) : TypedValue.NULL);
	}

}

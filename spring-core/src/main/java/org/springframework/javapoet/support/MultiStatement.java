/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.javapoet.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;


/**
 * A {@link CodeBlock} wrapper for multiple statements.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class MultiStatement {

	private final List<Statement> statements = new ArrayList<>();


	/**
	 * Specify if this instance is empty.
	 * @return {@code true} if no statement is registered, {@code false} otherwise
	 */
	public boolean isEmpty() {
		return this.statements.isEmpty();
	}

	/**
	 * Add the specified {@link CodeBlock codeblock} rendered as-is.
	 * @param codeBlock the code block to add
	 * @see #addStatement(CodeBlock) to add a code block that represents
	 * a statement
	 */
	public void add(CodeBlock codeBlock) {
		this.statements.add(Statement.of(codeBlock));
	}

	/**
	 * Add a {@link CodeBlock} rendered as-is using the specified callback.
	 * @param code the callback to use
	 * @see #addStatement(CodeBlock) to add a code block that represents
	 * a statement
	 */
	public void add(Consumer<Builder> code) {
		CodeBlock.Builder builder = CodeBlock.builder();
		code.accept(builder);
		add(builder.build());
	}

	/**
	 * Add a statement.
	 * @param statement the statement to add
	 */
	public void addStatement(CodeBlock statement) {
		this.statements.add(Statement.ofStatement(statement));
	}

	/**
	 * Add a statement using the specified callback.
	 * @param code the callback to use
	 */
	public void addStatement(Consumer<Builder> code) {
		CodeBlock.Builder builder = CodeBlock.builder();
		code.accept(builder);
		addStatement(builder.build());
	}

	/**
	 * Add a statement using the specified formatted String and the specified
	 * arguments.
	 * @param code the code of the statement
	 * @param args the arguments for placeholders
	 * @see CodeBlock#of(String, Object...)
	 */
	public void addStatement(String code, Object... args) {
		addStatement(CodeBlock.of(code, args));
	}

	/**
	 * Add the statements produced from the {@code itemGenerator} applied on the specified
	 * items.
	 * @param items the items to handle, each item is represented as a statement
	 * @param itemGenerator the item generator
	 * @param <T> the type of the item
	 */
	public <T> void addAll(Iterable<T> items, Function<T, CodeBlock> itemGenerator) {
		items.forEach(element -> addStatement(itemGenerator.apply(element)));
	}

	/**
	 * Return a {@link CodeBlock} that applies all the {@code statements} of this
	 * instance. If only one statement is available, it is not completed using the
	 * {@code ;} termination so that it can be used in the context of a lambda.
	 * @return the statement(s)
	 */
	public CodeBlock toCodeBlock() {
		Builder code = CodeBlock.builder();
		for (int i = 0; i < this.statements.size(); i++) {
			Statement statement = this.statements.get(i);
			statement.contribute(code, this.isMulti(), i == this.statements.size() - 1);
		}
		return code.build();
	}

	/**
	 * Return a {@link CodeBlock} that applies all the {@code statements} of this
	 * instance in the context of a lambda.
	 * @param lambda the context of the lambda, must end with {@code ->}
	 * @return the lambda body
	 */
	public CodeBlock toCodeBlock(CodeBlock lambda) {
		Builder code = CodeBlock.builder();
		code.add(lambda);
		if (isMulti()) {
			code.beginControlFlow("");
		}
		else {
			code.add(" ");
		}
		code.add(toCodeBlock());
		if (isMulti()) {
			code.add("\n").unindent().add("}");
		}
		return code.build();
	}

	/**
	 * Return a {@link CodeBlock} that applies all the {@code statements} of this
	 * instance in the context of a lambda.
	 * @param lambda the context of the lambda, must end with {@code ->}
	 * @return the lambda body
	 */
	public CodeBlock toCodeBlock(String lambda) {
		return toCodeBlock(CodeBlock.of(lambda));
	}

	private boolean isMulti() {
		return this.statements.size() > 1;
	}


	private static class Statement {

		private final CodeBlock codeBlock;

		private final boolean addStatementTermination;

		Statement(CodeBlock codeBlock, boolean addStatementTermination) {
			this.codeBlock = codeBlock;
			this.addStatementTermination = addStatementTermination;
		}

		void contribute(CodeBlock.Builder code, boolean multi, boolean isLastStatement) {
			code.add(this.codeBlock);
			if (this.addStatementTermination) {
				if (!isLastStatement) {
					code.add(";\n");
				}
				else if (multi) {
					code.add(";");
				}
			}
		}

		static Statement ofStatement(CodeBlock codeBlock) {
			return new Statement(codeBlock, true);
		}

		static Statement of(CodeBlock codeBlock) {
			return new Statement(codeBlock, false);
		}

	}

}

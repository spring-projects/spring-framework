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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.CodeBlock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MultiStatement}.
 *
 * @author Stephane Nicoll
 */
class MultiStatementTests {

	@Test
	void isEmptyWithNoStatement() {
		assertThat(new MultiStatement().isEmpty()).isTrue();
	}

	@Test
	void isEmptyWithStatement() {
		MultiStatement statements = new MultiStatement();
		statements.addStatement(CodeBlock.of("int i = 0"));
		assertThat(statements.isEmpty()).isFalse();
	}

	@Test
	void singleStatement() {
		MultiStatement statements = new MultiStatement();
		statements.addStatement("field.method($S)", "hello");
		CodeBlock codeBlock = statements.toCodeBlock();
		assertThat(codeBlock.toString()).isEqualTo("field.method(\"hello\")");
	}

	@Test
	void singleStatementWithCallback() {
		MultiStatement statements = new MultiStatement();
		statements.addStatement(code -> code.add("field.method($S)", "hello"));
		CodeBlock codeBlock = statements.toCodeBlock();
		assertThat(codeBlock.toString()).isEqualTo("field.method(\"hello\")");
	}

	@Test
	void singleStatementWithCodeBlock() {
		MultiStatement statements = new MultiStatement();
		statements.addStatement(CodeBlock.of("field.method($S)", "hello"));
		CodeBlock codeBlock = statements.toCodeBlock();
		assertThat(codeBlock.toString()).isEqualTo("field.method(\"hello\")");
	}

	@Test
	void multiStatements() {
		MultiStatement statements = new MultiStatement();
		statements.addStatement("field.method($S)", "hello");
		statements.addStatement("field.anotherMethod($S)", "hello");
		CodeBlock codeBlock = statements.toCodeBlock();
		assertThat(codeBlock.toString()).isEqualTo("""
				field.method("hello");
				field.anotherMethod("hello");""");
	}

	@Test
	void multiStatementsWithCodeBlockRenderedAsIs() {
		MultiStatement statements = new MultiStatement();
		statements.addStatement("field.method($S)", "hello");
		statements.add(CodeBlock.of(("// Hello\n")));
		statements.add(code -> code.add("// World\n"));
		statements.addStatement("field.anotherMethod($S)", "hello");
		CodeBlock codeBlock = statements.toCodeBlock();
		assertThat(codeBlock.toString()).isEqualTo("""
				field.method("hello");
				// Hello
				// World
				field.anotherMethod("hello");""");
	}

	@Test
	void singleStatementWithLambda() {
		MultiStatement statements = new MultiStatement();
		statements.addStatement("field.method($S)", "hello");
		CodeBlock codeBlock = statements.toCodeBlock(CodeBlock.of("() ->"));
		assertThat(codeBlock.toString()).isEqualTo("() -> field.method(\"hello\")");
	}

	@Test
	void multiStatementsWithLambda() {
		MultiStatement statements = new MultiStatement();
		statements.addStatement("field.method($S)", "hello");
		statements.addStatement("field.anotherMethod($S)", "hello");
		CodeBlock codeBlock = statements.toCodeBlock(CodeBlock.of("() ->"));
		assertThat(codeBlock.toString().lines()).containsExactly(
				"() -> {",
				"  field.method(\"hello\");",
				"  field.anotherMethod(\"hello\");",
				"}");
	}

	@Test
	void multiStatementsWithAddAll() {
		MultiStatement statements = new MultiStatement();
		statements.addAll(List.of(0, 1, 2),
				index -> CodeBlock.of("field[$L] = $S", index, "hello"));
		CodeBlock codeBlock = statements.toCodeBlock("() ->");
		assertThat(codeBlock.toString().lines()).containsExactly(
				"() -> {",
				"  field[0] = \"hello\";",
				"  field[1] = \"hello\";",
				"  field[2] = \"hello\";",
				"}");
	}

}

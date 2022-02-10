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

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.CodeBlock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CodeSnippet}.
 *
 * @author Stephane Nicoll
 */
class CodeSnippetTests {

	@Test
	void snippetUsesTabs() {
		CodeBlock.Builder code = CodeBlock.builder();
		code.beginControlFlow("if (condition)");
		code.addStatement("bean.doThis()");
		code.endControlFlow();
		CodeSnippet codeSnippet = CodeSnippet.of(code.build());
		assertThat(codeSnippet.getSnippet()).isEqualTo("""
				if (condition) {
					bean.doThis();
				}
				""");
	}

	@Test
	void snippetResolvesImports() {
		CodeSnippet codeSnippet = CodeSnippet.of(
				CodeBlock.of("$T list = new $T<>()", List.class, ArrayList.class));
		assertThat(codeSnippet.getSnippet()).isEqualTo("List list = new ArrayList<>()");
		assertThat(codeSnippet.hasImport(List.class)).isTrue();
		assertThat(codeSnippet.hasImport(ArrayList.class)).isTrue();
	}

	@Test
	void removeIndent() {
		CodeBlock.Builder code = CodeBlock.builder();
		code.beginControlFlow("if (condition)");
		code.addStatement("doStuff()");
		code.endControlFlow();
		CodeSnippet snippet = CodeSnippet.of(code.build());
		assertThat(snippet.getSnippet().lines()).contains("\tdoStuff();");
		assertThat(snippet.removeIndent(1).getSnippet().lines()).contains("doStuff();");
	}

	@Test
	void processProvidesSnippet() {
		assertThat(CodeSnippet.process(code -> code.add("$T list;", List.class)))
				.isEqualTo("List list;");
	}

}

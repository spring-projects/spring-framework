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

import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;


/**
 * A {@link CodeBlock} wrapper for joining multiple blocks.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class MultiCodeBlock {

	private final List<CodeBlock> codeBlocks = new ArrayList<>();


	/**
	 * Add the specified {@link CodeBlock}.
	 * @param code the code block to add
	 */
	public void add(CodeBlock code) {
		if (code.isEmpty()) {
			throw new IllegalArgumentException("Could not add empty CodeBlock");
		}
		this.codeBlocks.add(code);
	}

	/**
	 * Add a {@link CodeBlock} using the specified callback.
	 * @param code the callback to use
	 */
	public void add(Consumer<Builder> code) {
		Builder builder = CodeBlock.builder();
		code.accept(builder);
		add(builder.build());
	}

	/**
	 * Add a code block using the specified formatted String and the specified
	 * arguments.
	 * @param code the code
	 * @param arguments the arguments
	 * @see Builder#add(String, Object...)
	 */
	public void add(String code, Object... arguments) {
		add(CodeBlock.of(code, arguments));
	}

	/**
	 * Return a {@link CodeBlock} that joins the different blocks registered in
	 * this instance with the specified delimiter.
	 * @param delimiter the delimiter to use (not {@literal null})
	 * @return a {@link CodeBlock} joining the blocks of this instance with the
	 * specified {@code delimiter}
	 * @see CodeBlock#join(Iterable, String)
	 */
	public CodeBlock join(String delimiter) {
		return CodeBlock.join(this.codeBlocks, delimiter);
	}

}

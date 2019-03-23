/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.expression.spel.ast;

import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelNode;

/**
 * An 'identifier' {@link SpelNode}.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class Identifier extends SpelNodeImpl {

	private final TypedValue id;


	public Identifier(String payload, int pos) {
		super(pos);
		this.id = new TypedValue(payload);
	}


	@Override
	public String toStringAST() {
		return String.valueOf(this.id.getValue());
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) {
		return this.id;
	}

}

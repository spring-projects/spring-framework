/*
 * Copyright 2004-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.expression.spel.ast;

import org.antlr.runtime.Token;

/**
 * Represents the literal values TRUE and FALSE.
 * 
 * @author Andy Clement
 * 
 */
public class BooleanLiteral extends Literal {

	private final Boolean value;

	public BooleanLiteral(Token payload, boolean value) {
		super(payload);
		this.value = value;
	}

	@Override
	public Boolean getLiteralValue() {
		return value;
	}

}

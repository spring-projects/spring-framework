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
package org.springframework.expression.spel;


/**
 * Wrap a checked SpelException temporarily so that it can be passed through some infrastructure code
 * (for example Antlr) before being unwrapped at the top level.
 * 
 * @author Andy Clement
 */
public class WrappedSpelException extends RuntimeException {

	public WrappedSpelException(SpelException e) {
		super(e);
	}

	@Override
	public SpelException getCause() {
		return (SpelException) super.getCause();
	}
}

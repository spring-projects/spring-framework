/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.config.java.support;

import static java.lang.String.*;

import java.util.Stack;


/**
 * Thrown by {@link ConfigurationParser} upon detecting circular use of the {@link Import} annotation.
 * 
 * @author Chris Beams
 * @see Import
 * @see ImportStack
 * @see ImportStackHolder
 */
@SuppressWarnings("serial")
class CircularImportException extends IllegalStateException {
	public CircularImportException(ConfigurationClass attemptedImport, Stack<ConfigurationClass> currentImportStack) {
		super(format("A circular @Import has been detected: " +
		             "Illegal attempt by @Configuration class '%s' to import class '%s' as '%s' is " +
		             "already present in the current import stack [%s]",
		             currentImportStack.peek().getSimpleName(), attemptedImport.getSimpleName(),
		             attemptedImport.getSimpleName(), currentImportStack));
	}
}

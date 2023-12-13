/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans.testfixture.beans.factory.aot;

import org.springframework.aot.generate.ValueCodeGenerator;
import org.springframework.aot.generate.ValueCodeGenerator.Delegate;
import org.springframework.javapoet.CodeBlock;

/**
 * A custom value with its code generator {@link Delegate} implementation.
 *
 * @author Stephane Nicoll
 */
public record CustomPropertyValue(String value) {

	public static class ValueCodeGeneratorDelegate implements Delegate {
		@Override
		public CodeBlock generateCode(ValueCodeGenerator valueCodeGenerator, Object value) {
			if (value instanceof CustomPropertyValue data) {
				return CodeBlock.of("new $T($S)", CustomPropertyValue.class, data.value);
			}
			return null;
		}
	}

}

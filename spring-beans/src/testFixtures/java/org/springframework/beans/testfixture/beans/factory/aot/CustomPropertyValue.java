/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

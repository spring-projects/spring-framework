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

package org.springframework.aot.graalvm;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.oracle.graal.pointsto.infrastructure.SubstitutionProcessor;
import com.oracle.svm.core.meta.SubstrateObjectConstant;
import com.oracle.svm.core.util.UserError;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;
import org.graalvm.compiler.debug.DebugContext;

/**
 * {@link SubstitutionProcessor} to compute at build time the value of the
 * boolean static fields identified by {@link #patterns} in order to allow
 * efficient code shrinking without using class build time initialization.
 *
 * @author Phillip Webb
 * @author Sebastien Deleuze
 * @since 6.0
 */
class ConstantFieldSubstitutionProcessor extends SubstitutionProcessor {

	// Later should be an explicit signal, like an annotation or even a Java keyword
	private static Pattern[] patterns = {
			Pattern.compile(Pattern.quote("org.springframework.core.NativeDetector#imageCode")),
			Pattern.compile(Pattern.quote("org.springframework.") + ".*#.*Present"),
			Pattern.compile(Pattern.quote("org.springframework.") + ".*#.*PRESENT"),
			Pattern.compile(Pattern.quote("reactor.") + ".*#.*Available")
	};

	private final ThrowawayClassLoader throwawayClassLoader;

	private Set<String> seen = new LinkedHashSet<>();


	ConstantFieldSubstitutionProcessor(DebugContext debug, ClassLoader applicationClassLoader) {
		this.throwawayClassLoader = new ThrowawayClassLoader(applicationClassLoader);
	}


	@Override
	public ResolvedJavaField lookup(ResolvedJavaField field) {
		ResolvedJavaType declaringClass = field.getDeclaringClass();
		if (field.getType().getJavaKind() == JavaKind.Boolean && field.isStatic()) {
			String fieldIdentifier = declaringClass.toJavaName() + "#" + field.getName();
			for (Pattern pattern : patterns) {
				if (pattern.matcher(fieldIdentifier).matches()) {
					JavaConstant constant = lookupConstant(declaringClass.toJavaName(), field.getName());
					if (constant != null) {
						// TODO Use proper logging only when --verbose is specified when https://github.com/oracle/graal/issues/4669 will be fixed
						if (!this.seen.contains(fieldIdentifier)) {
							this.seen.add(fieldIdentifier);
							System.out.println("Field " + fieldIdentifier + " set to " + constant.toValueString() + " at build time");
						}
						return new ConstantReadableJavaField(field, constant);
					}
				}
			}
		}
		return super.lookup(field);
	}

	private JavaConstant lookupConstant(String className, String fieldName) {
		try {
			Class<?> throwawayClass = this.throwawayClassLoader.loadClass(className);
			Field field = throwawayClass.getDeclaredField(fieldName);
			field.setAccessible(true);
			Object value = field.get(null);
			if (!(value instanceof Boolean)) {
				throw UserError.abort("Unable to get the value of " + className + "." + fieldName);
			}
			return SubstrateObjectConstant.forBoxedValue(JavaKind.Boolean, value);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to read value from " + className + "." + fieldName, ex);
		}
	}

}

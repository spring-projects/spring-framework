/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aot;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;

import org.springframework.javapoet.CodeBlock;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Code generator to apply {@link AutowiredArguments}.
 *
 * <p>Generates code in the form: {@code args.get(0), args.get(1)} or
 * {@code args.get(0, String.class), args.get(1, Integer.class)}
 *
 * <p>The simpler form is only used if the target method or constructor is
 * unambiguous.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
public class AutowiredArgumentsCodeGenerator {

	private final Class<?> target;

	private final Executable executable;


	public AutowiredArgumentsCodeGenerator(Class<?> target, Executable executable) {
		this.target = target;
		this.executable = executable;
	}


	public CodeBlock generateCode(Class<?>[] parameterTypes) {
		return generateCode(parameterTypes, 0, "args");
	}

	public CodeBlock generateCode(Class<?>[] parameterTypes, int startIndex) {
		return generateCode(parameterTypes, startIndex, "args");
	}

	public CodeBlock generateCode(Class<?>[] parameterTypes, int startIndex, String variableName) {
		Assert.notNull(parameterTypes, "'parameterTypes' must not be null");
		Assert.notNull(variableName, "'variableName' must not be null");
		boolean ambiguous = isAmbiguous();
		CodeBlock.Builder code = CodeBlock.builder();
		for (int i = startIndex; i < parameterTypes.length; i++) {
			code.add(i > startIndex ? ", " : "");
			if (!ambiguous) {
				code.add("$L.get($L)", variableName, i);
			}
			else {
				code.add("$L.get($L, $T.class)", variableName, i, parameterTypes[i]);
			}
		}
		return code.build();
	}

	private boolean isAmbiguous() {
		if (this.executable instanceof Constructor<?> constructor) {
			return Arrays.stream(this.target.getDeclaredConstructors())
					.filter(Predicate.not(constructor::equals))
					.anyMatch(this::hasSameParameterCount);
		}
		if (this.executable instanceof Method method) {
			return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(this.target))
					.filter(Predicate.not(method::equals))
					.filter(candidate -> candidate.getName().equals(method.getName()))
					.anyMatch(this::hasSameParameterCount);
		}
		return true;
	}

	private boolean hasSameParameterCount(Executable executable) {
		return this.executable.getParameterCount() == executable.getParameterCount();
	}

}

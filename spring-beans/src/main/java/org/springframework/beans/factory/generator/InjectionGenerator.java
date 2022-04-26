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

package org.springframework.beans.factory.generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.aot.generator.ProtectedAccess;
import org.springframework.aot.generator.ProtectedAccess.Options;
import org.springframework.beans.factory.generator.config.BeanDefinitionRegistrar.BeanInstanceContext;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.util.ClassUtils;

/**
 * Generate the necessary code to {@link #generateInstantiation(Executable)
 * create a bean instance} or {@link #generateInjection(Member, boolean)
 * inject dependencies}.
 * <p/>
 * The generator assumes a number of variables to be accessible:
 * <ul>
 *     <li>{@code beanFactory}: the general {@code DefaultListableBeanFactory}</li>
 *     <li>{@code instanceContext}: the {@link BeanInstanceContext} callback</li>
 *     <li>{@code bean}: the variable that refers to the bean instance</li>
 * </ul>
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 */
public class InjectionGenerator {

	private static final Options METHOD_INJECTION_OPTIONS = Options.defaults()
			.useReflection(member -> false).build();

	private final BeanParameterGenerator parameterGenerator = new BeanParameterGenerator();

	private final BeanFieldGenerator fieldGenerator = new BeanFieldGenerator();


	/**
	 * Generate the necessary code to instantiate an object using the specified
	 * {@link Executable}. The code is suitable to be assigned to a variable
	 * or used as a {@literal return} statement.
	 * @param creator the executable to invoke to create an instance of the
	 * requested object
	 * @return the code to instantiate an object using the specified executable
	 */
	public CodeBlock generateInstantiation(Executable creator) {
		if (creator instanceof Constructor<?> constructor) {
			return generateConstructorInstantiation(constructor);
		}
		if (creator instanceof Method method) {
			return generateMethodInstantiation(method);
		}
		throw new IllegalArgumentException("Could not handle creator " + creator);
	}

	/**
	 * Generate the code to inject a value resolved by {@link BeanInstanceContext}
	 * in the specified {@link Member}.
	 * @param member the field or method to inject
	 * @param required whether the value is required
	 * @return a statement that injects a value to the specified member
	 * @see #getProtectedAccessInjectionOptions(Member)
	 */
	public CodeBlock generateInjection(Member member, boolean required) {
		if (member instanceof Method method) {
			return generateMethodInjection(method, required);
		}
		if (member instanceof Field field) {
			return generateFieldInjection(field, required);
		}
		throw new IllegalArgumentException("Could not handle member " + member);
	}

	/**
	 * Return the {@link Options} to use if protected access analysis is
	 * required for the specified {@link Member}.
	 * @param member the field or method to handle
	 * @return the options to use to analyse protected access
	 * @see ProtectedAccess
	 */
	public Options getProtectedAccessInjectionOptions(Member member) {
		if (member instanceof Method) {
			return METHOD_INJECTION_OPTIONS;
		}
		if (member instanceof Field) {
			return BeanFieldGenerator.FIELD_OPTIONS;
		}
		throw new IllegalArgumentException("Could not handle member " + member);
	}

	private CodeBlock generateConstructorInstantiation(Constructor<?> creator) {
		Builder code = CodeBlock.builder();
		Class<?> declaringType = ClassUtils.getUserClass(creator.getDeclaringClass());
		boolean innerClass = isInnerClass(declaringType);
		Class<?>[] parameterTypes = Arrays.stream(creator.getParameters()).map(Parameter::getType)
				.toArray(Class<?>[]::new);
		// Shortcut for common case
		if (innerClass && parameterTypes.length == 1) {
			code.add("beanFactory.getBean($T.class).new $L()", declaringType.getEnclosingClass(),
					declaringType.getSimpleName());
			return code.build();
		}
		if (parameterTypes.length == 0) {
			code.add("new $T()", declaringType);
			return code.build();
		}
		boolean isAmbiguous = Arrays.stream(creator.getDeclaringClass().getDeclaredConstructors())
				.filter(constructor -> constructor.getParameterCount() == parameterTypes.length).count() > 1;
		code.add("instanceContext.create(beanFactory, (attributes) ->");
		List<CodeBlock> parameters = resolveParameters(creator.getParameters(), isAmbiguous);
		if (innerClass) { // Remove the implicit argument
			parameters.remove(0);
		}

		code.add(" ");
		if (innerClass) {
			code.add("beanFactory.getBean($T.class).new $L(", declaringType.getEnclosingClass(),
					declaringType.getSimpleName());
		}
		else {
			code.add("new $T(", declaringType);
		}
		for (int i = 0; i < parameters.size(); i++) {
			code.add(parameters.get(i));
			if (i < parameters.size() - 1) {
				code.add(", ");
			}
		}
		code.add(")");
		code.add(")");
		return code.build();
	}

	private static boolean isInnerClass(Class<?> type) {
		return type.isMemberClass() && !Modifier.isStatic(type.getModifiers());
	}

	private CodeBlock generateMethodInstantiation(Method injectionPoint) {
		if (injectionPoint.getParameterCount() == 0) {
			Builder code = CodeBlock.builder();
			Class<?> declaringType = injectionPoint.getDeclaringClass();
			if (Modifier.isStatic(injectionPoint.getModifiers())) {
				code.add("$T", declaringType);
			}
			else {
				code.add("beanFactory.getBean($T.class)", declaringType);
			}
			code.add(".$L()", injectionPoint.getName());
			return code.build();
		}
		return generateMethodInvocation(injectionPoint, code -> code.add(".create(beanFactory, (attributes) ->"), true);
	}

	private CodeBlock generateMethodInjection(Method injectionPoint, boolean required) {
		Consumer<Builder> attributesResolver = code -> {
			if (required) {
				code.add(".invoke(beanFactory, (attributes) ->");
			}
			else {
				code.add(".resolve(beanFactory, false).ifResolved((attributes) ->");
			}
		};
		return generateMethodInvocation(injectionPoint, attributesResolver, false);
	}

	private CodeBlock generateMethodInvocation(Method injectionPoint, Consumer<Builder> attributesResolver, boolean instantiation) {
		Builder code = CodeBlock.builder();
		code.add("instanceContext");
		if (!instantiation) {
			code.add(".method($S, ", injectionPoint.getName());
			code.add(this.parameterGenerator.generateExecutableParameterTypes(injectionPoint));
			code.add(")\n").indent().indent();
		}
		attributesResolver.accept(code);
		Parameter[] methodParameters = injectionPoint.getParameters();
		boolean isAmbiguous = Arrays.stream(injectionPoint.getDeclaringClass().getDeclaredMethods())
				.filter(method -> method.getName().equals(injectionPoint.getName()) && method.getParameterCount() == methodParameters.length).count() > 1;
		List<CodeBlock> parameters = resolveParameters(methodParameters, isAmbiguous);
		code.add(" ");
		if (instantiation) {
			if (Modifier.isStatic(injectionPoint.getModifiers())) {
				code.add("$T", injectionPoint.getDeclaringClass());
			}
			else {
				code.add("beanFactory.getBean($T.class)", injectionPoint.getDeclaringClass());
			}
		}
		else {
			code.add("bean");
		}
		code.add(".$L(", injectionPoint.getName());
		code.add(CodeBlock.join(parameters, ", "));
		code.add(")");
		code.add(")");
		if (!instantiation) {
			code.unindent().unindent();
		}
		return code.build();
	}

	CodeBlock generateFieldInjection(Field injectionPoint, boolean required) {
		Builder code = CodeBlock.builder();
		code.add("instanceContext.field($S", injectionPoint.getName());
		code.add(")\n").indent().indent();
		if (required) {
			code.add(".invoke(beanFactory, ");
		}
		else {
			code.add(".resolve(beanFactory, false).ifResolved(");
		}
		code.add(this.fieldGenerator.generateSetValue("bean", injectionPoint,
				CodeBlock.of("attributes.get(0)")).toLambda("(attributes) ->"));
		code.add(")").unindent().unindent();
		return code.build();
	}

	private List<CodeBlock> resolveParameters(Parameter[] parameters, boolean shouldCast) {
		List<CodeBlock> parameterValues = new ArrayList<>();
		for (int i = 0; i < parameters.length; i++) {
			if (shouldCast) {
				parameterValues.add(CodeBlock.of("attributes.get($L, $T.class)", i, parameters[i].getType()));
			}
			else {
				parameterValues.add(CodeBlock.of("attributes.get($L)", i));
			}
		}
		return parameterValues;
	}

}

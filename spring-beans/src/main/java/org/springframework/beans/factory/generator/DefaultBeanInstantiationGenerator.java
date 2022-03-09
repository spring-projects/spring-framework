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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.generator.DefaultCodeContribution;
import org.springframework.aot.generator.ProtectedAccess.Options;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.ClassUtils;

/**
 * Default {@link BeanInstantiationGenerator} implementation.
 *
 * @author Stephane Nicoll
 * @see BeanInstantiationContribution
 */
class DefaultBeanInstantiationGenerator implements BeanInstantiationGenerator {

	private final Executable instanceCreator;

	private final List<BeanInstantiationContribution> contributions;

	private final InjectionGenerator injectionGenerator;

	private final Options beanInstanceOptions;


	DefaultBeanInstantiationGenerator(Executable instanceCreator, List<BeanInstantiationContribution> contributions) {
		this.instanceCreator = instanceCreator;
		this.contributions = List.copyOf(contributions);
		this.injectionGenerator = new InjectionGenerator();
		this.beanInstanceOptions = Options.defaults().useReflection(member -> false)
				.assignReturnType(member -> !this.contributions.isEmpty()).build();
	}

	@Override
	public Executable getInstanceCreator() {
		return this.instanceCreator;
	}

	@Override
	public CodeContribution generateBeanInstantiation(RuntimeHints runtimeHints) {
		DefaultCodeContribution codeContribution = new DefaultCodeContribution(runtimeHints);
		codeContribution.protectedAccess().analyze(this.instanceCreator, this.beanInstanceOptions);
		if (this.instanceCreator instanceof Constructor<?> constructor) {
			generateBeanInstantiation(codeContribution, constructor);
		}
		else if (this.instanceCreator instanceof Method method) {
			generateBeanInstantiation(codeContribution, method);
		}
		return codeContribution;
	}

	private void generateBeanInstantiation(CodeContribution codeContribution, Constructor<?> constructor) {
		Class<?> declaringType = ClassUtils.getUserClass(constructor.getDeclaringClass());
		boolean innerClass = isInnerClass(declaringType);
		boolean multiStatements = !this.contributions.isEmpty();
		int minArgs = isInnerClass(declaringType) ? 2 : 1;
		CodeBlock.Builder code = CodeBlock.builder();
		// Shortcut for common case
		if (!multiStatements && constructor.getParameterTypes().length < minArgs) {
			if (innerClass) {
				code.add("() -> beanFactory.getBean($T.class).new $L()",
						declaringType.getEnclosingClass(), declaringType.getSimpleName());
			}
			else {
				// Only apply the shortcut if there's one candidate
				if (declaringType.getDeclaredConstructors().length > 1) {
					code.add("() -> new $T()", declaringType);
				}
				else {
					code.add("$T::new", declaringType);
				}
			}
			codeContribution.statements().addStatement(code.build());
			return;
		}
		codeContribution.runtimeHints().reflection().registerConstructor(constructor,
				hint -> hint.withMode(ExecutableMode.INTROSPECT));
		code.add("(instanceContext) ->");
		branch(multiStatements, () -> code.beginControlFlow(""), () -> code.add(" "));
		if (multiStatements) {
			code.add("$T bean = ", declaringType);
		}
		code.add(this.injectionGenerator.generateInstantiation(constructor));
		codeContribution.statements().addStatement(code.build());

		if (multiStatements) {
			for (BeanInstantiationContribution contribution : this.contributions) {
				contribution.applyTo(codeContribution);
			}
			codeContribution.statements().addStatement("return bean")
					.add(codeBlock -> codeBlock.unindent().add("}"));
		}
	}

	private static boolean isInnerClass(Class<?> type) {
		return type.isMemberClass() && !Modifier.isStatic(type.getModifiers());
	}

	private void generateBeanInstantiation(CodeContribution codeContribution, Method method) {
		// Factory method can be introspected
		codeContribution.runtimeHints().reflection().registerMethod(method,
				hint -> hint.withMode(ExecutableMode.INTROSPECT));
		List<Class<?>> parameterTypes = new ArrayList<>(Arrays.asList(method.getParameterTypes()));
		boolean multiStatements = !this.contributions.isEmpty();
		Class<?> declaringType = method.getDeclaringClass();
		CodeBlock.Builder code = CodeBlock.builder();
		// Shortcut for common case
		if (!multiStatements && parameterTypes.isEmpty()) {
			code.add("() -> ");
			branch(Modifier.isStatic(method.getModifiers()),
					() -> code.add("$T", declaringType),
					() -> code.add("beanFactory.getBean($T.class)", declaringType));
			code.add(".$L()", method.getName());
			codeContribution.statements().addStatement(code.build());
			return;
		}
		code.add("(instanceContext) ->");
		branch(multiStatements, () -> code.beginControlFlow(""), () -> code.add(" "));
		if (multiStatements) {
			code.add("$T bean = ", method.getReturnType());
		}
		code.add(this.injectionGenerator.generateInstantiation(method));
		codeContribution.statements().addStatement(code.build());
		if (multiStatements) {
			for (BeanInstantiationContribution contribution : this.contributions) {
				contribution.applyTo(codeContribution);
			}
			codeContribution.statements().addStatement("return bean")
					.add(codeBlock -> codeBlock.unindent().add("}"));
		}
	}

	private static void branch(boolean condition, Runnable ifTrue, Runnable ifFalse) {
		if (condition) {
			ifTrue.run();
		}
		else {
			ifFalse.run();
		}
	}

}

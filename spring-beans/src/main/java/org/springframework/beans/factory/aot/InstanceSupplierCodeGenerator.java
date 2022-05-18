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

package org.springframework.beans.factory.aot;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Consumer;

import org.springframework.aot.generate.AccessVisibility;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.ThrowingSupplier;

/**
 * Internal code generator to create an {@link InstanceSupplier}.
 * <p>
 * Generates code in the form:<pre class="code">{@code
 * InstanceSupplier.of(TheGeneratedClass::getMyBeanInstance);
 * }</pre>
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
class InstanceSupplierCodeGenerator {

	private static final String REGISTERED_BEAN_PARAMETER_NAME = "registeredBean";

	private static final javax.lang.model.element.Modifier[] PRIVATE_STATIC = {
			javax.lang.model.element.Modifier.PRIVATE,
			javax.lang.model.element.Modifier.STATIC };

	private static final CodeBlock NO_ARGS = CodeBlock.of("");

	private static final Consumer<ExecutableHint.Builder> INTROSPECT = builder -> builder
			.withMode(ExecutableMode.INTROSPECT);


	private final GenerationContext generationContext;

	private final ClassName className;

	private final MethodGenerator methodGenerator;

	private final boolean allowDirectSupplierShortcut;


	InstanceSupplierCodeGenerator(GenerationContext generationContext,
			ClassName className, MethodGenerator methodGenerator,
			boolean allowDirectSupplierShortcut) {

		this.generationContext = generationContext;
		this.className = className;
		this.methodGenerator = methodGenerator;
		this.allowDirectSupplierShortcut = allowDirectSupplierShortcut;
	}


	CodeBlock generateCode(RegisteredBean registeredBean,
			Executable constructorOrFactoryMethod) {

		if (constructorOrFactoryMethod instanceof Constructor<?> constructor) {
			return generateCodeForConstructor(registeredBean, constructor);
		}
		if (constructorOrFactoryMethod instanceof Method method) {
			return generateCodeForFactoryMethod(registeredBean, method);
		}
		throw new IllegalStateException(
				"No suitable executor found for " + registeredBean.getBeanName());
	}

	private CodeBlock generateCodeForConstructor(RegisteredBean registeredBean,
			Constructor<?> constructor) {

		String name = registeredBean.getBeanName();
		Class<?> declaringClass = ClassUtils
				.getUserClass(constructor.getDeclaringClass());
		boolean dependsOnBean = ClassUtils.isInnerClass(declaringClass);
		AccessVisibility accessVisibility = getAccessVisibility(registeredBean,
				constructor);
		if (accessVisibility == AccessVisibility.PUBLIC
				|| accessVisibility == AccessVisibility.PACKAGE_PRIVATE) {
			return generateCodeForAccessibleConstructor(name, constructor, declaringClass,
					dependsOnBean);
		}
		return generateCodeForInaccessibleConstructor(name, constructor, declaringClass,
				dependsOnBean);
	}

	private CodeBlock generateCodeForAccessibleConstructor(String name,
			Constructor<?> constructor, Class<?> declaringClass, boolean dependsOnBean) {

		this.generationContext.getRuntimeHints().reflection()
				.registerConstructor(constructor, INTROSPECT);
		if (!dependsOnBean && constructor.getParameterCount() == 0) {
			if (!this.allowDirectSupplierShortcut) {
				return CodeBlock.of("$T.using($T::new)", InstanceSupplier.class,
						declaringClass);
			}
			if (!isThrowingCheckedException(constructor)) {
				return CodeBlock.of("$T::new", declaringClass);
			}
			return CodeBlock.of("$T.of($T::new)", ThrowingSupplier.class,
					declaringClass);
		}
		GeneratedMethod getInstanceMethod = generateGetInstanceMethod()
				.using(builder -> buildGetInstanceMethodForConstructor(builder, name,
						constructor, declaringClass, dependsOnBean, PRIVATE_STATIC));
		return CodeBlock.of("$T.of($T::$L)", InstanceSupplier.class, this.className,
				getInstanceMethod.getName());
	}

	private CodeBlock generateCodeForInaccessibleConstructor(String name,
			Constructor<?> constructor, Class<?> declaringClass, boolean dependsOnBean) {

		this.generationContext.getRuntimeHints().reflection()
				.registerConstructor(constructor);
		GeneratedMethod getInstanceMethod = generateGetInstanceMethod().using(builder -> {
			builder.addJavadoc("Instantiate the bean instance for '$L'.", name);
			builder.addModifiers(PRIVATE_STATIC);
			builder.returns(declaringClass);
			builder.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME);
			int parameterOffset = (!dependsOnBean) ? 0 : 1;
			builder.addStatement(
					generateResolverForConstructor(constructor, parameterOffset));
			builder.addStatement("return resolver.resolveAndInstantiate($L)",
					REGISTERED_BEAN_PARAMETER_NAME);
		});
		return CodeBlock.of("$T.of($T::$L)", InstanceSupplier.class, this.className,
				getInstanceMethod.getName());
	}

	private void buildGetInstanceMethodForConstructor(MethodSpec.Builder builder,
			String name, Constructor<?> constructor, Class<?> declaringClass,
			boolean dependsOnBean, javax.lang.model.element.Modifier... modifiers) {

		builder.addJavadoc("Create the bean instance for '$L'.", name);
		builder.addModifiers(modifiers);
		builder.returns(declaringClass);
		builder.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME);
		if (constructor.getParameterCount() == 0) {
			CodeBlock instantiationCode = generateNewInstanceCodeForConstructor(
					dependsOnBean, declaringClass, NO_ARGS);
			builder.addCode(generateReturnStatement(instantiationCode));
		}
		else {
			int parameterOffset = (!dependsOnBean) ? 0 : 1;
			CodeBlock.Builder code = CodeBlock.builder();
			code.addStatement(
					generateResolverForConstructor(constructor, parameterOffset));
			CodeBlock arguments = new AutowiredArgumentsCodeGenerator(declaringClass,
					constructor).generateCode(constructor.getParameterTypes(),
							parameterOffset);
			CodeBlock newInstance = generateNewInstanceCodeForConstructor(dependsOnBean,
					declaringClass, arguments);
			code.addStatement("return resolver.resolve($L, (args) -> $L)",
					REGISTERED_BEAN_PARAMETER_NAME, newInstance);
			builder.addCode(code.build());
		}
	}

	private CodeBlock generateResolverForConstructor(Constructor<?> constructor,
			int parameterOffset) {

		CodeBlock parameterTypes = generateParameterTypesCode(
				constructor.getParameterTypes(), parameterOffset);
		return CodeBlock.of("$T resolver = $T.forConstructor($L)",
				AutowiredInstantiationArgumentsResolver.class,
				AutowiredInstantiationArgumentsResolver.class, parameterTypes);
	}

	private CodeBlock generateNewInstanceCodeForConstructor(boolean dependsOnBean,
			Class<?> declaringClass, CodeBlock args) {

		if (!dependsOnBean) {
			return CodeBlock.of("new $T($L)", declaringClass, args);
		}
		return CodeBlock.of("$L.getBeanFactory().getBean($T.class).new $L($L)",
				REGISTERED_BEAN_PARAMETER_NAME, declaringClass.getEnclosingClass(),
				declaringClass.getSimpleName(), args);
	}

	private CodeBlock generateCodeForFactoryMethod(RegisteredBean registeredBean,
			Method factoryMethod) {

		String name = registeredBean.getBeanName();
		Class<?> declaringClass = ClassUtils
				.getUserClass(factoryMethod.getDeclaringClass());
		boolean dependsOnBean = !Modifier.isStatic(factoryMethod.getModifiers());
		AccessVisibility accessVisibility = getAccessVisibility(registeredBean,
				factoryMethod);
		if (accessVisibility == AccessVisibility.PUBLIC
				|| accessVisibility == AccessVisibility.PACKAGE_PRIVATE) {
			return generateCodeForAccessibleFactoryMethod(name, factoryMethod,
					declaringClass, dependsOnBean);
		}
		return generateCodeForInaccessibleFactoryMethod(name, factoryMethod,
				declaringClass);
	}

	private CodeBlock generateCodeForAccessibleFactoryMethod(String name,
			Method factoryMethod, Class<?> declaringClass, boolean dependsOnBean) {
		this.generationContext.getRuntimeHints().reflection()
				.registerMethod(factoryMethod, INTROSPECT);
		if (!dependsOnBean && factoryMethod.getParameterCount() == 0) {
			if (!this.allowDirectSupplierShortcut) {
				return CodeBlock.of("$T.using($T::$L)", InstanceSupplier.class,
						declaringClass, factoryMethod.getName());
			}
			if (!isThrowingCheckedException(factoryMethod)) {
				return CodeBlock.of("$T::$L", declaringClass, factoryMethod.getName());
			}
			return CodeBlock.of("$T.of($T::$L)", ThrowingSupplier.class, declaringClass,
					factoryMethod.getName());
		}
		GeneratedMethod getInstanceMethod = generateGetInstanceMethod()
				.using(builder -> buildGetInstanceMethodForFactoryMethod(builder, name,
						factoryMethod, declaringClass, dependsOnBean, PRIVATE_STATIC));
		return CodeBlock.of("$T.of($T::$L)", InstanceSupplier.class, this.className,
				getInstanceMethod.getName());
	}

	private CodeBlock generateCodeForInaccessibleFactoryMethod(String name,
			Method factoryMethod, Class<?> declaringClass) {

		this.generationContext.getRuntimeHints().reflection()
				.registerMethod(factoryMethod);
		GeneratedMethod getInstanceMethod = generateGetInstanceMethod().using(builder -> {
			builder.addJavadoc("Instantiate the bean instance for '$L'.", name);
			builder.addModifiers(PRIVATE_STATIC);
			builder.returns(factoryMethod.getReturnType());
			builder.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME);
			builder.addStatement(generateResolverForFactoryMethod(factoryMethod,
					declaringClass, factoryMethod.getName()));
			builder.addStatement("return resolver.resolveAndInstantiate($L)",
					REGISTERED_BEAN_PARAMETER_NAME);
		});
		return CodeBlock.of("$T.of($T::$L)", InstanceSupplier.class, this.className,
				getInstanceMethod.getName());
	}

	private void buildGetInstanceMethodForFactoryMethod(MethodSpec.Builder builder,
			String name, Method factoryMethod, Class<?> declaringClass,
			boolean dependsOnBean, javax.lang.model.element.Modifier... modifiers) {

		String factoryMethodName = factoryMethod.getName();
		builder.addJavadoc("Get the bean instance for '$L'.", name);
		builder.addModifiers(modifiers);
		builder.returns(factoryMethod.getReturnType());
		if (isThrowingCheckedException(factoryMethod)) {
			builder.addException(Exception.class);
		}
		builder.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME);
		if (factoryMethod.getParameterCount() == 0) {
			CodeBlock instantiationCode = generateNewInstanceCodeForMethod(dependsOnBean,
					declaringClass, factoryMethodName, NO_ARGS);
			builder.addCode(generateReturnStatement(instantiationCode));
		}
		else {
			CodeBlock.Builder code = CodeBlock.builder();
			code.addStatement(generateResolverForFactoryMethod(factoryMethod,
					declaringClass, factoryMethodName));
			CodeBlock arguments = new AutowiredArgumentsCodeGenerator(declaringClass,
					factoryMethod).generateCode(factoryMethod.getParameterTypes());
			CodeBlock newInstance = generateNewInstanceCodeForMethod(dependsOnBean,
					declaringClass, factoryMethodName, arguments);
			code.addStatement("return resolver.resolve($L, (args) -> $L)",
					REGISTERED_BEAN_PARAMETER_NAME, newInstance);
			builder.addCode(code.build());
		}
	}

	private CodeBlock generateResolverForFactoryMethod(Method factoryMethod,
			Class<?> declaringClass, String factoryMethodName) {

		if (factoryMethod.getParameterCount() == 0) {
			return CodeBlock.of("$T resolver = $T.forFactoryMethod($T.class, $S)",
					AutowiredInstantiationArgumentsResolver.class,
					AutowiredInstantiationArgumentsResolver.class, declaringClass,
					factoryMethodName);
		}
		CodeBlock parameterTypes = generateParameterTypesCode(
				factoryMethod.getParameterTypes(), 0);
		return CodeBlock.of("$T resolver = $T.forFactoryMethod($T.class, $S, $L)",
				AutowiredInstantiationArgumentsResolver.class,
				AutowiredInstantiationArgumentsResolver.class, declaringClass,
				factoryMethodName, parameterTypes);
	}

	private CodeBlock generateNewInstanceCodeForMethod(boolean dependsOnBean,
			Class<?> declaringClass, String factoryMethodName, CodeBlock args) {

		if (!dependsOnBean) {
			return CodeBlock.of("$T.$L($L)", declaringClass, factoryMethodName, args);
		}
		return CodeBlock.of("$L.getBeanFactory().getBean($T.class).$L($L)",
				REGISTERED_BEAN_PARAMETER_NAME, declaringClass, factoryMethodName, args);
	}

	private CodeBlock generateReturnStatement(CodeBlock instantiationCode) {
		CodeBlock.Builder code = CodeBlock.builder();
		code.addStatement("return $L", instantiationCode);
		return code.build();
	}

	protected AccessVisibility getAccessVisibility(RegisteredBean registeredBean,
			Member member) {

		AccessVisibility beanTypeAccessVisibility = AccessVisibility
				.forResolvableType(registeredBean.getBeanType());
		AccessVisibility memberAccessVisibility = AccessVisibility.forMember(member);
		return AccessVisibility.lowest(beanTypeAccessVisibility, memberAccessVisibility);
	}

	private CodeBlock generateParameterTypesCode(Class<?>[] parameterTypes, int offset) {
		CodeBlock.Builder builder = CodeBlock.builder();
		for (int i = offset; i < parameterTypes.length; i++) {
			builder.add(i != offset ? ", " : "");
			builder.add("$T.class", parameterTypes[i]);
		}
		return builder.build();
	}

	private GeneratedMethod generateGetInstanceMethod() {
		return this.methodGenerator.generateMethod("get", "instance");
	}

	private boolean isThrowingCheckedException(Executable executable) {
		return Arrays.stream(executable.getGenericExceptionTypes())
				.map(ResolvableType::forType).map(ResolvableType::toClass)
				.anyMatch(Exception.class::isAssignableFrom);
	}

}

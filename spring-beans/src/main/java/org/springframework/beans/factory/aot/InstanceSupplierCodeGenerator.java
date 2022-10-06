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

import org.springframework.aot.generate.AccessControl;
import org.springframework.aot.generate.AccessControl.Visibility;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.ThrowingSupplier;

/**
 * Internal code generator to create an {@link InstanceSupplier}, usually in
 * the form of a {@link BeanInstanceSupplier} that retains the executable
 * that is used to instantiate the bean.
 * <p>
 * Generated code is usually a method reference that generate the
 * {@link BeanInstanceSupplier}, but some shortcut can be used as well such
 * as:
 * <pre class="code">
 * {@code InstanceSupplier.of(TheGeneratedClass::getMyBeanInstance);}
 * </pre>
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
class InstanceSupplierCodeGenerator {

	private static final String REGISTERED_BEAN_PARAMETER_NAME = "registeredBean";

	private static final String ARGS_PARAMETER_NAME = "args";

	private static final javax.lang.model.element.Modifier[] PRIVATE_STATIC = {
			javax.lang.model.element.Modifier.PRIVATE,
			javax.lang.model.element.Modifier.STATIC };

	private static final CodeBlock NO_ARGS = CodeBlock.of("");


	private final GenerationContext generationContext;

	private final ClassName className;

	private final GeneratedMethods generatedMethods;

	private final boolean allowDirectSupplierShortcut;


	InstanceSupplierCodeGenerator(GenerationContext generationContext,
			ClassName className, GeneratedMethods generatedMethods, boolean allowDirectSupplierShortcut) {

		this.generationContext = generationContext;
		this.className = className;
		this.generatedMethods = generatedMethods;
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

	private CodeBlock generateCodeForConstructor(RegisteredBean registeredBean, Constructor<?> constructor) {
		String beanName = registeredBean.getBeanName();
		Class<?> beanClass = registeredBean.getBeanClass();
		Class<?> declaringClass = constructor.getDeclaringClass();
		boolean dependsOnBean = ClassUtils.isInnerClass(declaringClass);
		Visibility accessVisibility = getAccessVisibility(registeredBean, constructor);
		if (accessVisibility != Visibility.PRIVATE) {
			return generateCodeForAccessibleConstructor(beanName, beanClass, constructor,
					dependsOnBean, declaringClass);
		}
		return generateCodeForInaccessibleConstructor(beanName, beanClass, constructor, dependsOnBean);
	}

	private CodeBlock generateCodeForAccessibleConstructor(String beanName, Class<?> beanClass,
			Constructor<?> constructor, boolean dependsOnBean, Class<?> declaringClass) {

		this.generationContext.getRuntimeHints().reflection().registerConstructor(
				constructor, ExecutableMode.INTROSPECT);
		if (!dependsOnBean && constructor.getParameterCount() == 0) {
			if (!this.allowDirectSupplierShortcut) {
				return CodeBlock.of("$T.using($T::new)", InstanceSupplier.class, declaringClass);
			}
			if (!isThrowingCheckedException(constructor)) {
				return CodeBlock.of("$T::new", declaringClass);
			}
			return CodeBlock.of("$T.of($T::new)", ThrowingSupplier.class, declaringClass);
		}
		GeneratedMethod generatedMethod = generateGetInstanceSupplierMethod(method ->
				buildGetInstanceMethodForConstructor(method, beanName, beanClass, constructor,
						declaringClass, dependsOnBean, PRIVATE_STATIC));
		return generateReturnStatement(generatedMethod);
	}

	private CodeBlock generateCodeForInaccessibleConstructor(String beanName,
			Class<?> beanClass, Constructor<?> constructor, boolean dependsOnBean) {

		this.generationContext.getRuntimeHints().reflection()
				.registerConstructor(constructor, ExecutableMode.INVOKE);
		GeneratedMethod generatedMethod = generateGetInstanceSupplierMethod(method -> {
			method.addJavadoc("Get the bean instance supplier for '$L'.", beanName);
			method.addModifiers(PRIVATE_STATIC);
			method.returns(ParameterizedTypeName.get(BeanInstanceSupplier.class, beanClass));
			int parameterOffset = (!dependsOnBean) ? 0 : 1;
			method.addStatement(generateResolverForConstructor(beanClass, constructor, parameterOffset));
		});
		return generateReturnStatement(generatedMethod);
	}

	private void buildGetInstanceMethodForConstructor(MethodSpec.Builder method,
			String beanName, Class<?> beanClass, Constructor<?> constructor, Class<?> declaringClass,
			boolean dependsOnBean, javax.lang.model.element.Modifier... modifiers) {

		method.addJavadoc("Get the bean instance supplier for '$L'.", beanName);
		method.addModifiers(modifiers);
		method.returns(ParameterizedTypeName.get(BeanInstanceSupplier.class, beanClass));
		int parameterOffset = (!dependsOnBean) ? 0 : 1;
		CodeBlock.Builder code = CodeBlock.builder();
		code.add(generateResolverForConstructor(beanClass, constructor, parameterOffset));
		boolean hasArguments = constructor.getParameterCount() > 0;
		CodeBlock arguments = hasArguments ?
				new AutowiredArgumentsCodeGenerator(declaringClass, constructor)
						.generateCode(constructor.getParameterTypes(), parameterOffset)
				: NO_ARGS;
		CodeBlock newInstance = generateNewInstanceCodeForConstructor(dependsOnBean, declaringClass, arguments);
		code.add(generateWithGeneratorCode(hasArguments, newInstance));
		method.addStatement(code.build());
	}

	private CodeBlock generateResolverForConstructor(Class<?> beanClass,
			Constructor<?> constructor, int parameterOffset) {

		CodeBlock parameterTypes = generateParameterTypesCode(constructor.getParameterTypes(), parameterOffset);
		return CodeBlock.of("return $T.<$T>forConstructor($L)", BeanInstanceSupplier.class, beanClass, parameterTypes);
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

		String beanName = registeredBean.getBeanName();
		Class<?> beanClass = registeredBean.getBeanClass();
		Class<?> declaringClass = ClassUtils
				.getUserClass(factoryMethod.getDeclaringClass());
		boolean dependsOnBean = !Modifier.isStatic(factoryMethod.getModifiers());
		Visibility accessVisibility = getAccessVisibility(registeredBean, factoryMethod);
		if (accessVisibility != Visibility.PRIVATE) {
			return generateCodeForAccessibleFactoryMethod(
					beanName, beanClass, factoryMethod, declaringClass, dependsOnBean);
		}
		return generateCodeForInaccessibleFactoryMethod(beanName, beanClass, factoryMethod, declaringClass);
	}

	private CodeBlock generateCodeForAccessibleFactoryMethod(String beanName,
			Class<?> beanClass, Method factoryMethod, Class<?> declaringClass, boolean dependsOnBean) {

		this.generationContext.getRuntimeHints().reflection().registerMethod(
				factoryMethod, ExecutableMode.INTROSPECT);
		if (!dependsOnBean && factoryMethod.getParameterCount() == 0) {
			CodeBlock.Builder code = CodeBlock.builder();
			code.add("$T.<$T>forFactoryMethod($T.class, $S)", BeanInstanceSupplier.class,
					beanClass, declaringClass, factoryMethod.getName());
			code.add(".withGenerator($T::$L)", declaringClass, factoryMethod.getName());
			return code.build();
		}
		GeneratedMethod getInstanceMethod = generateGetInstanceSupplierMethod(method ->
				buildGetInstanceMethodForFactoryMethod(method, beanName, beanClass, factoryMethod,
						declaringClass, dependsOnBean, PRIVATE_STATIC));
		return generateReturnStatement(getInstanceMethod);
	}

	private CodeBlock generateCodeForInaccessibleFactoryMethod(String beanName, Class<?> beanClass,
			Method factoryMethod, Class<?> declaringClass) {

		this.generationContext.getRuntimeHints().reflection().registerMethod(factoryMethod, ExecutableMode.INVOKE);
		GeneratedMethod getInstanceMethod = generateGetInstanceSupplierMethod(method -> {
			method.addJavadoc("Get the bean instance supplier for '$L'.", beanName);
			method.addModifiers(PRIVATE_STATIC);
			method.returns(ParameterizedTypeName.get(BeanInstanceSupplier.class, beanClass));
			method.addStatement(generateInstanceSupplierForFactoryMethod(
					beanClass, factoryMethod, declaringClass, factoryMethod.getName()));
		});
		return generateReturnStatement(getInstanceMethod);
	}

	private void buildGetInstanceMethodForFactoryMethod(MethodSpec.Builder method,
			String beanName, Class<?> beanClass, Method factoryMethod, Class<?> declaringClass,
			boolean dependsOnBean, javax.lang.model.element.Modifier... modifiers) {

		String factoryMethodName = factoryMethod.getName();
		method.addJavadoc("Get the bean instance supplier for '$L'.", beanName);
		method.addModifiers(modifiers);
		method.returns(ParameterizedTypeName.get(BeanInstanceSupplier.class, beanClass));
		CodeBlock.Builder code = CodeBlock.builder();
		code.add(generateInstanceSupplierForFactoryMethod(
				beanClass, factoryMethod, declaringClass, factoryMethodName));
		boolean hasArguments = factoryMethod.getParameterCount() > 0;
		CodeBlock arguments = hasArguments ?
				new AutowiredArgumentsCodeGenerator(declaringClass, factoryMethod)
						.generateCode(factoryMethod.getParameterTypes())
				: NO_ARGS;
		CodeBlock newInstance = generateNewInstanceCodeForMethod(
				dependsOnBean, declaringClass, factoryMethodName, arguments);
		code.add(generateWithGeneratorCode(hasArguments, newInstance));
		method.addStatement(code.build());
	}

	private CodeBlock generateInstanceSupplierForFactoryMethod(Class<?> beanClass,
			Method factoryMethod, Class<?> declaringClass, String factoryMethodName) {

		if (factoryMethod.getParameterCount() == 0) {
			return CodeBlock.of("return $T.<$T>forFactoryMethod($T.class, $S)",
					BeanInstanceSupplier.class, beanClass, declaringClass,
					factoryMethodName);
		}
		CodeBlock parameterTypes = generateParameterTypesCode(factoryMethod.getParameterTypes(), 0);
		return CodeBlock.of("return $T.<$T>forFactoryMethod($T.class, $S, $L)",
				BeanInstanceSupplier.class, beanClass, declaringClass, factoryMethodName, parameterTypes);
	}

	private CodeBlock generateNewInstanceCodeForMethod(boolean dependsOnBean,
			Class<?> declaringClass, String factoryMethodName, CodeBlock args) {

		if (!dependsOnBean) {
			return CodeBlock.of("$T.$L($L)", declaringClass, factoryMethodName, args);
		}
		return CodeBlock.of("$L.getBeanFactory().getBean($T.class).$L($L)",
				REGISTERED_BEAN_PARAMETER_NAME, declaringClass, factoryMethodName, args);
	}

	private CodeBlock generateReturnStatement(GeneratedMethod generatedMethod) {
		return generatedMethod.toMethodReference().toInvokeCodeBlock(
				ArgumentCodeGenerator.none(), this.className);
	}

	private CodeBlock generateWithGeneratorCode(boolean hasArguments, CodeBlock newInstance) {
		CodeBlock lambdaArguments = (hasArguments
				? CodeBlock.of("($L, $L)", REGISTERED_BEAN_PARAMETER_NAME, ARGS_PARAMETER_NAME)
				: CodeBlock.of("($L)", REGISTERED_BEAN_PARAMETER_NAME));
		Builder code = CodeBlock.builder();
		code.add("\n");
		code.indent().indent();
		code.add(".withGenerator($L -> $L)", lambdaArguments, newInstance);
		code.unindent().unindent();
		return code.build();
	}

	private Visibility getAccessVisibility(RegisteredBean registeredBean, Member member) {
		AccessControl beanTypeAccessControl = AccessControl.forResolvableType(registeredBean.getBeanType());
		AccessControl memberAccessControl = AccessControl.forMember(member);
		return AccessControl.lowest(beanTypeAccessControl, memberAccessControl).getVisibility();
	}

	private CodeBlock generateParameterTypesCode(Class<?>[] parameterTypes, int offset) {
		CodeBlock.Builder code = CodeBlock.builder();
		for (int i = offset; i < parameterTypes.length; i++) {
			code.add(i != offset ? ", " : "");
			code.add("$T.class", parameterTypes[i]);
		}
		return code.build();
	}

	private GeneratedMethod generateGetInstanceSupplierMethod(Consumer<MethodSpec.Builder> method) {
		return this.generatedMethods.add("getInstanceSupplier", method);
	}

	private boolean isThrowingCheckedException(Executable executable) {
		return Arrays.stream(executable.getGenericExceptionTypes())
				.map(ResolvableType::forType).map(ResolvableType::toClass)
				.anyMatch(Exception.class::isAssignableFrom);
	}

}

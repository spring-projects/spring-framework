/*
 * Copyright 2002-2024 the original author or authors.
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
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.function.Consumer;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;

import org.springframework.aot.generate.AccessControl;
import org.springframework.aot.generate.AccessControl.Visibility;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AutowireCandidateResolver;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RegisteredBean.InstantiationDescriptor;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.ThrowingSupplier;

/**
 * Default code generator to create an {@link InstanceSupplier}, usually in
 * the form of a {@link BeanInstanceSupplier} that retains the executable
 * that is used to instantiate the bean. Takes care of registering the
 * necessary hints if reflection or a JDK proxy is required.
 *
 * <p>Generated code is usually a method reference that generates the
 * {@link BeanInstanceSupplier}, but some shortcut can be used as well such as:
 * <pre class="code">
 * {@code InstanceSupplier.of(TheGeneratedClass::getMyBeanInstance);}
 * </pre>
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 6.0
 * @see BeanRegistrationCodeFragments
 */
public class InstanceSupplierCodeGenerator {

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


	/**
	 * Create a new instance.
	 * @param generationContext the generation context
	 * @param className the class name of the bean to instantiate
	 * @param generatedMethods the generated methods
	 * @param allowDirectSupplierShortcut whether a direct supplier may be used rather
	 * than always needing an {@link InstanceSupplier}
	 */
	public InstanceSupplierCodeGenerator(GenerationContext generationContext,
			ClassName className, GeneratedMethods generatedMethods, boolean allowDirectSupplierShortcut) {

		this.generationContext = generationContext;
		this.className = className;
		this.generatedMethods = generatedMethods;
		this.allowDirectSupplierShortcut = allowDirectSupplierShortcut;
	}

	/**
	 * Generate the instance supplier code.
	 * @param registeredBean the bean to handle
	 * @param constructorOrFactoryMethod the executable to use to create the bean
	 * @return the generated code
	 * @deprecated in favor of {@link #generateCode(RegisteredBean, InstantiationDescriptor)}
	 */
	@Deprecated(since = "6.1.7")
	public CodeBlock generateCode(RegisteredBean registeredBean, Executable constructorOrFactoryMethod) {
		return generateCode(registeredBean, new InstantiationDescriptor(
				constructorOrFactoryMethod, constructorOrFactoryMethod.getDeclaringClass()));
	}

	/**
	 * Generate the instance supplier code.
	 * @param registeredBean the bean to handle
	 * @param instantiationDescriptor the executable to use to create the bean
	 * @return the generated code
	 * @since 6.1.7
	 */
	public CodeBlock generateCode(RegisteredBean registeredBean, InstantiationDescriptor instantiationDescriptor) {
		Executable constructorOrFactoryMethod = instantiationDescriptor.executable();
		registerRuntimeHintsIfNecessary(registeredBean, constructorOrFactoryMethod);
		if (constructorOrFactoryMethod instanceof Constructor<?> constructor) {
			return generateCodeForConstructor(registeredBean, constructor);
		}
		if (constructorOrFactoryMethod instanceof Method method && !KotlinDetector.isSuspendingFunction(method)) {
			return generateCodeForFactoryMethod(registeredBean, method, instantiationDescriptor.targetClass());
		}
		throw new AotBeanProcessingException(registeredBean, "no suitable constructor or factory method found");
	}

	private void registerRuntimeHintsIfNecessary(RegisteredBean registeredBean, Executable constructorOrFactoryMethod) {
		if (registeredBean.getBeanFactory() instanceof DefaultListableBeanFactory dlbf) {
			RuntimeHints runtimeHints = this.generationContext.getRuntimeHints();
			ProxyRuntimeHintsRegistrar registrar = new ProxyRuntimeHintsRegistrar(dlbf.getAutowireCandidateResolver());
			if (constructorOrFactoryMethod instanceof Method method) {
				registrar.registerRuntimeHints(runtimeHints, method);
			}
			else if (constructorOrFactoryMethod instanceof Constructor<?> constructor) {
				registrar.registerRuntimeHints(runtimeHints, constructor);
			}
		}
	}

	private CodeBlock generateCodeForConstructor(RegisteredBean registeredBean, Constructor<?> constructor) {
		String beanName = registeredBean.getBeanName();
		Class<?> beanClass = registeredBean.getBeanClass();
		Class<?> declaringClass = constructor.getDeclaringClass();
		boolean dependsOnBean = ClassUtils.isInnerClass(declaringClass);

		Visibility accessVisibility = getAccessVisibility(registeredBean, constructor);
		if (KotlinDetector.isKotlinReflectPresent() && KotlinDelegate.hasConstructorWithOptionalParameter(beanClass)) {
			return generateCodeForInaccessibleConstructor(beanName, beanClass, constructor,
					dependsOnBean, hints -> hints.registerType(beanClass, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
		}
		else if (accessVisibility != Visibility.PRIVATE) {
			return generateCodeForAccessibleConstructor(beanName, beanClass, constructor,
					dependsOnBean, declaringClass);
		}
		return generateCodeForInaccessibleConstructor(beanName, beanClass, constructor, dependsOnBean,
				hints -> hints.registerConstructor(constructor, ExecutableMode.INVOKE));
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

	private CodeBlock generateCodeForInaccessibleConstructor(String beanName, Class<?> beanClass,
			Constructor<?> constructor, boolean dependsOnBean, Consumer<ReflectionHints> hints) {

		CodeWarnings codeWarnings = new CodeWarnings();
		codeWarnings.detectDeprecation(beanClass, constructor)
				.detectDeprecation(Arrays.stream(constructor.getParameters()).map(Parameter::getType));
		hints.accept(this.generationContext.getRuntimeHints().reflection());

		GeneratedMethod generatedMethod = generateGetInstanceSupplierMethod(method -> {
			method.addJavadoc("Get the bean instance supplier for '$L'.", beanName);
			method.addModifiers(PRIVATE_STATIC);
			codeWarnings.suppress(method);
			method.returns(ParameterizedTypeName.get(BeanInstanceSupplier.class, beanClass));
			int parameterOffset = (!dependsOnBean) ? 0 : 1;
			method.addStatement(generateResolverForConstructor(beanClass, constructor, parameterOffset));
		});

		return generateReturnStatement(generatedMethod);
	}

	private void buildGetInstanceMethodForConstructor(MethodSpec.Builder method,
			String beanName, Class<?> beanClass, Constructor<?> constructor, Class<?> declaringClass,
			boolean dependsOnBean, javax.lang.model.element.Modifier... modifiers) {

		CodeWarnings codeWarnings = new CodeWarnings();
		codeWarnings.detectDeprecation(beanClass, constructor, declaringClass)
				.detectDeprecation(Arrays.stream(constructor.getParameters()).map(Parameter::getType));
		method.addJavadoc("Get the bean instance supplier for '$L'.", beanName);
		method.addModifiers(modifiers);
		codeWarnings.suppress(method);
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

	private CodeBlock generateCodeForFactoryMethod(RegisteredBean registeredBean, Method factoryMethod, Class<?> targetClass) {
		String beanName = registeredBean.getBeanName();
		Class<?> targetClassToUse = ClassUtils.getUserClass(targetClass);
		boolean dependsOnBean = !Modifier.isStatic(factoryMethod.getModifiers());

		Visibility accessVisibility = getAccessVisibility(registeredBean, factoryMethod);
		if (accessVisibility != Visibility.PRIVATE) {
			return generateCodeForAccessibleFactoryMethod(
					beanName, factoryMethod, targetClassToUse, dependsOnBean);
		}
		return generateCodeForInaccessibleFactoryMethod(beanName, factoryMethod, targetClassToUse);
	}

	private CodeBlock generateCodeForAccessibleFactoryMethod(String beanName,
			Method factoryMethod, Class<?> targetClass, boolean dependsOnBean) {

		this.generationContext.getRuntimeHints().reflection().registerMethod(
				factoryMethod, ExecutableMode.INTROSPECT);

		if (!dependsOnBean && factoryMethod.getParameterCount() == 0) {
			Class<?> suppliedType = ClassUtils.resolvePrimitiveIfNecessary(factoryMethod.getReturnType());
			CodeBlock.Builder code = CodeBlock.builder();
			code.add("$T.<$T>forFactoryMethod($T.class, $S)", BeanInstanceSupplier.class,
					suppliedType, targetClass, factoryMethod.getName());
			code.add(".withGenerator(($L) -> $T.$L())", REGISTERED_BEAN_PARAMETER_NAME,
					targetClass, factoryMethod.getName());
			return code.build();
		}

		GeneratedMethod getInstanceMethod = generateGetInstanceSupplierMethod(method ->
				buildGetInstanceMethodForFactoryMethod(method, beanName, factoryMethod,
						targetClass, dependsOnBean, PRIVATE_STATIC));
		return generateReturnStatement(getInstanceMethod);
	}

	private CodeBlock generateCodeForInaccessibleFactoryMethod(
			String beanName, Method factoryMethod, Class<?> targetClass) {

		this.generationContext.getRuntimeHints().reflection().registerMethod(factoryMethod, ExecutableMode.INVOKE);
		GeneratedMethod getInstanceMethod = generateGetInstanceSupplierMethod(method -> {
			Class<?> suppliedType = ClassUtils.resolvePrimitiveIfNecessary(factoryMethod.getReturnType());
			method.addJavadoc("Get the bean instance supplier for '$L'.", beanName);
			method.addModifiers(PRIVATE_STATIC);
			method.returns(ParameterizedTypeName.get(BeanInstanceSupplier.class, suppliedType));
			method.addStatement(generateInstanceSupplierForFactoryMethod(
					factoryMethod, suppliedType, targetClass, factoryMethod.getName()));
		});
		return generateReturnStatement(getInstanceMethod);
	}

	private void buildGetInstanceMethodForFactoryMethod(MethodSpec.Builder method,
			String beanName, Method factoryMethod, Class<?> targetClass,
			boolean dependsOnBean, javax.lang.model.element.Modifier... modifiers) {

		String factoryMethodName = factoryMethod.getName();
		Class<?> suppliedType = ClassUtils.resolvePrimitiveIfNecessary(factoryMethod.getReturnType());
		CodeWarnings codeWarnings = new CodeWarnings();
		codeWarnings.detectDeprecation(targetClass, factoryMethod, suppliedType)
				.detectDeprecation(Arrays.stream(factoryMethod.getParameters()).map(Parameter::getType));

		method.addJavadoc("Get the bean instance supplier for '$L'.", beanName);
		method.addModifiers(modifiers);
		codeWarnings.suppress(method);
		method.returns(ParameterizedTypeName.get(BeanInstanceSupplier.class, suppliedType));

		CodeBlock.Builder code = CodeBlock.builder();
		code.add(generateInstanceSupplierForFactoryMethod(
				factoryMethod, suppliedType, targetClass, factoryMethodName));

		boolean hasArguments = factoryMethod.getParameterCount() > 0;
		CodeBlock arguments = hasArguments ?
				new AutowiredArgumentsCodeGenerator(targetClass, factoryMethod)
						.generateCode(factoryMethod.getParameterTypes())
				: NO_ARGS;

		CodeBlock newInstance = generateNewInstanceCodeForMethod(
				dependsOnBean, targetClass, factoryMethodName, arguments);
		code.add(generateWithGeneratorCode(hasArguments, newInstance));
		method.addStatement(code.build());
	}

	private CodeBlock generateInstanceSupplierForFactoryMethod(Method factoryMethod,
			Class<?> suppliedType, Class<?> targetClass, String factoryMethodName) {

		if (factoryMethod.getParameterCount() == 0) {
			return CodeBlock.of("return $T.<$T>forFactoryMethod($T.class, $S)",
					BeanInstanceSupplier.class, suppliedType, targetClass, factoryMethodName);
		}

		CodeBlock parameterTypes = generateParameterTypesCode(factoryMethod.getParameterTypes(), 0);
		return CodeBlock.of("return $T.<$T>forFactoryMethod($T.class, $S, $L)",
				BeanInstanceSupplier.class, suppliedType, targetClass, factoryMethodName, parameterTypes);
	}

	private CodeBlock generateNewInstanceCodeForMethod(boolean dependsOnBean,
			Class<?> targetClass, String factoryMethodName, CodeBlock args) {

		if (!dependsOnBean) {
			return CodeBlock.of("$T.$L($L)", targetClass, factoryMethodName, args);
		}
		return CodeBlock.of("$L.getBeanFactory().getBean($T.class).$L($L)",
				REGISTERED_BEAN_PARAMETER_NAME, targetClass, factoryMethodName, args);
	}

	private CodeBlock generateReturnStatement(GeneratedMethod generatedMethod) {
		return generatedMethod.toMethodReference().toInvokeCodeBlock(
				ArgumentCodeGenerator.none(), this.className);
	}

	private CodeBlock generateWithGeneratorCode(boolean hasArguments, CodeBlock newInstance) {
		CodeBlock lambdaArguments = (hasArguments ?
				CodeBlock.of("($L, $L)", REGISTERED_BEAN_PARAMETER_NAME, ARGS_PARAMETER_NAME) :
				CodeBlock.of("($L)", REGISTERED_BEAN_PARAMETER_NAME));
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

	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 */
	private static class KotlinDelegate {

		public static boolean hasConstructorWithOptionalParameter(Class<?> beanClass) {
			if (KotlinDetector.isKotlinType(beanClass)) {
				KClass<?> kClass = JvmClassMappingKt.getKotlinClass(beanClass);
				for (KFunction<?> constructor : kClass.getConstructors()) {
					for (KParameter parameter : constructor.getParameters()) {
						if (parameter.isOptional()) {
							return true;
						}
					}
				}
			}
			return false;
		}

	}


	private static class ProxyRuntimeHintsRegistrar {

		private final AutowireCandidateResolver candidateResolver;

		public ProxyRuntimeHintsRegistrar(AutowireCandidateResolver candidateResolver) {
			this.candidateResolver = candidateResolver;
		}

		public void registerRuntimeHints(RuntimeHints runtimeHints, Method method) {
			Class<?>[] parameterTypes = method.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++) {
				MethodParameter methodParam = new MethodParameter(method, i);
				DependencyDescriptor dependencyDescriptor = new DependencyDescriptor(methodParam, true);
				registerProxyIfNecessary(runtimeHints, dependencyDescriptor);
			}
		}

		public void registerRuntimeHints(RuntimeHints runtimeHints, Constructor<?> constructor) {
			Class<?>[] parameterTypes = constructor.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++) {
				MethodParameter methodParam = new MethodParameter(constructor, i);
				DependencyDescriptor dependencyDescriptor = new DependencyDescriptor(
						methodParam, true);
				registerProxyIfNecessary(runtimeHints, dependencyDescriptor);
			}
		}

		private void registerProxyIfNecessary(RuntimeHints runtimeHints, DependencyDescriptor dependencyDescriptor) {
			Class<?> proxyType = this.candidateResolver.getLazyResolutionProxyClass(dependencyDescriptor, null);
			if (proxyType != null && Proxy.isProxyClass(proxyType)) {
				runtimeHints.proxies().registerJdkProxy(proxyType.getInterfaces());
			}
		}
	}

}

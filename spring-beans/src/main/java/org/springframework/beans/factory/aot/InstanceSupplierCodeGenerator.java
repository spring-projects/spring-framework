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
import org.springframework.lang.Nullable;
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
 * InstanceSupplier.of(TheGeneratedClass::getMyBeanInstance);
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

	private static final javax.lang.model.element.Modifier[] PRIVATE_STATIC =
			{javax.lang.model.element.Modifier.PRIVATE, javax.lang.model.element.Modifier.STATIC};

	private static final CodeBlock NO_ARGS = CodeBlock.of("");


	private final GenerationContext generationContext;

	private final ClassName className;

	private final GeneratedMethods generatedMethods;

	private final boolean allowDirectSupplierShortcut;


	/**
	 * Create a new generator instance.
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
			registrar.registerRuntimeHints(runtimeHints, constructorOrFactoryMethod);
		}
	}

	private CodeBlock generateCodeForConstructor(RegisteredBean registeredBean, Constructor<?> constructor) {
		String beanName = registeredBean.getBeanName();
		Class<?> beanClass = registeredBean.getBeanClass();

		if (KotlinDetector.isKotlinReflectPresent() && KotlinDelegate.hasConstructorWithOptionalParameter(beanClass)) {
			return generateCodeForInaccessibleConstructor(beanName, constructor,
					hints -> hints.registerType(beanClass, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
		}

		if (!isVisible(constructor, constructor.getDeclaringClass())) {
			return generateCodeForInaccessibleConstructor(beanName, constructor,
					hints -> hints.registerConstructor(constructor, ExecutableMode.INVOKE));
		}
		return generateCodeForAccessibleConstructor(beanName, constructor);
	}

	private CodeBlock generateCodeForAccessibleConstructor(String beanName, Constructor<?> constructor) {
		this.generationContext.getRuntimeHints().reflection().registerConstructor(
				constructor, ExecutableMode.INTROSPECT);

		if (constructor.getParameterCount() == 0) {
			if (!this.allowDirectSupplierShortcut) {
				return CodeBlock.of("$T.using($T::new)", InstanceSupplier.class, constructor.getDeclaringClass());
			}
			if (!isThrowingCheckedException(constructor)) {
				return CodeBlock.of("$T::new", constructor.getDeclaringClass());
			}
			return CodeBlock.of("$T.of($T::new)", ThrowingSupplier.class, constructor.getDeclaringClass());
		}

		GeneratedMethod generatedMethod = generateGetInstanceSupplierMethod(method ->
				buildGetInstanceMethodForConstructor(method, beanName, constructor, PRIVATE_STATIC));
		return generateReturnStatement(generatedMethod);
	}

	private CodeBlock generateCodeForInaccessibleConstructor(String beanName,
			Constructor<?> constructor, Consumer<ReflectionHints> hints) {

		CodeWarnings codeWarnings = new CodeWarnings();
		codeWarnings.detectDeprecation(constructor.getDeclaringClass(), constructor)
				.detectDeprecation(Arrays.stream(constructor.getParameters()).map(Parameter::getType));
		hints.accept(this.generationContext.getRuntimeHints().reflection());

		GeneratedMethod generatedMethod = generateGetInstanceSupplierMethod(method -> {
			method.addJavadoc("Get the bean instance supplier for '$L'.", beanName);
			method.addModifiers(PRIVATE_STATIC);
			codeWarnings.suppress(method);
			method.returns(ParameterizedTypeName.get(BeanInstanceSupplier.class, constructor.getDeclaringClass()));
			method.addStatement(generateResolverForConstructor(constructor));
		});

		return generateReturnStatement(generatedMethod);
	}

	private void buildGetInstanceMethodForConstructor(MethodSpec.Builder method, String beanName,
			Constructor<?> constructor, javax.lang.model.element.Modifier... modifiers) {

		Class<?> declaringClass = constructor.getDeclaringClass();

		CodeWarnings codeWarnings = new CodeWarnings();
		codeWarnings.detectDeprecation(declaringClass, constructor)
				.detectDeprecation(Arrays.stream(constructor.getParameters()).map(Parameter::getType));
		method.addJavadoc("Get the bean instance supplier for '$L'.", beanName);
		method.addModifiers(modifiers);
		codeWarnings.suppress(method);
		method.returns(ParameterizedTypeName.get(BeanInstanceSupplier.class, declaringClass));

		CodeBlock.Builder code = CodeBlock.builder();
		code.add(generateResolverForConstructor(constructor));
		boolean hasArguments = constructor.getParameterCount() > 0;
		boolean onInnerClass = ClassUtils.isInnerClass(declaringClass);

		CodeBlock arguments = hasArguments ?
				new AutowiredArgumentsCodeGenerator(declaringClass, constructor)
						.generateCode(constructor.getParameterTypes(), (onInnerClass ? 1 : 0))
				: NO_ARGS;

		CodeBlock newInstance = generateNewInstanceCodeForConstructor(declaringClass, arguments);
		code.add(generateWithGeneratorCode(hasArguments, newInstance));
		method.addStatement(code.build());
	}

	private CodeBlock generateResolverForConstructor(Constructor<?> constructor) {
		CodeBlock parameterTypes = generateParameterTypesCode(constructor.getParameterTypes());
		return CodeBlock.of("return $T.<$T>forConstructor($L)", BeanInstanceSupplier.class,
				constructor.getDeclaringClass(), parameterTypes);
	}

	private CodeBlock generateNewInstanceCodeForConstructor(Class<?> declaringClass, CodeBlock args) {
		if (ClassUtils.isInnerClass(declaringClass)) {
			return CodeBlock.of("$L.getBeanFactory().getBean($T.class).new $L($L)",
					REGISTERED_BEAN_PARAMETER_NAME, declaringClass.getEnclosingClass(),
					declaringClass.getSimpleName(), args);
		}
		return CodeBlock.of("new $T($L)", declaringClass, args);
	}

	private CodeBlock generateCodeForFactoryMethod(
			RegisteredBean registeredBean, Method factoryMethod, Class<?> targetClass) {

		if (!isVisible(factoryMethod, targetClass)) {
			return generateCodeForInaccessibleFactoryMethod(registeredBean.getBeanName(), factoryMethod, targetClass);
		}
		return generateCodeForAccessibleFactoryMethod(registeredBean.getBeanName(), factoryMethod, targetClass,
				registeredBean.getMergedBeanDefinition().getFactoryBeanName());
	}

	private CodeBlock generateCodeForAccessibleFactoryMethod(String beanName,
			Method factoryMethod, Class<?> targetClass, @Nullable String factoryBeanName) {

		this.generationContext.getRuntimeHints().reflection().registerMethod(factoryMethod, ExecutableMode.INTROSPECT);

		if (factoryBeanName == null && factoryMethod.getParameterCount() == 0) {
			Class<?> suppliedType = ClassUtils.resolvePrimitiveIfNecessary(factoryMethod.getReturnType());
			CodeBlock.Builder code = CodeBlock.builder();
			code.add("$T.<$T>forFactoryMethod($T.class, $S)", BeanInstanceSupplier.class,
					suppliedType, targetClass, factoryMethod.getName());
			code.add(".withGenerator(($L) -> $T.$L())", REGISTERED_BEAN_PARAMETER_NAME,
					ClassUtils.getUserClass(targetClass), factoryMethod.getName());
			return code.build();
		}

		GeneratedMethod getInstanceMethod = generateGetInstanceSupplierMethod(method ->
				buildGetInstanceMethodForFactoryMethod(method, beanName, factoryMethod,
						targetClass, factoryBeanName, PRIVATE_STATIC));
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
			@Nullable String factoryBeanName, javax.lang.model.element.Modifier... modifiers) {

		String factoryMethodName = factoryMethod.getName();
		Class<?> suppliedType = ClassUtils.resolvePrimitiveIfNecessary(factoryMethod.getReturnType());
		CodeWarnings codeWarnings = new CodeWarnings();
		codeWarnings.detectDeprecation(ClassUtils.getUserClass(targetClass), factoryMethod, suppliedType)
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
				new AutowiredArgumentsCodeGenerator(ClassUtils.getUserClass(targetClass), factoryMethod)
						.generateCode(factoryMethod.getParameterTypes())
				: NO_ARGS;

		CodeBlock newInstance = generateNewInstanceCodeForMethod(
				factoryBeanName, ClassUtils.getUserClass(targetClass), factoryMethodName, arguments);
		code.add(generateWithGeneratorCode(hasArguments, newInstance));
		method.addStatement(code.build());
	}

	private CodeBlock generateInstanceSupplierForFactoryMethod(Method factoryMethod,
			Class<?> suppliedType, Class<?> targetClass, String factoryMethodName) {

		if (factoryMethod.getParameterCount() == 0) {
			return CodeBlock.of("return $T.<$T>forFactoryMethod($T.class, $S)",
					BeanInstanceSupplier.class, suppliedType, targetClass, factoryMethodName);
		}

		CodeBlock parameterTypes = generateParameterTypesCode(factoryMethod.getParameterTypes());
		return CodeBlock.of("return $T.<$T>forFactoryMethod($T.class, $S, $L)",
				BeanInstanceSupplier.class, suppliedType, targetClass, factoryMethodName, parameterTypes);
	}

	private CodeBlock generateNewInstanceCodeForMethod(@Nullable String factoryBeanName,
			Class<?> targetClass, String factoryMethodName, CodeBlock args) {

		if (factoryBeanName == null) {
			return CodeBlock.of("$T.$L($L)", targetClass, factoryMethodName, args);
		}
		return CodeBlock.of("$L.getBeanFactory().getBean(\"$L\", $T.class).$L($L)",
				REGISTERED_BEAN_PARAMETER_NAME, factoryBeanName, targetClass, factoryMethodName, args);
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

	private boolean isVisible(Member member, Class<?> targetClass) {
		AccessControl classAccessControl = AccessControl.forClass(targetClass);
		AccessControl memberAccessControl = AccessControl.forMember(member);
		Visibility visibility = AccessControl.lowest(classAccessControl, memberAccessControl).getVisibility();
		return (visibility == Visibility.PUBLIC || (visibility != Visibility.PRIVATE &&
				member.getDeclaringClass().getPackageName().equals(this.className.packageName())));
		}

	private CodeBlock generateParameterTypesCode(Class<?>[] parameterTypes) {
		CodeBlock.Builder code = CodeBlock.builder();
		for (int i = 0; i < parameterTypes.length; i++) {
			code.add(i > 0 ? ", " : "");
			code.add("$T.class", parameterTypes[i]);
		}
		return code.build();
	}

	private GeneratedMethod generateGetInstanceSupplierMethod(Consumer<MethodSpec.Builder> method) {
		return this.generatedMethods.add("getInstanceSupplier", method);
	}

	private boolean isThrowingCheckedException(Executable executable) {
		return Arrays.stream(executable.getGenericExceptionTypes())
				.map(ResolvableType::forType)
				.map(ResolvableType::toClass)
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


	private record ProxyRuntimeHintsRegistrar(AutowireCandidateResolver candidateResolver) {

		public void registerRuntimeHints(RuntimeHints runtimeHints, Executable executable) {
			Class<?>[] parameterTypes = executable.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++) {
				MethodParameter methodParam = MethodParameter.forExecutable(executable, i);
				DependencyDescriptor dependencyDescriptor = new DependencyDescriptor(methodParam, true);
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

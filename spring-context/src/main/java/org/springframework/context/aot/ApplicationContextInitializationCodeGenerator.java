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

package org.springframework.context.aot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.lang.Nullable;

/**
 * Internal code generator to create the {@link ApplicationContextInitializer}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
class ApplicationContextInitializationCodeGenerator implements BeanFactoryInitializationCode {

	private static final String INITIALIZE_METHOD = "initialize";

	private static final String APPLICATION_CONTEXT_VARIABLE = "applicationContext";

	private final GenericApplicationContext applicationContext;

	private final GeneratedClass generatedClass;

	private final List<MethodReference> initializers = new ArrayList<>();


	ApplicationContextInitializationCodeGenerator(GenericApplicationContext applicationContext, GenerationContext generationContext) {
		this.applicationContext = applicationContext;
		this.generatedClass = generationContext.getGeneratedClasses()
				.addForFeature("ApplicationContextInitializer", this::generateType);
		this.generatedClass.reserveMethodNames(INITIALIZE_METHOD);
	}


	private void generateType(TypeSpec.Builder type) {
		type.addJavadoc(
				"{@link $T} to restore an application context based on previous AOT processing.",
				ApplicationContextInitializer.class);
		type.addModifiers(Modifier.PUBLIC);
		type.addSuperinterface(ParameterizedTypeName.get(
				ApplicationContextInitializer.class, GenericApplicationContext.class));
		type.addMethod(generateInitializeMethod());
	}

	private MethodSpec generateInitializeMethod() {
		MethodSpec.Builder method = MethodSpec.methodBuilder(INITIALIZE_METHOD);
		method.addAnnotation(Override.class);
		method.addModifiers(Modifier.PUBLIC);
		method.addParameter(GenericApplicationContext.class, APPLICATION_CONTEXT_VARIABLE);
		method.addCode(generateInitializeCode());
		return method.build();
	}

	private CodeBlock generateInitializeCode() {
		CodeBlock.Builder code = CodeBlock.builder();
		code.addStatement("$T $L = $L.getDefaultListableBeanFactory()",
				DefaultListableBeanFactory.class, BEAN_FACTORY_VARIABLE,
				APPLICATION_CONTEXT_VARIABLE);
		code.addStatement("$L.setAutowireCandidateResolver(new $T())",
				BEAN_FACTORY_VARIABLE, ContextAnnotationAutowireCandidateResolver.class);
		code.addStatement("$L.setDependencyComparator($T.INSTANCE)",
				BEAN_FACTORY_VARIABLE, AnnotationAwareOrderComparator.class);
		code.add(generateActiveProfilesInitializeCode());
		ArgumentCodeGenerator argCodeGenerator = createInitializerMethodArgumentCodeGenerator();
		for (MethodReference initializer : this.initializers) {
			code.addStatement(initializer.toInvokeCodeBlock(argCodeGenerator, this.generatedClass.getName()));
		}
		return code.build();
	}

	private CodeBlock generateActiveProfilesInitializeCode() {
		CodeBlock.Builder code = CodeBlock.builder();
		ConfigurableEnvironment environment = this.applicationContext.getEnvironment();
		if (!Arrays.equals(environment.getActiveProfiles(), environment.getDefaultProfiles())) {
			for (String activeProfile : environment.getActiveProfiles()) {
				code.addStatement("$L.getEnvironment().addActiveProfile($S)", APPLICATION_CONTEXT_VARIABLE, activeProfile);
			}
		}
		return code.build();
	}

	static ArgumentCodeGenerator createInitializerMethodArgumentCodeGenerator() {
		return ArgumentCodeGenerator.from(new InitializerMethodArgumentCodeGenerator());
	}

	GeneratedClass getGeneratedClass() {
		return this.generatedClass;
	}

	@Override
	public GeneratedMethods getMethods() {
		return this.generatedClass.getMethods();
	}

	@Override
	public void addInitializer(MethodReference methodReference) {
		this.initializers.add(methodReference);
	}

	private static class InitializerMethodArgumentCodeGenerator implements Function<TypeName, CodeBlock> {

		@Override
		@Nullable
		public CodeBlock apply(TypeName typeName) {
			return (typeName instanceof ClassName className ? apply(className) : null);
		}

		@Nullable
		private CodeBlock apply(ClassName className) {
			String name = className.canonicalName();
			if (name.equals(DefaultListableBeanFactory.class.getName())
					|| name.equals(ConfigurableListableBeanFactory.class.getName())) {
				return CodeBlock.of(BEAN_FACTORY_VARIABLE);
			}
			else if (name.equals(ConfigurableEnvironment.class.getName())
					|| name.equals(Environment.class.getName())) {
				return CodeBlock.of("$L.getEnvironment()", APPLICATION_CONTEXT_VARIABLE);
			}
			else if (name.equals(ResourceLoader.class.getName())) {
				return CodeBlock.of(APPLICATION_CONTEXT_VARIABLE);
			}
			return null;
		}
	}

}

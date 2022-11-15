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

package org.springframework.test.context.aot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedClasses;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.log.LogMessage;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.javapoet.WildcardTypeName;
import org.springframework.util.MultiValueMap;

/**
 * Internal code generator for mappings used by {@link AotTestContextInitializers}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class AotTestContextInitializersCodeGenerator {

	private static final Log logger = LogFactory.getLog(AotTestContextInitializersCodeGenerator.class);

	// ApplicationContextInitializer<? extends ConfigurableApplicationContext>
	private static final ParameterizedTypeName CONTEXT_INITIALIZER = ParameterizedTypeName.get(
			ClassName.get(ApplicationContextInitializer.class),
			WildcardTypeName.subtypeOf(ConfigurableApplicationContext.class));

	// Supplier<ApplicationContextInitializer<? extends ConfigurableApplicationContext>>
	private static final ParameterizedTypeName CONTEXT_INITIALIZER_SUPPLIER = ParameterizedTypeName
			.get(ClassName.get(Supplier.class), CONTEXT_INITIALIZER);

	// Map<String, Supplier<ApplicationContextInitializer<? extends ConfigurableApplicationContext>>>
	private static final TypeName CONTEXT_INITIALIZER_SUPPLIER_MAP = ParameterizedTypeName
			.get(ClassName.get(Map.class), ClassName.get(String.class), CONTEXT_INITIALIZER_SUPPLIER);

	// Class<ApplicationContextInitializer<?>
	private static final ParameterizedTypeName CONTEXT_INITIALIZER_CLASS = ParameterizedTypeName
			.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(
				ParameterizedTypeName.get(ClassName.get(ApplicationContextInitializer.class),
					WildcardTypeName.subtypeOf(Object.class))));

	// Map<String, Class<ApplicationContextInitializer<?>>>
	private static final TypeName CONTEXT_INITIALIZER_CLASS_MAP = ParameterizedTypeName
			.get(ClassName.get(Map.class), ClassName.get(String.class), CONTEXT_INITIALIZER_CLASS);

	private static final String GENERATED_SUFFIX = "Generated";

	// TODO Consider an alternative means for specifying the name of the generated class.
	// Ideally we would generate a class named: org.springframework.test.context.aot.GeneratedAotTestContextInitializers
	static final String GENERATED_MAPPINGS_CLASS_NAME = AotTestContextInitializers.class.getName() + "__" + GENERATED_SUFFIX;

	static final String GET_CONTEXT_INITIALIZERS_METHOD_NAME = "getContextInitializers";

	static final String GET_CONTEXT_INITIALIZER_CLASSES_METHOD_NAME = "getContextInitializerClasses";


	private final MultiValueMap<ClassName, Class<?>> initializerClassMappings;

	private final GeneratedClass generatedClass;


	AotTestContextInitializersCodeGenerator(MultiValueMap<ClassName, Class<?>> initializerClassMappings,
			GeneratedClasses generatedClasses) {

		this.initializerClassMappings = initializerClassMappings;
		this.generatedClass = generatedClasses.addForFeature(GENERATED_SUFFIX, this::generateType);
	}


	GeneratedClass getGeneratedClass() {
		return this.generatedClass;
	}

	private void generateType(TypeSpec.Builder type) {
		logger.debug(LogMessage.format("Generating AOT test mappings in %s",
				this.generatedClass.getName().reflectionName()));
		type.addJavadoc("Generated mappings for {@link $T}.", AotTestContextInitializers.class);
		type.addModifiers(Modifier.PUBLIC);
		type.addMethod(contextInitializersMappingMethod());
		type.addMethod(contextInitializerClassesMappingMethod());
	}

	private MethodSpec contextInitializersMappingMethod() {
		MethodSpec.Builder method = MethodSpec.methodBuilder(GET_CONTEXT_INITIALIZERS_METHOD_NAME);
		method.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
		method.returns(CONTEXT_INITIALIZER_SUPPLIER_MAP);
		method.addCode(generateContextInitializersMappingCode());
		return method.build();
	}

	private CodeBlock generateContextInitializersMappingCode() {
		CodeBlock.Builder code = CodeBlock.builder();
		code.addStatement("$T map = new $T<>()", CONTEXT_INITIALIZER_SUPPLIER_MAP, HashMap.class);
		this.initializerClassMappings.forEach((className, testClasses) -> {
			List<String> testClassNames = testClasses.stream().map(Class::getName).toList();
			logger.trace(LogMessage.format(
					"Generating mapping from AOT context initializer supplier [%s] to test classes %s",
					className.reflectionName(), testClassNames));
			testClassNames.forEach(testClassName ->
				code.addStatement("map.put($S, () -> new $T())", testClassName, className));
		});
		code.addStatement("return map");
		return code.build();
	}

	private MethodSpec contextInitializerClassesMappingMethod() {
		MethodSpec.Builder method = MethodSpec.methodBuilder(GET_CONTEXT_INITIALIZER_CLASSES_METHOD_NAME);
		method.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
		method.returns(CONTEXT_INITIALIZER_CLASS_MAP);
		method.addCode(generateContextInitializerClassesMappingCode());
		return method.build();
	}

	private CodeBlock generateContextInitializerClassesMappingCode() {
		CodeBlock.Builder code = CodeBlock.builder();
		code.addStatement("$T map = new $T<>()", CONTEXT_INITIALIZER_CLASS_MAP, HashMap.class);
		this.initializerClassMappings.forEach((className, testClasses) -> {
			List<String> testClassNames = testClasses.stream().map(Class::getName).toList();
			logger.trace(LogMessage.format(
					"Generating mapping from AOT context initializer class [%s] to test classes %s",
					className.reflectionName(), testClassNames));
			testClassNames.forEach(testClassName ->
				code.addStatement("map.put($S, $T.class)", testClassName, className));
		});
		code.addStatement("return map");
		return code.build();
	}

}

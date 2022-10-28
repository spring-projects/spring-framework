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
import java.util.Map;

import javax.lang.model.element.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedClasses;
import org.springframework.core.log.LogMessage;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeName;
import org.springframework.javapoet.TypeSpec;

/**
 * Internal code generator for {@link AotTestAttributes}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class AotTestAttributesCodeGenerator {

	private static final Log logger = LogFactory.getLog(AotTestAttributesCodeGenerator.class);

	// Map<String, String>
	private static final TypeName MAP_TYPE = ParameterizedTypeName.get(Map.class, String.class, String.class);

	private static final String GENERATED_SUFFIX = "Generated";

	// TODO Consider an alternative means for specifying the name of the generated class.
	// Ideally we would generate a class named: org.springframework.test.context.aot.GeneratedAotTestAttributes
	static final String GENERATED_ATTRIBUTES_CLASS_NAME = AotTestAttributes.class.getName() + "__" + GENERATED_SUFFIX;

	static final String GENERATED_ATTRIBUTES_METHOD_NAME = "getAttributes";


	private final Map<String, String> attributes;

	private final GeneratedClass generatedClass;


	AotTestAttributesCodeGenerator(Map<String, String> attributes, GeneratedClasses generatedClasses) {
		this.attributes = attributes;
		this.generatedClass = generatedClasses.addForFeature(GENERATED_SUFFIX, this::generateType);
	}


	GeneratedClass getGeneratedClass() {
		return this.generatedClass;
	}

	private void generateType(TypeSpec.Builder type) {
		logger.debug(LogMessage.format("Generating AOT test attributes in %s",
				this.generatedClass.getName().reflectionName()));
		type.addJavadoc("Generated map for {@link $T}.", AotTestAttributes.class);
		type.addModifiers(Modifier.PUBLIC);
		type.addMethod(generateMethod());
	}

	private MethodSpec generateMethod() {
		MethodSpec.Builder method = MethodSpec.methodBuilder(GENERATED_ATTRIBUTES_METHOD_NAME);
		method.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
		method.returns(MAP_TYPE);
		method.addCode(generateCode());
		return method.build();
	}

	private CodeBlock generateCode() {
		CodeBlock.Builder code = CodeBlock.builder();
		code.addStatement("$T map = new $T<>()", MAP_TYPE, HashMap.class);
		this.attributes.forEach((key, value) -> {
			logger.trace(LogMessage.format("Storing AOT test attribute: %s = %s", key, value));
			code.addStatement("map.put($S, $S)", key, value);
		});
		code.addStatement("return map");
		return code.build();
	}

}

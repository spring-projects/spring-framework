/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.service.registry;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.ValueCodeGenerator;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanDefinitionPropertyValueCodeGeneratorDelegates;
import org.springframework.beans.testfixture.beans.factory.aot.DeferredTypeBuilder;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.service.registry.GroupsMetadata.Registration;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;
import org.springframework.web.service.registry.echo.EchoA;
import org.springframework.web.service.registry.echo.EchoB;
import org.springframework.web.service.registry.greeting.GreetingA;
import org.springframework.web.service.registry.greeting.GreetingB;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GroupsMetadataValueDelegate}.
 *
 * @author Stephane Nicoll
 */
@CompileWithForkedClassLoader
class GroupsMetadataValueDelegateTests {

	@Test
	void generateRegistrationWithOnlyName() {
		Registration registration = new Registration("test", ClientType.UNSPECIFIED);
		compile(registration, (instance, compiled) -> assertThat(instance)
				.isInstanceOfSatisfying(Registration.class, hasRegistration("test", ClientType.UNSPECIFIED)));
	}

	@Test
	void generateRegistrationWitNoHttpServiceTypeName() {
		Registration registration = new Registration("test", ClientType.REST_CLIENT);
		compile(registration, (instance, compiled) -> assertThat(instance)
				.isInstanceOfSatisfying(Registration.class, hasRegistration("test", ClientType.REST_CLIENT)));
	}

	@Test
	void generateRegistrationWitOneHttpServiceTypeName() {
		Registration registration = new Registration("test", ClientType.WEB_CLIENT,
				httpServiceTypeNames("com.example.MyClient"));
		compile(registration, (instance, compiled) -> assertThat(instance)
				.isInstanceOfSatisfying(Registration.class, hasRegistration(
						"test", ClientType.WEB_CLIENT, "com.example.MyClient")));
	}

	@Test
	void generateRegistrationWitHttpServiceTypeNames() {
		Registration registration = new Registration("test", ClientType.WEB_CLIENT,
				httpServiceTypeNames("com.example.MyClient", "com.example.another.TestClient"));
		compile(registration, (instance, compiled) -> assertThat(instance)
				.isInstanceOfSatisfying(Registration.class, hasRegistration(
						"test", ClientType.WEB_CLIENT, "com.example.MyClient", "com.example.another.TestClient")));
	}

	@Test
	void generateGroupsMetadataEmpty() {
		compile(new GroupsMetadata(), (instance, compiled) -> assertThat(instance)
				.isInstanceOfSatisfying(GroupsMetadata.class, metadata -> assertThat(metadata.groups(compiled.getClassLoader())).isEmpty()));
	}

	@Test
	void generateGroupsMetadataSingleGroup() {
		GroupsMetadata groupsMetadata = new GroupsMetadata();
		groupsMetadata.getOrCreateGroup("test-group", ClientType.REST_CLIENT).httpServiceTypeNames().add(EchoA.class.getName());
		compile(groupsMetadata, (instance, compiled) -> assertThat(instance)
				.isInstanceOfSatisfying(GroupsMetadata.class, metadata -> assertThat(metadata.groups(compiled.getClassLoader()))
						.singleElement().satisfies(hasHttpServiceGroup("test-group", ClientType.REST_CLIENT, EchoA.class))));
	}

	@Test
	void generateGroupsMetadataMultipleGroupsSimple() {
		GroupsMetadata groupsMetadata = new GroupsMetadata();
		groupsMetadata.getOrCreateGroup("test-group", ClientType.UNSPECIFIED).httpServiceTypeNames()
				.addAll(List.of(EchoA.class.getName(), EchoB.class.getName()));
		groupsMetadata.getOrCreateGroup("another-group", ClientType.WEB_CLIENT).httpServiceTypeNames()
				.addAll(List.of(GreetingA.class.getName(), GreetingB.class.getName()));

		Function<GeneratedClass, ValueCodeGenerator> valueCodeGeneratorFactory = generatedClass ->
				ValueCodeGenerator.withDefaults().add(List.of(new GroupsMetadataValueDelegate()));
		compile(valueCodeGeneratorFactory, groupsMetadata, (instance, compiled) -> assertThat(instance)
				.isInstanceOfSatisfying(GroupsMetadata.class, metadata -> assertThat(metadata.groups(compiled.getClassLoader()))
						.satisfiesOnlyOnce(hasHttpServiceGroup("test-group", ClientType.REST_CLIENT, EchoA.class, EchoB.class))
						.satisfiesOnlyOnce(hasHttpServiceGroup("another-group", ClientType.WEB_CLIENT, GreetingA.class, GreetingB.class))
						.hasSize(2)));
	}

	@Test
	void generateGroupsMetadataMultipleGroups() {
		GroupsMetadata groupsMetadata = new GroupsMetadata();
		groupsMetadata.getOrCreateGroup("test-group", ClientType.UNSPECIFIED).httpServiceTypeNames()
				.addAll(List.of(EchoA.class.getName(), EchoB.class.getName()));
		groupsMetadata.getOrCreateGroup("another-group", ClientType.WEB_CLIENT).httpServiceTypeNames()
				.addAll(List.of(GreetingA.class.getName(), GreetingB.class.getName()));

		compile(groupsMetadata, (instance, compiled) -> assertThat(instance)
				.isInstanceOfSatisfying(GroupsMetadata.class, metadata -> assertThat(metadata.groups(compiled.getClassLoader()))
						.satisfiesOnlyOnce(hasHttpServiceGroup("test-group", ClientType.REST_CLIENT, EchoA.class, EchoB.class))
						.satisfiesOnlyOnce(hasHttpServiceGroup("another-group", ClientType.WEB_CLIENT, GreetingA.class, GreetingB.class))
						.hasSize(2)));
	}

	private LinkedHashSet<String> httpServiceTypeNames(String... names) {
		return new LinkedHashSet<>(Arrays.asList(names));
	}

	private Consumer<Registration> hasRegistration(String name, ClientType clientType, String... httpServiceTypeNames) {
		return registration -> {
			assertThat(registration.name()).isEqualTo(name);
			assertThat(registration.clientType()).isEqualTo(clientType);
			assertThat(registration.httpServiceTypeNames()).isInstanceOf(LinkedHashSet.class)
					.containsExactly(httpServiceTypeNames);
		};
	}

	private Consumer<HttpServiceGroup> hasHttpServiceGroup(String name, ClientType clientType, Class<?>... httpServiceTypeNames) {
		return group -> {
			assertThat(group.name()).isEqualTo(name);
			assertThat(group.clientType()).isEqualTo(clientType);
			assertThat(group.httpServiceTypes()).containsOnly(httpServiceTypeNames);
		};
	}


	private void compile(Function<GeneratedClass, ValueCodeGenerator> valueCodeGeneratorFactory,
			Object value, BiConsumer<Object, Compiled> result) {
		TestGenerationContext generationContext = new TestGenerationContext();
		DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();
		GeneratedClass generatedClass = generationContext.getGeneratedClasses().addForFeatureComponent("TestCode", GroupsMetadata.class, typeBuilder);
		ValueCodeGenerator valueCodeGenerator = valueCodeGeneratorFactory.apply(generatedClass);
		CodeBlock generatedCode = valueCodeGenerator.generateCode(value);
		typeBuilder.set(type -> {
			type.addModifiers(Modifier.PUBLIC);
			type.addMethod(MethodSpec.methodBuilder("get").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					.returns(Object.class).addStatement("return $L", generatedCode).build());
		});
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().with(generationContext).compile(compiled ->
				result.accept(getGeneratedCodeReturnValue(compiled, generatedClass), compiled));
	}

	private void compile(Object value, BiConsumer<Object, Compiled> result) {
		compile(this::createValueCodeGenerator, value, result);
	}

	private ValueCodeGenerator createValueCodeGenerator(GeneratedClass generatedClass) {
		return BeanDefinitionPropertyValueCodeGeneratorDelegates.createValueCodeGenerator(
				generatedClass.getMethods(), List.of(new GroupsMetadataValueDelegate()));
	}


	private static Object getGeneratedCodeReturnValue(Compiled compiled, GeneratedClass generatedClass) {
		try {
			Object instance = compiled.getInstance(Object.class, generatedClass.getName().reflectionName());
			Method get = ReflectionUtils.findMethod(instance.getClass(), "get");
			return get.invoke(null);
		}
		catch (Exception ex) {
			throw new RuntimeException("Failed to invoke generated code '%s':".formatted(generatedClass.getName()), ex);
		}
	}

}

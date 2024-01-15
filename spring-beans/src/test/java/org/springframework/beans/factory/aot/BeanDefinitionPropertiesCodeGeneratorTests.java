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

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.ValueCodeGenerator.Delegate;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.aot.CustomPropertyValue;
import org.springframework.beans.testfixture.beans.factory.aot.DeferredTypeBuilder;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanDefinitionPropertiesCodeGenerator}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Olga Maciaszek-Sharma
 * @author Sam Brannen
 * @since 6.0
 */
class BeanDefinitionPropertiesCodeGeneratorTests {

	private final RootBeanDefinition beanDefinition = new RootBeanDefinition();

	private final TestGenerationContext generationContext = new TestGenerationContext();

	@Test
	void setPrimaryWhenFalse() {
		this.beanDefinition.setPrimary(false);
		compile((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setPrimary");
			assertThat(actual.isPrimary()).isFalse();
		});
	}

	@Test
	void setPrimaryWhenTrue() {
		this.beanDefinition.setPrimary(true);
		compile((actual, compiled) -> assertThat(actual.isPrimary()).isTrue());
	}

	@Test
	void setScopeWhenEmptyString() {
		this.beanDefinition.setScope("");
		compile((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setScope");
			assertThat(actual.getScope()).isEmpty();
		});
	}

	@Test
	void setScopeWhenSingleton() {
		this.beanDefinition.setScope("singleton");
		compile((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setScope");
			assertThat(actual.getScope()).isEmpty();
		});
	}

	@Test
	void setScopeWhenOther() {
		this.beanDefinition.setScope("prototype");
		compile((actual, compiled) -> assertThat(actual.getScope()).isEqualTo("prototype"));
	}

	@Test
	void setDependsOnWhenEmpty() {
		this.beanDefinition.setDependsOn();
		compile((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setDependsOn");
			assertThat(actual.getDependsOn()).isNull();
		});
	}

	@Test
	void setDependsOnWhenNotEmpty() {
		this.beanDefinition.setDependsOn("a", "b", "c");
		compile((actual, compiled) -> assertThat(actual.getDependsOn()).containsExactly("a", "b", "c"));
	}

	@Test
	void setLazyInitWhenNoSet() {
		compile((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setLazyInit");
			assertThat(actual.isLazyInit()).isFalse();
			assertThat(actual.getLazyInit()).isNull();
		});
	}

	@Test
	void setLazyInitWhenFalse() {
		this.beanDefinition.setLazyInit(false);
		compile((actual, compiled) -> {
			assertThat(actual.isLazyInit()).isFalse();
			assertThat(actual.getLazyInit()).isFalse();
		});
	}

	@Test
	void setLazyInitWhenTrue() {
		this.beanDefinition.setLazyInit(true);
		compile((actual, compiled) -> {
			assertThat(actual.isLazyInit()).isTrue();
			assertThat(actual.getLazyInit()).isTrue();
		});
	}

	@Test
	void setAutowireCandidateWhenFalse() {
		this.beanDefinition.setAutowireCandidate(false);
		compile((actual, compiled) -> assertThat(actual.isAutowireCandidate()).isFalse());
	}

	@Test
	void setAutowireCandidateWhenTrue() {
		this.beanDefinition.setAutowireCandidate(true);
		compile((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setAutowireCandidate");
			assertThat(actual.isAutowireCandidate()).isTrue();
		});
	}

	@Test
	void setSyntheticWhenFalse() {
		this.beanDefinition.setSynthetic(false);
		compile((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setSynthetic");
			assertThat(actual.isSynthetic()).isFalse();
		});
	}

	@Test
	void setSyntheticWhenTrue() {
		this.beanDefinition.setSynthetic(true);
		compile((actual, compiled) -> assertThat(actual.isSynthetic()).isTrue());
	}

	@Test
	void setRoleWhenApplication() {
		this.beanDefinition.setRole(BeanDefinition.ROLE_APPLICATION);
		compile((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setRole");
			assertThat(actual.getRole()).isEqualTo(BeanDefinition.ROLE_APPLICATION);
		});
	}

	@Test
	void setRoleWhenInfrastructure() {
		this.beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		compile((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).contains("setRole(BeanDefinition.ROLE_INFRASTRUCTURE);");
			assertThat(actual.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
		});
	}

	@Test
	void setRoleWhenSupport() {
		this.beanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);
		compile((actual, compiled) -> {
			assertThat(compiled.getSourceFile()).contains("setRole(BeanDefinition.ROLE_SUPPORT);");
			assertThat(actual.getRole()).isEqualTo(BeanDefinition.ROLE_SUPPORT);
		});
	}

	@Test
	void setRoleWhenOther() {
		this.beanDefinition.setRole(999);
		compile((actual, compiled) -> assertThat(actual.getRole()).isEqualTo(999));
	}

	@Test
	void constructorArgumentValuesWhenIndexedValues() {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, String.class);
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1, "test");
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(2, 123);
		compile((actual, compiled) -> {
			ConstructorArgumentValues argumentValues = actual.getConstructorArgumentValues();
			Map<Integer, ValueHolder> values = argumentValues.getIndexedArgumentValues();
			assertThat(values.get(0)).satisfies(assertValueHolder(String.class, null, null));
			assertThat(values.get(1)).satisfies(assertValueHolder("test", null, null));
			assertThat(values.get(2)).satisfies(assertValueHolder(123, null, null));
			assertThat(values).hasSize(3);
			assertThat(argumentValues.getGenericArgumentValues()).isEmpty();
		});
	}

	@Test
	void constructorArgumentValuesWhenIndexedNullValue() {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, (Object) null);
		compile((actual, compiled) -> {
			ConstructorArgumentValues argumentValues = actual.getConstructorArgumentValues();
			Map<Integer, ValueHolder> values = argumentValues.getIndexedArgumentValues();
			assertThat(values.get(0)).satisfies(assertValueHolder(null, null, null));
			assertThat(values).hasSize(1);
			assertThat(argumentValues.getGenericArgumentValues()).isEmpty();
		});
	}

	@Test
	void constructorArgumentValuesWhenGenericValuesWithName() {
		this.beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(String.class);
		this.beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(2, Long.class.getName());
		this.beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(
				new ValueHolder("value", null, "param1"));
		this.beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(
				new ValueHolder("another", CharSequence.class.getName(), "param2"));
		compile((actual, compiled) -> {
			ConstructorArgumentValues argumentValues = actual.getConstructorArgumentValues();
			List<ValueHolder> values = argumentValues.getGenericArgumentValues();
			assertThat(values).satisfiesExactly(
					assertValueHolder(String.class, null, null),
					assertValueHolder(2, Long.class, null),
					assertValueHolder("value", null, "param1"),
					assertValueHolder("another", CharSequence.class, "param2"));
			assertThat(argumentValues.getIndexedArgumentValues()).isEmpty();
		});
	}

	private Consumer<ValueHolder> assertValueHolder(
			@Nullable Object value, @Nullable Class<?> type, @Nullable String name) {

		return valueHolder -> {
			assertThat(valueHolder.getValue()).isEqualTo(value);
			assertThat(valueHolder.getType()).isEqualTo((type != null ? type.getName() : null));
			assertThat(valueHolder.getName()).isEqualTo(name);
		};
	}

	@Test
	void propertyValuesWhenValues() {
		this.beanDefinition.setTargetType(PropertyValuesBean.class);
		this.beanDefinition.getPropertyValues().add("test", String.class);
		this.beanDefinition.getPropertyValues().add("spring", "framework");
		compile((actual, compiled) -> {
			assertThat(actual.getPropertyValues().get("test")).isEqualTo(String.class);
			assertThat(actual.getPropertyValues().get("spring")).isEqualTo("framework");
		});
		assertHasMethodInvokeHints(PropertyValuesBean.class, "setTest", "setSpring");
		assertHasDeclaredFieldsHint(PropertyValuesBean.class);
	}

	@Test
	void propertyValuesWhenValuesOnParentClass() {
		this.beanDefinition.setTargetType(ExtendedPropertyValuesBean.class);
		this.beanDefinition.getPropertyValues().add("test", String.class);
		this.beanDefinition.getPropertyValues().add("spring", "framework");
		compile((actual, compiled) -> {
			assertThat(actual.getPropertyValues().get("test")).isEqualTo(String.class);
			assertThat(actual.getPropertyValues().get("spring")).isEqualTo("framework");
		});
		assertHasMethodInvokeHints(PropertyValuesBean.class, "setTest", "setSpring");
		assertHasDeclaredFieldsHint(ExtendedPropertyValuesBean.class);
		assertHasDeclaredFieldsHint(PropertyValuesBean.class);
	}

	@Test
	void propertyValuesWhenContainsBeanReference() {
		this.beanDefinition.getPropertyValues().add("myService", new RuntimeBeanNameReference("test"));
		compile((actual, compiled) -> {
			assertThat(actual.getPropertyValues().contains("myService")).isTrue();
			assertThat(actual.getPropertyValues().get("myService"))
					.isInstanceOfSatisfying(RuntimeBeanReference.class,
							beanReference -> assertThat(beanReference.getBeanName()).isEqualTo("test"));
		});
	}

	@Test
	void propertyValuesWhenContainsManagedList() {
		ManagedList<Object> managedList = new ManagedList<>();
		managedList.add(new RuntimeBeanNameReference("test"));
		this.beanDefinition.getPropertyValues().add("value", managedList);
		compile((actual, compiled) -> {
			Object value = actual.getPropertyValues().get("value");
			assertThat(value).isInstanceOf(ManagedList.class).asInstanceOf(InstanceOfAssertFactories.LIST)
					.singleElement().isInstanceOf(BeanReference.class);
		});
	}

	@Test
	void propertyValuesWhenContainsManagedSet() {
		ManagedSet<Object> managedSet = new ManagedSet<>();
		managedSet.add(new RuntimeBeanNameReference("test"));
		this.beanDefinition.getPropertyValues().add("value", managedSet);
		compile((actual, compiled) -> {
			Object value = actual.getPropertyValues().get("value");
			assertThat(value).isInstanceOf(ManagedSet.class)
					.asInstanceOf(InstanceOfAssertFactories.COLLECTION)
					.singleElement().isInstanceOf(BeanReference.class);
		});
	}

	@Test
	void propertyValuesWhenContainsManagedMap() {
		ManagedMap<String, Object> managedMap = new ManagedMap<>();
		managedMap.put("test", new RuntimeBeanNameReference("test"));
		this.beanDefinition.getPropertyValues().add("value", managedMap);
		compile((actual, compiled) -> {
			Object value = actual.getPropertyValues().get("value");
			assertThat(value).isInstanceOf(ManagedMap.class)
					.asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
					.hasEntrySatisfying("test", ref -> assertThat(ref).isInstanceOf(BeanReference.class));
		});
	}

	@Test
	void propertyValuesWhenValuesOnFactoryBeanClass() {
		this.beanDefinition.setTargetType(String.class);
		this.beanDefinition.setBeanClass(PropertyValuesFactoryBean.class);
		this.beanDefinition.getPropertyValues().add("prefix", "Hello");
		this.beanDefinition.getPropertyValues().add("name", "World");
		compile((actual, compiled) -> {
			assertThat(actual.getPropertyValues().get("prefix")).isEqualTo("Hello");
			assertThat(actual.getPropertyValues().get("name")).isEqualTo("World");
		});
		assertHasMethodInvokeHints(PropertyValuesFactoryBean.class, "setPrefix", "setName");
		assertHasDeclaredFieldsHint(PropertyValuesFactoryBean.class);
	}

	@Test
	void propertyValuesWhenCustomValuesUsingDelegate() {
		this.beanDefinition.setTargetType(PropertyValuesBean.class);
		this.beanDefinition.getPropertyValues().add("test", new CustomPropertyValue("test"));
		this.beanDefinition.getPropertyValues().add("spring", new CustomPropertyValue("framework"));
		compile(value -> true, List.of(new CustomPropertyValue.ValueCodeGeneratorDelegate()), (actual, compiled) -> {
			assertThat(actual.getPropertyValues().get("test")).isInstanceOfSatisfying(CustomPropertyValue.class,
					customPropertyValue -> assertThat(customPropertyValue.value()).isEqualTo("test"));
			assertThat(actual.getPropertyValues().get("spring")).isInstanceOfSatisfying(CustomPropertyValue.class,
					customPropertyValue -> assertThat(customPropertyValue.value()).isEqualTo("framework"));
		});
		assertHasMethodInvokeHints(PropertyValuesBean.class, "setTest", "setSpring");
		assertHasDeclaredFieldsHint(PropertyValuesBean.class);
	}

	@Test
	void attributesWhenAllFiltered() {
		this.beanDefinition.setAttribute("a", "A");
		this.beanDefinition.setAttribute("b", "B");
		Predicate<String> attributeFilter = attribute -> false;
		compile(attributeFilter, (actual, compiled) -> {
			assertThat(compiled.getSourceFile()).doesNotContain("setAttribute");
			assertThat(actual.getAttribute("a")).isNull();
			assertThat(actual.getAttribute("b")).isNull();
		});
	}

	@Test
	void attributesWhenSomeFiltered() {
		this.beanDefinition.setAttribute("a", "A");
		this.beanDefinition.setAttribute("b", "B");
		Predicate<String> attributeFilter = "a"::equals;
		compile(attributeFilter, (actual, compiled) -> {
			assertThat(actual.getAttribute("a")).isEqualTo("A");
			assertThat(actual.getAttribute("b")).isNull();
		});
	}

	@Test
	void qualifiersWhenQualifierHasNoValue() {
		this.beanDefinition.addQualifier(new AutowireCandidateQualifier("com.example.Qualifier"));
		compile((actual, compiled) -> {
			assertThat(actual.getQualifiers()).singleElement().satisfies(isQualifierFor("com.example.Qualifier", null));
			assertThat(this.beanDefinition.getQualifiers()).isEqualTo(actual.getQualifiers());
		});
	}

	@Test
	void qualifiersWhenQualifierHasStringValue() {
		this.beanDefinition.addQualifier(new AutowireCandidateQualifier("com.example.Qualifier", "id"));
		compile((actual, compiled) -> {
			assertThat(actual.getQualifiers()).singleElement().satisfies(isQualifierFor("com.example.Qualifier", "id"));
			assertThat(this.beanDefinition.getQualifiers()).isEqualTo(actual.getQualifiers());
		});
	}

	@Test
	void qualifiersWhenMultipleQualifiers() {
		this.beanDefinition.addQualifier(new AutowireCandidateQualifier("com.example.Qualifier", "id"));
		this.beanDefinition.addQualifier(new AutowireCandidateQualifier("com.example.Another", ChronoUnit.SECONDS));
		compile((actual, compiled) -> {
			List<AutowireCandidateQualifier> qualifiers = new ArrayList<>(actual.getQualifiers());
			assertThat(qualifiers).satisfiesExactly(isQualifierFor("com.example.Qualifier", "id"),
					isQualifierFor("com.example.Another", ChronoUnit.SECONDS));
		});
	}

	private Consumer<AutowireCandidateQualifier> isQualifierFor(String typeName, Object value) {
		return qualifier -> {
			assertThat(qualifier.getTypeName()).isEqualTo(typeName);
			assertThat(qualifier.getAttribute(AutowireCandidateQualifier.VALUE_KEY)).isEqualTo(value);
		};
	}

	@Test
	void multipleItems() {
		this.beanDefinition.setPrimary(true);
		this.beanDefinition.setScope("test");
		this.beanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);
		compile((actual, compiled) -> {
			assertThat(actual.isPrimary()).isTrue();
			assertThat(actual.getScope()).isEqualTo("test");
			assertThat(actual.getRole()).isEqualTo(BeanDefinition.ROLE_SUPPORT);
		});
	}

	@Nested
	class InitDestroyMethodTests {

		private static final String privateInitMethod = InitDestroyBean.class.getName() + ".privateInit";

		private static final String privateDestroyMethod = InitDestroyBean.class.getName() + ".privateDestroy";

		@Test
		void noInitMethod() {
			beanDefinition.setTargetType(InitDestroyBean.class);
			compile((beanDef, compiled) -> assertThat(beanDef.getInitMethodNames()).isNull());
		}

		@Test
		void singleInitMethod() {
			beanDefinition.setTargetType(InitDestroyBean.class);
			beanDefinition.setInitMethodName("init");
			compile((beanDef, compiled) -> assertThat(beanDef.getInitMethodNames()).containsExactly("init"));
			assertHasMethodInvokeHints(InitDestroyBean.class, "init");
		}

		@Test
		void singleInitMethodFromInterface() {
			beanDefinition.setTargetType(InitializableTestBean.class);
			beanDefinition.setInitMethodName("initialize");
			compile((beanDef, compiled) -> assertThat(beanDef.getInitMethodNames()).containsExactly("initialize"));
			assertHasMethodInvokeHints(InitializableTestBean.class, "initialize");
			assertHasMethodInvokeHints(Initializable.class, "initialize");
		}

		@Test
		void privateInitMethod() {
			beanDefinition.setTargetType(InitDestroyBean.class);
			beanDefinition.setInitMethodName(privateInitMethod);
			compile((beanDef, compiled) -> assertThat(beanDef.getInitMethodNames()).containsExactly(privateInitMethod));
			assertHasMethodInvokeHints(InitDestroyBean.class, "privateInit");
		}

		@Test
		void multipleInitMethods() {
			beanDefinition.setTargetType(InitDestroyBean.class);
			beanDefinition.setInitMethodNames("init", privateInitMethod);
			compile((beanDef, compiled) -> assertThat(beanDef.getInitMethodNames()).containsExactly("init", privateInitMethod));
			assertHasMethodInvokeHints(InitDestroyBean.class, "init", "privateInit");
		}

		@Test
		void noDestroyMethod() {
			beanDefinition.setTargetType(InitDestroyBean.class);
			compile((beanDef, compiled) -> assertThat(beanDef.getDestroyMethodNames()).isNull());
			assertReflectionOnPublisher();
		}

		@Test
		void singleDestroyMethod() {
			beanDefinition.setTargetType(InitDestroyBean.class);
			beanDefinition.setDestroyMethodName("destroy");
			compile((beanDef, compiled) -> assertThat(beanDef.getDestroyMethodNames()).containsExactly("destroy"));
			assertHasMethodInvokeHints(InitDestroyBean.class, "destroy");
			assertReflectionOnPublisher();
		}

		@Test
		void singleDestroyMethodFromInterface() {
			beanDefinition.setTargetType(DisposableTestBean.class);
			beanDefinition.setDestroyMethodName("dispose");
			compile((beanDef, compiled) -> assertThat(beanDef.getDestroyMethodNames()).containsExactly("dispose"));
			assertHasMethodInvokeHints(DisposableTestBean.class, "dispose");
			assertHasMethodInvokeHints(Disposable.class, "dispose");
			assertReflectionOnPublisher();
		}

		@Test
		void privateDestroyMethod() {
			beanDefinition.setTargetType(InitDestroyBean.class);
			beanDefinition.setDestroyMethodName(privateDestroyMethod);
			compile((beanDef, compiled) -> assertThat(beanDef.getDestroyMethodNames()).containsExactly(privateDestroyMethod));
			assertHasMethodInvokeHints(InitDestroyBean.class, "privateDestroy");
			assertReflectionOnPublisher();
		}

		@Test
		void multipleDestroyMethods() {
			beanDefinition.setTargetType(InitDestroyBean.class);
			beanDefinition.setDestroyMethodNames("destroy", privateDestroyMethod);
			compile((beanDef, compiled) -> assertThat(beanDef.getDestroyMethodNames()).containsExactly("destroy", privateDestroyMethod));
			assertHasMethodInvokeHints(InitDestroyBean.class, "destroy", "privateDestroy");
			assertReflectionOnPublisher();
		}

		private void assertReflectionOnPublisher() {
			assertThat(RuntimeHintsPredicates.reflection().onType(Publisher.class)).accepts(generationContext.getRuntimeHints());
		}

	}

	private void assertHasMethodInvokeHints(Class<?> beanType, String... methodNames) {
		assertThat(methodNames).allMatch(methodName -> RuntimeHintsPredicates.reflection()
				.onMethod(beanType, methodName).invoke()
				.test(this.generationContext.getRuntimeHints()));
	}

	private void assertHasDeclaredFieldsHint(Class<?> beanType) {
		assertThat(RuntimeHintsPredicates.reflection()
				.onType(beanType).withMemberCategory(MemberCategory.DECLARED_FIELDS))
				.accepts(this.generationContext.getRuntimeHints());
	}

	private void compile(BiConsumer<RootBeanDefinition, Compiled> result) {
		compile(attribute -> true, result);
	}

	private void compile(Predicate<String> attributeFilter,
			BiConsumer<RootBeanDefinition, Compiled> result) {
		compile(attributeFilter, Collections.emptyList(), result);
	}

	private void compile(Predicate<String> attributeFilter, List<Delegate> additionalDelegates,
			BiConsumer<RootBeanDefinition, Compiled> result) {
		DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();
		GeneratedClass generatedClass = this.generationContext.getGeneratedClasses().addForFeature("TestCode", typeBuilder);
		BeanDefinitionPropertiesCodeGenerator codeGenerator = new BeanDefinitionPropertiesCodeGenerator(
				this.generationContext.getRuntimeHints(), attributeFilter,
				generatedClass.getMethods(), additionalDelegates, (name, value) -> null);
		CodeBlock generatedCode = codeGenerator.generateCode(this.beanDefinition);
		typeBuilder.set(type -> {
			type.addModifiers(Modifier.PUBLIC);
			type.addSuperinterface(ParameterizedTypeName.get(Supplier.class, RootBeanDefinition.class));
			type.addMethod(MethodSpec.methodBuilder("get")
					.addModifiers(Modifier.PUBLIC)
					.returns(RootBeanDefinition.class)
					.addStatement("$T beanDefinition = new $T()", RootBeanDefinition.class, RootBeanDefinition.class)
					.addStatement("$T beanFactory = new $T()", DefaultListableBeanFactory.class, DefaultListableBeanFactory.class)
					.addCode(generatedCode)
					.addStatement("return beanDefinition").build());
		});
		this.generationContext.writeGeneratedContent();
		TestCompiler.forSystem().with(this.generationContext).compile(compiled -> {
			RootBeanDefinition suppliedBeanDefinition = (RootBeanDefinition) compiled.getInstance(Supplier.class).get();
			result.accept(suppliedBeanDefinition, compiled);
		});
	}

	static class InitDestroyBean {

		void init() {
		}

		@SuppressWarnings("unused")
		private void privateInit() {
		}

		void destroy() {
		}

		@SuppressWarnings("unused")
		private void privateDestroy() {
		}

	}

	interface Initializable {

		void initialize();
	}

	static class InitializableTestBean implements Initializable {

		@Override
		public void initialize() {
		}
	}

	interface Disposable {

		void dispose();
	}

	static class DisposableTestBean implements Disposable {

		@Override
		public void dispose() {
		}

	}

	static class PropertyValuesBean {

		private Class<?> test;

		private String spring;

		public Class<?> getTest() {
			return this.test;
		}

		public void setTest(Class<?> test) {
			this.test = test;
		}

		public String getSpring() {
			return this.spring;
		}

		public void setSpring(String spring) {
			this.spring = spring;
		}

	}

	static class ExtendedPropertyValuesBean extends PropertyValuesBean {

	}

	static class PropertyValuesFactoryBean implements FactoryBean<String> {

		private String prefix;

		private String name;

		public String getPrefix() {
			return this.prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Nullable
		@Override
		public String getObject() {
			return getPrefix() + " " + getName();
		}

		@Nullable
		@Override
		public Class<?> getObjectType() {
			return String.class;
		}

	}

}

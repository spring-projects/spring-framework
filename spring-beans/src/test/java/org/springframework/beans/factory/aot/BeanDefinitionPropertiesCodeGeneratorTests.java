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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.RootBeanDefinition;
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
		compile((actual, compiled) -> assertThat(actual.getScope())
				.isEqualTo("prototype"));
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
		compile((actual, compiled) -> assertThat(actual.getDependsOn())
				.containsExactly("a", "b", "c"));
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
		compile(
				(actual, compiled) -> assertThat(actual.isAutowireCandidate()).isFalse());
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
		compile(
				(actual, compiled) -> assertThat(actual.isSynthetic()).isTrue());
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
			assertThat(compiled.getSourceFile())
					.contains("setRole(BeanDefinition.ROLE_INFRASTRUCTURE);");
			assertThat(actual.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
		});
	}

	@Test
	void setRoleWhenSupport() {
		this.beanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);
		compile((actual, compiled) -> {
			assertThat(compiled.getSourceFile())
					.contains("setRole(BeanDefinition.ROLE_SUPPORT);");
			assertThat(actual.getRole()).isEqualTo(BeanDefinition.ROLE_SUPPORT);
		});
	}

	@Test
	void setRoleWhenOther() {
		this.beanDefinition.setRole(999);
		compile(
				(actual, compiled) -> assertThat(actual.getRole()).isEqualTo(999));
	}

	@Test
	void setInitMethodWhenSingleInitMethod() {
		this.beanDefinition.setTargetType(InitDestroyBean.class);
		this.beanDefinition.setInitMethodName("i1");
		compile((actual, compiled) -> assertThat(actual.getInitMethodNames())
				.containsExactly("i1"));
		String[] methodNames = { "i1" };
		assertHasMethodInvokeHints(InitDestroyBean.class, methodNames);
	}

	@Test
	void setInitMethodWhenNoInitMethod() {
		this.beanDefinition.setTargetType(InitDestroyBean.class);
		compile((actual, compiled) -> assertThat(actual.getInitMethodNames()).isNull());
	}

	@Test
	void setInitMethodWhenMultipleInitMethods() {
		this.beanDefinition.setTargetType(InitDestroyBean.class);
		this.beanDefinition.setInitMethodNames("i1", "i2");
		compile((actual, compiled) -> assertThat(actual.getInitMethodNames())
				.containsExactly("i1", "i2"));
		String[] methodNames = { "i1", "i2" };
		assertHasMethodInvokeHints(InitDestroyBean.class, methodNames);
	}

	@Test
	void setDestroyMethodWhenDestroyInitMethod() {
		this.beanDefinition.setTargetType(InitDestroyBean.class);
		this.beanDefinition.setDestroyMethodName("d1");
		compile(
				(actual, compiled) -> assertThat(actual.getDestroyMethodNames())
						.containsExactly("d1"));
		String[] methodNames = { "d1" };
		assertHasMethodInvokeHints(InitDestroyBean.class, methodNames);
	}

	@Test
	void setDestroyMethodWhenNoDestroyMethod() {
		this.beanDefinition.setTargetType(InitDestroyBean.class);
		compile((actual, compiled) -> assertThat(actual.getDestroyMethodNames()).isNull());
	}

	@Test
	void setDestroyMethodWhenMultipleDestroyMethods() {
		this.beanDefinition.setTargetType(InitDestroyBean.class);
		this.beanDefinition.setDestroyMethodNames("d1", "d2");
		compile(
				(actual, compiled) -> assertThat(actual.getDestroyMethodNames())
						.containsExactly("d1", "d2"));
		String[] methodNames = { "d1", "d2" };
		assertHasMethodInvokeHints(InitDestroyBean.class, methodNames);
	}

	private void assertHasMethodInvokeHints(Class<?> beanType, String... methodNames) {
		assertThat(methodNames).allMatch(methodName -> RuntimeHintsPredicates.reflection()
				.onMethod(beanType, methodName).invoke()
				.test(this.generationContext.getRuntimeHints()));
	}

	@Test
	void constructorArgumentValuesWhenValues() {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0,
				String.class);
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1,
				"test");
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(2,
				123);
		compile((actual, compiled) -> {
			Map<Integer, ValueHolder> values = actual.getConstructorArgumentValues()
					.getIndexedArgumentValues();
			assertThat(values.get(0).getValue()).isEqualTo(String.class);
			assertThat(values.get(1).getValue()).isEqualTo("test");
			assertThat(values.get(2).getValue()).isEqualTo(123);
		});
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
		String[] methodNames = { "setTest", "setSpring" };
		assertHasMethodInvokeHints(PropertyValuesBean.class, methodNames);
	}

	@Test
	void propertyValuesWhenContainsBeanReference() {
		this.beanDefinition.getPropertyValues().add("myService",
				new RuntimeBeanNameReference("test"));
		compile((actual, compiled) -> {
			assertThat(actual.getPropertyValues().contains("myService")).isTrue();
			assertThat(actual.getPropertyValues().get("myService"))
					.isInstanceOfSatisfying(RuntimeBeanReference.class,
							beanReference -> assertThat(beanReference.getBeanName())
									.isEqualTo("test"));
		});
	}

	@Test
	void propertyValuesWhenContainsManagedList() {
		ManagedList<Object> managedList = new ManagedList<>();
		managedList.add(new RuntimeBeanNameReference("test"));
		this.beanDefinition.getPropertyValues().add("value", managedList);
		compile((actual, compiled) -> {
			Object value = actual.getPropertyValues().get("value");
			assertThat(value).isInstanceOf(ManagedList.class);
			assertThat(((List<?>) value).get(0)).isInstanceOf(BeanReference.class);
		});
	}

	@Test
	void propertyValuesWhenContainsManagedSet() {
		ManagedSet<Object> managedSet = new ManagedSet<>();
		managedSet.add(new RuntimeBeanNameReference("test"));
		this.beanDefinition.getPropertyValues().add("value", managedSet);
		compile((actual, compiled) -> {
			Object value = actual.getPropertyValues().get("value");
			assertThat(value).isInstanceOf(ManagedSet.class);
			assertThat(((Set<?>) value).iterator().next())
					.isInstanceOf(BeanReference.class);
		});
	}

	@Test
	void propertyValuesWhenContainsManagedMap() {
		ManagedMap<String, Object> managedMap = new ManagedMap<>();
		managedMap.put("test", new RuntimeBeanNameReference("test"));
		this.beanDefinition.getPropertyValues().add("value", managedMap);
		compile((actual, compiled) -> {
			Object value = actual.getPropertyValues().get("value");
			assertThat(value).isInstanceOf(ManagedMap.class);
			assertThat(((Map<?, ?>) value).get("test")).isInstanceOf(BeanReference.class);
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
		String[] methodNames = { "setPrefix", "setName" };
		assertHasMethodInvokeHints(PropertyValuesFactoryBean.class, methodNames);
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

	private void compile(BiConsumer<RootBeanDefinition, Compiled> result) {
		compile(attribute -> true, result);
	}

	private void compile(
			Predicate<String> attributeFilter,
			BiConsumer<RootBeanDefinition, Compiled> result) {
		DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();
		GeneratedClass generatedClass = this.generationContext.getGeneratedClasses().addForFeature("TestCode", typeBuilder);
		BeanDefinitionPropertiesCodeGenerator codeGenerator = new BeanDefinitionPropertiesCodeGenerator(
				this.generationContext.getRuntimeHints(), attributeFilter,
				generatedClass.getMethods(), (name, value) -> null);
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
		TestCompiler.forSystem().withFiles(this.generationContext.getGeneratedFiles()).compile(compiled -> {
			RootBeanDefinition suppliedBeanDefinition = (RootBeanDefinition) compiled
					.getInstance(Supplier.class).get();
			result.accept(suppliedBeanDefinition, compiled);
		});
	}

	static class InitDestroyBean {

		void i1() {
		}

		void i2() {
		}

		void d1() {
		}

		void d2() {
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
		public String getObject() throws Exception {
			return getPrefix() + " " + getName();
		}

		@Nullable
		@Override
		public Class<?> getObjectType() {
			return String.class;
		}

	}

}

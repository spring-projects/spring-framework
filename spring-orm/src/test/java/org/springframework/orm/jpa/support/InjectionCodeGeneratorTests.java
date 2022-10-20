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

package org.springframework.orm.jpa.support;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.TestBeanWithPackagePrivateField;
import org.springframework.beans.testfixture.beans.TestBeanWithPackagePrivateMethod;
import org.springframework.beans.testfixture.beans.TestBeanWithPrivateMethod;
import org.springframework.beans.testfixture.beans.TestBeanWithPublicField;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InjectionCodeGenerator}.
 *
 * @author Phillip Webb
 */
class InjectionCodeGeneratorTests {

	private static final String INSTANCE_VARIABLE = "instance";

	private static final ClassName TEST_TARGET = ClassName.get("com.example", "Test");

	private final RuntimeHints hints = new RuntimeHints();

	@Test
	void generateCodeWhenPublicFieldInjectsValue() {
		TestBeanWithPublicField bean = new TestBeanWithPublicField();
		Field field = ReflectionUtils.findField(bean.getClass(), "age");
		ClassName targetClassName = TEST_TARGET;
		CodeBlock generatedCode = createGenerator(targetClassName).generateInjectionCode(
				field, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
		testCompiledResult(targetClassName, generatedCode, TestBeanWithPublicField.class, (actual, compiled) -> {
			TestBeanWithPublicField instance = new TestBeanWithPublicField();
			actual.accept(instance);
			assertThat(instance).extracting("age").isEqualTo(123);
			assertThat(compiled.getSourceFile()).contains("instance.age = 123");
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void generateCodeWhenPackagePrivateFieldInTargetPackageInjectsValue() {
		TestBeanWithPackagePrivateField bean = new TestBeanWithPackagePrivateField();
		Field field = ReflectionUtils.findField(bean.getClass(), "age");
		ClassName targetClassName = ClassName.get(TestBeanWithPackagePrivateField.class.getPackageName(), "Test");
		CodeBlock generatedCode = createGenerator(targetClassName).generateInjectionCode(
				field, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
		testCompiledResult(targetClassName, generatedCode, TestBeanWithPackagePrivateField.class, (actual, compiled) -> {
			TestBeanWithPackagePrivateField instance = new TestBeanWithPackagePrivateField();
			actual.accept(instance);
			assertThat(instance).extracting("age").isEqualTo(123);
			assertThat(compiled.getSourceFile()).contains("instance.age = 123");
		});
	}

	@Test
	void generateCodeWhenPackagePrivateFieldInAnotherPackageUsesReflection() {
		TestBeanWithPackagePrivateField bean = new TestBeanWithPackagePrivateField();
		Field field = ReflectionUtils.findField(bean.getClass(), "age");
		ClassName targetClassName = TEST_TARGET;
		CodeBlock generatedCode = createGenerator(targetClassName).generateInjectionCode(
				field, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
		testCompiledResult(targetClassName, generatedCode, TestBeanWithPackagePrivateField.class, (actual, compiled) -> {
			TestBeanWithPackagePrivateField instance = new TestBeanWithPackagePrivateField();
			actual.accept(instance);
			assertThat(instance).extracting("age").isEqualTo(123);
			assertThat(compiled.getSourceFile()).contains("setField(");
		});
	}

	@Test
	void generateCodeWhenPrivateFieldInjectsValueUsingReflection() {
		TestBean bean = new TestBean();
		Field field = ReflectionUtils.findField(bean.getClass(), "age");
		ClassName targetClassName = ClassName.get(TestBean.class);
		CodeBlock generatedCode = createGenerator(targetClassName).generateInjectionCode(
				field, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
		testCompiledResult(targetClassName, generatedCode, TestBean.class, (actual, compiled) -> {
			TestBean instance = new TestBean();
			actual.accept(instance);
			assertThat(instance).extracting("age").isEqualTo(123);
			assertThat(compiled.getSourceFile()).contains("setField(");
		});
	}

	@Test
	void generateCodeWhenPrivateFieldAddsHint() {
		TestBean bean = new TestBean();
		Field field = ReflectionUtils.findField(bean.getClass(), "age");
		createGenerator(TEST_TARGET).generateInjectionCode(
				field, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
		assertThat(RuntimeHintsPredicates.reflection().onField(TestBean.class, "age"))
				.accepts(this.hints);
	}

	@Test
	void generateCodeWhenPublicMethodInjectsValue() {
		TestBean bean = new TestBean();
		Method method = ReflectionUtils.findMethod(bean.getClass(), "setAge", int.class);
		ClassName targetClassName = TEST_TARGET;
		CodeBlock generatedCode = createGenerator(targetClassName).generateInjectionCode(
				method, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
		testCompiledResult(targetClassName, generatedCode, TestBean.class, (actual, compiled) -> {
			TestBean instance = new TestBean();
			actual.accept(instance);
			assertThat(instance).extracting("age").isEqualTo(123);
			assertThat(compiled.getSourceFile()).contains("instance.setAge(");
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void generateCodeWhenPackagePrivateMethodInTargetPackageInjectsValue() {
		TestBeanWithPackagePrivateMethod bean = new TestBeanWithPackagePrivateMethod();
		Method method = ReflectionUtils.findMethod(bean.getClass(), "setAge", int.class);
		ClassName targetClassName = ClassName.get(TestBeanWithPackagePrivateMethod.class);
		CodeBlock generatedCode = createGenerator(targetClassName).generateInjectionCode(
				method, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
		testCompiledResult(targetClassName, generatedCode, TestBeanWithPackagePrivateMethod.class, (actual, compiled) -> {
			TestBeanWithPackagePrivateMethod instance = new TestBeanWithPackagePrivateMethod();
			actual.accept(instance);
			assertThat(instance).extracting("age").isEqualTo(123);
			assertThat(compiled.getSourceFile()).contains("instance.setAge(");
		});
	}

	@Test
	void generateCodeWhenPackagePrivateMethodInAnotherPackageUsesReflection() {
		TestBeanWithPackagePrivateMethod bean = new TestBeanWithPackagePrivateMethod();
		Method method = ReflectionUtils.findMethod(bean.getClass(), "setAge", int.class);
		ClassName targetClassName = TEST_TARGET;
		CodeBlock generatedCode = createGenerator(targetClassName).generateInjectionCode(
				method, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
		testCompiledResult(targetClassName, generatedCode, TestBeanWithPackagePrivateMethod.class, (actual, compiled) -> {
			TestBeanWithPackagePrivateMethod instance = new TestBeanWithPackagePrivateMethod();
			actual.accept(instance);
			assertThat(instance).extracting("age").isEqualTo(123);
			assertThat(compiled.getSourceFile()).contains("invokeMethod(");
		});
	}

	@Test
	void generateCodeWhenPrivateMethodInjectsValueUsingReflection() {
		TestBeanWithPrivateMethod bean = new TestBeanWithPrivateMethod();
		Method method = ReflectionUtils.findMethod(bean.getClass(), "setAge", int.class);
		ClassName targetClassName = ClassName.get(TestBeanWithPrivateMethod.class);
		CodeBlock generatedCode = createGenerator(targetClassName)
				.generateInjectionCode(method, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
		testCompiledResult(targetClassName, generatedCode, TestBeanWithPrivateMethod.class, (actual, compiled) -> {
			TestBeanWithPrivateMethod instance = new TestBeanWithPrivateMethod();
			actual.accept(instance);
			assertThat(instance).extracting("age").isEqualTo(123);
			assertThat(compiled.getSourceFile()).contains("invokeMethod(");
		});
	}

	@Test
	void generateCodeWhenPrivateMethodAddsHint() {
		TestBeanWithPrivateMethod bean = new TestBeanWithPrivateMethod();
		Method method = ReflectionUtils.findMethod(bean.getClass(), "setAge", int.class);
		createGenerator(TEST_TARGET).generateInjectionCode(
				method, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
		assertThat(RuntimeHintsPredicates.reflection()
				.onMethod(TestBeanWithPrivateMethod.class, "setAge").invoke()).accepts(this.hints);
	}

	private InjectionCodeGenerator createGenerator(ClassName target) {
		return new InjectionCodeGenerator(target, this.hints);
	}

	@SuppressWarnings("unchecked")
	private <T> void testCompiledResult(ClassName generatedClasName, CodeBlock generatedCode, Class<T> target,
			BiConsumer<Consumer<T>, Compiled> result) {
		JavaFile javaFile = createJavaFile(generatedClasName, generatedCode, target);
		TestCompiler.forSystem().compile(javaFile::writeTo,
				compiled -> result.accept(compiled.getInstance(Consumer.class), compiled));
	}

	private JavaFile createJavaFile(ClassName generatedClasName, CodeBlock generatedCode, Class<?> target) {
		TypeSpec.Builder builder = TypeSpec.classBuilder(generatedClasName.simpleName() + "__Injector");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addSuperinterface(ParameterizedTypeName.get(Consumer.class, target));
		builder.addMethod(MethodSpec.methodBuilder("accept").addModifiers(Modifier.PUBLIC)
				.addParameter(target, INSTANCE_VARIABLE).addCode(generatedCode).build());
		return JavaFile.builder(generatedClasName.packageName(), builder.build()).build();
	}

}

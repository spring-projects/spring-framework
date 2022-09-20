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
import org.springframework.beans.testfixture.beans.TestBeanWithPrivateMethod;
import org.springframework.beans.testfixture.beans.TestBeanWithPublicField;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
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

	private RuntimeHints hints = new RuntimeHints();

	private InjectionCodeGenerator generator = new InjectionCodeGenerator(hints);

	@Test
	void generateCodeWhenPublicFieldInjectsValue() {
		TestBeanWithPublicField bean = new TestBeanWithPublicField();
		Field field = ReflectionUtils.findField(bean.getClass(), "age");
		CodeBlock generatedCode = this.generator.generateInjectionCode(field, INSTANCE_VARIABLE,
				CodeBlock.of("$L", 123));
		testCompiledResult(generatedCode, TestBeanWithPublicField.class, (actual, compiled) -> {
			TestBeanWithPublicField instance = new TestBeanWithPublicField();
			actual.accept(instance);
			assertThat(instance).extracting("age").isEqualTo(123);
			assertThat(compiled.getSourceFile()).contains("instance.age = 123");
		});
	}

	@Test
	void generateCodeWhenPrivateFieldInjectsValueUsingReflection() {
		TestBean bean = new TestBean();
		Field field = ReflectionUtils.findField(bean.getClass(), "age");
		CodeBlock generatedCode = this.generator.generateInjectionCode(field, INSTANCE_VARIABLE,
				CodeBlock.of("$L", 123));
		testCompiledResult(generatedCode, TestBean.class, (actual, compiled) -> {
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
		this.generator.generateInjectionCode(field, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
		assertThat(RuntimeHintsPredicates.reflection().onField(TestBean.class, "age"))
				.accepts(this.hints);
	}

	@Test
	void generateCodeWhenPublicMethodInjectsValue() {
		TestBean bean = new TestBean();
		Method method = ReflectionUtils.findMethod(bean.getClass(), "setAge", int.class);
		CodeBlock generatedCode = this.generator.generateInjectionCode(method, INSTANCE_VARIABLE,
				CodeBlock.of("$L", 123));
		testCompiledResult(generatedCode, TestBean.class, (actual, compiled) -> {
			TestBean instance = new TestBean();
			actual.accept(instance);
			assertThat(instance).extracting("age").isEqualTo(123);
			assertThat(compiled.getSourceFile()).contains("instance.setAge(");
		});
	}

	@Test
	void generateCodeWhenPrivateMethodInjectsValueUsingReflection() {
		TestBeanWithPrivateMethod bean = new TestBeanWithPrivateMethod();
		Method method = ReflectionUtils.findMethod(bean.getClass(), "setAge", int.class);
		CodeBlock generatedCode = this.generator.generateInjectionCode(method, INSTANCE_VARIABLE,
				CodeBlock.of("$L", 123));
		testCompiledResult(generatedCode, TestBeanWithPrivateMethod.class, (actual, compiled) -> {
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
		this.generator.generateInjectionCode(method, INSTANCE_VARIABLE, CodeBlock.of("$L", 123));
		assertThat(RuntimeHintsPredicates.reflection()
				.onMethod(TestBeanWithPrivateMethod.class, "setAge").invoke()).accepts(this.hints);
	}

	@SuppressWarnings("unchecked")
	private <T> void testCompiledResult(CodeBlock generatedCode, Class<T> target,
			BiConsumer<Consumer<T>, Compiled> result) {
		JavaFile javaFile = createJavaFile(generatedCode, target);
		TestCompiler.forSystem().compile(javaFile::writeTo,
				compiled -> result.accept(compiled.getInstance(Consumer.class), compiled));
	}

	private JavaFile createJavaFile(CodeBlock generatedCode, Class<?> target) {
		TypeSpec.Builder builder = TypeSpec.classBuilder("Injector");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addSuperinterface(ParameterizedTypeName.get(Consumer.class, target));
		builder.addMethod(MethodSpec.methodBuilder("accept").addModifiers(Modifier.PUBLIC)
				.addParameter(target, INSTANCE_VARIABLE).addCode(generatedCode).build());
		return JavaFile.builder("__", builder.build()).build();
	}

}

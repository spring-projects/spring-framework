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

import java.io.InputStream;
import java.io.OutputStream;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.testfixture.beans.factory.aot.DeferredTypeBuilder;
import org.springframework.core.ResolvableType;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link BeanDefinitionPropertyValueCodeGenerator}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @see BeanDefinitionPropertyValueCodeGeneratorTests
 */
class BeanDefinitionPropertyValueCodeGeneratorTests {

	private void compile(Object value, BiConsumer<Object, Compiled> result) {
		TestGenerationContext generationContext = new TestGenerationContext();
		DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();
		GeneratedClass generatedClass = generationContext.getGeneratedClasses().addForFeature("TestCode", typeBuilder);
		CodeBlock generatedCode = new BeanDefinitionPropertyValueCodeGenerator(
				generatedClass.getMethods()).generateCode(value);
		typeBuilder.set(type -> {
			type.addModifiers(Modifier.PUBLIC);
			type.addSuperinterface(
					ParameterizedTypeName.get(Supplier.class, Object.class));
			type.addMethod(MethodSpec.methodBuilder("get").addModifiers(Modifier.PUBLIC)
					.returns(Object.class).addStatement("return $L", generatedCode).build());
		});
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().withFiles(generationContext.getGeneratedFiles()).compile(compiled ->
				result.accept(compiled.getInstance(Supplier.class).get(), compiled));
	}

	@Nested
	class NullTests {

		@Test
		void generateWhenNull() {
			compile(null, (instance, compiled) -> assertThat(instance).isNull());
		}

	}

	@Nested
	class PrimitiveTests {

		@Test
		void generateWhenBoolean() {
			compile(true, (instance, compiled) -> {
				assertThat(instance).isEqualTo(Boolean.TRUE);
				assertThat(compiled.getSourceFile()).contains("true");
			});
		}

		@Test
		void generateWhenByte() {
			compile((byte) 2, (instance, compiled) -> {
				assertThat(instance).isEqualTo((byte) 2);
				assertThat(compiled.getSourceFile()).contains("(byte) 2");
			});
		}

		@Test
		void generateWhenShort() {
			compile((short) 3, (instance, compiled) -> {
				assertThat(instance).isEqualTo((short) 3);
				assertThat(compiled.getSourceFile()).contains("(short) 3");
			});
		}

		@Test
		void generateWhenInt() {
			compile(4, (instance, compiled) -> {
				assertThat(instance).isEqualTo(4);
				assertThat(compiled.getSourceFile()).contains("return 4;");
			});
		}

		@Test
		void generateWhenLong() {
			compile(5L, (instance, compiled) -> {
				assertThat(instance).isEqualTo(5L);
				assertThat(compiled.getSourceFile()).contains("5L");
			});
		}

		@Test
		void generateWhenFloat() {
			compile(0.1F, (instance, compiled) -> {
				assertThat(instance).isEqualTo(0.1F);
				assertThat(compiled.getSourceFile()).contains("0.1F");
			});
		}

		@Test
		void generateWhenDouble() {
			compile(0.2, (instance, compiled) -> {
				assertThat(instance).isEqualTo(0.2);
				assertThat(compiled.getSourceFile()).contains("(double) 0.2");
			});
		}

		@Test
		void generateWhenChar() {
			compile('a', (instance, compiled) -> {
				assertThat(instance).isEqualTo('a');
				assertThat(compiled.getSourceFile()).contains("'a'");
			});
		}

		@Test
		void generateWhenSimpleEscapedCharReturnsEscaped() {
			testEscaped('\b', "'\\b'");
			testEscaped('\t', "'\\t'");
			testEscaped('\n', "'\\n'");
			testEscaped('\f', "'\\f'");
			testEscaped('\r', "'\\r'");
			testEscaped('\"', "'\"'");
			testEscaped('\'', "'\\''");
			testEscaped('\\', "'\\\\'");
		}

		@Test
		void generatedWhenUnicodeEscapedCharReturnsEscaped() {
			testEscaped('\u007f', "'\\u007f'");
		}

		private void testEscaped(char value, String expectedSourceContent) {
			compile(value, (instance, compiled) -> {
				assertThat(instance).isEqualTo(value);
				assertThat(compiled.getSourceFile()).contains(expectedSourceContent);
			});
		}

	}

	@Nested
	class StringTests {

		@Test
		void generateWhenString() {
			compile("test\n", (instance, compiled) -> {
				assertThat(instance).isEqualTo("test\n");
				assertThat(compiled.getSourceFile()).contains("\n");
			});
		}

	}

	@Nested
	class EnumTests {

		@Test
		void generateWhenEnum() {
			compile(ChronoUnit.DAYS, (instance, compiled) -> {
				assertThat(instance).isEqualTo(ChronoUnit.DAYS);
				assertThat(compiled.getSourceFile()).contains("ChronoUnit.DAYS");
			});
		}

		@Test
		void generateWhenEnumWithClassBody() {
			compile(EnumWithClassBody.TWO, (instance, compiled) -> {
				assertThat(instance).isEqualTo(EnumWithClassBody.TWO);
				assertThat(compiled.getSourceFile()).contains("EnumWithClassBody.TWO");
			});
		}

	}

	@Nested
	class ClassTests {

		@Test
		void generateWhenClass() {
			compile(InputStream.class, (instance, compiled) -> assertThat(instance)
					.isEqualTo(InputStream.class));
		}

		@Test
		void generateWhenCglibClass() {
			compile(ExampleClass$$GeneratedBy.class, (instance,
					compiled) -> assertThat(instance).isEqualTo(ExampleClass.class));
		}

	}

	@Nested
	class ResolvableTypeTests {

		@Test
		void generateWhenSimpleResolvableType() {
			ResolvableType resolvableType = ResolvableType.forClass(String.class);
			compile(resolvableType, (instance, compiled) -> assertThat(instance)
					.isEqualTo(resolvableType));
		}

		@Test
		void generateWhenNoneResolvableType() {
			ResolvableType resolvableType = ResolvableType.NONE;
			compile(resolvableType, (instance, compiled) -> {
				assertThat(instance).isEqualTo(resolvableType);
				assertThat(compiled.getSourceFile()).contains("ResolvableType.NONE");
			});
		}

		@Test
		void generateWhenGenericResolvableType() {
			ResolvableType resolvableType = ResolvableType
					.forClassWithGenerics(List.class, String.class);
			compile(resolvableType, (instance, compiled) -> assertThat(instance)
					.isEqualTo(resolvableType));
		}

		@Test
		void generateWhenNestedGenericResolvableType() {
			ResolvableType stringList = ResolvableType.forClassWithGenerics(List.class,
					String.class);
			ResolvableType resolvableType = ResolvableType.forClassWithGenerics(Map.class,
					ResolvableType.forClass(Integer.class), stringList);
			compile(resolvableType, (instance, compiled) -> assertThat(instance)
					.isEqualTo(resolvableType));
		}

	}

	@Nested
	class ArrayTests {

		@Test
		void generateWhenPrimitiveArray() {
			byte[] bytes = { 0, 1, 2 };
			compile(bytes, (instance, compiler) -> {
				assertThat(instance).isEqualTo(bytes);
				assertThat(compiler.getSourceFile()).contains("new byte[]");
			});
		}

		@Test
		void generateWhenWrapperArray() {
			Byte[] bytes = { 0, 1, 2 };
			compile(bytes, (instance, compiler) -> {
				assertThat(instance).isEqualTo(bytes);
				assertThat(compiler.getSourceFile()).contains("new Byte[]");
			});
		}

		@Test
		void generateWhenClassArray() {
			Class<?>[] classes = new Class<?>[] { InputStream.class, OutputStream.class };
			compile(classes, (instance, compiler) -> {
				assertThat(instance).isEqualTo(classes);
				assertThat(compiler.getSourceFile()).contains("new Class[]");
			});
		}

	}

	@Nested
	class ManagedListTests {

		@Test
		void generateWhenStringManagedList() {
			ManagedList<String> list = new ManagedList<>();
			list.add("a");
			list.add("b");
			list.add("c");
			compile(list, (instance, compiler) -> assertThat(instance).isEqualTo(list)
					.isInstanceOf(ManagedList.class));
		}

		@Test
		void generateWhenEmptyManagedList() {
			ManagedList<String> list = new ManagedList<>();
			compile(list, (instance, compiler) -> assertThat(instance).isEqualTo(list)
					.isInstanceOf(ManagedList.class));
		}

	}

	@Nested
	class ManagedSetTests {

		@Test
		void generateWhenStringManagedSet() {
			ManagedSet<String> set = new ManagedSet<>();
			set.add("a");
			set.add("b");
			set.add("c");
			compile(set, (instance, compiler) -> assertThat(instance).isEqualTo(set)
					.isInstanceOf(ManagedSet.class));
		}

		@Test
		void generateWhenEmptyManagedSet() {
			ManagedSet<String> set = new ManagedSet<>();
			compile(set, (instance, compiler) -> assertThat(instance).isEqualTo(set)
					.isInstanceOf(ManagedSet.class));
		}

	}

	@Nested
	class ManagedMapTests {

		@Test
		void generateWhenManagedMap() {
			ManagedMap<String, String> map = new ManagedMap<>();
			map.put("k1", "v1");
			map.put("k2", "v2");
			compile(map, (instance, compiler) -> assertThat(instance).isEqualTo(map)
					.isInstanceOf(ManagedMap.class));
		}

		@Test
		void generateWhenEmptyManagedMap() {
			ManagedMap<String, String> map = new ManagedMap<>();
			compile(map, (instance, compiler) -> assertThat(instance).isEqualTo(map)
					.isInstanceOf(ManagedMap.class));
		}

	}

	@Nested
	class ListTests {

		@Test
		void generateWhenStringList() {
			List<String> list = List.of("a", "b", "c");
			compile(list, (instance, compiler) -> assertThat(instance).isEqualTo(list)
					.isNotInstanceOf(ManagedList.class));
		}

		@Test
		void generateWhenEmptyList() {
			List<String> list = List.of();
			compile(list, (instance, compiler) -> {
				assertThat(instance).isEqualTo(list);
				assertThat(compiler.getSourceFile()).contains("Collections.emptyList();");
			});
		}

	}

	@Nested
	class SetTests {

		@Test
		void generateWhenStringSet() {
			Set<String> set = Set.of("a", "b", "c");
			compile(set, (instance, compiler) -> assertThat(instance).isEqualTo(set)
					.isNotInstanceOf(ManagedSet.class));
		}

		@Test
		void generateWhenEmptySet() {
			Set<String> set = Set.of();
			compile(set, (instance, compiler) -> {
				assertThat(instance).isEqualTo(set);
				assertThat(compiler.getSourceFile()).contains("Collections.emptySet();");
			});
		}

		@Test
		void generateWhenLinkedHashSet() {
			Set<String> set = new LinkedHashSet<>(List.of("a", "b", "c"));
			compile(set, (instance, compiler) -> {
				assertThat(instance).isEqualTo(set).isInstanceOf(LinkedHashSet.class);
				assertThat(compiler.getSourceFile())
						.contains("new LinkedHashSet(List.of(");
			});
		}

	}

	@Nested
	class MapTests {

		@Test
		void generateWhenSmallMap() {
			Map<String, String> map = Map.of("k1", "v1", "k2", "v2");
			compile(map, (instance, compiler) -> {
				assertThat(instance).isEqualTo(map);
				assertThat(compiler.getSourceFile()).contains("Map.of(");
			});
		}

		@Test
		void generateWhenMapWithOverTenElements() {
			Map<String, String> map = new HashMap<>();
			for (int i = 1; i <= 11; i++) {
				map.put("k" + i, "v" + i);
			}
			compile(map, (instance, compiler) -> {
				assertThat(instance).isEqualTo(map);
				assertThat(compiler.getSourceFile()).contains("Map.ofEntries(");
			});
		}

		@Test
		void generateWhenLinkedHashMap() {
			Map<String, String> map = new LinkedHashMap<>();
			map.put("a", "A");
			map.put("b", "B");
			map.put("c", "C");
			compile(map, (instance, compiler) -> {
				assertThat(instance).isEqualTo(map).isInstanceOf(LinkedHashMap.class);
				assertThat(compiler.getSourceFile()).contains("getMap()");
			});
		}

	}

	@Nested
	class BeanReferenceTests {

		@Test
		void generatedWhenBeanNameReference() {
			RuntimeBeanNameReference beanReference = new RuntimeBeanNameReference("test");
			compile(beanReference, (instance, compiler) -> {
				RuntimeBeanReference actual = (RuntimeBeanReference) instance;
				assertThat(actual.getBeanName()).isEqualTo(beanReference.getBeanName());
			});
		}

		@Test
		void generatedWhenBeanReferenceByName() {
			RuntimeBeanReference beanReference = new RuntimeBeanReference("test");
			compile(beanReference, (instance, compiler) -> {
				RuntimeBeanReference actual = (RuntimeBeanReference) instance;
				assertThat(actual.getBeanName()).isEqualTo(beanReference.getBeanName());
				assertThat(actual.getBeanType()).isEqualTo(beanReference.getBeanType());
			});
		}

		@Test
		void generatedWhenBeanReferenceByType() {
			BeanReference beanReference = new RuntimeBeanReference(String.class);
			compile(beanReference, (instance, compiler) -> {
				RuntimeBeanReference actual = (RuntimeBeanReference) instance;
				assertThat(actual.getBeanType()).isEqualTo(String.class);
			});
		}

	}

	@Nested
	static class ExceptionTests {

		@Test
		void generateWhenUnsupportedDataTypeThrowsException() {
			SampleValue sampleValue = new SampleValue("one");
			assertThatIllegalArgumentException().isThrownBy(() -> generateCode(sampleValue))
					.withMessageContaining("Failed to generate code for")
					.withMessageContaining(sampleValue.toString())
					.withMessageContaining(SampleValue.class.getName())
					.havingCause()
					.withMessageContaining("Code generation does not support")
					.withMessageContaining(SampleValue.class.getName());
		}

		@Test
		void generateWhenListOfUnsupportedElement() {
			SampleValue one = new SampleValue("one");
			SampleValue two = new SampleValue("two");
			List<SampleValue> list = List.of(one, two);
			assertThatIllegalArgumentException().isThrownBy(() -> generateCode(list))
					.withMessageContaining("Failed to generate code for")
					.withMessageContaining(list.toString())
					.withMessageContaining(list.getClass().getName())
					.havingCause()
					.withMessageContaining("Failed to generate code for")
					.withMessageContaining(one.toString())
					.withMessageContaining("?")
					.havingCause()
					.withMessageContaining("Code generation does not support ?");
		}

		private void generateCode(Object value) {
			TestGenerationContext context = new TestGenerationContext();
			GeneratedClass generatedClass = context.getGeneratedClasses()
					.addForFeature("Test", type -> {});
			new BeanDefinitionPropertyValueCodeGenerator(generatedClass.getMethods())
					.generateCode(value);
		}

		record SampleValue(String name) {}

	}

}

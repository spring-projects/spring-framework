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

package org.springframework.aot.generate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.api.StringAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.aot.generate.ValueCodeGenerator.Delegate;
import org.springframework.core.ResolvableType;
import org.springframework.core.testfixture.aot.generate.value.EnumWithClassBody;
import org.springframework.core.testfixture.aot.generate.value.ExampleClass;
import org.springframework.core.testfixture.aot.generate.value.ExampleClass$$GeneratedBy;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.FieldSpec;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeSpec;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link ValueCodeGenerator}.
 *
 * @author Stephane Nicoll
 */
class ValueCodeGeneratorTests {


	@Nested
	class ConfigurationTests {

		@Test
		void createWithListOfDelegatesInvokeThemInOrder() {
			Delegate first = mock(Delegate.class);
			Delegate second = mock(Delegate.class);
			Delegate third = mock(Delegate.class);
			ValueCodeGenerator codeGenerator = ValueCodeGenerator
					.with(List.of(first, second, third));
			Object value = "";
			given(third.generateCode(codeGenerator, value))
					.willReturn(CodeBlock.of("test"));
			CodeBlock code = codeGenerator.generateCode(value);
			assertThat(code).hasToString("test");
			InOrder ordered = inOrder(first, second, third);
			ordered.verify(first).generateCode(codeGenerator, value);
			ordered.verify(second).generateCode(codeGenerator, value);
			ordered.verify(third).generateCode(codeGenerator, value);
		}

		@Test
		void generateCodeWithMatchingDelegateStops() {
			Delegate first = mock(Delegate.class);
			Delegate second = mock(Delegate.class);
			ValueCodeGenerator codeGenerator = ValueCodeGenerator
					.with(List.of(first, second));
			Object value = "";
			given(first.generateCode(codeGenerator, value))
					.willReturn(CodeBlock.of("test"));
			CodeBlock code = codeGenerator.generateCode(value);
			assertThat(code).hasToString("test");
			verify(first).generateCode(codeGenerator, value);
			verifyNoInteractions(second);
		}

		@Test
		void scopedReturnsImmutableCopy() {
			ValueCodeGenerator valueCodeGenerator = ValueCodeGenerator.withDefaults();
			GeneratedMethods generatedMethods = new GeneratedMethods(
					ClassName.get("com.example", "Test"), MethodName::toString);
			ValueCodeGenerator scopedValueCodeGenerator = valueCodeGenerator.scoped(generatedMethods);
			assertThat(scopedValueCodeGenerator).isNotSameAs(valueCodeGenerator);
			assertThat(scopedValueCodeGenerator.getGeneratedMethods()).isSameAs(generatedMethods);
			assertThat(valueCodeGenerator.getGeneratedMethods()).isNull();
		}

	}

	@Nested
	class NullTests {

		@Test
		void generateWhenNull() {
			assertThat(generateCode(null)).hasToString("null");
		}

	}

	@Nested
	class PrimitiveTests {

		@Test
		void generateWhenBoolean() {
			assertThat(generateCode(true)).hasToString("true");
		}

		@Test
		void generateWhenByte() {
			assertThat(generateCode((byte) 2)).hasToString("(byte) 2");
		}

		@Test
		void generateWhenShort() {
			assertThat(generateCode((short) 3)).hasToString("(short) 3");
		}

		@Test
		void generateWhenInt() {
			assertThat(generateCode(4)).hasToString("4");
		}

		@Test
		void generateWhenLong() {
			assertThat(generateCode(5L)).hasToString("5L");
		}

		@Test
		void generateWhenFloat() {
			assertThat(generateCode(0.1F)).hasToString("0.1F");
		}

		@Test
		void generateWhenDouble() {
			assertThat(generateCode(0.2)).hasToString("(double) 0.2");
		}

		@Test
		void generateWhenChar() {
			assertThat(generateCode('a')).hasToString("'a'");
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
			assertThat(generateCode(value)).hasToString(expectedSourceContent);
		}

	}

	@Nested
	class StringTests {

		@Test
		void generateWhenString() {
			assertThat(generateCode("test")).hasToString("\"test\"");
		}


		@Test
		void generateWhenStringWithCarriageReturn() {
			assertThat(generateCode("test\n")).isEqualTo(CodeBlock.of("$S", "test\n"));
		}

	}

	@Nested
	class CharsetTests {

		@Test
		void generateWhenCharset() {
			assertThat(resolve(generateCode(StandardCharsets.UTF_8))).hasImport(Charset.class)
					.hasValueCode("Charset.forName(\"UTF-8\")");
		}

	}

	@Nested
	class EnumTests {

		@Test
		void generateWhenEnum() {
			assertThat(resolve(generateCode(ChronoUnit.DAYS)))
					.hasImport(ChronoUnit.class).hasValueCode("ChronoUnit.DAYS");
		}

		@Test
		void generateWhenEnumWithClassBody() {
			assertThat(resolve(generateCode(EnumWithClassBody.TWO)))
					.hasImport(EnumWithClassBody.class).hasValueCode("EnumWithClassBody.TWO");
		}

	}

	@Nested
	class ClassTests {

		@Test
		void generateWhenClass() {
			assertThat(resolve(generateCode(InputStream.class)))
					.hasImport(InputStream.class).hasValueCode("InputStream.class");
		}

		@Test
		void generateWhenCglibClass() {
			assertThat(resolve(generateCode(ExampleClass$$GeneratedBy.class)))
					.hasImport(ExampleClass.class).hasValueCode("ExampleClass.class");
		}

	}

	@Nested
	class ResolvableTypeTests {

		@Test
		void generateWhenSimpleResolvableType() {
			ResolvableType resolvableType = ResolvableType.forClass(String.class);
			assertThat(resolve(generateCode(resolvableType)))
					.hasImport(ResolvableType.class)
					.hasValueCode("ResolvableType.forClass(String.class)");
		}

		@Test
		void generateWhenNoneResolvableType() {
			ResolvableType resolvableType = ResolvableType.NONE;
			assertThat(resolve(generateCode(resolvableType)))
					.hasImport(ResolvableType.class).hasValueCode("ResolvableType.NONE");
		}

		@Test
		void generateWhenGenericResolvableType() {
			ResolvableType resolvableType = ResolvableType
					.forClassWithGenerics(List.class, String.class);
			assertThat(resolve(generateCode(resolvableType)))
					.hasImport(ResolvableType.class, List.class)
					.hasValueCode("ResolvableType.forClassWithGenerics(List.class, String.class)");
		}

		@Test
		void generateWhenNestedGenericResolvableType() {
			ResolvableType stringList = ResolvableType.forClassWithGenerics(List.class,
					String.class);
			ResolvableType resolvableType = ResolvableType.forClassWithGenerics(Map.class,
					ResolvableType.forClass(Integer.class), stringList);
			assertThat(resolve(generateCode(resolvableType)))
					.hasImport(ResolvableType.class, List.class, Map.class).hasValueCode(
							"ResolvableType.forClassWithGenerics(Map.class, ResolvableType.forClass(Integer.class), "
									+ "ResolvableType.forClassWithGenerics(List.class, String.class))");
		}

		@Test
		void generateWhenUnresolvedGenericType() throws NoSuchFieldException {
			ResolvableType resolvableType = ResolvableType
					.forField(SampleTypes.class.getField("genericList"));
			assertThat(resolve(generateCode(resolvableType)))
					.hasImport(ResolvableType.class, List.class)
					.hasValueCode("ResolvableType.forClass(List.class)");
		}

		@Test
		void generateWhenUnresolvedNestedGenericType() throws NoSuchFieldException {
			ResolvableType resolvableType = ResolvableType
					.forField(SampleTypes.class.getField("mapWithNestedGenericInValueType"));
			assertThat(resolve(generateCode(resolvableType)))
					.hasImport(ResolvableType.class, List.class)
					.hasValueCode("""
							ResolvableType.forClassWithGenerics(Map.class, ResolvableType.forClass(String.class), \
							ResolvableType.forClass(List.class))""");
		}

		static class SampleTypes<A> {

			public List<A> genericList;

			public Map<String, List<A>> mapWithNestedGenericInValueType;

		}

	}

	@Nested
	class ArrayTests {

		@Test
		void generateWhenPrimitiveArray() {
			int[] array = { 0, 1, 2 };
			assertThat(generateCode(array)).hasToString("new int[] {0, 1, 2}");
		}

		@Test
		void generateWhenWrapperArray() {
			Integer[] array = { 0, 1, 2 };
			assertThat(resolve(generateCode(array))).hasValueCode("new Integer[] {0, 1, 2}");
		}

		@Test
		void generateWhenClassArray() {
			Class<?>[] array = new Class<?>[] { InputStream.class, OutputStream.class };
			assertThat(resolve(generateCode(array))).hasImport(InputStream.class, OutputStream.class)
					.hasValueCode("new Class[] {InputStream.class, OutputStream.class}");
		}

	}

	@Nested
	class ListTests {

		@Test
		void generateWhenStringList() {
			List<String> list = List.of("a", "b", "c");
			assertThat(resolve(generateCode(list))).hasImport(List.class)
					.hasValueCode("List.of(\"a\", \"b\", \"c\")");
		}

		@Test
		void generateWhenEmptyList() {
			List<String> list = List.of();
			assertThat(resolve(generateCode(list))).hasImport(Collections.class)
					.hasValueCode("Collections.emptyList()");
		}

	}

	@Nested
	class SetTests {

		@Test
		void generateWhenStringSet() {
			Set<String> set = Set.of("a", "b", "c");
			assertThat(resolve(generateCode(set))).hasImport(Set.class)
					.hasValueCode("Set.of(\"a\", \"b\", \"c\")");
		}

		@Test
		void generateWhenEmptySet() {
			Set<String> set = Set.of();
			assertThat(resolve(generateCode(set))).hasImport(Collections.class)
					.hasValueCode("Collections.emptySet()");
		}

		@Test
		void generateWhenLinkedHashSet() {
			Set<String> set = new LinkedHashSet<>(List.of("a", "b", "c"));
			assertThat(resolve(generateCode(set))).hasImport(List.class, LinkedHashSet.class)
					.hasValueCode("new LinkedHashSet(List.of(\"a\", \"b\", \"c\"))");
		}

		@Test
		void generateWhenSetOfClass() {
			Set<Class<?>> set = Set.of(InputStream.class, OutputStream.class);
			assertThat(resolve(generateCode(set))).hasImport(Set.class, InputStream.class, OutputStream.class)
					.valueCode().contains("Set.of(", "InputStream.class", "OutputStream.class");
		}

	}

	@Nested
	class MapTests {

		@Test
		void generateWhenSmallMap() {
			Map<String, String> map = Map.of("k1", "v1", "k2", "v2");
			assertThat(resolve(generateCode(map))).hasImport(Map.class)
					.hasValueCode("Map.of(\"k1\", \"v1\", \"k2\", \"v2\")");
		}

		@Test
		void generateWhenMapWithOverTenElements() {
			Map<String, String> map = new HashMap<>();
			for (int i = 1; i <= 11; i++) {
				map.put("k" + i, "v" + i);
			}
			assertThat(resolve(generateCode(map))).hasImport(Map.class)
					.valueCode().startsWith("Map.ofEntries(");
		}

	}

	@Nested
	class ExceptionTests {

		@Test
		void generateWhenUnsupportedValue() {
			StringWriter sw = new StringWriter();
			assertThatExceptionOfType(ValueCodeGenerationException.class)
					.isThrownBy(() -> generateCode(sw))
					.withCauseInstanceOf(UnsupportedTypeValueCodeGenerationException.class)
					.satisfies(ex -> assertThat(ex.getValue()).isEqualTo(sw));
		}

		@Test
		void generateWhenUnsupportedDataTypeThrowsException() {
			StringWriter sampleValue = new StringWriter();
			assertThatExceptionOfType(ValueCodeGenerationException.class).isThrownBy(() -> generateCode(sampleValue))
					.withMessageContaining("Failed to generate code for")
					.withMessageContaining(sampleValue.toString())
					.withMessageContaining(StringWriter.class.getName())
					.havingCause()
					.withMessageContaining("Code generation does not support")
					.withMessageContaining(StringWriter.class.getName());
		}

		@Test
		void generateWhenListOfUnsupportedElement() {
			StringWriter one = new StringWriter();
			StringWriter two = new StringWriter();
			List<StringWriter> list = List.of(one, two);
			assertThatExceptionOfType(ValueCodeGenerationException.class).isThrownBy(() -> generateCode(list))
					.withMessageContaining("Failed to generate code for")
					.withMessageContaining(list.toString())
					.withMessageContaining(list.getClass().getName())
					.havingCause()
					.withMessageContaining("Failed to generate code for")
					.withMessageContaining(one.toString())
					.withMessageContaining(StringWriter.class.getName())
					.havingCause()
					.withMessageContaining("Code generation does not support " + StringWriter.class.getName());
		}

	}

	private static CodeBlock generateCode(@Nullable Object value) {
		return ValueCodeGenerator.withDefaults().generateCode(value);
	}

	private static ValueCode resolve(CodeBlock valueCode) {
		String code = writeCode(valueCode);
		List<String> imports = code.lines()
				.filter(candidate -> candidate.startsWith("import") && candidate.endsWith(";"))
				.map(line -> line.substring("import".length(), line.length() - 1))
				.map(String::trim).toList();
		int start = code.indexOf("value = ");
		int end = code.indexOf(";", start);
		return new ValueCode(code.substring(start + "value = ".length(), end), imports);
	}

	private static String writeCode(CodeBlock valueCode) {
		FieldSpec field = FieldSpec.builder(Object.class, "value")
				.initializer(valueCode)
				.build();
		TypeSpec helloWorld = TypeSpec.classBuilder("Test").addField(field).build();
		JavaFile javaFile = JavaFile.builder("com.example", helloWorld).build();
		StringWriter out = new StringWriter();
		try {
			javaFile.writeTo(out);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		return out.toString();
	}

	static class ValueCodeAssert extends AbstractAssert<ValueCodeAssert, ValueCode> {

		public ValueCodeAssert(ValueCode actual) {
			super(actual, ValueCodeAssert.class);
		}

		ValueCodeAssert hasImport(Class<?>... imports) {
			for (Class<?> anImport : imports) {
				assertThat(this.actual.imports).contains(anImport.getName());
			}
			return this;
		}

		ValueCodeAssert hasValueCode(String code) {
			assertThat(this.actual.code).isEqualTo(code);
			return this;
		}

		StringAssert valueCode() {
			return new StringAssert(this.actual.code);
		}

	}

	record ValueCode(String code, List<String> imports) implements AssertProvider<ValueCodeAssert> {

		@Override
		public ValueCodeAssert assertThat() {
			return new ValueCodeAssert(this);
		}
	}

}

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

package org.springframework.expression.spel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Expression evaluation where the {@link TypeConverter} plugged in uses the
 * {@link org.springframework.core.convert.support.GenericConversionService}.
 *
 * @author Andy Clement
 * @author Dave Syer
 * @author Sam Brannen
 */
class ExpressionWithConversionTests extends AbstractExpressionTests {

	private static final List<String> listOfString = List.of("1", "2", "3");
	private static final List<Integer> listOfInteger = List.of(4, 5, 6);

	private static final TypeDescriptor typeDescriptorForListOfString =
			new TypeDescriptor(ReflectionUtils.findField(ExpressionWithConversionTests.class, "listOfString"));
	private static final TypeDescriptor typeDescriptorForListOfInteger =
			new TypeDescriptor(ReflectionUtils.findField(ExpressionWithConversionTests.class, "listOfInteger"));


	/**
	 * Test the service can convert what we are about to use in the expression evaluation tests.
	 */
	@BeforeAll
	@SuppressWarnings("unchecked")
	static void verifyConversionsAreSupportedByStandardTypeConverter() {
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		TypeConverter typeConverter = evaluationContext.getTypeConverter();

		// List<Integer> to List<String>
		assertThat(typeDescriptorForListOfString.getElementTypeDescriptor().getType())
				.isEqualTo(String.class);
		List<String> strings = (List<String>) typeConverter.convertValue(listOfInteger,
				typeDescriptorForListOfInteger, typeDescriptorForListOfString);
		assertThat(strings).containsExactly("4", "5", "6");

		// List<String> to List<Integer>
		assertThat(typeDescriptorForListOfInteger.getElementTypeDescriptor().getType())
				.isEqualTo(Integer.class);
		List<Integer> integers = (List<Integer>) typeConverter.convertValue(listOfString,
				typeDescriptorForListOfString, typeDescriptorForListOfInteger);
		assertThat(integers).containsExactly(1, 2, 3);
	}


	@Test
	void setParameterizedList() {
		StandardEvaluationContext context = TestScenarioCreator.getTestEvaluationContext();

		Expression e = parser.parseExpression("listOfInteger.size()");
		assertThat(e.getValue(context, Integer.class)).isZero();

		// Assign a List<String> to the List<Integer> field - the component elements should be converted
		parser.parseExpression("listOfInteger").setValue(context, listOfString);
		// size now 3
		assertThat(e.getValue(context, Integer.class)).isEqualTo(3);
		// element type correctly Integer
		Class<?> clazz = parser.parseExpression("listOfInteger[1].getClass()").getValue(context, Class.class);
		assertThat(clazz).isEqualTo(Integer.class);
	}

	@Test
	void coercionToCollectionOfPrimitive() throws Exception {

		class TestTarget {
			@SuppressWarnings("unused")
			public int sum(Collection<Integer> numbers) {
				return numbers.stream().reduce(0, (a, b) -> a + b);
			}
		}

		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		TypeConverter typeConverter = evaluationContext.getTypeConverter();

		TypeDescriptor collectionType = new TypeDescriptor(new MethodParameter(TestTarget.class.getDeclaredMethod(
				"sum", Collection.class), 0));
		// The type conversion is possible
		assertThat(typeConverter.canConvert(TypeDescriptor.valueOf(String.class), collectionType)).isTrue();
		// ... and it can be done successfully
		assertThat(typeConverter.convertValue("1,2,3,4", TypeDescriptor.valueOf(String.class), collectionType))
				.hasToString("[1, 2, 3, 4]");

		evaluationContext.setVariable("target", new TestTarget());

		// OK up to here, so the evaluation should be fine...
		int sum = parser.parseExpression("#target.sum(#root)").getValue(evaluationContext, "1,2,3,4", int.class);
		assertThat(sum).isEqualTo(10);
	}

	@Test
	void convert() {
		Foo root = new Foo("bar");
		Collection<String> foos = Set.of("baz");

		StandardEvaluationContext context = new StandardEvaluationContext(root);

		// property access
		Expression expression = parser.parseExpression("foos");
		expression.setValue(context, foos);
		Foo baz = root.getFoos().iterator().next();
		assertThat(baz.value).isEqualTo("baz");

		// method call
		expression = parser.parseExpression("setFoos(#foos)");
		context.setVariable("foos", foos);
		expression.getValue(context);
		baz = root.getFoos().iterator().next();
		assertThat(baz.value).isEqualTo("baz");

		// method call with result from method call
		expression = parser.parseExpression("setFoos(getFoosAsStrings())");
		expression.getValue(context);
		baz = root.getFoos().iterator().next();
		assertThat(baz.value).isEqualTo("baz");

		// method call with result from method call
		expression = parser.parseExpression("setFoos(getFoosAsObjects())");
		expression.getValue(context);
		baz = root.getFoos().iterator().next();
		assertThat(baz.value).isEqualTo("quux");
	}

	@Test  // gh-34544
	void convertOptionalToContainedTargetForMethodInvocations() {
		StandardEvaluationContext context = new StandardEvaluationContext(new JediService());

		// Verify findByName('Yoda') returns an Optional.
		Expression expression = parser.parseExpression("findByName('Yoda') instanceof T(java.util.Optional)");
		assertThat(expression.getValue(context, Boolean.class)).isTrue();

		// Verify we can pass a Jedi directly to greet().
		expression = parser.parseExpression("greet(findByName('Yoda').get())");
		assertThat(expression.getValue(context, String.class)).isEqualTo("Hello, Yoda");

		// Verify that an Optional<Jedi> will be unwrapped to a Jedi to pass to greet().
		expression = parser.parseExpression("greet(findByName('Yoda'))");
		assertThat(expression.getValue(context, String.class)).isEqualTo("Hello, Yoda");

		// Verify that an empty Optional will be converted to null to pass to greet().
		expression = parser.parseExpression("greet(findByName(''))");
		assertThat(expression.getValue(context, String.class)).isEqualTo("Hello, null");
	}


	public static class Foo {

		public final String value;

		private Collection<Foo> foos;

		public Foo(String value) {
			this.value = value;
		}

		public void setFoos(Collection<Foo> foos) {
			this.foos = foos;
		}

		public Collection<Foo> getFoos() {
			return this.foos;
		}

		public Collection<String> getFoosAsStrings() {
			return Set.of("baz");
		}

		public Collection<?> getFoosAsObjects() {
			return Set.of("quux");
		}
	}

	record Jedi(String name) {
	}

	static class JediService {

		public Optional<Jedi> findByName(String name) {
			if (name.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(new Jedi(name));
		}

		public String greet(@Nullable Jedi jedi) {
			return "Hello, " + (jedi != null ? jedi.name() : null);
		}
	}

}

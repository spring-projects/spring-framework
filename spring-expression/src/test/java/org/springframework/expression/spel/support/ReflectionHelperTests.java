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

package org.springframework.expression.spel.support;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.ParseException;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.AbstractExpressionTests;
import org.springframework.expression.spel.SpelUtilities;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.support.ReflectionHelper.ArgumentsMatchKind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.InstanceOfAssertFactories.array;
import static org.springframework.expression.spel.support.ReflectionHelper.ArgumentsMatchKind.CLOSE;
import static org.springframework.expression.spel.support.ReflectionHelper.ArgumentsMatchKind.EXACT;
import static org.springframework.expression.spel.support.ReflectionHelper.ArgumentsMatchKind.REQUIRES_CONVERSION;

/**
 * Tests for {@link ReflectionHelper}, {@link SpelUtilities}, {@link TypedValue},
 * {@link ReflectivePropertyAccessor}, ...
 *
 * @author Andy Clement
 * @author Sam Brannen
 */
class ReflectionHelperTests extends AbstractExpressionTests {

	private final StandardTypeConverter tc = new StandardTypeConverter();


	@Test
	void utilities() throws ParseException {
		SpelExpression expr = (SpelExpression)parser.parseExpression("3+4+5+6+7-2");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		SpelUtilities.printAbstractSyntaxTree(ps, expr);
		ps.flush();
		String s = baos.toString();
//		===> Expression '3+4+5+6+7-2' - AST start
//		OperatorMinus  value:(((((3 + 4) + 5) + 6) + 7) - 2)  #children:2
//		  OperatorPlus  value:((((3 + 4) + 5) + 6) + 7)  #children:2
//		    OperatorPlus  value:(((3 + 4) + 5) + 6)  #children:2
//		      OperatorPlus  value:((3 + 4) + 5)  #children:2
//		        OperatorPlus  value:(3 + 4)  #children:2
//		          CompoundExpression  value:3
//		            IntLiteral  value:3
//		          CompoundExpression  value:4
//		            IntLiteral  value:4
//		        CompoundExpression  value:5
//		          IntLiteral  value:5
//		      CompoundExpression  value:6
//		        IntLiteral  value:6
//		    CompoundExpression  value:7
//		      IntLiteral  value:7
//		  CompoundExpression  value:2
//		    IntLiteral  value:2
//		===> Expression '3+4+5+6+7-2' - AST end
		assertThat(s).contains("===> Expression '3+4+5+6+7-2' - AST start");
		assertThat(s).contains(" OpPlus  value:((((3 + 4) + 5) + 6) + 7)  #children:2");
	}

	@Test
	void typedValue() {
		TypedValue tv1 = new TypedValue("hello");
		TypedValue tv2 = new TypedValue("hello");
		TypedValue tv3 = new TypedValue("bye");
		assertThat(tv1.getTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(tv1.toString()).isEqualTo("TypedValue: 'hello' of [java.lang.String]");
		assertThat(tv2).isEqualTo(tv1);
		assertThat(tv1).isEqualTo(tv2);
		assertThat(tv3).isNotEqualTo(tv1);
		assertThat(tv3).isNotEqualTo(tv2);
		assertThat(tv1).isNotEqualTo(tv3);
		assertThat(tv2).isNotEqualTo(tv3);
		assertThat(tv2).hasSameHashCodeAs(tv1);
		assertThat(tv3).doesNotHaveSameHashCodeAs(tv1);
		assertThat(tv3).doesNotHaveSameHashCodeAs(tv2);
	}

	@Test
	void reflectionHelperCompareArguments_ExactMatching() {
		// Calling foo(String) with (String) is exact match
		checkMatch(new Class<?>[] {String.class}, new Class<?>[] {String.class}, tc, EXACT);

		// Calling foo(String,Integer) with (String,Integer) is exact match
		checkMatch(new Class<?>[] {String.class, Integer.class}, new Class<?>[] {String.class, Integer.class}, tc, EXACT);
	}

	@Test
	void reflectionHelperCompareArguments_CloseMatching() {
		// Calling foo(List) with (ArrayList) is close match (no conversion required)
		checkMatch(new Class<?>[] {ArrayList.class}, new Class<?>[] {List.class}, tc, CLOSE);

		// Passing (Sub,String) on call to foo(Super,String) is close match
		checkMatch(new Class<?>[] {Sub.class, String.class}, new Class<?>[] {Super.class, String.class}, tc, CLOSE);

		// Passing (String,Sub) on call to foo(String,Super) is close match
		checkMatch(new Class<?>[] {String.class, Sub.class}, new Class<?>[] {String.class, Super.class}, tc, CLOSE);
	}

	@Test
	void reflectionHelperCompareArguments_CloseMatching_WithAutoBoxing() {
		// Calling foo(String,int) with (String,Integer) requires boxing conversion of argument one
		checkMatch(new Class<?>[] {String.class, int.class}, new Class<?>[] {String.class, Integer.class},tc, CLOSE);

		// Passing (int,String) on call to foo(Integer,String) requires boxing conversion of argument zero
		checkMatch(new Class<?>[] {int.class, String.class}, new Class<?>[] {Integer.class, String.class},tc, CLOSE);

		// Passing (int,Sub) on call to foo(Integer,Super) requires boxing conversion of argument zero
		checkMatch(new Class<?>[] {int.class, Sub.class}, new Class<?>[] {Integer.class, Super.class}, tc, CLOSE);

		// Passing (int,Sub,boolean) on call to foo(Integer,Super,Boolean) requires boxing conversion of arguments zero and two
		checkMatch(new Class<?>[] {int.class, Sub.class, boolean.class}, new Class<?>[] {Integer.class, Super.class, Boolean.class}, tc, CLOSE);
	}

	@Test
	void reflectionHelperCompareArguments_NotAMatch() {
		StandardTypeConverter typeConverter = new StandardTypeConverter();

		// Passing (Super,String) on call to foo(Sub,String) is not a match
		checkMatch(new Class<?>[] {Super.class,String.class}, new Class<?>[] {Sub.class,String.class}, typeConverter, null);
	}

	@Test
	void reflectionHelperCompareArguments_Varargs() {
		// Passing (String[]) on call to (String[]) is exact match
		checkMatchVarargs(new Class<?>[] {String[].class}, new Class<?>[] {String[].class}, tc, EXACT);

		// Passing (Integer, String[]) on call to (Integer, String[]) is exact match
		checkMatchVarargs(new Class<?>[] {Integer.class, String[].class}, new Class<?>[] {Integer.class, String[].class}, tc, EXACT);

		// Passing (String, Integer, String[]) on call to (String, String, String[]) is exact match
		checkMatchVarargs(new Class<?>[] {String.class, Integer.class, String[].class}, new Class<?>[] {String.class,Integer.class, String[].class}, tc, EXACT);

		// Passing (Sub, String[]) on call to (Super, String[]) is exact match
		checkMatchVarargs(new Class<?>[] {Sub.class, String[].class}, new Class<?>[] {Super.class,String[].class}, tc, CLOSE);

		// Passing (Integer, String[]) on call to (String, String[]) is exact match
		checkMatchVarargs(new Class<?>[] {Integer.class, String[].class}, new Class<?>[] {String.class, String[].class}, tc, REQUIRES_CONVERSION);

		// Passing (Integer, Sub, String[]) on call to (String, Super, String[]) is exact match
		checkMatchVarargs(new Class<?>[] {Integer.class, Sub.class, String[].class}, new Class<?>[] {String.class, Super.class, String[].class}, tc, REQUIRES_CONVERSION);

		// Passing (String) on call to (String[]) is exact match
		checkMatchVarargs(new Class<?>[] {String.class}, new Class<?>[] {String[].class}, tc, EXACT);

		// Passing (Integer,String) on call to (Integer,String[]) is exact match
		checkMatchVarargs(new Class<?>[] {Integer.class, String.class}, new Class<?>[] {Integer.class, String[].class}, tc, EXACT);

		// Passing (String) on call to (Integer[]) is conversion match (String to Integer)
		checkMatchVarargs(new Class<?>[] {String.class}, new Class<?>[] {Integer[].class}, tc, REQUIRES_CONVERSION);

		// Passing (Sub) on call to (Super[]) is close match
		checkMatchVarargs(new Class<?>[] {Sub.class}, new Class<?>[] {Super[].class}, tc, CLOSE);

		// Passing (Super) on call to (Sub[]) is not a match
		checkMatchVarargs(new Class<?>[] {Super.class}, new Class<?>[] {Sub[].class}, tc, null);

		checkMatchVarargs(new Class<?>[] {Unconvertable.class, String.class}, new Class<?>[] {Sub.class, Super[].class}, tc, null);

		checkMatchVarargs(new Class<?>[] {Integer.class, Integer.class, String.class}, new Class<?>[] {String.class, String.class, Super[].class}, tc, null);

		checkMatchVarargs(new Class<?>[] {Unconvertable.class, String.class}, new Class<?>[] {Sub.class, Super[].class}, tc, null);

		checkMatchVarargs(new Class<?>[] {Integer.class, Integer.class, String.class}, new Class<?>[] {String.class, String.class, Super[].class}, tc, null);

		checkMatchVarargs(new Class<?>[] {Integer.class, Integer.class, Sub.class}, new Class<?>[] {String.class, String.class, Super[].class}, tc, REQUIRES_CONVERSION);

		checkMatchVarargs(new Class<?>[] {Integer.class, Integer.class, Integer.class}, new Class<?>[] {Integer.class, String[].class}, tc, REQUIRES_CONVERSION);
		// what happens on (Integer,String) passed to (Integer[]) ?
	}

	@Test
	void convertArguments() throws Exception {
		Method oneArg = TestInterface.class.getMethod("oneArg", String.class);
		Method twoArg = TestInterface.class.getMethod("twoArg", String.class, String[].class);

		// basic conversion int>String
		Object[] args = new Object[] {3};
		ReflectionHelper.convertArguments(tc, args, oneArg, null);
		checkArguments(args, "3");

		// varargs but nothing to convert
		args = new Object[] {3};
		ReflectionHelper.convertArguments(tc, args, twoArg, 1);
		checkArguments(args, "3");

		// varargs with nothing needing conversion
		args = new Object[] {3, "abc", "abc"};
		ReflectionHelper.convertArguments(tc, args, twoArg, 1);
		checkArguments(args, "3", "abc", "abc");

		// varargs with conversion required
		args = new Object[] {3, false ,3.0d};
		ReflectionHelper.convertArguments(tc, args, twoArg, 1);
		checkArguments(args, "3", "false", "3.0");
	}

	@Test
	void convertAllArguments() throws Exception {
		Method oneArg = TestInterface.class.getMethod("oneArg", String.class);
		Method twoArg = TestInterface.class.getMethod("twoArg", String.class, String[].class);

		// Simple conversion: int to string
		Object[] args = new Object[] {3};
		ReflectionHelper.convertAllArguments(tc, args, oneArg);
		checkArguments(args, "3");

		// varargs conversion
		args = new Object[] {3, false, 3.0f};
		ReflectionHelper.convertAllArguments(tc, args, twoArg);
		checkArguments(args, "3", "false", "3.0");

		// varargs conversion but no varargs
		args = new Object[] {3};
		ReflectionHelper.convertAllArguments(tc, args, twoArg);
		checkArguments(args, "3");

		// null value
		args = new Object[] {3, null, 3.0f};
		ReflectionHelper.convertAllArguments(tc, args, twoArg);
		checkArguments(args, "3", null, "3.0");
	}

	@Test
	void setupArgumentsForVarargsInvocationPreconditions() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ReflectionHelper.setupArgumentsForVarargsInvocation(new Class[] {}, "a"))
				.withMessage("Required parameter types array must not be empty");

		assertThatIllegalArgumentException()
				.isThrownBy(() -> ReflectionHelper.setupArgumentsForVarargsInvocation(
						new Class<?>[] { Integer.class, Integer.class }, 123))
				.withMessage("The last required parameter type must be an array to support varargs invocation");
	}

	@Test
	void setupArgumentsForVarargsInvocation() {
		Object[] newArray;

		newArray = ReflectionHelper.setupArgumentsForVarargsInvocation(new Class<?>[] { String[].class }, "a", "b", "c");
		assertThat(newArray)
				.singleElement()
				.asInstanceOf(array(String[].class))
				.containsExactly("a", "b", "c");

		newArray = ReflectionHelper.setupArgumentsForVarargsInvocation(new Class<?>[] { Object[].class }, "a", "b", "c");
		assertThat(newArray)
				.singleElement()
				.asInstanceOf(array(Object[].class))
				.containsExactly("a", "b", "c");

		newArray = ReflectionHelper.setupArgumentsForVarargsInvocation(
				new Class<?>[] { Integer.class, Integer.class, String[].class }, 123, 456, "a", "b", "c");
		assertThat(newArray).satisfiesExactly(
				one -> assertThat(one).isEqualTo(123),
				two -> assertThat(two).isEqualTo(456),
				three -> assertThat(three).asInstanceOf(array(String[].class)).containsExactly("a", "b", "c"));

		newArray = ReflectionHelper.setupArgumentsForVarargsInvocation(new Class<?>[] { String[].class });
		assertThat(newArray)
				.singleElement()
				.asInstanceOf(array(String[].class))
				.isEmpty();

		newArray = ReflectionHelper.setupArgumentsForVarargsInvocation(
				new Class<?>[] { String[].class }, new Object[] { new String[] { "a", "b", "c" } });
		assertThat(newArray)
				.singleElement()
				.asInstanceOf(array(String[].class))
				.containsExactly("a", "b", "c");

		newArray = ReflectionHelper.setupArgumentsForVarargsInvocation(
				new Class<?>[] { Object[].class }, new Object[] { new String[] { "a", "b", "c" } });
		assertThat(newArray)
				.singleElement()
				.asInstanceOf(array(Object[].class))
				.containsExactly("a", "b", "c");

		newArray = ReflectionHelper.setupArgumentsForVarargsInvocation(new Class<?>[] { String[].class }, "a");
		assertThat(newArray)
				.singleElement()
				.asInstanceOf(array(String[].class))
				.containsExactly("a");

		newArray = ReflectionHelper.setupArgumentsForVarargsInvocation(new Class<?>[] { String[].class }, new Object[] { null });
		assertThat(newArray)
				.singleElement()
				.asInstanceOf(array(String[].class))
				.singleElement()
				.isNull();
	}

	@Test
	void reflectivePropertyAccessor() throws Exception {
		ReflectivePropertyAccessor rpa = new ReflectivePropertyAccessor();
		Tester t = new Tester();
		t.setProperty("hello");
		EvaluationContext ctx = new StandardEvaluationContext(t);
		assertThat(rpa.canRead(ctx, t, "property")).isTrue();
		assertThat(rpa.read(ctx, t, "property").getValue()).isEqualTo("hello");
		// cached accessor used
		assertThat(rpa.read(ctx, t, "property").getValue()).isEqualTo("hello");

		assertThat(rpa.canRead(ctx, t, "field")).isTrue();
		assertThat(rpa.read(ctx, t, "field").getValue()).isEqualTo(3);
		// cached accessor used
		assertThat(rpa.read(ctx, t, "field").getValue()).isEqualTo(3);

		assertThat(rpa.canWrite(ctx, t, "property")).isTrue();
		rpa.write(ctx, t, "property", "goodbye");
		rpa.write(ctx, t, "property", "goodbye"); // cached accessor used

		assertThat(rpa.canWrite(ctx, t, "field")).isTrue();
		rpa.write(ctx, t, "field", 12);
		rpa.write(ctx, t, "field", 12);

		// Attempted write as first activity on this field and property to drive testing
		// of populating type descriptor cache
		rpa.write(ctx, t, "field2", 3);
		rpa.write(ctx, t, "property2", "doodoo");
		assertThat(rpa.read(ctx, t, "field2").getValue()).isEqualTo(3);

		// Attempted read as first activity on this field and property (no canRead before them)
		assertThat(rpa.read(ctx, t, "field3").getValue()).isEqualTo(0);
		assertThat(rpa.read(ctx, t, "property3").getValue()).isEqualTo("doodoo");

		// Access through is method
		assertThat(rpa.read(ctx, t, "field3").getValue()).isEqualTo(0);
		assertThat(rpa.read(ctx, t, "property4").getValue()).isEqualTo(false);
		assertThat(rpa.canRead(ctx, t, "property4")).isTrue();

		// repro SPR-9123, ReflectivePropertyAccessor JavaBean property names compliance tests
		assertThat(rpa.read(ctx, t, "iD").getValue()).isEqualTo("iD");
		assertThat(rpa.canRead(ctx, t, "iD")).isTrue();
		assertThat(rpa.read(ctx, t, "id").getValue()).isEqualTo("id");
		assertThat(rpa.canRead(ctx, t, "id")).isTrue();
		assertThat(rpa.read(ctx, t, "ID").getValue()).isEqualTo("ID");
		assertThat(rpa.canRead(ctx, t, "ID")).isTrue();
		// note: "Id" is not a valid JavaBean name, nevertheless it is treated as "id"
		assertThat(rpa.read(ctx, t, "Id").getValue()).isEqualTo("id");
		assertThat(rpa.canRead(ctx, t, "Id")).isTrue();

		// repro SPR-10994
		assertThat(rpa.read(ctx, t, "xyZ").getValue()).isEqualTo("xyZ");
		assertThat(rpa.canRead(ctx, t, "xyZ")).isTrue();
		assertThat(rpa.read(ctx, t, "xY").getValue()).isEqualTo("xY");
		assertThat(rpa.canRead(ctx, t, "xY")).isTrue();

		// SPR-10122, ReflectivePropertyAccessor JavaBean property names compliance tests - setters
		rpa.write(ctx, t, "pEBS", "Test String");
		assertThat(rpa.read(ctx, t, "pEBS").getValue()).isEqualTo("Test String");
	}

	@Test
	void optimalReflectivePropertyAccessor() throws Exception {
		ReflectivePropertyAccessor reflective = new ReflectivePropertyAccessor();
		Tester tester = new Tester();
		tester.setProperty("hello");
		EvaluationContext ctx = new StandardEvaluationContext(tester);
		assertThat(reflective.canRead(ctx, tester, "property")).isTrue();
		assertThat(reflective.read(ctx, tester, "property").getValue()).isEqualTo("hello");
		// cached accessor used
		assertThat(reflective.read(ctx, tester, "property").getValue()).isEqualTo("hello");

		PropertyAccessor property = reflective.createOptimalAccessor(ctx, tester, "property");
		assertThat(property.canRead(ctx, tester, "property")).isTrue();
		assertThat(property.canRead(ctx, tester, "property2")).isFalse();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				property.canWrite(ctx, tester, "property"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				property.canWrite(ctx, tester, "property2"));
		assertThat(property.read(ctx, tester, "property").getValue()).isEqualTo("hello");
		// cached accessor used
		assertThat(property.read(ctx, tester, "property").getValue()).isEqualTo("hello");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(property::getSpecificTargetClasses);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				property.write(ctx, tester, "property", null));

		PropertyAccessor field = reflective.createOptimalAccessor(ctx, tester, "field");
		assertThat(field.canRead(ctx, tester, "field")).isTrue();
		assertThat(field.canRead(ctx, tester, "field2")).isFalse();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				field.canWrite(ctx, tester, "field"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				field.canWrite(ctx, tester, "field2"));
		assertThat(field.read(ctx, tester, "field").getValue()).isEqualTo(3);
		// cached accessor used
		assertThat(field.read(ctx, tester, "field").getValue()).isEqualTo(3);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(field::getSpecificTargetClasses);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				field.write(ctx, tester, "field", null));
	}

	@Test
	void reflectiveMethodResolverForJdkProxies() throws Exception {
		Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Runnable.class }, (p, m, args) -> null);

		MethodResolver resolver = new ReflectiveMethodResolver();
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

		// Nonexistent method
		MethodExecutor bogus = resolver.resolve(evaluationContext, proxy, "bogus", List.of());
		assertThat(bogus).as("MethodExecutor for bogus()").isNull();

		// Method in interface
		MethodExecutor run = resolver.resolve(evaluationContext, proxy, "run", List.of());
		assertThat(run).as("MethodExecutor for run()").isNotNull();

		// Methods in Object
		MethodExecutor toString = resolver.resolve(evaluationContext, proxy, "toString", List.of());
		assertThat(toString).as("MethodExecutor for toString()").isNotNull();
		MethodExecutor hashCode = resolver.resolve(evaluationContext, proxy, "hashCode", List.of());
		assertThat(hashCode).as("MethodExecutor for hashCode()").isNotNull();
		MethodExecutor equals = resolver.resolve(evaluationContext, proxy, "equals", typeDescriptors(Object.class));
		assertThat(equals).as("MethodExecutor for equals()").isNotNull();
	}

	/**
	 * Used to validate the match returned from a compareArguments call.
	 */
	private void checkMatch(Class<?>[] inputTypes, Class<?>[] expectedTypes, StandardTypeConverter typeConverter, ArgumentsMatchKind expectedMatchKind) {
		ArgumentsMatchKind matchKind = ReflectionHelper.compareArguments(typeDescriptors(expectedTypes), typeDescriptors(inputTypes), typeConverter);
		if (expectedMatchKind == null) {
			assertThat(matchKind).as("Did not expect them to match in any way").isNull();
		}
		else {
			assertThat(matchKind).as("Should not be a null match").isNotNull();
		}

		if (expectedMatchKind == EXACT) {
			assertThat(matchKind.isExactMatch()).isTrue();
		}
		else if (expectedMatchKind == CLOSE) {
			assertThat(matchKind.isCloseMatch()).isTrue();
		}
		else if (expectedMatchKind == REQUIRES_CONVERSION) {
			assertThat(matchKind.isMatchRequiringConversion()).as("expected to be a match requiring conversion, but was " + matchKind).isTrue();
		}
	}

	/**
	 * Used to validate the match returned from a compareArgumentsVarargs call.
	 */
	private static void checkMatchVarargs(Class<?>[] inputTypes, Class<?>[] expectedTypes,
			StandardTypeConverter typeConverter, ArgumentsMatchKind expectedMatchKind) {

		ArgumentsMatchKind matchKind =
				ReflectionHelper.compareArgumentsVarargs(typeDescriptors(expectedTypes), typeDescriptors(inputTypes), typeConverter);
		if (expectedMatchKind == null) {
			assertThat(matchKind).as("Did not expect them to match in any way: " + matchKind).isNull();
		}
		else {
			assertThat(matchKind).as("Should not be a null match").isNotNull();
			switch (expectedMatchKind) {
				case EXACT -> assertThat(matchKind.isExactMatch()).isTrue();
				case CLOSE -> assertThat(matchKind.isCloseMatch()).isTrue();
				case REQUIRES_CONVERSION -> assertThat(matchKind.isMatchRequiringConversion())
						.as("expected to be a match requiring conversion, but was " + matchKind).isTrue();
			}
		}
	}

	private static void checkArguments(Object[] args, Object... expected) {
		assertThat(args).hasSize(expected.length);
		for (int i = 0; i < expected.length; i++) {
			assertThat(args[i]).isEqualTo(expected[i]);
		}
	}

	private static List<TypeDescriptor> typeDescriptors(Class<?>... types) {
		return Arrays.stream(types).map(TypeDescriptor::valueOf).toList();
	}


	interface TestInterface {

		void oneArg(String arg1);

		void twoArg(String arg1, String... arg2);
	}


	static class Super {
	}


	static class Sub extends Super {
	}


	static class Unconvertable {
	}


	static class Tester {

		String property;
		public int field = 3;
		public int field2;
		public int field3 = 0;
		String property2;
		String property3 = "doodoo";
		boolean property4 = false;
		String iD = "iD";
		String id = "id";
		String ID = "ID";
		String pEBS = "pEBS";
		String xY = "xY";
		String xyZ = "xyZ";

		public String getProperty() { return property; }

		public void setProperty(String value) { property = value; }

		public void setProperty2(String value) { property2 = value; }

		public String getProperty3() { return property3; }

		public boolean isProperty4() { return property4; }

		public String getiD() { return iD; }

		public String getId() { return id; }

		public String getID() { return ID; }

		public String getXY() { return xY; }

		public String getXyZ() { return xyZ; }

		public String getpEBS() { return pEBS; }

		public void setpEBS(String pEBS) { this.pEBS = pEBS; }
	}

}

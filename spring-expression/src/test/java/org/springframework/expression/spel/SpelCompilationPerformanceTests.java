/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelCompiler;

import static org.junit.Assert.*;

/**
 * Checks the speed of compiled SpEL expressions.
 * By default these tests are marked Ignore since they can fail on a busy machine because they
 * compare relative performance of interpreted vs compiled.
 *
 * @author Andy Clement
 * @since 4.1
 */
@Ignore
public class SpelCompilationPerformanceTests extends AbstractExpressionTests {

	int count = 50000; // Number of evaluations that are timed in one run
	int iterations = 10; // Number of times to repeat 'count' evaluations (for averaging)
	private final static boolean noisyTests = true;

	Expression expression;

	public static class Payload {
		Two[] DR = new Two[]{new Two()};

		public Two[] getDR() {
			return DR;
		}
	}

	public static class Two {
		Three DRFixedSection = new Three();
		public Three getDRFixedSection() {
			return DRFixedSection;
		}
	}

	public static class Three {
		double duration = 0.4d;
		public double getDuration() {
			return duration;
		}
	}

	public static class NumberHolder {
		public int payload = 36;
	}

	/**
	 * This test verifies the new support for compiling mathematical expressions with
	 * different operand types.
	 */
	@Test
	public void compilingMathematicalExpressionsWithDifferentOperandTypes() throws Exception {
		NumberHolder nh = new NumberHolder();
		expression = parser.parseExpression("(T(Integer).valueOf(payload).doubleValue())/18D");
		Object o = expression.getValue(nh);
		assertEquals(2d,o);
		System.out.println("Performance check for SpEL expression: '(T(Integer).valueOf(payload).doubleValue())/18D'");
		long stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(nh);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(nh);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(nh);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		compile(expression);
		System.out.println("Now compiled:");
		o = expression.getValue(nh);
		assertEquals(2d, o);

		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(nh);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(nh);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(nh);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");

		expression = parser.parseExpression("payload/18D");
		o = expression.getValue(nh);
		assertEquals(2d,o);
		System.out.println("Performance check for SpEL expression: 'payload/18D'");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(nh);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(nh);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(nh);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		compile(expression);
		System.out.println("Now compiled:");
		o = expression.getValue(nh);
		assertEquals(2d, o);

		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(nh);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(nh);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(nh);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
	}

	@Test
	public void inlineLists() throws Exception {
		expression = parser.parseExpression("{'abcde','ijklm'}[0].substring({1,3,4}[0],{1,3,4}[1])");
		Object o = expression.getValue();
		assertEquals("bc",o);
		System.out.println("Performance check for SpEL expression: '{'abcde','ijklm'}[0].substring({1,3,4}[0],{1,3,4}[1])'");
		long stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue();
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue();
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue();
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		compile(expression);
		System.out.println("Now compiled:");
		o = expression.getValue();
		assertEquals("bc", o);

		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue();
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue();
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue();
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
	}

	@Test
	public void inlineNestedLists() throws Exception {
		expression = parser.parseExpression("{'abcde',{'ijklm','nopqr'}}[1][0].substring({1,3,4}[0],{1,3,4}[1])");
		Object o = expression.getValue();
		assertEquals("jk",o);
		System.out.println("Performance check for SpEL expression: '{'abcde','ijklm'}[0].substring({1,3,4}[0],{1,3,4}[1])'");
		long stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue();
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue();
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue();
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		compile(expression);
		System.out.println("Now compiled:");
		o = expression.getValue();
		assertEquals("jk", o);

		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue();
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue();
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue();
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
	}


	@Test
	public void stringConcatenation() throws Exception {
		expression = parser.parseExpression("'hello' + getWorld() + ' spring'");
		Greeter g = new Greeter();
		Object o = expression.getValue(g);
		assertEquals("helloworld spring", o);

		System.out.println("Performance check for SpEL expression: 'hello' + getWorld() + ' spring'");
		long stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(g);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(g);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(g);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		compile(expression);
		System.out.println("Now compiled:");
		o = expression.getValue(g);
		assertEquals("helloworld spring", o);

		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(g);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(g);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
		stime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			o = expression.getValue(g);
		}
		System.out.println("One million iterations: " + (System.currentTimeMillis()-stime) + "ms");
	}

	public static class Greeter {
		public String getWorld() {
			return "world";
		}
	}

	@Test
	public void complexExpressionPerformance() throws Exception {
		Payload payload = new Payload();
		Expression expression = parser.parseExpression("DR[0].DRFixedSection.duration lt 0.1");
		boolean b = false;
		long iTotal = 0,cTotal = 0;

		// warmup
		for (int i = 0; i < count; i++) {
			b = expression.getValue(payload, Boolean.TYPE);
		}

		log("timing interpreted: ");
		for (int i = 0; i < iterations; i++) {
			long stime = System.currentTimeMillis();
			for (int j = 0; j < count; j++) {
				b = expression.getValue(payload, Boolean.TYPE);
			}
			long etime = System.currentTimeMillis();
			long interpretedSpeed = (etime - stime);
			iTotal+=interpretedSpeed;
			log(interpretedSpeed+"ms ");
		}
		logln();

		compile(expression);
		boolean bc = false;
		expression.getValue(payload, Boolean.TYPE);
		log("timing compiled: ");
		for (int i = 0; i < iterations; i++) {
			long stime = System.currentTimeMillis();
			for (int j = 0; j < count; j++) {
				bc = expression.getValue(payload, Boolean.TYPE);
			}
			long etime = System.currentTimeMillis();
			long compiledSpeed = (etime - stime);
			cTotal+=compiledSpeed;
			log(compiledSpeed+"ms ");
		}
		logln();

		reportPerformance("complex expression",iTotal, cTotal);

		// Verify the result
		assertFalse(b);

		// Verify the same result for compiled vs interpreted
		assertEquals(b, bc);

		// Verify if the input changes, the result changes
		payload.DR[0].DRFixedSection.duration = 0.04d;
		bc = expression.getValue(payload, Boolean.TYPE);
		assertTrue(bc);
	}

	public static class HW {
		public String hello() {
			return "foobar";
		}
	}

	@Test
	public void compilingMethodReference() throws Exception {
		long interpretedTotal = 0, compiledTotal = 0;
		long stime,etime;
		String interpretedResult = null,compiledResult = null;

		HW testdata = new HW();
		Expression expression = parser.parseExpression("hello()");

		// warmup
		for (int i = 0; i < count; i++) {
			interpretedResult = expression.getValue(testdata, String.class);
		}

		log("timing interpreted: ");
		for (int i = 0; i < iterations; i++) {
			stime = System.currentTimeMillis();
			for (int j = 0; j < count; j++) {
				interpretedResult = expression.getValue(testdata, String.class);
			}
			etime = System.currentTimeMillis();
			long interpretedSpeed = (etime - stime);
			interpretedTotal+=interpretedSpeed;
			log(interpretedSpeed+"ms ");
		}
		logln();

		compile(expression);

		log("timing compiled: ");
		expression.getValue(testdata, String.class);
		for (int i = 0; i < iterations; i++) {
			stime = System.currentTimeMillis();
			for (int j = 0; j < count; j++) {
				compiledResult = expression.getValue(testdata, String.class);
			}
			etime = System.currentTimeMillis();
			long compiledSpeed = (etime - stime);
			compiledTotal+=compiledSpeed;
			log(compiledSpeed+"ms ");
		}
		logln();

		assertEquals(interpretedResult,compiledResult);
		reportPerformance("method reference", interpretedTotal, compiledTotal);
		if (compiledTotal>=interpretedTotal) {
			fail("Compiled version is slower than interpreted!");
		}
	}




	public static class TestClass2 {
		public String name = "Santa";
		private String name2 = "foobar";
		public String getName2() {
			return name2;
		}
		public Foo foo = new Foo();
		public static class Foo {
			public Bar bar = new Bar();
			Bar b = new Bar();
			public Bar getBaz() {
				return b;
			}
			public Bar bay() {
				return b;
			}
		}
		public static class Bar {
			public String boo = "oranges";
		}
	}

	@Test
	public void compilingPropertyReferenceField() throws Exception {
		long interpretedTotal = 0, compiledTotal = 0, stime, etime;
		String interpretedResult = null, compiledResult = null;

		TestClass2 testdata = new TestClass2();
		Expression expression = parser.parseExpression("name");

		// warmup
		for (int i = 0; i < count; i++) {
			expression.getValue(testdata, String.class);
		}

		log("timing interpreted: ");
		for (int i = 0; i < iterations; i++) {
			stime = System.currentTimeMillis();
			for (int j = 0; j < count; j++) {
				interpretedResult = expression.getValue(testdata, String.class);
			}
			etime = System.currentTimeMillis();
			long interpretedSpeed = (etime - stime);
			interpretedTotal+=interpretedSpeed;
			log(interpretedSpeed+"ms ");
		}
		logln();

		compile(expression);

		log("timing compiled: ");
		expression.getValue(testdata, String.class);
		for (int i = 0; i < iterations; i++) {
			stime = System.currentTimeMillis();
			for (int j = 0; j < count; j++) {
				compiledResult = expression.getValue(testdata, String.class);
			}
			etime = System.currentTimeMillis();
			long compiledSpeed = (etime - stime);
			compiledTotal+=compiledSpeed;
			log(compiledSpeed+"ms ");
		}
		logln();

		assertEquals(interpretedResult,compiledResult);
		reportPerformance("property reference (field)",interpretedTotal, compiledTotal);
	}

	@Test
	public void compilingPropertyReferenceNestedField() throws Exception {
		long interpretedTotal = 0, compiledTotal = 0, stime, etime;
		String interpretedResult = null, compiledResult = null;

		TestClass2 testdata = new TestClass2();

		Expression expression = parser.parseExpression("foo.bar.boo");

		// warmup
		for (int i = 0; i < count; i++) {
			expression.getValue(testdata, String.class);
		}

		log("timing interpreted: ");
		for (int i = 0; i < iterations; i++) {
			stime = System.currentTimeMillis();
			for (int j = 0; j < count; j++) {
				interpretedResult = expression.getValue(testdata, String.class);
			}
			etime = System.currentTimeMillis();
			long interpretedSpeed = (etime - stime);
			interpretedTotal+=interpretedSpeed;
			log(interpretedSpeed+"ms ");
		}
		logln();

		compile(expression);

		log("timing compiled: ");
		expression.getValue(testdata, String.class);
		for (int i = 0; i < iterations; i++) {
			stime = System.currentTimeMillis();
			for (int j = 0; j < count; j++) {
				compiledResult = expression.getValue(testdata, String.class);
			}
			etime = System.currentTimeMillis();
			long compiledSpeed = (etime - stime);
			compiledTotal+=compiledSpeed;
			log(compiledSpeed+"ms ");
		}
		logln();

		assertEquals(interpretedResult,compiledResult);
		reportPerformance("property reference (nested field)",interpretedTotal, compiledTotal);
	}

	@Test
	public void compilingPropertyReferenceNestedMixedFieldGetter() throws Exception {
		long interpretedTotal = 0, compiledTotal = 0, stime, etime;
		String interpretedResult = null, compiledResult = null;

		TestClass2 testdata = new TestClass2();
		Expression expression = parser.parseExpression("foo.baz.boo");

		// warmup
		for (int i = 0; i < count; i++) {
			expression.getValue(testdata, String.class);
		}
		log("timing interpreted: ");
		for (int i = 0; i < iterations; i++) {
			stime = System.currentTimeMillis();
			for (int j = 0; j < count; j++) {
				interpretedResult = expression.getValue(testdata, String.class);
			}
			etime = System.currentTimeMillis();
			long interpretedSpeed = (etime - stime);
			interpretedTotal+=interpretedSpeed;
			log(interpretedSpeed+"ms ");
		}
		logln();

		compile(expression);

		log("timing compiled: ");
		expression.getValue(testdata, String.class);
		for (int i = 0; i < iterations; i++) {
			stime = System.currentTimeMillis();
			for (int j = 0; j < count; j++) {
				compiledResult = expression.getValue(testdata, String.class);
			}
			etime = System.currentTimeMillis();
			long compiledSpeed = (etime - stime);
			compiledTotal+=compiledSpeed;
			log(compiledSpeed+"ms ");
		}
		logln();

		assertEquals(interpretedResult,compiledResult);
		reportPerformance("nested property reference (mixed field/getter)",interpretedTotal, compiledTotal);
	}

	@Test
	public void compilingNestedMixedFieldPropertyReferenceMethodReference() throws Exception {
		long interpretedTotal = 0, compiledTotal = 0, stime, etime;
		String interpretedResult = null, compiledResult = null;

		TestClass2 testdata = new TestClass2();
		Expression expression = parser.parseExpression("foo.bay().boo");

		// warmup
		for (int i = 0; i < count; i++) {
			expression.getValue(testdata, String.class);
		}

		log("timing interpreted: ");
		for (int i = 0; i < iterations; i++) {
			stime = System.currentTimeMillis();
			for (int j = 0; j < count; j++) {
				interpretedResult = expression.getValue(testdata, String.class);
			}
			etime = System.currentTimeMillis();
			long interpretedSpeed = (etime - stime);
			interpretedTotal += interpretedSpeed;
			log(interpretedSpeed + "ms ");
		}
		logln();

		compile(expression);

		log("timing compiled: ");
		expression.getValue(testdata, String.class);
		for (int i = 0; i < iterations; i++) {
			stime = System.currentTimeMillis();
			for (int j = 0; j < count; j++) {
				compiledResult = expression.getValue(testdata, String.class);
			}
			etime = System.currentTimeMillis();
			long compiledSpeed = (etime - stime);
			compiledTotal += compiledSpeed;
			log(compiledSpeed + "ms ");

		}
		logln();

		assertEquals(interpretedResult,compiledResult);
		reportPerformance("nested reference (mixed field/method)", interpretedTotal, compiledTotal);
	}

	@Test
	public void compilingPropertyReferenceGetter() throws Exception {
		long interpretedTotal = 0, compiledTotal = 0, stime, etime;
		String interpretedResult = null, compiledResult = null;

		TestClass2 testdata = new TestClass2();
		Expression expression = parser.parseExpression("name2");

		// warmup
		for (int i = 0;i < count; i++) {
			expression.getValue(testdata, String.class);
		}

		log("timing interpreted: ");
		for (int i = 0; i < iterations; i++) {
			stime = System.currentTimeMillis();
			for (int j = 0; j < count; j++) {
				interpretedResult = expression.getValue(testdata, String.class);
			}
			etime = System.currentTimeMillis();
			long interpretedSpeed = (etime - stime);
			interpretedTotal+=interpretedSpeed;
			log(interpretedSpeed+"ms ");
		}
		logln();


		compile(expression);

		log("timing compiled: ");
		expression.getValue(testdata, String.class);
		for (int i = 0; i < iterations; i++) {
			stime = System.currentTimeMillis();
			for (int j = 0; j < count; j++) {
				compiledResult = expression.getValue(testdata, String.class);
			}
			etime = System.currentTimeMillis();
			long compiledSpeed = (etime - stime);
			compiledTotal+=compiledSpeed;
			log(compiledSpeed+"ms ");

		}
		logln();

		assertEquals(interpretedResult,compiledResult);

		reportPerformance("property reference (getter)", interpretedTotal, compiledTotal);
		if (compiledTotal>=interpretedTotal) {
			fail("Compiled version is slower than interpreted!");
		}
	}

	// ---

	private void reportPerformance(String title, long interpretedTotal, long compiledTotal) {
		double averageInterpreted = interpretedTotal/(iterations);
		double averageCompiled = compiledTotal/(iterations);
		double ratio = (averageCompiled/averageInterpreted)*100.0d;
		logln(">>"+title+": average for "+count+": compiled="+averageCompiled+"ms interpreted="+averageInterpreted+"ms: compiled takes " + ((int)ratio)+"% of the interpreted time");
		if (averageCompiled>averageInterpreted) {
			fail("Compiled version took longer than interpreted!  CompiledSpeed=~"+averageCompiled+
					"ms InterpretedSpeed="+averageInterpreted+"ms");
		}
		logln();
	}

	private void log(String message) {
		if (noisyTests) {
			System.out.print(message);
		}
	}

	private void logln(String... message) {
		if (noisyTests) {
			if (message!=null && message.length>0) {
				System.out.println(message[0]);
			}
			else {
				System.out.println();
			}
		}
	}

	private void compile(Expression expression) {
		assertTrue(SpelCompiler.compile(expression));
	}
}

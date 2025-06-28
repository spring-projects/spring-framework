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

package org.springframework.context.annotation.jsr330;

import java.util.Enumeration;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.atinject.tck.Tck;
import org.atinject.tck.auto.Car;
import org.atinject.tck.auto.Convertible;
import org.atinject.tck.auto.Drivers;
import org.atinject.tck.auto.DriversSeat;
import org.atinject.tck.auto.FuelTank;
import org.atinject.tck.auto.Seat;
import org.atinject.tck.auto.Tire;
import org.atinject.tck.auto.V8Engine;
import org.atinject.tck.auto.accessories.Cupholder;
import org.atinject.tck.auto.accessories.SpareTire;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Jsr330ScopeMetadataResolver;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.GenericApplicationContext;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * {@code @Inject} Technology Compatibility Kit (TCK) tests.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see org.atinject.tck.Tck
 */
class SpringAtInjectTckTests {

	@TestFactory
	Stream<? extends DynamicNode> runTechnologyCompatibilityKit() {
		TestSuite testSuite = (TestSuite) Tck.testsFor(buildCar(), false, true);
		return generateDynamicTests(testSuite);
	}


	@SuppressWarnings("unchecked")
	private static Car buildCar() {
		GenericApplicationContext ac = new GenericApplicationContext();
		AnnotatedBeanDefinitionReader bdr = new AnnotatedBeanDefinitionReader(ac);
		bdr.setScopeMetadataResolver(new Jsr330ScopeMetadataResolver());

		bdr.registerBean(Convertible.class);
		bdr.registerBean(DriversSeat.class, Drivers.class);
		bdr.registerBean(Seat.class, Primary.class);
		bdr.registerBean(V8Engine.class);
		bdr.registerBean(SpareTire.class, "spare");
		bdr.registerBean(Cupholder.class);
		bdr.registerBean(Tire.class, Primary.class);
		bdr.registerBean(FuelTank.class);

		ac.refresh();
		return ac.getBean(Car.class);
	}

	private static Stream<? extends DynamicNode> generateDynamicTests(TestSuite testSuite) {
		return stream(testSuite.tests()).map(test -> {
			if (test instanceof TestSuite nestedSuite) {
				return dynamicContainer(nestedSuite.getName(), generateDynamicTests(nestedSuite));
			}
			if (test instanceof TestCase testCase) {
				return dynamicTest(testCase.getName(), () -> runTestCase(testCase));
			}
			throw new IllegalStateException("Unsupported Test type: " + test.getClass().getName());
		});
	}

	private static void runTestCase(TestCase testCase) {
		TestResult testResult = new TestResult();
		testCase.run(testResult);
		assertSuccessfulResults(testResult);
	}

	private static void assertSuccessfulResults(TestResult testResult) {
		if (!testResult.wasSuccessful()) {
			Throwable throwable = Stream.concat(stream(testResult.failures()), stream(testResult.errors()))
					.map(TestFailure::thrownException)
					.findFirst()
					.get();

			if (throwable instanceof Error error) {
				throw error;
			}
			if (throwable instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new AssertionError(throwable);
		}
	}

	private static <T> Stream<T> stream(Enumeration<T> enumeration) {
		Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(
				enumeration.asIterator(), Spliterator.ORDERED);
		return StreamSupport.stream(spliterator, false);
	}

}

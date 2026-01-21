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

import java.net.URI;
import java.util.Collections;
import java.util.stream.Stream;

import junit.framework.TestCase;
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
import org.springframework.util.ClassUtils;

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
		Class<?> suiteClass = resolveTestSuiteClass(testSuite);
		return generateDynamicTests(testSuite, suiteClass);
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

	private static Stream<? extends DynamicNode> generateDynamicTests(TestSuite testSuite, Class<?> suiteClass) {
		return Collections.list(testSuite.tests()).stream().map(test -> {
			if (test instanceof TestSuite nestedSuite) {
				Class<?> nestedSuiteClass = resolveTestSuiteClass(nestedSuite);
				URI uri = URI.create("class:" + nestedSuiteClass.getName());
				return dynamicContainer(nestedSuite.getName(), uri, generateDynamicTests(nestedSuite, nestedSuiteClass));
			}
			if (test instanceof TestCase testCase) {
				URI uri = URI.create("method:" + suiteClass.getName() + "#" + testCase.getName());
				return dynamicTest(testCase.getName(), uri, () -> runTestCase(testCase));
			}
			throw new IllegalStateException("Unsupported Test type: " + test.getClass().getName());
		});
	}

	private static void runTestCase(TestCase testCase) throws Throwable {
		TestResult testResult = new TestResult();
		testCase.run(testResult);
		if (testResult.failureCount() > 0) {
			throw testResult.failures().nextElement().thrownException();
		}
		if (testResult.errorCount() > 0) {
			throw testResult.errors().nextElement().thrownException();
		}
	}

	private static Class<?> resolveTestSuiteClass(TestSuite testSuite) {
		return ClassUtils.resolveClassName(testSuite.getName(), Tck.class.getClassLoader());
	}

}

/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.mock.staticmock;

/**
 * Annotation-based aspect to use in test build to enable mocking static methods
 * on JPA-annotated {@code @Entity} classes, as used by Roo for finders.
 *
 * <p>Mocking will occur in the call stack of any method in a class (typically a test class)
 * that is annotated with the @MockStaticEntityMethods annotation.
 *
 * <p>Also provides static methods to simplify the programming model for
 * entering playback mode and setting expected return values.
 *
 * <p>Usage:
 * <ol>
 * <li>Annotate a test class with @MockStaticEntityMethods.
 * <li>In each test method, AnnotationDrivenStaticEntityMockingControl will begin in recording mode.
 * Invoke static methods on Entity classes, with each recording-mode invocation
 * being followed by an invocation to the static expectReturn() or expectThrow()
 * method on AnnotationDrivenStaticEntityMockingControl.
 * <li>Invoke the static AnnotationDrivenStaticEntityMockingControl() method.
 * <li>Call the code you wish to test that uses the static methods. Verification will
 * occur automatically.
 * </ol>
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @see MockStaticEntityMethods
 */
public aspect AnnotationDrivenStaticEntityMockingControl extends AbstractMethodMockingControl {

	/**
	 * Stop recording mock calls and enter playback state
	 */
	public static void playback() {
		AnnotationDrivenStaticEntityMockingControl.aspectOf().playbackInternal();
	}

	public static void expectReturn(Object retVal) {
		AnnotationDrivenStaticEntityMockingControl.aspectOf().expectReturnInternal(retVal);
	}

	public static void expectThrow(Throwable throwable) {
		AnnotationDrivenStaticEntityMockingControl.aspectOf().expectThrowInternal(throwable);
	}

	// Only matches directly annotated @Test methods, to allow methods in
	// @MockStatics classes to invoke each other without resetting the mocking environment
	protected pointcut mockStaticsTestMethod() : execution(public * (@MockStaticEntityMethods *).*(..));

	protected pointcut methodToMock() : execution(public static * (@javax.persistence.Entity *).*(..));

}

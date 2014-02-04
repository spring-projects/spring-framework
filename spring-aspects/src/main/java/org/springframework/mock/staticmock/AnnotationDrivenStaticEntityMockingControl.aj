/*
 * Copyright 2002-2014 the original author or authors.
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
 * Annotation-based aspect to use in test builds to enable mocking of static methods
 * on JPA-annotated {@code @Entity} classes, as used by Spring Roo for so-called
 * <em>finder methods</em>.
 *
 * <p>Mocking will occur within the call stack of any method in a class (typically a
 * test class) that is annotated with {@code @MockStaticEntityMethods}.
 *
 * <p>This aspect also provides static methods to simplify the programming model for
 * setting expectations and entering playback mode.
 *
 * <p>Usage:
 * <ol>
 * <li>Annotate a test class with {@code @MockStaticEntityMethods}.
 * <li>In each test method, {@code AnnotationDrivenStaticEntityMockingControl}
 * will begin in <em>recording</em> mode.
 * <li>Invoke static methods on JPA-annotated {@code @Entity} classes, with each
 * recording-mode invocation being followed by an invocation of either the static
 * {@link #expectReturn(Object)} method or the static {@link #expectThrow(Throwable)}
 * method on {@code AnnotationDrivenStaticEntityMockingControl}.
 * <li>Invoke the static {@link #playback()} method.
 * <li>Call the code you wish to test that uses the static methods.
 * <li>Verification will occur automatically.
 * </ol>
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Sam Brannen
 * @see MockStaticEntityMethods
 */
public aspect AnnotationDrivenStaticEntityMockingControl extends AbstractMethodMockingControl {

	/**
	 * Expect the supplied {@link Object} to be returned by the previous static
	 * method invocation.
	 * @see #playback()
	 */
	public static void expectReturn(Object retVal) {
		AnnotationDrivenStaticEntityMockingControl.aspectOf().expectReturnInternal(retVal);
	}

	/**
	 * Expect the supplied {@link Throwable} to be thrown by the previous static
	 * method invocation.
	 * @see #playback()
	 */
	public static void expectThrow(Throwable throwable) {
		AnnotationDrivenStaticEntityMockingControl.aspectOf().expectThrowInternal(throwable);
	}

	/**
	 * Stop recording mock expectations and enter <em>playback</em> mode.
	 * @see #expectReturn(Object)
	 * @see #expectThrow(Throwable)
	 */
	public static void playback() {
		AnnotationDrivenStaticEntityMockingControl.aspectOf().playbackInternal();
	}
	
	// Apparently, the following pointcut was originally defined to only match
	// methods directly annotated with @Test (in order to allow methods in
	// @MockStaticEntityMethods classes to invoke each other without resetting
	// the mocking environment); however, this is no longer the case. The current
	// pointcut applies to all public methods in @MockStaticEntityMethods classes.
	protected pointcut mockStaticsTestMethod() : execution(public * (@MockStaticEntityMethods *).*(..));

	protected pointcut methodToMock() : execution(public static * (@javax.persistence.Entity *).*(..));

}

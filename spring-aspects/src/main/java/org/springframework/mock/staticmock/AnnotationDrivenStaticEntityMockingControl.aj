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

import org.aspectj.lang.annotation.RequiredTypes;
import org.aspectj.lang.annotation.SuppressAjWarnings;

/**
 * Annotation-based aspect to use in test builds to enable mocking of static methods
 * on JPA-annotated {@code @Entity} classes, as used by Spring Roo for so-called
 * <em>finder methods</em>.
 *
 * <p>Mocking will occur within the call stack of any {@code public} method in a
 * class (typically a test class) that is annotated with {@code @MockStaticEntityMethods}.
 * Thus mocking is not limited to {@code @Test} methods. Furthermore, new mock
 * state will be created for the invocation of each such public method, even when
 * the method is invoked from another such public method.
 *
 * <p>This aspect also provides static methods to simplify the programming model for
 * setting expectations and entering playback mode.
 *
 * <h3>Usage</h3>
 * <ol>
 * <li>Annotate a test class with {@code @MockStaticEntityMethods}.
 * <li>In each test method, the {@code AnnotationDrivenStaticEntityMockingControl}
 * will begin in <em>recording</em> mode.
 * <li>Invoke static methods on JPA-annotated {@code @Entity} classes, with each
 * recording-mode invocation being followed by an invocation of either
 * {@link #expectReturn(Object)} or {@link #expectThrow(Throwable)} on the
 * {@code AnnotationDrivenStaticEntityMockingControl}.
 * <li>Invoke the {@link #playback()} method.
 * <li>Call the code you wish to test that uses the static methods on the
 * JPA-annotated {@code @Entity} classes.
 * <li>Verification will occur automatically after the test method has executed
 * and returned. However, mock verification will not occur if the test method
 * throws an exception.
 * </ol>
 *
 * <h3>Programmatic Control of the Mock</h3>
 * <p>For scenarios where it would be convenient to programmatically <em>verify</em>
 * the recorded expectations or <em>reset</em> the state of the mock, consider
 * using combinations of {@link #verify()} and {@link #reset()}.
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Sam Brannen
 * @see MockStaticEntityMethods
 */
@RequiredTypes("javax.persistence.Entity")
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

	/**
	 * Verify that all expectations have been fulfilled.
	 * @since 4.0.2
	 * @see #reset()
	 */
	public static void verify() {
		AnnotationDrivenStaticEntityMockingControl.aspectOf().verifyInternal();
	}

	/**
	 * Reset the state of the mock and enter <em>recording</em> mode.
	 * @since 4.0.2
	 * @see #verify()
	 */
	public static void reset() {
		AnnotationDrivenStaticEntityMockingControl.aspectOf().resetInternal();
	}

	// Apparently, the following pointcut was originally defined to only match
	// methods directly annotated with @Test (in order to allow methods in
	// @MockStaticEntityMethods classes to invoke each other without creating a
	// new mocking environment); however, this is no longer the case. The current
	// pointcut applies to all public methods in @MockStaticEntityMethods classes.
	@SuppressAjWarnings("adviceDidNotMatch")
	protected pointcut mockStaticsTestMethod() : execution(public * (@MockStaticEntityMethods *).*(..));

	@SuppressAjWarnings("adviceDidNotMatch")
	protected pointcut methodToMock() : execution(public static * (@javax.persistence.Entity *).*(..));

}

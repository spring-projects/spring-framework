/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.mock.easymock;

import org.easymock.MockControl;

/**
 * Makes those tests that require a <b>single</b> (scalar) EasyMock mock
 * control and object easier to author.
 *
 * @author Rick Evans
 * @since 2.0
 */
public abstract class AbstractScalarMockTemplate {

	private static final int NORMAL = 0;
	private static final int NICE = 1;
	private static final int STRICT = 2;


	private Class mockInterface;

	private int mode = NORMAL;


	/**
	 * Creates a new instance of the {@link AbstractScalarMockTemplate} class.
	 */
	protected AbstractScalarMockTemplate() {
	}

	/**
	 * Creates a new instance of the {@link AbstractScalarMockTemplate} class.
	 * @param mockInterface the interface that is to be mocked
	 */
	protected AbstractScalarMockTemplate(Class mockInterface) {
		this.mockInterface = mockInterface;
	}

	/**
	 * Creates a new instance of the {@link AbstractScalarMockTemplate} class.
	 * @param mockInterface the interface that is to be mocked
	 * @param nice <code>true</code> if a "nice" mock control is to be created;
	 * <code>false</code> if a "strict" control is to be created
	 */
	protected AbstractScalarMockTemplate(Class mockInterface, boolean nice) {
		this.mockInterface = mockInterface;
		this.mode = nice ? NICE : STRICT;
	}


	/**
	 * Sets the interface that is to be mocked.
	 * @param mockInterface the interface that is to be mocked
	 */
	public void setMockInterface(Class mockInterface) {
		this.mockInterface = mockInterface;
	}

	/**
	 * Gets the interface that is to be mocked.
	 * @return the interface that is to be mocked
	 */
	public Class getMockInterface() {
		return mockInterface;
	}


	/**
	 * Setup any expectations for the test.
	 * <p>The default implementation is a no-op; i.e. no expectations are set.
	 * @param mockControl the EasyMock {@link org.easymock.MockControl} for the mocked object
	 * @param mockObject the mocked object
	 * @throws Exception if calling methods on the supplied <code>mockObject</code>
	 * that are declared as throwing one more exceptions (just here to satisfy the compiler really).
	 */
	public void setupExpectations(MockControl mockControl, Object mockObject) throws Exception {
	}

	/**
	 * Do the EasyMock-driven test.
	 * <p>This is the driving template method, and should not typically need to overriden.
	 * @throws Exception if an exception is thrown during testing
	 */
	public void test() throws Exception {
		MockControl mockControl = createMockControl();
		Object mockObject = mockControl.getMock();
		setupExpectations(mockControl, mockObject);
		mockControl.replay();
		doTest(mockObject);
		mockControl.verify();
	}

	/**
	 * Do the actual test using the supplied mock.
	 * @param mockObject the mocked object
	 * @throws Exception if an exception is thrown during the test logic
	 */
	public abstract void doTest(Object mockObject) throws Exception;


	/**
	 * Create a {@link org.easymock.MockControl} for the mocked interface.
	 * @return a {@link org.easymock.MockControl} for the mocked interface
	 */
	protected MockControl createMockControl() {
		return this.mode == NORMAL
				? MockControl.createControl(this.getMockInterface())
				: this.mode == NICE
				? MockControl.createNiceControl(this.getMockInterface())
				: MockControl.createStrictControl(this.getMockInterface());
	}

}

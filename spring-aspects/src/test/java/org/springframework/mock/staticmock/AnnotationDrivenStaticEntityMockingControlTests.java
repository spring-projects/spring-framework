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

import javax.persistence.PersistenceException;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.springframework.mock.staticmock.AnnotationDrivenStaticEntityMockingControl.*;

/**
 * Tests for Spring's static entity mocking framework (i.e., @{@link MockStaticEntityMethods}
 * and {@link AnnotationDrivenStaticEntityMockingControl}).
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Sam Brannen
 */
@MockStaticEntityMethods
public class AnnotationDrivenStaticEntityMockingControlTests {

	@Test
	public void noArgumentMethodInvocationReturnsInt() {
		int expectedCount = 13;
		Person.countPeople();
		expectReturn(expectedCount);
		playback();
		assertEquals(expectedCount, Person.countPeople());
	}

	@Test(expected = PersistenceException.class)
	public void noArgumentMethodInvocationThrowsException() {
		Person.countPeople();
		expectThrow(new PersistenceException());
		playback();
		Person.countPeople();
	}

	@Test
	public void methodArgumentsMatch() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		expectReturn(found);
		playback();
		assertEquals(found, Person.findPerson(id));
	}

	@Test
	public void longSeriesOfCalls() {
		long id1 = 13;
		long id2 = 24;
		Person found1 = new Person();
		Person.findPerson(id1);
		expectReturn(found1);
		Person found2 = new Person();
		Person.findPerson(id2);
		expectReturn(found2);
		Person.findPerson(id1);
		expectReturn(found1);
		Person.countPeople();
		expectReturn(0);
		playback();

		assertEquals(found1, Person.findPerson(id1));
		assertEquals(found2, Person.findPerson(id2));
		assertEquals(found1, Person.findPerson(id1));
		assertEquals(0, Person.countPeople());
	}

	@Test(expected = IllegalArgumentException.class)
	public void methodArgumentsDoNotMatchAndReturnsObject() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		expectReturn(found);
		playback();
		assertEquals(found, Person.findPerson(id + 1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void methodArgumentsDoNotMatchAndThrowsException() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		expectThrow(new PersistenceException());
		playback();
		assertEquals(found, Person.findPerson(id + 1));
	}

	@Test
	public void reentrantCallToPrivateMethod() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		expectReturn(found);
		playback();
		privateMethod(found, id);
	}

	private void privateMethod(Person found, long id) {
		assertEquals(found, Person.findPerson(id));
	}

	@Test
	public void reentrantCallToPublicMethod() {
		final Long ID = 13L;
		Person.findPerson(ID);
		expectReturn(new Person());
		playback();
		try {
			publicMethod();
			fail("Should have thrown an IllegalStateException");
		}
		catch (IllegalStateException e) {
			String snippet = "Calls have been recorded, but playback state was never reached.";
			assertTrue("Exception message should contain [" + snippet + "]", e.getMessage().contains(snippet));
		}

		// Now to keep the mock for "this" method happy:
		Person.findPerson(ID);
	}

	public void publicMethod() {
		// At this point, since publicMethod() is a public method in a class
		// annotated with @MockStaticEntityMethods, the current mock state is
		// fresh. In other words, we are again in recording mode. As such, any
		// call to a mocked method will return null. See the implementation of
		// the <methodToMock() && cflowbelow(mockStaticsTestMethod())> around
		// advice in AbstractMethodMockingControl for details.
		assertNull(Person.findPerson(99L));
	}

	@Test(expected = IllegalStateException.class)
	public void unexpectedCall() {
		playback();
		Person.countPeople();
	}

	@Test(expected = IllegalStateException.class)
	public void tooFewCalls() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		expectReturn(found);
		Person.countPeople();
		expectReturn(25);
		playback();
		assertEquals(found, Person.findPerson(id));
	}

	@Test
	public void noExpectationsAndNoPlayback() {
		// Ensure that mock verification doesn't blow up if playback() was not invoked and
		// no expectations were set.
	}

	@Test
	public void noExpectationsButWithPlayback() {
		// Ensure that mock verification doesn't blow up if playback() was invoked but no
		// expectations were set.
		playback();
	}

	@Test(expected = IllegalStateException.class)
	public void doesNotEnterPlaybackMode() {
		Person.countPeople();
	}

	@Test(expected = IllegalStateException.class)
	public void doesNotSetExpectedReturnValue() {
		Person.countPeople();
		playback();
	}

	/**
	 * Currently, the method mocking aspect will not verify the state of the
	 * expectations within the mock if a test method throws an exception.
	 *
	 * <p>The reason is that the {@code mockStaticsTestMethod()} advice in
	 * {@link AbstractMethodMockingControl} is declared as
	 * {@code after() returning} instead of simply {@code after()}.
	 *
	 * <p>If the aforementioned advice is changed to a generic "after" advice,
	 * this test method will fail with an {@link IllegalStateException} (thrown
	 * by the mock aspect) instead of the {@link UnsupportedOperationException}
	 * thrown by the test method itself.
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void mockVerificationDoesNotOccurIfTestFailsWithAnExpectedException() {
		Person.countPeople();
		playback();
		// We intentionally do not execute any of the recorded method invocations in order
		// to demonstrate that mock verification does not occur if an
		// exception is thrown.
		throw new UnsupportedOperationException();
	}

	@Test
	public void resetMockWithoutVerficationAndStartOverWithoutRedeclaringExpectations() {
		final Long ID = 13L;
		Person.findPerson(ID);
		expectReturn(new Person());

		reset();

		Person.findPerson(ID);
		// Omit expectation.
		playback();

		try {
			Person.findPerson(ID);
			fail("Should have thrown an IllegalStateException");
		}
		catch (IllegalStateException e) {
			String snippet = "Behavior of Call with signature";
			assertTrue("Exception message should contain [" + snippet + "]", e.getMessage().contains(snippet));
		}
	}

	@Test
	public void resetMockWithoutVerificationAndStartOver() {
		final Long ID = 13L;
		Person found = new Person();
		Person.findPerson(ID);
		expectReturn(found);

		reset();

		// Intentionally use a different ID:
		final long ID_2 = ID + 1;
		Person.findPerson(ID_2);
		expectReturn(found);
		playback();

		assertEquals(found, Person.findPerson(ID_2));
	}

	@Test
	public void verifyResetAndStartOver() {
		final Long ID_1 = 13L;
		Person found1 = new Person();
		Person.findPerson(ID_1);
		expectReturn(found1);
		playback();

		assertEquals(found1, Person.findPerson(ID_1));
		verify();
		reset();

		// Intentionally use a different ID:
		final long ID_2 = ID_1 + 1;
		Person found2 = new Person();
		Person.findPerson(ID_2);
		expectReturn(found2);
		playback();

		assertEquals(found2, Person.findPerson(ID_2));
	}

	@Test
	public void verifyWithTooFewCalls() {
		final Long ID = 13L;
		Person found = new Person();
		Person.findPerson(ID);
		expectReturn(found);
		Person.findPerson(ID);
		expectReturn(found);
		playback();

		assertEquals(found, Person.findPerson(ID));

		try {
			verify();
			fail("Should have thrown an IllegalStateException");
		}
		catch (IllegalStateException e) {
			assertEquals("Expected 2 calls, but received 1", e.getMessage());
			// Since verify() failed, we need to manually reset so that the test method
			// does not fail.
			reset();
		}
	}

}

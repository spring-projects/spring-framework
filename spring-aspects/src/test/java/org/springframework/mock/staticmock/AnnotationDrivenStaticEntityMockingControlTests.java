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

import java.rmi.RemoteException;

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
		AnnotationDrivenStaticEntityMockingControl.expectReturn(found);
		AnnotationDrivenStaticEntityMockingControl.playback();
		assertEquals(found, Person.findPerson(id + 1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void methodArgumentsDoNotMatchAndThrowsException() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		AnnotationDrivenStaticEntityMockingControl.expectThrow(new PersistenceException());
		AnnotationDrivenStaticEntityMockingControl.playback();
		assertEquals(found, Person.findPerson(id + 1));
	}

	@Test
	public void reentrant() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		expectReturn(found);
		playback();
		called(found, id);
	}

	private void called(Person found, long id) {
		assertEquals(found, Person.findPerson(id));
	}

	@Test(expected = IllegalStateException.class)
	public void rejectUnexpectedCall() {
		AnnotationDrivenStaticEntityMockingControl.playback();
		Person.countPeople();
	}

	@Test(expected = IllegalStateException.class)
	public void tooFewCalls() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		AnnotationDrivenStaticEntityMockingControl.expectReturn(found);
		Person.countPeople();
		AnnotationDrivenStaticEntityMockingControl.expectReturn(25);
		AnnotationDrivenStaticEntityMockingControl.playback();
		assertEquals(found, Person.findPerson(id));
	}

	@Test
	public void empty() {
		// Test that verification check doesn't blow up if no replay() call happened.
	}

	@Test(expected = IllegalStateException.class)
	public void doesNotEnterPlaybackMode() {
		Person.countPeople();
	}

	@Test(expected = IllegalStateException.class)
	public void doesNotSetExpectedReturnValue() {
		Person.countPeople();
		AnnotationDrivenStaticEntityMockingControl.playback();
	}

	/**
	 * Note: this test method currently does NOT actually verify that the mock
	 * verification fails.
	 */
	// TODO Determine if it's possible for a mock verification failure to fail a test in
	// JUnit 4+ if the test method itself throws an expected exception.
	@Test(expected = RemoteException.class)
	public void verificationFailsEvenWhenTestFailsInExpectedManner() throws Exception {
		Person.countPeople();
		AnnotationDrivenStaticEntityMockingControl.playback();
		// No calls in order to allow verification failure
		throw new RemoteException();
	}

}

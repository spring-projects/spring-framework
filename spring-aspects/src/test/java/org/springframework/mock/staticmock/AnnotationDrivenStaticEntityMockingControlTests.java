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

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.springframework.mock.staticmock.AnnotationDrivenStaticEntityMockingControl.*;

/**
 * Tests for static entity mocking framework.
 * 
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Sam Brannen
 */
@MockStaticEntityMethods
public class AnnotationDrivenStaticEntityMockingControlTests {

	// TODO Fix failing test
	@Ignore
	@Test
	public void noArgIntReturn() {
		int expectedCount = 13;
		Person.countPeople();
		expectReturn(expectedCount);
		playback();
		assertEquals(expectedCount, Person.countPeople());
	}

	// TODO Fix failing test
	@Ignore
	@Test(expected = PersistenceException.class)
	public void noArgThrows() {
		Person.countPeople();
		expectThrow(new PersistenceException());
		playback();
		Person.countPeople();
	}

	// TODO Fix failing test
	@Ignore
	@Test
	public void argMethodMatches() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		expectReturn(found);
		playback();
		assertEquals(found, Person.findPerson(id));
	}

	// TODO Fix failing test
	@Ignore
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

	/**
	 * Note delegation is used when tests are invalid and should fail, as otherwise the
	 * failure will occur on the verify() method in the aspect after this method returns,
	 * failing the test case.
	 */
	// TODO Fix failing test
	@Ignore
	@Test
	public void argMethodNoMatchExpectReturn() {
		try {
			new Delegate().argMethodNoMatchExpectReturn();
			fail();
		}
		catch (IllegalArgumentException expected) {
		}
	}

	// TODO Fix failing test
	@Ignore
	@Test(expected = IllegalArgumentException.class)
	public void argMethodNoMatchExpectThrow() {
		new Delegate().argMethodNoMatchExpectThrow();
	}

	private void called(Person found, long id) {
		assertEquals(found, Person.findPerson(id));
	}

	// TODO Fix failing test
	@Ignore
	@Test
	public void reentrant() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		expectReturn(found);
		playback();
		called(found, id);
	}

	@Test(expected = IllegalStateException.class)
	public void rejectUnexpectedCall() {
		new Delegate().rejectUnexpectedCall();
	}

	// TODO Fix failing test
	@Ignore
	@Test(expected = IllegalStateException.class)
	public void failTooFewCalls() {
		new Delegate().failTooFewCalls();
	}

	@Test
	public void empty() {
		// Test that verification check doesn't blow up if no replay() call happened
	}

	@Test(expected = IllegalStateException.class)
	public void doesntEverReplay() {
		new Delegate().doesntEverReplay();
	}

	@Test(expected = IllegalStateException.class)
	public void doesntEverSetReturn() {
		new Delegate().doesntEverSetReturn();
	}

}

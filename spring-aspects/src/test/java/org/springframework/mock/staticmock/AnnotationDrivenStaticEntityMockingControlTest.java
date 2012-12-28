/*
 * Copyright 2002-2012 the original author or authors.
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

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.springframework.mock.staticmock.AnnotationDrivenStaticEntityMockingControl.*;


/**
 * Test for static entity mocking framework.
 * @author Rod Johnson
 * @author Ramnivas Laddad
 *
 */
@MockStaticEntityMethods
@RunWith(JUnit4.class)
public class AnnotationDrivenStaticEntityMockingControlTest {

	@Test
	public void testNoArgIntReturn() {
		int expectedCount = 13;
		Person.countPeople();
		expectReturn(expectedCount);
		playback();
		Assert.assertEquals(expectedCount, Person.countPeople());
	}

	@Test(expected=PersistenceException.class)
	public void testNoArgThrows() {
		Person.countPeople();
		expectThrow(new PersistenceException());
		playback();
		Person.countPeople();
	}

	@Test
	public void testArgMethodMatches() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		expectReturn(found);
		playback();
		Assert.assertEquals(found, Person.findPerson(id));
	}


	@Test
	public void testLongSeriesOfCalls() {
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

		Assert.assertEquals(found1, Person.findPerson(id1));
		Assert.assertEquals(found2, Person.findPerson(id2));
		Assert.assertEquals(found1, Person.findPerson(id1));
		Assert.assertEquals(0, Person.countPeople());
	}

	// Note delegation is used when tests are invalid and should fail, as otherwise
	// the failure will occur on the verify() method in the aspect after
	// this method returns, failing the test case
	@Test
	public void testArgMethodNoMatchExpectReturn() {
		try {
			new Delegate().testArgMethodNoMatchExpectReturn();
			Assert.fail();
		} catch (IllegalArgumentException expected) {
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testArgMethodNoMatchExpectThrow() {
		new Delegate().testArgMethodNoMatchExpectThrow();
	}

	private void called(Person found, long id) {
		Assert.assertEquals(found, Person.findPerson(id));
	}

	@Test
	public void testReentrant() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		expectReturn(found);
		playback();
		called(found, id);
	}

	@Test(expected=IllegalStateException.class)
	public void testRejectUnexpectedCall() {
		new Delegate().rejectUnexpectedCall();
	}

	@Test(expected=IllegalStateException.class)
	public void testFailTooFewCalls() {
		new Delegate().failTooFewCalls();
	}

	@Test
	public void testEmpty() {
		// Test that verification check doesn't blow up if no replay() call happened
	}

	@Test(expected=IllegalStateException.class)
	public void testDoesntEverReplay() {
		new Delegate().doesntEverReplay();
	}

	@Test(expected=IllegalStateException.class)
	public void testDoesntEverSetReturn() {
		new Delegate().doesntEverSetReturn();
	}
}


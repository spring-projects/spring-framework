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

import java.rmi.RemoteException;

import javax.persistence.PersistenceException;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.staticmock.AnnotationDrivenStaticEntityMockingControl;
import org.springframework.mock.staticmock.MockStaticEntityMethods;

//Used because verification failures occur after method returns,
//so we can't test for them in the test case itself
@MockStaticEntityMethods
@Ignore // This isn't meant for direct testing; rather it is driven from AnnotationDrivenStaticEntityMockingControl
public class Delegate {

	@Test
	public void testArgMethodNoMatchExpectReturn() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		AnnotationDrivenStaticEntityMockingControl.expectReturn(found);
		AnnotationDrivenStaticEntityMockingControl.playback();
		Assert.assertEquals(found, Person.findPerson(id + 1));
	}

	@Test
	public void testArgMethodNoMatchExpectThrow() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		AnnotationDrivenStaticEntityMockingControl.expectThrow(new PersistenceException());
		AnnotationDrivenStaticEntityMockingControl.playback();
		Assert.assertEquals(found, Person.findPerson(id + 1));
	}

	@Test
	public void failTooFewCalls() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		AnnotationDrivenStaticEntityMockingControl.expectReturn(found);
		Person.countPeople();
		AnnotationDrivenStaticEntityMockingControl.expectReturn(25);
		AnnotationDrivenStaticEntityMockingControl.playback();
		Assert.assertEquals(found, Person.findPerson(id));
	}

	@Test
	public void doesntEverReplay() {
		Person.countPeople();
	}

	@Test
	public void doesntEverSetReturn() {
		Person.countPeople();
		AnnotationDrivenStaticEntityMockingControl.playback();
	}

	@Test
	public void rejectUnexpectedCall() {
		AnnotationDrivenStaticEntityMockingControl.playback();
		Person.countPeople();
	}

	@Test(expected=RemoteException.class)
	public void testVerificationFailsEvenWhenTestFailsInExpectedManner() throws RemoteException {
		Person.countPeople();
		AnnotationDrivenStaticEntityMockingControl.playback();
		// No calls to allow verification failure
		throw new RemoteException();
	}
}

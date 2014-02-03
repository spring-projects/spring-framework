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

import org.junit.Ignore;

import static org.junit.Assert.*;

/**
 * This isn't meant for direct testing; rather it is driven from
 * {@link AnnotationDrivenStaticEntityMockingControlTests}.
 * 
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Sam Brannen
 */
@MockStaticEntityMethods
@Ignore("Used because verification failures occur after method returns, so we can't test for them in the test case itself")
public class Delegate {

	public void argMethodNoMatchExpectReturn() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		AnnotationDrivenStaticEntityMockingControl.expectReturn(found);
		AnnotationDrivenStaticEntityMockingControl.playback();
		assertEquals(found, Person.findPerson(id + 1));
	}

	public void argMethodNoMatchExpectThrow() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		AnnotationDrivenStaticEntityMockingControl.expectThrow(new PersistenceException());
		AnnotationDrivenStaticEntityMockingControl.playback();
		assertEquals(found, Person.findPerson(id + 1));
	}

	public void failTooFewCalls() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		AnnotationDrivenStaticEntityMockingControl.expectReturn(found);
		Person.countPeople();
		AnnotationDrivenStaticEntityMockingControl.expectReturn(25);
		AnnotationDrivenStaticEntityMockingControl.playback();
		assertEquals(found, Person.findPerson(id));
	}

	public void doesntEverReplay() {
		Person.countPeople();
	}

	public void doesntEverSetReturn() {
		Person.countPeople();
		AnnotationDrivenStaticEntityMockingControl.playback();
	}

	public void rejectUnexpectedCall() {
		AnnotationDrivenStaticEntityMockingControl.playback();
		Person.countPeople();
	}

	public void verificationFailsEvenWhenTestFailsInExpectedManner()
			throws RemoteException {
		Person.countPeople();
		AnnotationDrivenStaticEntityMockingControl.playback();
		// No calls to allow verification failure
		throw new RemoteException();
	}

}

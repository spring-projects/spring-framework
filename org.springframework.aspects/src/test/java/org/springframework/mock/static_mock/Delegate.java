package org.springframework.mock.static_mock;

import java.rmi.RemoteException;

import javax.persistence.PersistenceException;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

//Used because verification failures occur after method returns,
//so we can't test for them in the test case itself
@MockStaticEntityMethods
@Ignore // This isn't meant for direct testing; rather it is driven from JUnintStaticEntityMockingControlTest
public class Delegate {

	@Test
	public void testArgMethodNoMatchExpectReturn() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		JUnitStaticEntityMockingControl.expectReturn(found);
		JUnitStaticEntityMockingControl.playback();
		Assert.assertEquals(found, Person.findPerson(id + 1));
	}

	@Test
	public void testArgMethodNoMatchExpectThrow() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		JUnitStaticEntityMockingControl.expectThrow(new PersistenceException());
		JUnitStaticEntityMockingControl.playback();
		Assert.assertEquals(found, Person.findPerson(id + 1));
	}
	
	@Test
	public void failTooFewCalls() {
		long id = 13;
		Person found = new Person();
		Person.findPerson(id);
		JUnitStaticEntityMockingControl.expectReturn(found);
		Person.countPeople();
		JUnitStaticEntityMockingControl.expectReturn(25);
		JUnitStaticEntityMockingControl.playback();
		Assert.assertEquals(found, Person.findPerson(id));
	}

	@Test
	public void doesntEverReplay() {
		Person.countPeople();
	}
	
	@Test
	public void doesntEverSetReturn() {
		Person.countPeople();
		JUnitStaticEntityMockingControl.playback();
	}

	@Test
	public void rejectUnexpectedCall() {
		JUnitStaticEntityMockingControl.playback();
		Person.countPeople();
	}
	
	@Test(expected=RemoteException.class)
	public void testVerificationFailsEvenWhenTestFailsInExpectedManner() throws RemoteException {
		Person.countPeople();
		JUnitStaticEntityMockingControl.playback();
		// No calls to allow verification failure
		throw new RemoteException();
	}
}

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

package org.springframework.jndi;

import javax.naming.Context;
import javax.naming.NameNotFoundException;

import junit.framework.TestCase;
import org.easymock.MockControl;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 08.07.2003
 */
public class JndiTemplateTests extends TestCase {

	public void testLookupSucceeds() throws Exception {
		Object o = new Object();
		String name = "foo";
		MockControl mc = MockControl.createControl(Context.class);
		final Context mock = (Context) mc.getMock();
		mock.lookup(name);
		mc.setReturnValue(o);
		mock.close();
		mc.setVoidCallable(1);
		mc.replay();

		JndiTemplate jt = new JndiTemplate() {
			protected Context createInitialContext() {
				return mock;
			}
		};

		Object o2 = jt.lookup(name);
		assertEquals(o, o2);
		mc.verify();
	}
	
	public void testLookupFails() throws Exception {
		NameNotFoundException ne = new NameNotFoundException();
		String name = "foo";
		MockControl mc = MockControl.createControl(Context.class);
		final Context mock = (Context) mc.getMock();
		mock.lookup(name);
		mc.setThrowable(ne);
		mock.close();
		mc.setVoidCallable(1);
		mc.replay();

		JndiTemplate jt = new JndiTemplate() {
			protected Context createInitialContext() {
				return mock;
			}
		};

		try {
			jt.lookup(name);
			fail("Should have thrown NamingException");
		}
		catch (NameNotFoundException ex) {
			// Ok
		}
		mc.verify();
	}
	
	public void testLookupReturnsNull() throws Exception {
		String name = "foo";
		MockControl mc = MockControl.createControl(Context.class);
		final Context mock = (Context) mc.getMock();
		mock.lookup(name);
		mc.setReturnValue(null);
		mock.close();
		mc.setVoidCallable(1);
		mc.replay();

		JndiTemplate jt = new JndiTemplate() {
			protected Context createInitialContext() {
				return mock;
			}
		};

		try {
			jt.lookup(name);
			fail("Should have thrown NamingException");
		}
		catch (NameNotFoundException ex) {
			// Ok
		}
		mc.verify();
	}

	public void testLookupFailsWithTypeMismatch() throws Exception {
		Object o = new Object();
		String name = "foo";
		MockControl mc = MockControl.createControl(Context.class);
		final Context mock = (Context) mc.getMock();
		mock.lookup(name);
		mc.setReturnValue(o);
		mock.close();
		mc.setVoidCallable(1);
		mc.replay();

		JndiTemplate jt = new JndiTemplate() {
			protected Context createInitialContext() {
				return mock;
			}
		};

		try {
			jt.lookup(name, String.class);
			fail("Should have thrown TypeMismatchNamingException");
		}
		catch (TypeMismatchNamingException ex) {
			// Ok
		}
		mc.verify();
	}

	public void testBind() throws Exception {
		Object o = new Object();
		String name = "foo";
		MockControl mc = MockControl.createControl(Context.class);
		final Context mock = (Context) mc.getMock();
		mock.bind(name, o);
		mc.setVoidCallable(1);
		mock.close();
		mc.setVoidCallable(1);
		mc.replay();
		
		JndiTemplate jt = new JndiTemplate() {
			protected Context createInitialContext() {
				return mock;
			}
		};
		
		jt.bind(name, o);
		mc.verify();
	}
	
	public void testRebind() throws Exception {
		Object o = new Object();
		String name = "foo";
		MockControl mc = MockControl.createControl(Context.class);
		final Context mock = (Context) mc.getMock();
		mock.rebind(name, o);
		mc.setVoidCallable(1);
		mock.close();
		mc.setVoidCallable(1);
		mc.replay();

		JndiTemplate jt = new JndiTemplate() {
			protected Context createInitialContext() {
				return mock;
			}
		};

		jt.rebind(name, o);
		mc.verify();
	}

	public void testUnbind() throws Exception {
		String name = "something";
		MockControl mc = MockControl.createControl(Context.class);
		final Context mock = (Context) mc.getMock();
		mock.unbind(name);
		mc.setVoidCallable(1);
		mock.close();
		mc.setVoidCallable(1);
		mc.replay();
	
		JndiTemplate jt = new JndiTemplate() {
			protected Context createInitialContext() {
				return mock;
			}
		};
	
		jt.unbind(name);
		mc.verify();
	}

}

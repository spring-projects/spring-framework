/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 08.07.2003
 */
public class JndiTemplateTests {

	@Test
	public void testLookupSucceeds() throws Exception {
		Object o = new Object();
		String name = "foo";
		final Context context = mock(Context.class);
		given(context.lookup(name)).willReturn(o);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return context;
			}
		};

		Object o2 = jt.lookup(name);
		assertEquals(o, o2);
		verify(context).close();
	}

	@Test
	public void testLookupFails() throws Exception {
		NameNotFoundException ne = new NameNotFoundException();
		String name = "foo";
		final Context context = mock(Context.class);
		given(context.lookup(name)).willThrow(ne);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return context;
			}
		};

		try {
			jt.lookup(name);
			fail("Should have thrown NamingException");
		}
		catch (NameNotFoundException ex) {
			// Ok
		}
		verify(context).close();
	}

	@Test
	public void testLookupReturnsNull() throws Exception {
		String name = "foo";
		final Context context = mock(Context.class);
		given(context.lookup(name)).willReturn(null);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return context;
			}
		};

		try {
			jt.lookup(name);
			fail("Should have thrown NamingException");
		}
		catch (NameNotFoundException ex) {
			// Ok
		}
		verify(context).close();
	}

	@Test
	public void testLookupFailsWithTypeMismatch() throws Exception {
		Object o = new Object();
		String name = "foo";
		final Context context = mock(Context.class);
		given(context.lookup(name)).willReturn(o);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return context;
			}
		};

		try {
			jt.lookup(name, String.class);
			fail("Should have thrown TypeMismatchNamingException");
		}
		catch (TypeMismatchNamingException ex) {
			// Ok
		}
		verify(context).close();
	}

	@Test
	public void testBind() throws Exception {
		Object o = new Object();
		String name = "foo";
		final Context context = mock(Context.class);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return context;
			}
		};

		jt.bind(name, o);
		verify(context).bind(name, o);
		verify(context).close();
	}

	@Test
	public void testRebind() throws Exception {
		Object o = new Object();
		String name = "foo";
		final Context context = mock(Context.class);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return context;
			}
		};

		jt.rebind(name, o);
		verify(context).rebind(name, o);
		verify(context).close();
;
	}

	@Test
	public void testUnbind() throws Exception {
		String name = "something";
		final Context context = mock(Context.class);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return context;
			}
		};

		jt.unbind(name);
		verify(context).unbind(name);
		verify(context).close();
	}

}

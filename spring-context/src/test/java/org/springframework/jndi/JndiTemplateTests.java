/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 08.07.2003
 */
class JndiTemplateTests {

	@Test
	void testLookupSucceeds() throws Exception {
		Object o = new Object();
		String name = "foo";
		final Context context = mock();
		given(context.lookup(name)).willReturn(o);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return context;
			}
		};

		Object o2 = jt.lookup(name);
		assertThat(o2).isEqualTo(o);
		verify(context).close();
	}

	@Test
	void testLookupFails() throws Exception {
		NameNotFoundException ne = new NameNotFoundException();
		String name = "foo";
		final Context context = mock();
		given(context.lookup(name)).willThrow(ne);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return context;
			}
		};

		assertThatExceptionOfType(NameNotFoundException.class).isThrownBy(() ->
				jt.lookup(name));
		verify(context).close();
	}

	@Test
	void testLookupReturnsNull() throws Exception {
		String name = "foo";
		final Context context = mock();
		given(context.lookup(name)).willReturn(null);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return context;
			}
		};

		assertThatExceptionOfType(NameNotFoundException.class).isThrownBy(() ->
				jt.lookup(name));
		verify(context).close();
	}

	@Test
	void testLookupFailsWithTypeMismatch() throws Exception {
		Object o = new Object();
		String name = "foo";
		final Context context = mock();
		given(context.lookup(name)).willReturn(o);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return context;
			}
		};

		assertThatExceptionOfType(TypeMismatchNamingException.class).isThrownBy(() ->
				jt.lookup(name, String.class));
		verify(context).close();
	}

	@Test
	void testBind() throws Exception {
		Object o = new Object();
		String name = "foo";
		final Context context = mock();

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
	void testRebind() throws Exception {
		Object o = new Object();
		String name = "foo";
		final Context context = mock();

		JndiTemplate jt = new JndiTemplate() {
			@Override
			protected Context createInitialContext() {
				return context;
			}
		};

		jt.rebind(name, o);
		verify(context).rebind(name, o);
		verify(context).close();
	}

	@Test
	void testUnbind() throws Exception {
		String name = "something";
		final Context context = mock();

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

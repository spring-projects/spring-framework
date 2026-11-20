/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void lookupSucceeds() throws Exception {
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
	void lookupFails() throws Exception {
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
	void lookupReturnsNull() throws Exception {
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
	void lookupFailsWithTypeMismatch() throws Exception {
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
	void bind() throws Exception {
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
	void rebind() throws Exception {
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
	void unbind() throws Exception {
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

/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.mock.jndi;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link SimpleNamingContextBuilder} and {@link SimpleNamingContext}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 */
@SuppressWarnings("deprecation")
class SimpleNamingContextTests {

	@Test
	void namingContextBuilder() throws NamingException {
		SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
		InitialContextFactory factory = builder.createInitialContextFactory(null);

		DataSource ds = new StubDataSource();
		builder.bind("java:comp/env/jdbc/myds", ds);
		Object obj = new Object();
		builder.bind("myobject", obj);

		Context context1 = factory.getInitialContext(null);
		assertThat(context1.lookup("java:comp/env/jdbc/myds") == ds).as("Correct DataSource registered").isTrue();
		assertThat(context1.lookup("myobject") == obj).as("Correct Object registered").isTrue();

		Hashtable<String, String> env2 = new Hashtable<>();
		env2.put("key1", "value1");
		Context context2 = factory.getInitialContext(env2);
		assertThat(context2.lookup("java:comp/env/jdbc/myds") == ds).as("Correct DataSource registered").isTrue();
		assertThat(context2.lookup("myobject") == obj).as("Correct Object registered").isTrue();
		assertThat(context2.getEnvironment() != env2).as("Correct environment").isTrue();
		assertThat("value1".equals(context2.getEnvironment().get("key1"))).as("Correct key1").isTrue();

		Integer i = new Integer(0);
		context1.rebind("myinteger", i);
		String s = "";
		context2.bind("mystring", s);

		Context context3 = (Context) context2.lookup("");
		context3.rename("java:comp/env/jdbc/myds", "jdbc/myds");
		context3.unbind("myobject");

		assertThat(context3.getEnvironment() != context2.getEnvironment()).as("Correct environment").isTrue();
		context3.addToEnvironment("key2", "value2");
		assertThat("value2".equals(context3.getEnvironment().get("key2"))).as("key2 added").isTrue();
		context3.removeFromEnvironment("key1");
		assertThat(context3.getEnvironment().get("key1") == null).as("key1 removed").isTrue();

		assertThat(context1.lookup("jdbc/myds") == ds).as("Correct DataSource registered").isTrue();
		assertThatExceptionOfType(NameNotFoundException.class).isThrownBy(() ->
				context1.lookup("myobject"));
		assertThat(context1.lookup("myinteger") == i).as("Correct Integer registered").isTrue();
		assertThat(context1.lookup("mystring") == s).as("Correct String registered").isTrue();

		assertThat(context2.lookup("jdbc/myds") == ds).as("Correct DataSource registered").isTrue();
		assertThatExceptionOfType(NameNotFoundException.class).isThrownBy(() ->
				context2.lookup("myobject"));
		assertThat(context2.lookup("myinteger") == i).as("Correct Integer registered").isTrue();
		assertThat(context2.lookup("mystring") == s).as("Correct String registered").isTrue();

		assertThat(context3.lookup("jdbc/myds") == ds).as("Correct DataSource registered").isTrue();
		assertThatExceptionOfType(NameNotFoundException.class).isThrownBy(() ->
				context3.lookup("myobject"));
		assertThat(context3.lookup("myinteger") == i).as("Correct Integer registered").isTrue();
		assertThat(context3.lookup("mystring") == s).as("Correct String registered").isTrue();

		Map<String, Binding> bindingMap = new HashMap<>();
		NamingEnumeration<?> bindingEnum = context3.listBindings("");
		while (bindingEnum.hasMoreElements()) {
			Binding binding = (Binding) bindingEnum.nextElement();
			bindingMap.put(binding.getName(), binding);
		}
		boolean condition = bindingMap.get("jdbc").getObject() instanceof Context;
		assertThat(condition).as("Correct jdbc subcontext").isTrue();
		assertThat(SimpleNamingContext.class.getName().equals(bindingMap.get("jdbc").getClassName())).as("Correct jdbc subcontext").isTrue();

		Context jdbcContext = (Context) context3.lookup("jdbc");
		jdbcContext.bind("mydsX", ds);
		Map<String, Binding> subBindingMap = new HashMap<>();
		NamingEnumeration<?> subBindingEnum = jdbcContext.listBindings("");
		while (subBindingEnum.hasMoreElements()) {
			Binding binding = (Binding) subBindingEnum.nextElement();
			subBindingMap.put(binding.getName(), binding);
		}

		assertThat(ds.equals(subBindingMap.get("myds").getObject())).as("Correct DataSource registered").isTrue();
		assertThat(StubDataSource.class.getName().equals(subBindingMap.get("myds").getClassName())).as("Correct DataSource registered").isTrue();
		assertThat(ds.equals(subBindingMap.get("mydsX").getObject())).as("Correct DataSource registered").isTrue();
		assertThat(StubDataSource.class.getName().equals(subBindingMap.get("mydsX").getClassName())).as("Correct DataSource registered").isTrue();
		assertThat(i.equals(bindingMap.get("myinteger").getObject())).as("Correct Integer registered").isTrue();
		assertThat(Integer.class.getName().equals(bindingMap.get("myinteger").getClassName())).as("Correct Integer registered").isTrue();
		assertThat(s.equals(bindingMap.get("mystring").getObject())).as("Correct String registered").isTrue();
		assertThat(String.class.getName().equals(bindingMap.get("mystring").getClassName())).as("Correct String registered").isTrue();

		context1.createSubcontext("jdbc").bind("sub/subds", ds);

		Map<String, String> pairMap = new HashMap<>();
		NamingEnumeration<?> pairEnum = context2.list("jdbc");
		while (pairEnum.hasMore()) {
			NameClassPair pair = (NameClassPair) pairEnum.next();
			pairMap.put(pair.getName(), pair.getClassName());
		}
		assertThat(SimpleNamingContext.class.getName().equals(pairMap.get("sub"))).as("Correct sub subcontext").isTrue();

		Context subContext = (Context) context2.lookup("jdbc/sub");
		Map<String, String> subPairMap = new HashMap<>();
		NamingEnumeration<?> subPairEnum = subContext.list("");
		while (subPairEnum.hasMoreElements()) {
			NameClassPair pair = (NameClassPair) subPairEnum.next();
			subPairMap.put(pair.getName(), pair.getClassName());
		}

		assertThat(StubDataSource.class.getName().equals(subPairMap.get("subds"))).as("Correct DataSource registered").isTrue();
		assertThat(StubDataSource.class.getName().equals(pairMap.get("myds"))).as("Correct DataSource registered").isTrue();
		assertThat(StubDataSource.class.getName().equals(pairMap.get("mydsX"))).as("Correct DataSource registered").isTrue();

		pairMap.clear();
		pairEnum = context1.list("jdbc/");
		while (pairEnum.hasMore()) {
			NameClassPair pair = (NameClassPair) pairEnum.next();
			pairMap.put(pair.getName(), pair.getClassName());
		}
		assertThat(StubDataSource.class.getName().equals(pairMap.get("myds"))).as("Correct DataSource registered").isTrue();
		assertThat(StubDataSource.class.getName().equals(pairMap.get("mydsX"))).as("Correct DataSource registered").isTrue();
	}

	/**
	 * Demonstrates how emptyActivatedContextBuilder() method can be
	 * used repeatedly, and how it affects creating a new InitialContext()
	 */
	@Test
	void createInitialContext() throws Exception {
		SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
		String name = "foo";
		Object o = new Object();
		builder.bind(name, o);
		// Check it affects JNDI
		Context ctx = new InitialContext();
		assertThat(ctx.lookup(name) == o).isTrue();
		// Check it returns mutable contexts
		ctx.unbind(name);
		InitialContext badCtx1 = new InitialContext();
		assertThatExceptionOfType(NamingException.class).isThrownBy(() ->
				badCtx1.lookup(name));

		// Check the same call will work again, but the context is empty
		builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
		InitialContext badCtx2 = new InitialContext();
		assertThatExceptionOfType(NamingException.class).isThrownBy(() ->
				badCtx2.lookup(name));
		Object o2 = new Object();
		builder.bind(name, o2);
		assertThat(o2).isEqualTo(badCtx2.lookup(name));
	}


	static class StubDataSource implements DataSource {

		@Override
		public Connection getConnection() throws SQLException {
			return null;
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			return null;
		}

		@Override
		public PrintWriter getLogWriter() throws SQLException {
			return null;
		}

		@Override
		public int getLoginTimeout() throws SQLException {
			return 0;
		}

		@Override
		public void setLogWriter(PrintWriter arg0) throws SQLException {

		}

		@Override
		public void setLoginTimeout(int arg0) throws SQLException {

		}

		@Override
		public boolean isWrapperFor(Class<?> arg0) throws SQLException {
			return false;
		}

		@Override
		public <T> T unwrap(Class<T> arg0) throws SQLException {
			return null;
		}

		@Override
		public Logger getParentLogger() {
			return null;
		}
	}

}

/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.orm.jpa.hibernate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.jspecify.annotations.Nullable;

/**
 * Delegate for creating shareable {@link Session}/{@link StatelessSession}
 * references for a given {@link SessionFactory}.
 *
 * <p>Typically used next to {@link LocalSessionFactoryBuilder}. Note that
 * {@link LocalSessionFactoryBean} exposes shared {@link Session} as well
 * as {@link StatelessSession} references for dependency injection already,
 * avoiding the need to define separate beans for the shared sessions.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see LocalSessionFactoryBuilder
 * @see LocalSessionFactoryBean
 * @see org.springframework.orm.jpa.SharedEntityManagerCreator
 */
public abstract class SharedSessionCreator {

	/**
	 * Create a shared {@link Session} proxy for the given {@link SessionFactory}.
	 * <p>The returned instance behaves like {@link SessionFactory#getCurrentSession()}
	 * but without the manual get call, automatically delegating every {@link Session}
	 * method invocation to the current thread-bound transactional session instance.
	 * Designed to work with {@link HibernateTransactionManager} as well as JTA.
	 * <p>Alternatively, use {@link SessionFactory#getCurrentSession()} directly.
	 * @param sessionFactory the SessionFactory to build the Session proxy for
	 * @see SessionFactory#getCurrentSession()
	 */
	public static Session createSharedSession(SessionFactory sessionFactory) {
		return (Session) Proxy.newProxyInstance(SharedSessionCreator.class.getClassLoader(),
				new Class<?>[] {Session.class},
				new SharedSessionInvocationHandler(sessionFactory, sessionFactory::getCurrentSession));
	}

	/**
	 * Create a shared {@link StatelessSession} proxy for the given {@link SessionFactory}.
	 * <p>The returned instance automatically delegates every {@link StatelessSession}
	 * method invocation to the current thread-bound transactional session instance.
	 * On the first invocation within a new transaction, a {@link StatelessSession}
	 * will be opened for the current transactional JDBC Connection.
	 * <p>Works with {@link HibernateTransactionManager} (side by side with a
	 * thread-bound regular Session that drives the transaction) as well as
	 * {@link org.springframework.jdbc.support.JdbcTransactionManager} or
	 * {@link org.springframework.transaction.jta.JtaTransactionManager}
	 * (with a plain StatelessSession on top of a transactional JDBC Connection).
	 * <p>Alternatively, call {@link SpringSessionContext#currentStatelessSession}
	 * for every operation, avoiding the need for a proxy.
	 * @param sessionFactory the SessionFactory to build the StatelessSession proxy for
	 * @see SpringSessionContext#currentStatelessSession(SessionFactory)
	 */
	public static StatelessSession createSharedStatelessSession(SessionFactory sessionFactory) {
		return (StatelessSession) Proxy.newProxyInstance(SharedSessionCreator.class.getClassLoader(),
				new Class<?>[] {StatelessSession.class},
				new SharedSessionInvocationHandler(sessionFactory,
						() -> SpringSessionContext.currentStatelessSession(sessionFactory)));
	}


	private static class SharedSessionInvocationHandler implements InvocationHandler {

		private final SessionFactory sessionFactory;

		private final Supplier<Object> currentSessionSupplier;

		public SharedSessionInvocationHandler(SessionFactory sessionFactory, Supplier<Object> currentSessionSupplier) {
			this.sessionFactory = sessionFactory;
			this.currentSessionSupplier = currentSessionSupplier;
		}

		@Override
		public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
				case "equals" -> {
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				}
				case "hashCode" -> {
					// Use hashCode of EntityManager proxy.
					return hashCode();
				}
				case "toString" -> {
					// Deliver toString without touching a target EntityManager.
					return "Shared Session proxy for target factory [" + this.sessionFactory + "]";
				}
				case "getSessionFactory", "getEntityManagerFactory" -> {
					// JPA 2.0: return EntityManagerFactory without creating an EntityManager.
					return this.sessionFactory;
				}
				case "getCriteriaBuilder", "getMetamodel" -> {
					// JPA 2.0: return EntityManagerFactory's CriteriaBuilder/Metamodel (avoid creation of EntityManager)
					try {
						return SessionFactory.class.getMethod(method.getName()).invoke(this.sessionFactory);
					}
					catch (InvocationTargetException ex) {
						throw ex.getTargetException();
					}
				}
				case "unwrap" -> {
					// JPA 2.0: handle unwrap method - could be a proxy match.
					Class<?> targetClass = (Class<?>) args[0];
					if (targetClass != null && targetClass.isInstance(proxy)) {
						return proxy;
					}
				}
				case "isOpen" -> {
					// Handle isOpen method: always return true.
					return true;
				}
				case "close" -> {
					// Handle close method: suppress, not valid.
					return null;
				}
				case "getTransaction" -> {
					throw new IllegalStateException(
							"Not allowed to create transaction on shared EntityManager - " +
							"use Spring transactions or EJB CMT instead");
				}
			}

			Object target = this.currentSessionSupplier.get();
			try {
				return method.invoke(target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}

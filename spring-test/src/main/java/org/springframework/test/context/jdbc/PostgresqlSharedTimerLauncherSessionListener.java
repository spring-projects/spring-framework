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

package org.springframework.test.context.jdbc;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link LauncherSessionListener} that eagerly initializes the PostgreSQL JDBC driver's
 * shared {@link java.util.Timer} on the JUnit launcher thread.
 * <p>This prevents the driver's {@code TimerThread} from inheriting Spring's
 * {@code TransactionContextHolder} from a transactional test thread, which would otherwise
 * pin an {@code ApplicationContext} for the lifetime of the JVM.
 *
 * @since 7.0.x
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/36737">gh-36737</a>
 */
public final class PostgresqlSharedTimerLauncherSessionListener implements LauncherSessionListener {

	private static final String POSTGRESQL_DRIVER_CLASS_NAME = "org.postgresql.Driver";


	@Override
	public void launcherSessionOpened(LauncherSession session) {
		initializeSharedTimer();
	}

	private static void initializeSharedTimer() {
		try {
			Class<?> driverClass = ClassUtils.forName(POSTGRESQL_DRIVER_CLASS_NAME, null);
			Method getSharedTimer = ClassUtils.getMethodIfAvailable(driverClass, "getSharedTimer");
			if (getSharedTimer == null) {
				return;
			}
			@Nullable Object sharedTimer = ReflectionUtils.invokeMethod(getSharedTimer, null);
			if (sharedTimer == null) {
				return;
			}
			Method getTimer = ClassUtils.getMethodIfAvailable(sharedTimer.getClass(), "getTimer");
			if (getTimer != null) {
				ReflectionUtils.invokeMethod(getTimer, sharedTimer);
			}
		}
		catch (ClassNotFoundException | LinkageError ex) {
			// PostgreSQL JDBC driver not present
		}
	}

}

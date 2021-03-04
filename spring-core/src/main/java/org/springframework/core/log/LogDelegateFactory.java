/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Factory for common {@link Log} delegates with Spring's logging conventions.
 *
 * <p>Mainly for internal use within the framework with Apache Commons Logging,
 * typically in the form of the {@code spring-jcl} bridge but also compatible
 * with other Commons Logging bridges.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.1
 * @see org.apache.commons.logging.LogFactory
 */
public final class LogDelegateFactory {

	private LogDelegateFactory() {
	}


	/**
	 * Create a composite logger that delegates to a primary or falls back on a
	 * secondary logger if logging for the primary logger is not enabled.
	 * <p>This may be used for fallback logging from lower-level packages that
	 * logically should log together with some higher-level package but the two
	 * don't happen to share a suitable parent package (e.g. logging for the web
	 * and lower-level http and codec packages). For such cases the primary
	 * (class-based) logger can be wrapped with a shared fallback logger.
	 * @param primaryLogger primary logger to try first
	 * @param secondaryLogger secondary logger
	 * @param tertiaryLoggers optional vararg of further fallback loggers
	 * @return the resulting composite logger for the related categories
	 */
	public static Log getCompositeLog(Log primaryLogger, Log secondaryLogger, Log... tertiaryLoggers) {
		List<Log> loggers = new ArrayList<>(2 + tertiaryLoggers.length);
		loggers.add(primaryLogger);
		loggers.add(secondaryLogger);
		Collections.addAll(loggers, tertiaryLoggers);
		return new CompositeLog(loggers);
	}

	/**
	 * Create a "hidden" logger with a category name prefixed with "_", thus
	 * precluding it from being enabled together with other log categories from
	 * the same package. This is useful for specialized output that is either
	 * too verbose or otherwise optional or unnecessary to see all the time.
	 * @param clazz the class for which to create a logger
	 * @return a Log with the category {@code "_" + fully-qualified class name}
	 */
	public static Log getHiddenLog(Class<?> clazz) {
		return getHiddenLog(clazz.getName());
	}

	/**
	 * Create a "hidden" logger with a category name prefixed with "_", thus
	 * precluding it from being enabled together with other log categories from
	 * the same package. This is useful for specialized output that is either
	 * too verbose or otherwise optional or unnecessary to see all the time.
	 * @param category the log category to use
	 * @return a Log with the category {@code "_" + category}
	 * @since 5.3.5
	 */
	public static Log getHiddenLog(String category) {
		return LogFactory.getLog("_" + category);
	}

}

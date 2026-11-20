/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

/**
 * Log facade used to handle annotation introspection failures (in particular
 * {@code TypeNotPresentExceptions}). Allows annotation processing to continue,
 * assuming that when Class attribute values are not resolvable the annotation
 * should effectively be ignored.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 */
enum IntrospectionFailureLogger {

	DEBUG {
		@Override
		public boolean isEnabled() {
			return getLogger().isDebugEnabled();
		}
		@Override
		public void log(String message) {
			getLogger().debug(message);
		}
	},

	INFO {
		@Override
		public boolean isEnabled() {
			return getLogger().isInfoEnabled();
		}
		@Override
		public void log(String message) {
			getLogger().info(message);
		}
	},

	WARN {
		@Override
		public boolean isEnabled() {
			return getLogger().isWarnEnabled();
		}
		@Override
		public void log(String message) {
			getLogger().warn(message);
		}
	};


	private static @Nullable Log logger;


	void log(String message, @Nullable Object source, Throwable ex) {
		String on = (source != null ? " on " + source : "");
		log(message + on + ": " + ex);
	}

	abstract boolean isEnabled();

	abstract void log(String message);


	private static Log getLogger() {
		Log logger = IntrospectionFailureLogger.logger;
		if (logger == null) {
			logger = LogFactory.getLog(MergedAnnotation.class);
			IntrospectionFailureLogger.logger = logger;
		}
		return logger;
	}

}

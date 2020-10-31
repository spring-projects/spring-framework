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

package org.springframework.core.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;

/**
 * Log facade used to handle annotation introspection failures (in particular
 * {@code TypeNotPresentExceptions}). Allows annotation processing to continue,
 * assuming that when Class attribute values are not resolvable the annotation
 * should effectively disappear.
 *
 * @author Phillip Webb
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
	};


	@Nullable
	private static Log logger;


	void log(String message, @Nullable Object source, Exception ex) {
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

/*
 * Copyright 2002-2017 the original author or authors.
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

package org.apache.commons.logging;

import java.io.Serializable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

/**
 * A minimal incarnation of Apache Commons Logging's {@code LogFactory} API,
 * providing just the two common static {@link Log} lookup methods.
 * This should be source and binary compatible with all common use of the
 * Commons Logging API (i.e. {@code LogFactory.getLog(Class/String)} setup).
 *
 * <p>This implementation does not support any of Commons Logging's flexible
 * configuration. It rather only checks for the presence of the Log4J 2.x API
 * and the SLF4J 1.7 API in the framework classpath, falling back to
 * {@code java.util.logging} if none of the two is available. In that sense,
 * it works as a replacement for the Log4j 2 Commons Logging bridge as well as
 * the JCL-over-SLF4J bridge, both of which become irrelevant for Spring-based
 * setups as a consequence (with no need for manual excludes of the standard
 * Commons Logging API jar anymore either). Furthermore, for simple setups
 * without an external logging provider, Spring does not require any extra jar
 * on the classpath anymore since this embedded log factory automatically
 * switches to {@code java.util.logging} in such a scenario.
 *
 * <p><b>Note that this Commons Logging variant is only meant to be used for
 * framework logging purposes, both in the core framework and in extensions.</b>
 * For applications, prefer direct use of Log4J or SLF4J or {@code java.util.logging}.
 *
 * @author Juergen Hoeller
 * @since 5.0
 */
public abstract class LogFactory {

	private static LogApi logApi = LogApi.JUL;

	static {
		ClassLoader cl = LogFactory.class.getClassLoader();
		try {
			// Try Log4J 2.x API
			cl.loadClass("org.apache.logging.log4j.spi.LoggerContext");
			logApi = LogApi.LOG4J;
		}
		catch (ClassNotFoundException ex) {
			try {
				// Try SLF4J 1.7 API
				cl.loadClass("org.slf4j.LoggerFactory");
				logApi = LogApi.SLF4J;
			}
			catch (ClassNotFoundException ex2) {
				// Keep java.util.logging as default
			}
		}
	}


	/**
	 * Convenience method to return a named logger.
	 * @param clazz containing Class from which a log name will be derived
	 */
	public static Log getLog(Class<?> clazz) {
		return getLog(clazz.getName());
	}

	/**
	 * Convenience method to return a named logger.
	 * @param name logical name of the <code>Log</code> instance to be returned
	 */
	public static Log getLog(String name) {
		switch (logApi) {
			case LOG4J:
				return Log4jDelegate.createLog(name);
			case SLF4J:
				return Slf4jDelegate.createLog(name);
			default:
				return new JavaUtilLog(name);
		}
	}


	private enum LogApi {LOG4J, SLF4J, JUL}


	private static class Log4jDelegate {

		public static Log createLog(String name) {
			return new Log4jLog(name);
		}
	}


	private static class Slf4jDelegate {

		public static Log createLog(String name) {
			return new Slf4jLog(name);
		}
	}


	@SuppressWarnings("serial")
	private static class Log4jLog implements Log, Serializable {

		private static final String FQCN = Log4jLog.class.getName();

		private static final LoggerContext loggerContext = LogManager.getContext();

		private final ExtendedLogger logger;

		public Log4jLog(String name) {
			this.logger = loggerContext.getLogger(name);
		}

		@Override
		public boolean isDebugEnabled() {
			return logger.isEnabled(Level.DEBUG, null, null);
		}

		@Override
		public boolean isErrorEnabled() {
			return logger.isEnabled(Level.ERROR, null, null);
		}

		@Override
		public boolean isFatalEnabled() {
			return logger.isEnabled(Level.FATAL, null, null);
		}

		@Override
		public boolean isInfoEnabled() {
			return logger.isEnabled(Level.INFO, null, null);
		}

		@Override
		public boolean isTraceEnabled() {
			return logger.isEnabled(Level.TRACE, null, null);
		}

		@Override
		public boolean isWarnEnabled() {
			return logger.isEnabled(Level.WARN, null, null);
		}

		@Override
		public void debug(Object message) {
			logger.logIfEnabled(FQCN, Level.DEBUG, null, message, null);
		}

		@Override
		public void debug(Object message, Throwable exception) {
			logger.logIfEnabled(FQCN, Level.DEBUG, null, message, exception);
		}

		@Override
		public void error(Object message) {
			logger.logIfEnabled(FQCN, Level.ERROR, null, message, null);
		}

		@Override
		public void error(Object message, Throwable exception) {
			logger.logIfEnabled(FQCN, Level.ERROR, null, message, exception);
		}

		@Override
		public void fatal(Object message) {
			logger.logIfEnabled(FQCN, Level.FATAL, null, message, null);
		}

		@Override
		public void fatal(Object message, Throwable exception) {
			logger.logIfEnabled(FQCN, Level.FATAL, null, message, exception);
		}

		@Override
		public void info(Object message) {
			logger.logIfEnabled(FQCN, Level.INFO, null, message, null);
		}

		@Override
		public void info(Object message, Throwable exception) {
			logger.logIfEnabled(FQCN, Level.INFO, null, message, exception);
		}

		@Override
		public void trace(Object message) {
			logger.logIfEnabled(FQCN, Level.TRACE, null, message, null);
		}

		@Override
		public void trace(Object message, Throwable exception) {
			logger.logIfEnabled(FQCN, Level.TRACE, null, message, exception);
		}

		@Override
		public void warn(Object message) {
			logger.logIfEnabled(FQCN, Level.WARN, null, message, null);
		}

		@Override
		public void warn(Object message, Throwable exception) {
			logger.logIfEnabled(FQCN, Level.WARN, null, message, exception);
		}
	}


	@SuppressWarnings("serial")
	private static class Slf4jLog implements Log, Serializable {

		private static final String FQCN = Slf4jLog.class.getName();

		private final String name;

		private transient Logger logger;

		private transient LocationAwareLogger locLogger;

		public Slf4jLog(String name) {
			this.name = name;
			this.logger = LoggerFactory.getLogger(this.name);
			this.locLogger = (this.logger instanceof LocationAwareLogger ? (LocationAwareLogger) this.logger : null);
		}

		public boolean isDebugEnabled() {
			return this.logger.isDebugEnabled();
		}

		public boolean isErrorEnabled() {
			return this.logger.isErrorEnabled();
		}

		public boolean isFatalEnabled() {
			return this.logger.isErrorEnabled();
		}

		public boolean isInfoEnabled() {
			return this.logger.isInfoEnabled();
		}

		public boolean isTraceEnabled() {
			return this.logger.isTraceEnabled();
		}

		public boolean isWarnEnabled() {
			return this.logger.isWarnEnabled();
		}

		public void debug(Object message) {
			if (this.locLogger != null) {
				this.locLogger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, String.valueOf(message), null, null);
			}
			else {
				this.logger.debug(String.valueOf(message));
			}
		}

		public void debug(Object message, Throwable exception) {
			if (this.locLogger != null) {
				this.locLogger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, String.valueOf(message), null, exception);
			}
			else {
				this.logger.debug(String.valueOf(message), exception);
			}
		}

		public void error(Object message) {
			if (this.locLogger != null) {
				this.locLogger.log(null, FQCN, LocationAwareLogger.ERROR_INT, String.valueOf(message), null, null);
			}
			else {
				this.logger.error(String.valueOf(message));
			}
		}

		public void error(Object message, Throwable exception) {
			if (this.locLogger != null) {
				this.locLogger.log(null, FQCN, LocationAwareLogger.ERROR_INT, String.valueOf(message), null, exception);
			}
			else {
				this.logger.error(String.valueOf(message), exception);
			}
		}

		public void fatal(Object message) {
			if (this.locLogger != null) {
				this.locLogger.log(null, FQCN, LocationAwareLogger.ERROR_INT, String.valueOf(message), null, null);
			}
			else {
				this.logger.error(String.valueOf(message));
			}
		}

		public void fatal(Object message, Throwable exception) {
			if (this.locLogger != null) {
				this.locLogger.log(null, FQCN, LocationAwareLogger.ERROR_INT, String.valueOf(message), null, exception);
			}
			else {
				this.logger.error(String.valueOf(message), exception);
			}
		}

		public void info(Object message) {
			if (this.locLogger != null) {
				this.locLogger.log(null, FQCN, LocationAwareLogger.INFO_INT, String.valueOf(message), null, null);
			}
			else {
				this.logger.info(String.valueOf(message));
			}
		}

		public void info(Object message, Throwable exception) {
			if (this.locLogger != null) {
				this.locLogger.log(null, FQCN, LocationAwareLogger.INFO_INT, String.valueOf(message), null, exception);
			}
			else {
				this.logger.info(String.valueOf(message), exception);
			}
		}

		public void trace(Object message) {
			if (this.locLogger != null) {
				this.locLogger.log(null, FQCN, LocationAwareLogger.TRACE_INT, String.valueOf(message), null, null);
			}
			else {
				this.logger.trace(String.valueOf(message));
			}
		}

		public void trace(Object message, Throwable exception) {
			if (this.locLogger != null) {
				this.locLogger.log(null, FQCN, LocationAwareLogger.TRACE_INT, String.valueOf(message), null, exception);
			}
			else {
				this.logger.trace(String.valueOf(message), exception);
			}
		}

		public void warn(Object message) {
			if (this.locLogger != null) {
				this.locLogger.log(null, FQCN, LocationAwareLogger.WARN_INT, String.valueOf(message), null, null);
			}
			else {
				this.logger.warn(String.valueOf(message));
			}
		}

		public void warn(Object message, Throwable exception) {
			if (this.locLogger != null) {
				this.locLogger.log(null, FQCN, LocationAwareLogger.WARN_INT, String.valueOf(message), null, exception);
			}
			else {
				this.logger.warn(String.valueOf(message), exception);
			}
		}

		protected Object readResolve() {
			return new Slf4jLog(this.name);
		}
	}


	@SuppressWarnings("serial")
	private static class JavaUtilLog implements Log, Serializable {

		private String name;

		private transient java.util.logging.Logger logger;

		public JavaUtilLog(String name) {
			this.name = name;
			this.logger = java.util.logging.Logger.getLogger(name);
		}

		public boolean isDebugEnabled() {
			return this.logger.isLoggable(java.util.logging.Level.FINE);
		}

		public boolean isErrorEnabled() {
			return this.logger.isLoggable(java.util.logging.Level.SEVERE);
		}

		public boolean isFatalEnabled() {
			return this.logger.isLoggable(java.util.logging.Level.SEVERE);
		}

		public boolean isInfoEnabled() {
			return this.logger.isLoggable(java.util.logging.Level.INFO);
		}

		public boolean isTraceEnabled() {
			return this.logger.isLoggable(java.util.logging.Level.FINEST);
		}

		public boolean isWarnEnabled() {
			return this.logger.isLoggable(java.util.logging.Level.WARNING);
		}

		public void debug(Object message) {
			log(java.util.logging.Level.FINE, message, null);
		}

		public void debug(Object message, Throwable exception) {
			log(java.util.logging.Level.FINE, message, exception);
		}

		public void error(Object message) {
			log(java.util.logging.Level.SEVERE, message, null);
		}

		public void error(Object message, Throwable exception) {
			log(java.util.logging.Level.SEVERE, message, exception);
		}

		public void fatal(Object message) {
			log(java.util.logging.Level.SEVERE, message, null);
		}

		public void fatal(Object message, Throwable exception) {
			log(java.util.logging.Level.SEVERE, message, exception);
		}

		public void info(Object message) {
			log(java.util.logging.Level.INFO, message, null);
		}

		public void info(Object message, Throwable exception) {
			log(java.util.logging.Level.INFO, message, exception);
		}

		public void trace(Object message) {
			log(java.util.logging.Level.FINEST, message, null);
		}

		public void trace(Object message, Throwable exception) {
			log(java.util.logging.Level.FINEST, message, exception);
		}

		public void warn(Object message) {
			log(java.util.logging.Level.WARNING, message, null);
		}

		public void warn(Object message, Throwable exception) {
			log(java.util.logging.Level.WARNING, message, exception);
		}

		private void log(java.util.logging.Level level, Object message, Throwable exception) {
			if (logger.isLoggable(level)) {
				LocationResolvingLogRecord rec = new LocationResolvingLogRecord(level, String.valueOf(message));
				rec.setLoggerName(this.name);
				rec.setResourceBundleName(logger.getResourceBundleName());
				rec.setResourceBundle(logger.getResourceBundle());
				rec.setThrown(exception);
				logger.log(rec);
			}
		}

		protected Object readResolve() {
			return new JavaUtilLog(this.name);
		}
	}


	@SuppressWarnings("serial")
	private static class LocationResolvingLogRecord extends java.util.logging.LogRecord {

		private static final String FQCN = JavaUtilLog.class.getName();

		private volatile boolean resolved;

		public LocationResolvingLogRecord(java.util.logging.Level level, String msg) {
			super(level, msg);
		}

		public String getSourceClassName() {
			if (!this.resolved) {
				resolve();
			}
			return super.getSourceClassName();
		}

		public void setSourceClassName(String sourceClassName) {
			super.setSourceClassName(sourceClassName);
			this.resolved = true;
		}

		public String getSourceMethodName() {
			if (!this.resolved) {
				resolve();
			}
			return super.getSourceMethodName();
		}

		public void setSourceMethodName(String sourceMethodName) {
			super.setSourceMethodName(sourceMethodName);
			this.resolved = true;
		}

		private void resolve() {
			StackTraceElement[] stack = new Throwable().getStackTrace();
			String sourceClassName = null;
			String sourceMethodName = null;
			boolean found = false;
			for (StackTraceElement element : stack) {
				String className = element.getClassName();
				if (FQCN.equals(className)) {
					found = true;
				}
				else if (found) {
					sourceClassName = className;
					sourceMethodName = element.getMethodName();
					break;
				}
			}
			setSourceClassName(sourceClassName);
			setSourceMethodName(sourceMethodName);
		}

		protected Object writeReplace() {
			java.util.logging.LogRecord serialized = new java.util.logging.LogRecord(getLevel(), getMessage());
			serialized.setLoggerName(getLoggerName());
			serialized.setResourceBundle(getResourceBundle());
			serialized.setResourceBundleName(getResourceBundleName());
			serialized.setSourceClassName(getSourceClassName());
			serialized.setSourceMethodName(getSourceMethodName());
			serialized.setSequenceNumber(getSequenceNumber());
			serialized.setParameters(getParameters());
			serialized.setThreadID(getThreadID());
			serialized.setMillis(getMillis());
			serialized.setThrown(getThrown());
			return serialized;
		}
	}

}

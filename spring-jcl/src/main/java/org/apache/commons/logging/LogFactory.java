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
import java.util.logging.LogRecord;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

/**
 * A minimal incarnation of Apache Commons Logging's {@code LogFactory} API,
 * providing just the common {@link Log} lookup methods. This is inspired
 * by the JCL-over-SLF4J bridge and should be source as well as binary
 * compatible with all common use of the Commons Logging API (in particular:
 * with {@code LogFactory.getLog(Class/String)} field initializers).
 *
 * <p>This implementation does not support Commons Logging's original provider
 * detection. It rather only checks for the presence of the Log4j 2.x API
 * and the SLF4J 1.7 API in the Spring Framework classpath, falling back to
 * {@code java.util.logging} if none of the two is available. In that sense,
 * it works as a replacement for the Log4j 2 Commons Logging bridge as well as
 * the JCL-over-SLF4J bridge, both of which become irrelevant for Spring-based
 * setups as a consequence (with no need for manual excludes of the standard
 * Commons Logging API jar anymore either). Furthermore, for simple setups
 * without an external logging provider, Spring does not require any extra jar
 * on the classpath anymore since this embedded log factory automatically
 * delegates to {@code java.util.logging} in such a scenario.
 *
 * <p><b>Note that this Commons Logging variant is only meant to be used for
 * infrastructure logging purposes in the core framework and in extensions.</b>
 * It also serves as a common bridge for third-party libraries using the
 * Commons Logging API, e.g. Apache HttpClient, Castor and HtmlUnit, bringing
 * them into the same consistent arrangement without any extra bridge jars.
 *
 * <p><b>For logging need in application code, prefer direct use of Log4j 2.x
 * or SLF4J or {@code java.util.logging}.</b> Simply put Log4j 2.x or Logback
 * (or another SLF4J provider) onto your classpath, without any extra bridges,
 * and let the framework auto-adapt to your choice.
 *
 * @author Juergen Hoeller (for the {@code spring-jcl} variant)
 * @since 5.0
 */
public abstract class LogFactory {

	private static LogApi logApi = LogApi.JUL;

	static {
		ClassLoader cl = LogFactory.class.getClassLoader();
		try {
			// Try Log4j 2.x API
			cl.loadClass("org.apache.logging.log4j.spi.ExtendedLogger");
			logApi = LogApi.LOG4J;
		}
		catch (ClassNotFoundException ex1) {
			try {
				// Try SLF4J 1.7 SPI
				cl.loadClass("org.slf4j.spi.LocationAwareLogger");
				logApi = LogApi.SLF4J_LAL;
			}
			catch (ClassNotFoundException ex2) {
				try {
					// Try SLF4J 1.7 API
					cl.loadClass("org.slf4j.Logger");
					logApi = LogApi.SLF4J;
				}
				catch (ClassNotFoundException ex3) {
					// Keep java.util.logging as default
				}
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
			case SLF4J_LAL:
				return Slf4jDelegate.createLocationAwareLog(name);
			case SLF4J:
				return Slf4jDelegate.createLog(name);
			default:
				// Defensively use lazy-initializing delegate class here as well since the
				// java.logging module is not present by default on JDK 9. We are requiring
				// its presence if neither Log4j nor SLF4J is available; however, in the
				// case of Log4j or SLF4J, we are trying to prevent early initialization
				// of the JavaUtilLog adapter - e.g. by a JVM in debug mode - when eagerly
				// trying to parse the bytecode for all the cases of this switch clause.
				return JavaUtilDelegate.createLog(name);
		}
	}

	/**
	 * This method only exists for compatibility with unusual Commons Logging API
	 * usage like e.g. {@code LogFactory.getFactory().getInstance(Class/String)}.
	 * @see #getInstance(Class)
	 * @see #getInstance(String)
	 * @deprecated in favor of {@link #getLog(Class)}/{@link #getLog(String)}
	 */
	@Deprecated
	public static LogFactory getFactory() {
		return new LogFactory() {};
	}

	/**
	 * Convenience method to return a named logger.
	 * <p>This variant just dispatches straight to {@link #getLog(Class)}.
	 * @param clazz containing Class from which a log name will be derived
	 * @deprecated in favor of {@link #getLog(Class)}
	 */
	@Deprecated
	public Log getInstance(Class<?> clazz) {
		return getLog(clazz);
	}

	/**
	 * Convenience method to return a named logger.
	 * <p>This variant just dispatches straight to {@link #getLog(String)}.
	 * @param name logical name of the <code>Log</code> instance to be returned
	 * @deprecated in favor of {@link #getLog(String)}
	 */
	@Deprecated
	public Log getInstance(String name) {
		return getLog(name);
	}


	private enum LogApi {LOG4J, SLF4J_LAL, SLF4J, JUL}


	private static class Log4jDelegate {

		public static Log createLog(String name) {
			return new Log4jLog(name);
		}
	}


	private static class Slf4jDelegate {

		public static Log createLocationAwareLog(String name) {
			Logger logger = LoggerFactory.getLogger(name);
			return (logger instanceof LocationAwareLogger ?
					new Slf4jLocationAwareLog((LocationAwareLogger) logger) : new Slf4jLog<>(logger));
		}

		public static Log createLog(String name) {
			return new Slf4jLog<>(LoggerFactory.getLogger(name));
		}
	}


	private static class JavaUtilDelegate {

		public static Log createLog(String name) {
			return new JavaUtilLog(name);
		}
	}


	@SuppressWarnings("serial")
	private static class Log4jLog implements Log, Serializable {

		private static final String FQCN = Log4jLog.class.getName();

		private static final LoggerContext loggerContext =
				LogManager.getContext(Log4jLog.class.getClassLoader(), false);

		private final ExtendedLogger logger;

		public Log4jLog(String name) {
			this.logger = loggerContext.getLogger(name);
		}

		@Override
		public boolean isFatalEnabled() {
			return this.logger.isEnabled(Level.FATAL);
		}

		@Override
		public boolean isErrorEnabled() {
			return this.logger.isEnabled(Level.ERROR);
		}

		@Override
		public boolean isWarnEnabled() {
			return this.logger.isEnabled(Level.WARN);
		}

		@Override
		public boolean isInfoEnabled() {
			return this.logger.isEnabled(Level.INFO);
		}

		@Override
		public boolean isDebugEnabled() {
			return this.logger.isEnabled(Level.DEBUG);
		}

		@Override
		public boolean isTraceEnabled() {
			return this.logger.isEnabled(Level.TRACE);
		}

		@Override
		public void fatal(Object message) {
			log(Level.FATAL, message, null);
		}

		@Override
		public void fatal(Object message, Throwable exception) {
			log(Level.FATAL, message, exception);
		}

		@Override
		public void error(Object message) {
			log(Level.ERROR, message, null);
		}

		@Override
		public void error(Object message, Throwable exception) {
			log(Level.ERROR, message, exception);
		}

		@Override
		public void warn(Object message) {
			log(Level.WARN, message, null);
		}

		@Override
		public void warn(Object message, Throwable exception) {
			log(Level.WARN, message, exception);
		}

		@Override
		public void info(Object message) {
			log(Level.INFO, message, null);
		}

		@Override
		public void info(Object message, Throwable exception) {
			log(Level.INFO, message, exception);
		}

		@Override
		public void debug(Object message) {
			log(Level.DEBUG, message, null);
		}

		@Override
		public void debug(Object message, Throwable exception) {
			log(Level.DEBUG, message, exception);
		}

		@Override
		public void trace(Object message) {
			log(Level.TRACE, message, null);
		}

		@Override
		public void trace(Object message, Throwable exception) {
			log(Level.TRACE, message, exception);
		}

		private void log(Level level, Object message, Throwable exception) {
			if (message instanceof String) {
				// Explicitly pass a String argument, avoiding Log4j's argument expansion
				// for message objects in case of "{}" sequences (SPR-16226)
				if (exception != null) {
					this.logger.logIfEnabled(FQCN, level, null, (String) message, exception);
				}
				else {
					this.logger.logIfEnabled(FQCN, level, null, (String) message);
				}
			}
			else {
				this.logger.logIfEnabled(FQCN, level, null, message, exception);
			}
		}
	}


	@SuppressWarnings("serial")
	private static class Slf4jLog<T extends Logger> implements Log, Serializable {

		protected final String name;

		protected transient T logger;

		public Slf4jLog(T logger) {
			this.name = logger.getName();
			this.logger = logger;
		}

		public boolean isFatalEnabled() {
			return isErrorEnabled();
		}

		public boolean isErrorEnabled() {
			return this.logger.isErrorEnabled();
		}

		public boolean isWarnEnabled() {
			return this.logger.isWarnEnabled();
		}

		public boolean isInfoEnabled() {
			return this.logger.isInfoEnabled();
		}

		public boolean isDebugEnabled() {
			return this.logger.isDebugEnabled();
		}

		public boolean isTraceEnabled() {
			return this.logger.isTraceEnabled();
		}

		public void fatal(Object message) {
			error(message);
		}

		public void fatal(Object message, Throwable exception) {
			error(message, exception);
		}

		public void error(Object message) {
			if (message instanceof String || this.logger.isErrorEnabled()) {
				this.logger.error(String.valueOf(message));
			}
		}

		public void error(Object message, Throwable exception) {
			if (message instanceof String || this.logger.isErrorEnabled()) {
				this.logger.error(String.valueOf(message), exception);
			}
		}

		public void warn(Object message) {
			if (message instanceof String || this.logger.isWarnEnabled()) {
				this.logger.warn(String.valueOf(message));
			}
		}

		public void warn(Object message, Throwable exception) {
			if (message instanceof String || this.logger.isWarnEnabled()) {
				this.logger.warn(String.valueOf(message), exception);
			}
		}

		public void info(Object message) {
			if (message instanceof String || this.logger.isInfoEnabled()) {
				this.logger.info(String.valueOf(message));
			}
		}

		public void info(Object message, Throwable exception) {
			if (message instanceof String || this.logger.isInfoEnabled()) {
				this.logger.info(String.valueOf(message), exception);
			}
		}

		public void debug(Object message) {
			if (message instanceof String || this.logger.isDebugEnabled()) {
				this.logger.debug(String.valueOf(message));
			}
		}

		public void debug(Object message, Throwable exception) {
			if (message instanceof String || this.logger.isDebugEnabled()) {
				this.logger.debug(String.valueOf(message), exception);
			}
		}

		public void trace(Object message) {
			if (message instanceof String || this.logger.isTraceEnabled()) {
				this.logger.trace(String.valueOf(message));
			}
		}

		public void trace(Object message, Throwable exception) {
			if (message instanceof String || this.logger.isTraceEnabled()) {
				this.logger.trace(String.valueOf(message), exception);
			}
		}

		protected Object readResolve() {
			return Slf4jDelegate.createLog(this.name);
		}
	}


	@SuppressWarnings("serial")
	private static class Slf4jLocationAwareLog extends Slf4jLog<LocationAwareLogger> implements Serializable {

		private static final String FQCN = Slf4jLocationAwareLog.class.getName();

		public Slf4jLocationAwareLog(LocationAwareLogger logger) {
			super(logger);
		}

		@Override
		public void fatal(Object message) {
			error(message);
		}

		@Override
		public void fatal(Object message, Throwable exception) {
			error(message, exception);
		}

		@Override
		public void error(Object message) {
			if (message instanceof String || this.logger.isErrorEnabled()) {
				this.logger.log(null, FQCN, LocationAwareLogger.ERROR_INT, String.valueOf(message), null, null);
			}
		}

		@Override
		public void error(Object message, Throwable exception) {
			if (message instanceof String || this.logger.isErrorEnabled()) {
				this.logger.log(null, FQCN, LocationAwareLogger.ERROR_INT, String.valueOf(message), null, exception);
			}
		}

		@Override
		public void warn(Object message) {
			if (message instanceof String || this.logger.isWarnEnabled()) {
				this.logger.log(null, FQCN, LocationAwareLogger.WARN_INT, String.valueOf(message), null, null);
			}
		}

		@Override
		public void warn(Object message, Throwable exception) {
			if (message instanceof String || this.logger.isWarnEnabled()) {
				this.logger.log(null, FQCN, LocationAwareLogger.WARN_INT, String.valueOf(message), null, exception);
			}
		}

		@Override
		public void info(Object message) {
			if (message instanceof String || this.logger.isInfoEnabled()) {
				this.logger.log(null, FQCN, LocationAwareLogger.INFO_INT, String.valueOf(message), null, null);
			}
		}

		@Override
		public void info(Object message, Throwable exception) {
			if (message instanceof String || this.logger.isInfoEnabled()) {
				this.logger.log(null, FQCN, LocationAwareLogger.INFO_INT, String.valueOf(message), null, exception);
			}
		}

		@Override
		public void debug(Object message) {
			if (message instanceof String || this.logger.isDebugEnabled()) {
				this.logger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, String.valueOf(message), null, null);
			}
		}

		@Override
		public void debug(Object message, Throwable exception) {
			if (message instanceof String || this.logger.isDebugEnabled()) {
				this.logger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, String.valueOf(message), null, exception);
			}
		}

		@Override
		public void trace(Object message) {
			if (message instanceof String || this.logger.isTraceEnabled()) {
				this.logger.log(null, FQCN, LocationAwareLogger.TRACE_INT, String.valueOf(message), null, null);
			}
		}

		@Override
		public void trace(Object message, Throwable exception) {
			if (message instanceof String || this.logger.isTraceEnabled()) {
				this.logger.log(null, FQCN, LocationAwareLogger.TRACE_INT, String.valueOf(message), null, exception);
			}
		}

		@Override
		protected Object readResolve() {
			return Slf4jDelegate.createLocationAwareLog(this.name);
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

		public boolean isFatalEnabled() {
			return isErrorEnabled();
		}

		public boolean isErrorEnabled() {
			return this.logger.isLoggable(java.util.logging.Level.SEVERE);
		}

		public boolean isWarnEnabled() {
			return this.logger.isLoggable(java.util.logging.Level.WARNING);
		}

		public boolean isInfoEnabled() {
			return this.logger.isLoggable(java.util.logging.Level.INFO);
		}

		public boolean isDebugEnabled() {
			return this.logger.isLoggable(java.util.logging.Level.FINE);
		}

		public boolean isTraceEnabled() {
			return this.logger.isLoggable(java.util.logging.Level.FINEST);
		}

		public void fatal(Object message) {
			error(message);
		}

		public void fatal(Object message, Throwable exception) {
			error(message, exception);
		}

		public void error(Object message) {
			log(java.util.logging.Level.SEVERE, message, null);
		}

		public void error(Object message, Throwable exception) {
			log(java.util.logging.Level.SEVERE, message, exception);
		}

		public void warn(Object message) {
			log(java.util.logging.Level.WARNING, message, null);
		}

		public void warn(Object message, Throwable exception) {
			log(java.util.logging.Level.WARNING, message, exception);
		}

		public void info(Object message) {
			log(java.util.logging.Level.INFO, message, null);
		}

		public void info(Object message, Throwable exception) {
			log(java.util.logging.Level.INFO, message, exception);
		}

		public void debug(Object message) {
			log(java.util.logging.Level.FINE, message, null);
		}

		public void debug(Object message, Throwable exception) {
			log(java.util.logging.Level.FINE, message, exception);
		}

		public void trace(Object message) {
			log(java.util.logging.Level.FINEST, message, null);
		}

		public void trace(Object message, Throwable exception) {
			log(java.util.logging.Level.FINEST, message, exception);
		}

		private void log(java.util.logging.Level level, Object message, Throwable exception) {
			if (logger.isLoggable(level)) {
				LogRecord rec;
				if (message instanceof LogRecord) {
					rec = (LogRecord) message;
				}
				else {
					rec = new LocationResolvingLogRecord(level, String.valueOf(message));
					rec.setLoggerName(this.name);
					rec.setResourceBundleName(logger.getResourceBundleName());
					rec.setResourceBundle(logger.getResourceBundle());
					rec.setThrown(exception);
				}
				logger.log(rec);
			}
		}

		protected Object readResolve() {
			return new JavaUtilLog(this.name);
		}
	}


	@SuppressWarnings("serial")
	private static class LocationResolvingLogRecord extends LogRecord {

		private static final String FQCN = JavaUtilLog.class.getName();

		private volatile boolean resolved;

		public LocationResolvingLogRecord(java.util.logging.Level level, String msg) {
			super(level, msg);
		}

		@Override
		public String getSourceClassName() {
			if (!this.resolved) {
				resolve();
			}
			return super.getSourceClassName();
		}

		@Override
		public void setSourceClassName(String sourceClassName) {
			super.setSourceClassName(sourceClassName);
			this.resolved = true;
		}

		@Override
		public String getSourceMethodName() {
			if (!this.resolved) {
				resolve();
			}
			return super.getSourceMethodName();
		}

		@Override
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

		@SuppressWarnings("deprecation")  // setMillis is deprecated in JDK 9
		protected Object writeReplace() {
			LogRecord serialized = new LogRecord(getLevel(), getMessage());
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

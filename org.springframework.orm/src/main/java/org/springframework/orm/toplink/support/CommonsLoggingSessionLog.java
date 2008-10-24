/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.orm.toplink.support;

import java.lang.reflect.Method;

import oracle.toplink.internal.databaseaccess.Accessor;
import oracle.toplink.logging.AbstractSessionLog;
import oracle.toplink.logging.SessionLogEntry;
import oracle.toplink.publicinterface.Session;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ReflectionUtils;

/**
 * TopLink 10.1.3+ SessionLog implementation that logs through Commons Logging.
 *
 * <p>The namespace used is "oracle.toplink.xxx", with the latter part being
 * the TopLink log category ("sql"/"transaction"/etc). In case of no category
 * given, "session" will be used as default. This allows for fine-grained
 * filtering of log messages, for example through Log4J configuration.
 *
 * <p>Maps TopLink's SEVERE level to CL ERROR, TopLink's WARNING to CL WARN,
 * TopLink's INFO to CL INFO, TopLink's CONFIG/FINE/FINER to CL DEBUG,
 * and TopLink's FINEST to CL TRACE. This results in common CL log behavior:
 * INFO logging only at startup; operation logging at DEBUG level. Debug logging
 * can be further filtered according to categories: for example, activate Log4J
 * DEBUG logging for category "oracle.toplink.sql" to see the generated SQL.
 *
 * <p><b>Note:</b> This implementation will only work on TopLink 10.1.3 or higher,
 * as it is built against TopLink's new SessionLog facilities in the
 * <code>oracle.toplink.logging</code> package, supporting log categories.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see CommonsLoggingSessionLog904
 * @see oracle.toplink.logging.JavaLog
 * @see org.springframework.orm.toplink.LocalSessionFactoryBean#setSessionLog
 */
public class CommonsLoggingSessionLog extends AbstractSessionLog {

	public static final String NAMESPACE_PREFIX = "oracle.toplink.";

	public static final String DEFAULT_NAMESPACE = "session";

	public static final String DEFAULT_SEPARATOR = "--";


	private static Method getSessionMethod;

	private static Method getExceptionMethod;

	static {
		try {
			getSessionMethod = SessionLogEntry.class.getMethod("getSession", new Class[0]);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Could not find method SessionLogEntry.getSession()");
		}
		try {
			getExceptionMethod = SessionLogEntry.class.getMethod("getException", new Class[0]);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Could not find method SessionLogEntry.getException()");
		}
	}


	private String separator = DEFAULT_SEPARATOR;


	/**
	 * Specify the separator between TopLink's supplemental details
	 * (session, connection) and the log message itself. Default is "--".
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * Return the separator between TopLink's supplemental details
	 * (session, connection) and the log message itself. Default is "--".
	 */
	public String getSeparator() {
		return this.separator;
	}


	public void log(SessionLogEntry entry) {
		Log logger = LogFactory.getLog(getCategory(entry));
		switch (entry.getLevel()) {
			case SEVERE:
				if (logger.isErrorEnabled()) {
					if (entry.hasException()) {
						logger.error(getMessageString(entry), getException(entry));
					}
					else {
						logger.error(getMessageString(entry));
					}
				}
				break;
			case WARNING:
				if (logger.isWarnEnabled()) {
					if (entry.hasException()) {
						logger.warn(getMessageString(entry), getException(entry));
					}
					else {
						logger.warn(getMessageString(entry));
					}
				}
				break;
			case INFO:
				if (logger.isInfoEnabled()) {
					if (entry.hasException()) {
						logger.info(getMessageString(entry), getException(entry));
					}
					else {
						logger.info(getMessageString(entry));
					}
				}
				break;
			case CONFIG:
			case FINE:
			case FINER:
				if (logger.isDebugEnabled()) {
					if (entry.hasException()) {
						logger.debug(getMessageString(entry), getException(entry));
					}
					else {
						logger.debug(getMessageString(entry));
					}
				}
				break;
			case FINEST:
				if (logger.isTraceEnabled()) {
					if (entry.hasException()) {
						logger.trace(getMessageString(entry), getException(entry));
					}
					else {
						logger.trace(getMessageString(entry));
					}
				}
				break;
		}
	}

	/**
	 * Determine the log category for the given log entry.
	 * <p>If the entry carries a name space value, it will be appended
	 * to the "oracle.toplink." prefix; else, "oracle.toplink.session"
	 * will be used.
	 */
	protected String getCategory(SessionLogEntry entry) {
		String namespace = entry.getNameSpace();
		return NAMESPACE_PREFIX + (namespace != null ? namespace : DEFAULT_NAMESPACE);
	}

	/**
	 * Build the message String for the given log entry, including the
	 * supplemental details (session, connection) and the formatted message.
	 * @see #getSessionString(oracle.toplink.sessions.Session)
	 * @see #getConnectionString(oracle.toplink.internal.databaseaccess.Accessor)
	 * @see #formatMessage(oracle.toplink.logging.SessionLogEntry)
	 * @see #getSeparator()
	 */
	protected String getMessageString(SessionLogEntry entry) {
		StringBuffer buf = new StringBuffer();
		Session session = getSession(entry);
		if (session != null) {
			buf.append(getSessionString(session));
			buf.append(getSeparator());
		}
		Accessor connection = entry.getConnection();
		if (connection != null) {
			buf.append(getConnectionString(connection));
			buf.append(getSeparator());
		}
		buf.append(formatMessage(entry));
		return buf.toString();
	}

	/**
	 * Extract the exception from the given log entry.
	 * <p>The default implementation calls <code>SessionLogEntry.getSession</code>
	 * via reflection: The return type varies between TopLink 10.1.3 and 11
	 * (<code>Session</code> vs <code>AbstractSession</code>, respectively).
	 */
	protected Session getSession(SessionLogEntry entry) {
		return (Session) ReflectionUtils.invokeMethod(getSessionMethod, entry);
	}

	/**
	 * Extract the exception from the given log entry.
	 * <p>The default implementation calls <code>SessionLogEntry.getException</code>
	 * via reflection: The return type varies between TopLink 9.0.4 and 10.1.3
	 * (<code>Exception</code> vs <code>Throwable</code>, respectively).
	 */
	protected Throwable getException(SessionLogEntry entry) {
		return (Throwable) ReflectionUtils.invokeMethod(getExceptionMethod, entry);
	}

}

/*
 * Copyright 2002-2005 the original author or authors.
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

import oracle.toplink.sessions.DefaultSessionLog;
import oracle.toplink.sessions.Session;
import oracle.toplink.sessions.SessionLogEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TopLink 9.0.4 SessionLog implementation that logs through Commons Logging.
 *
 * <p>The namespace used is "oracle.toplink.session". Fine-grained filtering
 * of log messages, for example through Log4J configuration, is <i>not</i>
 * available on TopLink 9.0.4: Consider upgrading to TopLink 10.1.3 and
 * using the CommonsLoggingSessionLog class instead.
 *
 * <p>TopLink log entries with exceptions are logged at CL WARN level,
 * TopLink debug log entries at CL TRACE level, and any other log entry
 * at CL DEBUG level. Finer-grained mapping to log levels is unfortunately
 * not possible on TopLink 9.0.4.
 *
 * <p><b>Note:</b> This implementation will only actually work on TopLink 9.0.4,
 * as it is built against TopLink's old SessionLog facilities in the
 * <code>oracle.toplink.sessions</code> package, which are effectively
 * obsolete (deprecated and bypassed) as of TopLink 10.1.3.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see CommonsLoggingSessionLog
 * @see oracle.toplink.sessions.DefaultSessionLog
 * @see org.springframework.orm.toplink.LocalSessionFactoryBean#setSessionLog
 */
public class CommonsLoggingSessionLog904 extends DefaultSessionLog {

	public static final String NAMESPACE = "oracle.toplink.session";

	public static final String DEFAULT_SEPARATOR = "--";


	private final Log logger = LogFactory.getLog(NAMESPACE);

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
		return separator;
	}


	public void log(SessionLogEntry entry) {
		if (entry.hasException()) {
			if (shouldLogExceptions() && logger.isWarnEnabled()) {
				this.logger.warn(getMessageString(entry), entry.getException());
			}
		}
		else if (entry.isDebug()) {
			if (shouldLogDebug() && logger.isTraceEnabled()) {
				this.logger.trace(getMessageString(entry));
			}
		}
		else {
			if (logger.isDebugEnabled()) {
				this.logger.debug(getMessageString(entry));
			}
		}
	}

	/**
	 * Build the message String for the given log entry, including the
	 * supplemental details (session, connection) and the message text.
	 * @see #getSeparator()
	 */
	protected String getMessageString(SessionLogEntry entry) {
		StringBuffer buf = new StringBuffer();
		if (shouldPrintSession()) {
			buf.append(getSessionName(entry.getSession()));
			buf.append("(");
			buf.append(String.valueOf(System.identityHashCode(entry.getSession())));
			buf.append(")");
			buf.append(getSeparator());
		}
		if (shouldPrintConnection() && entry.getConnection() != null) {
			buf.append("Connection");
			buf.append("(");
			buf.append(String.valueOf(System.identityHashCode(entry.getConnection())));
			buf.append(")");
			buf.append(getSeparator());
		}
		buf.append(entry.getMessage());
		return buf.toString();
	}

	/**
	 * Return the name to be used for the given Session
	 * ("UnitOfWork"/"ServerSession"/"ClientSession"/etc).
	 */
	protected String getSessionName(Session session) {
		if (session.isUnitOfWork()) {
			return "UnitOfWork";
		}
		if (session.isServerSession()) {
			return "ServerSession";
		}
		if (session.isClientSession()) {
			return "ClientSession";
		}
		if (session.isSessionBroker()) {
			return "SessionBroker";
		}
		if (session.isRemoteSession()) {
			return "RemoteSession";
		}
		if (session.isDatabaseSession()) {
			return "DatabaseSession";
		}
		return "Session";
	}

}

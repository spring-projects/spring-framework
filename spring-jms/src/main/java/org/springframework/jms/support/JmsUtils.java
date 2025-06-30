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

package org.springframework.jms.support;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.QueueBrowser;
import jakarta.jms.QueueRequestor;
import jakarta.jms.Session;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.jms.InvalidClientIDException;
import org.springframework.jms.InvalidDestinationException;
import org.springframework.jms.InvalidSelectorException;
import org.springframework.jms.JmsException;
import org.springframework.jms.JmsSecurityException;
import org.springframework.jms.MessageEOFException;
import org.springframework.jms.MessageFormatException;
import org.springframework.jms.MessageNotReadableException;
import org.springframework.jms.MessageNotWriteableException;
import org.springframework.jms.ResourceAllocationException;
import org.springframework.jms.TransactionInProgressException;
import org.springframework.jms.TransactionRolledBackException;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.util.Assert;

/**
 * Generic utility methods for working with JMS. Mainly for internal use
 * within the framework, but also useful for custom JMS access code.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.1
 */
public abstract class JmsUtils {

	private static final Log logger = LogFactory.getLog(JmsUtils.class);


	/**
	 * Close the given JMS Connection and ignore any thrown exception.
	 * <p>This is useful for typical {@code finally} blocks in manual JMS code.
	 * @param con the JMS Connection to close (may be {@code null})
	 */
	public static void closeConnection(@Nullable Connection con) {
		closeConnection(con, false);
	}

	/**
	 * Close the given JMS Connection and ignore any thrown exception.
	 * <p>This is useful for typical {@code finally} blocks in manual JMS code.
	 * @param con the JMS Connection to close (may be {@code null})
	 * @param stop whether to call {@code stop()} before closing
	 */
	public static void closeConnection(@Nullable Connection con, boolean stop) {
		if (con != null) {
			try {
				if (stop) {
					try (con) {
						con.stop();
					}
				}
				else {
					con.close();
				}
			}
			catch (jakarta.jms.IllegalStateException ex) {
				logger.debug("Ignoring Connection state exception - assuming already closed: " + ex);
			}
			catch (JMSException ex) {
				logger.debug("Could not close JMS Connection", ex);
			}
			catch (Throwable ex) {
				// We don't trust the JMS provider: It might throw RuntimeException or Error.
				logger.debug("Unexpected exception on closing JMS Connection", ex);
			}
		}
	}

	/**
	 * Close the given JMS Session and ignore any thrown exception.
	 * <p>This is useful for typical {@code finally} blocks in manual JMS code.
	 * @param session the JMS Session to close (may be {@code null})
	 */
	public static void closeSession(@Nullable Session session) {
		if (session != null) {
			try {
				session.close();
			}
			catch (JMSException ex) {
				logger.trace("Could not close JMS Session", ex);
			}
			catch (Throwable ex) {
				// We don't trust the JMS provider: It might throw RuntimeException or Error.
				logger.trace("Unexpected exception on closing JMS Session", ex);
			}
		}
	}

	/**
	 * Close the given JMS MessageProducer and ignore any thrown exception.
	 * <p>This is useful for typical {@code finally} blocks in manual JMS code.
	 * @param producer the JMS MessageProducer to close (may be {@code null})
	 */
	public static void closeMessageProducer(@Nullable MessageProducer producer) {
		if (producer != null) {
			try {
				producer.close();
			}
			catch (JMSException ex) {
				logger.trace("Could not close JMS MessageProducer", ex);
			}
			catch (Throwable ex) {
				// We don't trust the JMS provider: It might throw RuntimeException or Error.
				logger.trace("Unexpected exception on closing JMS MessageProducer", ex);
			}
		}
	}

	/**
	 * Close the given JMS MessageConsumer and ignore any thrown exception.
	 * <p>This is useful for typical {@code finally} blocks in manual JMS code.
	 * @param consumer the JMS MessageConsumer to close (may be {@code null})
	 */
	public static void closeMessageConsumer(@Nullable MessageConsumer consumer) {
		if (consumer != null) {
			// Clear interruptions to ensure that the consumer closes successfully...
			// (working around misbehaving JMS providers such as ActiveMQ)
			boolean wasInterrupted = Thread.interrupted();
			try {
				consumer.close();
			}
			catch (JMSException ex) {
				logger.trace("Could not close JMS MessageConsumer", ex);
			}
			catch (Throwable ex) {
				// We don't trust the JMS provider: It might throw RuntimeException or Error.
				logger.trace("Unexpected exception on closing JMS MessageConsumer", ex);
			}
			finally {
				if (wasInterrupted) {
					// Reset the interrupted flag as it was before.
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	/**
	 * Close the given JMS QueueBrowser and ignore any thrown exception.
	 * <p>This is useful for typical {@code finally} blocks in manual JMS code.
	 * @param browser the JMS QueueBrowser to close (may be {@code null})
	 */
	public static void closeQueueBrowser(@Nullable QueueBrowser browser) {
		if (browser != null) {
			try {
				browser.close();
			}
			catch (JMSException ex) {
				logger.trace("Could not close JMS QueueBrowser", ex);
			}
			catch (Throwable ex) {
				// We don't trust the JMS provider: It might throw RuntimeException or Error.
				logger.trace("Unexpected exception on closing JMS QueueBrowser", ex);
			}
		}
	}

	/**
	 * Close the given JMS QueueRequestor and ignore any thrown exception.
	 * <p>This is useful for typical {@code finally} blocks in manual JMS code.
	 * @param requestor the JMS QueueRequestor to close (may be {@code null})
	 */
	public static void closeQueueRequestor(@Nullable QueueRequestor requestor) {
		if (requestor != null) {
			try {
				requestor.close();
			}
			catch (JMSException ex) {
				logger.trace("Could not close JMS QueueRequestor", ex);
			}
			catch (Throwable ex) {
				// We don't trust the JMS provider: It might throw RuntimeException or Error.
				logger.trace("Unexpected exception on closing JMS QueueRequestor", ex);
			}
		}
	}

	/**
	 * Commit the Session if not within a JTA transaction.
	 * @param session the JMS Session to commit
	 * @throws JMSException if committing failed
	 */
	public static void commitIfNecessary(Session session) throws JMSException {
		Assert.notNull(session, "Session must not be null");
		try {
			session.commit();
		}
		catch (jakarta.jms.TransactionInProgressException ex) {
			// Ignore -> can only happen in case of a JTA transaction.
		}
	}

	/**
	 * Roll back the Session if not within a JTA transaction.
	 * @param session the JMS Session to rollback
	 * @throws JMSException if committing failed
	 */
	public static void rollbackIfNecessary(Session session) throws JMSException {
		Assert.notNull(session, "Session must not be null");
		try {
			session.rollback();
		}
		catch (jakarta.jms.TransactionInProgressException ex) {
			// Ignore -> can only happen in case of a JTA transaction.
		}
	}

	/**
	 * Build a descriptive exception message for the given JMSException,
	 * incorporating a linked exception's message if appropriate.
	 * @param ex the JMSException to build a message for
	 * @return the descriptive message String
	 * @see jakarta.jms.JMSException#getLinkedException()
	 */
	public static @Nullable String buildExceptionMessage(JMSException ex) {
		String message = ex.getMessage();
		Exception linkedEx = ex.getLinkedException();
		if (linkedEx != null) {
			if (message == null) {
				message = linkedEx.toString();
			}
			else {
				String linkedMessage = linkedEx.getMessage();
				if (linkedMessage != null && !message.contains(linkedMessage)) {
					message = message + "; nested exception is " + linkedEx;
				}
			}
		}
		return message;
	}

	/**
	 * Convert the specified checked {@link jakarta.jms.JMSException JMSException} to a
	 * Spring runtime {@link org.springframework.jms.JmsException JmsException} equivalent.
	 * @param ex the original checked JMSException to convert
	 * @return the Spring runtime JmsException wrapping the given exception
	 */
	public static JmsException convertJmsAccessException(JMSException ex) {
		Assert.notNull(ex, "JMSException must not be null");

		if (ex instanceof jakarta.jms.IllegalStateException jakartaISE) {
			return new org.springframework.jms.IllegalStateException(jakartaISE);
		}
		if (ex instanceof jakarta.jms.InvalidClientIDException jakartaICIDE) {
			return new InvalidClientIDException(jakartaICIDE);
		}
		if (ex instanceof jakarta.jms.InvalidDestinationException jakartaIDE) {
			return new InvalidDestinationException(jakartaIDE);
		}
		if (ex instanceof jakarta.jms.InvalidSelectorException jakartaISE) {
			return new InvalidSelectorException(jakartaISE);
		}
		if (ex instanceof jakarta.jms.JMSSecurityException jakartaJMSSE) {
			return new JmsSecurityException(jakartaJMSSE);
		}
		if (ex instanceof jakarta.jms.MessageEOFException jakartaMEOFE) {
			return new MessageEOFException(jakartaMEOFE);
		}
		if (ex instanceof jakarta.jms.MessageFormatException jakartaMFE) {
			return new MessageFormatException(jakartaMFE);
		}
		if (ex instanceof jakarta.jms.MessageNotReadableException jakartaMNRE) {
			return new MessageNotReadableException(jakartaMNRE);
		}
		if (ex instanceof jakarta.jms.MessageNotWriteableException jakartaMNWE) {
			return new MessageNotWriteableException(jakartaMNWE);
		}
		if (ex instanceof jakarta.jms.ResourceAllocationException jakartaRAE) {
			return new ResourceAllocationException(jakartaRAE);
		}
		if (ex instanceof jakarta.jms.TransactionInProgressException jakartaTIPE) {
			return new TransactionInProgressException(jakartaTIPE);
		}
		if (ex instanceof jakarta.jms.TransactionRolledBackException jakartaTRBE) {
			return new TransactionRolledBackException(jakartaTRBE);
		}

		// fallback
		return new UncategorizedJmsException(ex);
	}

}

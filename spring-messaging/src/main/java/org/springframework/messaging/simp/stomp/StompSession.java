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

package org.springframework.messaging.simp.stomp;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * Represents a STOMP session with operations to send messages,
 * create subscriptions and receive messages on those subscriptions.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface StompSession {

	/**
	 * Return the id for the session.
	 */
	String getSessionId();

	/**
	 * Whether the session is connected.
	 */
	boolean isConnected();

	/**
	 * When enabled, a receipt header is automatically added to future
	 * {@code send} and {@code subscribe} operations on this session, which
	 * causes the server to return a RECEIPT. An application can then use
	 * the {@link StompSession.Receiptable Receiptable} returned from the
	 * operation to track the receipt.
	 * <p>A receipt header can also be added manually through the overloaded
	 * methods that accept {@code StompHeaders}.
	 */
	void setAutoReceipt(boolean enabled);

	/**
	 * Send a message to the specified destination, converting the payload to a
	 * {@code byte[]} with the help of a
	 * {@link org.springframework.messaging.converter.MessageConverter MessageConverter}.
	 * @param destination the destination to send a message to
	 * @param payload the message payload
	 * @return a Receiptable for tracking receipts
	 */
	Receiptable send(String destination, Object payload);

	/**
	 * An overloaded version of {@link #send(String, Object)} with full
	 * {@link StompHeaders} instead of just a destination. The headers must
	 * contain a destination and may also have other headers such as
	 * "content-type" or custom headers for the broker to propagate to
	 * subscribers, or broker-specific, non-standard headers.
	 * @param headers the message headers
	 * @param payload the message payload
	 * @return a Receiptable for tracking receipts
	 */
	Receiptable send(StompHeaders headers, Object payload);

	/**
	 * Subscribe to the given destination by sending a SUBSCRIBE frame and handle
	 * received messages with the specified {@link StompFrameHandler}.
	 * @param destination the destination to subscribe to
	 * @param handler the handler for received messages
	 * @return a handle to use to unsubscribe and/or track receipts
	 */
	Subscription subscribe(String destination, StompFrameHandler handler);

	/**
	 * An overloaded version of {@link #subscribe(String, StompFrameHandler)}
	 * with full {@link StompHeaders} instead of just a destination.
	 * @param headers the headers for the subscribed message frame
	 * @param handler the handler for received messages
	 * @return a handle to use to unsubscribe and/or track receipts
	 */
	Subscription subscribe(StompHeaders headers, StompFrameHandler handler);

	/**
	 * Send an acknowledgement whether a message was consumed or not resulting
	 * in an ACK or NACK frame respectively.
	 * <p><strong>Note:</strong> to use this when subscribing you must set the
	 * {@link StompHeaders#setAck(String) ack} header to "client" or
	 * "client-individual" in order ot use this.
	 * @param messageId the id of the message
	 * @param consumed whether the message was consumed or not
	 * @return a Receiptable for tracking receipts
	 * @since 4.3
	 */
	Receiptable acknowledge(String messageId, boolean consumed);

	/**
	 * An overloaded version of {@link #acknowledge(String, boolean)} with
	 * full {@link StompHeaders} instead of just a {@code messageId}.
	 * @param headers the headers for the ACK or NACK message frame
	 * @param consumed whether the message was consumed or not
	 * @return a Receiptable for tracking receipts
	 * @since 5.0.5
	 */
	Receiptable acknowledge(StompHeaders headers, boolean consumed);

	/**
	 * Disconnect the session by sending a DISCONNECT frame.
	 */
	void disconnect();

	/**
	 * Variant of {@link #disconnect()} with headers.
	 * @param headers the headers for the disconnect message frame
	 * @since 5.2.2
	 */
	void disconnect(StompHeaders headers);


	/**
	 * A handle to use to track receipts.
	 * @see #setAutoReceipt(boolean)
	 */
	interface Receiptable {

		/**
		 * Return the receipt id, or {@code null} if the STOMP frame for which
		 * the handle was returned did not have a "receipt" header.
		 */
		@Nullable String getReceiptId();

		/**
		 * Task to invoke when a receipt is received.
		 * @param task the task to invoke
		 * @throws java.lang.IllegalArgumentException if the receiptId is {@code null}
		 */
		void addReceiptTask(Runnable task);

		/**
		 * Variant of {@link #addReceiptTask(Runnable)} with a {@link Consumer}
		 * of the headers from the {@code RECEIPT} frame.
		 * @param task the consumer to invoke
		 * @throws java.lang.IllegalArgumentException if the receiptId is {@code null}
		 * @since 5.3.23
		 */
		void addReceiptTask(Consumer<StompHeaders> task);

		/**
		 * Task to invoke when a receipt is not received in the configured time.
		 * @param task the task to invoke
		 * @throws java.lang.IllegalArgumentException if the receiptId is {@code null}
		 * @see org.springframework.messaging.simp.stomp.StompClientSupport#setReceiptTimeLimit(long)
		 */
		void addReceiptLostTask(Runnable task);
	}


	/**
	 * A handle to use to unsubscribe or to track a receipt.
	 */
	interface Subscription extends Receiptable {

		/**
		 * Return the id for the subscription.
		 */
		@Nullable String getSubscriptionId();

		/**
		 * Return the headers used on the SUBSCRIBE frame.
		 * @since 5.0
		 */
		StompHeaders getSubscriptionHeaders();

		/**
		 * Remove the subscription by sending an UNSUBSCRIBE frame.
		 */
		void unsubscribe();

		/**
		 * Alternative to {@link #unsubscribe()} with additional custom headers
		 * to send to the server.
		 * <p><strong>Note:</strong> There is no need to set the subscription id.
		 * @param headers the custom headers, if any
		 * @since 5.0
		 */
		void unsubscribe(@Nullable StompHeaders headers);
	}

}

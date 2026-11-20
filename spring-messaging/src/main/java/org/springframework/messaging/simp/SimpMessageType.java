/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp;

/**
 * A generic representation of different kinds of messages found in simple messaging
 * protocols like STOMP.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public enum SimpMessageType {

	CONNECT,

	CONNECT_ACK,

	MESSAGE,

	SUBSCRIBE,

	UNSUBSCRIBE,

	HEARTBEAT,

	DISCONNECT,

	DISCONNECT_ACK,

	OTHER

}

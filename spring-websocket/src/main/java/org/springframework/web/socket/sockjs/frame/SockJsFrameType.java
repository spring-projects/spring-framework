/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.frame;

/**
 * SockJS frame types.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public enum SockJsFrameType {

	OPEN, HEARTBEAT, MESSAGE, CLOSE

}

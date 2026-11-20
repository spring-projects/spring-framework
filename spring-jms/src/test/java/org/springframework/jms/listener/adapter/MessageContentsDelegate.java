/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.listener.adapter;

import java.util.Map;

/**
 * See the MessageListenerAdapterTests class for usage.
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public interface MessageContentsDelegate {

	void handleMessage(CharSequence message);

	void handleMessage(Map<String, Object> message);

	void handleMessage(byte[] message);

	void handleMessage(Number message);

	void handleMessage(Object message);

}

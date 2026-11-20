/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.listener.adapter;

import jakarta.jms.TextMessage;

/**
 * See the MessageListenerAdapterTests class for usage.
 *
 * @author Rick Evans
 */
public interface ResponsiveJmsTextMessageReturningMessageDelegate {

	TextMessage handleMessage(TextMessage message);

}

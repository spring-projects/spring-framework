/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.adapter.jetty;

import org.eclipse.jetty.websocket.api.ExtensionConfig;
import org.eclipse.jetty.websocket.common.JettyExtensionConfig;

import org.springframework.web.socket.WebSocketExtension;

/**
 * Adapter class to convert a {@link WebSocketExtension} to a Jetty
 * {@link ExtensionConfig}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketToJettyExtensionConfigAdapter extends JettyExtensionConfig {

	public WebSocketToJettyExtensionConfigAdapter(WebSocketExtension extension) {
		super(extension.getName());
		extension.getParameters().forEach(super::setParameter);
	}

}

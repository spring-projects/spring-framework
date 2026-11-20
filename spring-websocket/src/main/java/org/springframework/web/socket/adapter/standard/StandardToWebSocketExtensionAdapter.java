/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.adapter.standard;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.websocket.Extension;

import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.socket.WebSocketExtension;

/**
 * A subclass of {@link WebSocketExtension} that can be constructed from a
 * {@link jakarta.websocket.Extension}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardToWebSocketExtensionAdapter extends WebSocketExtension {

	public StandardToWebSocketExtensionAdapter(Extension extension) {
		super(extension.getName(), initParameters(extension));
	}

	private static Map<String, String> initParameters(Extension extension) {
		List<Extension.Parameter> parameters = extension.getParameters();
		Map<String, String> result = new LinkedCaseInsensitiveMap<>(parameters.size(), Locale.ROOT);
		for (Extension.Parameter parameter : parameters) {
			result.put(parameter.getName(), parameter.getValue());
		}
		return result;
	}

}

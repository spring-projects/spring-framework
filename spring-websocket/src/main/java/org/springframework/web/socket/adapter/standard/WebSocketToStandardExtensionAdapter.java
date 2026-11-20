/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.adapter.standard;

import java.util.ArrayList;
import java.util.List;

import jakarta.websocket.Extension;
import org.jspecify.annotations.Nullable;

import org.springframework.web.socket.WebSocketExtension;

/**
 * Adapt an instance of {@link org.springframework.web.socket.WebSocketExtension} to
 * the {@link jakarta.websocket.Extension} interface.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketToStandardExtensionAdapter implements Extension {

	private final String name;

	private final List<Parameter> parameters = new ArrayList<>();


	public WebSocketToStandardExtensionAdapter(final WebSocketExtension extension) {
		this.name = extension.getName();
		for (final String paramName : extension.getParameters().keySet()) {
			this.parameters.add(new Parameter() {
				@Override
				public String getName() {
					return paramName;
				}
				@Override
				public @Nullable String getValue() {
					return extension.getParameters().get(paramName);
				}
			});
		}
	}


	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public List<Parameter> getParameters() {
		return this.parameters;
	}

}

/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.socket.adapter.standard;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.websocket.Extension;

import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.socket.WebSocketExtension;

/**
 * A subclass of {@link org.springframework.web.socket.WebSocketExtension} that
 * can be constructed from a {@link javax.websocket.Extension}.
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
		Map<String, String> result = new LinkedCaseInsensitiveMap<>(parameters.size(), Locale.ENGLISH);
		for (Extension.Parameter parameter : parameters) {
			result.put(parameter.getName(), parameter.getValue());
		}
		return result;
	}

}

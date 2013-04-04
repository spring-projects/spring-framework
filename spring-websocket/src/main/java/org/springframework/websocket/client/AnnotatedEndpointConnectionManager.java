/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.websocket.client;

import java.io.IOException;

import javax.websocket.DeploymentException;
import javax.websocket.Session;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class AnnotatedEndpointConnectionManager extends AbstractEndpointConnectionManager {


	public AnnotatedEndpointConnectionManager(Class<?> endpointClass, String uriTemplate, Object... uriVariables) {
		super(endpointClass, uriTemplate, uriVariables);
	}

	public AnnotatedEndpointConnectionManager(Object endpointBean, String uriTemplate, Object... uriVariables) {
		super(endpointBean, uriTemplate, uriVariables);
	}

	@Override
	protected Session connect(Object endpoint) throws DeploymentException, IOException {
		return getWebSocketContainer().connectToServer(endpoint, getUri());
	}

}

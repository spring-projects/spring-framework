/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.sockjs.server.support;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.sockjs.server.AbstractSockJsService;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

/**
 * A Spring MVC HandlerMapping matching requests to SockJS services by prefix.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsServiceHandlerMapping extends AbstractHandlerMapping {

	private static Log logger = LogFactory.getLog(SockJsServiceHandlerMapping.class);

	private final List<AbstractSockJsService> sockJsServices;


	public SockJsServiceHandlerMapping(AbstractSockJsService... sockJsServices) {
		this.sockJsServices = Arrays.asList(sockJsServices);
	}

	@Override
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {

		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for SockJS service match to path " + lookupPath);
		}

		for (AbstractSockJsService service : this.sockJsServices) {
			if (lookupPath.startsWith(service.getPrefix())) {
				if (logger.isDebugEnabled()) {
					logger.debug("Matched to " + service);
				}
				String sockJsPath = lookupPath.substring(service.getPrefix().length());
				return new SockJsServiceHttpRequestHandler(sockJsPath, service);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Did not find a match");
		}

		return null;
	}

}

/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.ui.velocity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogSystem;

/**
 * Velocity LogSystem implementation for Jakarta Commons Logging.
 * Used by VelocityConfigurer to redirect log output.
 *
 * @author Juergen Hoeller
 * @since 07.08.2003
 * @see VelocityEngineFactoryBean
 */
public class CommonsLoggingLogSystem implements LogSystem {

	private static final Log logger = LogFactory.getLog(VelocityEngine.class);

	public void init(RuntimeServices runtimeServices) {
	}

	public void logVelocityMessage(int type, String msg) {
		switch (type) {
			case ERROR_ID:
				logger.error(msg);
				break;
			case WARN_ID:
				logger.warn(msg);
				break;
			case INFO_ID:
				logger.info(msg);
				break;
			case DEBUG_ID:
				logger.debug(msg);
				break;
		}
	}

}

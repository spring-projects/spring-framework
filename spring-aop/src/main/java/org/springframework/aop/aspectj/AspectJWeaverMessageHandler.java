/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.aop.aspectj;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.bridge.AbortException;
import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.IMessage.Kind;
import org.aspectj.bridge.IMessageHandler;

/**
 * Implementation of AspectJ's {@link IMessageHandler} interface that
 * routes AspectJ weaving messages through the same logging system as the
 * regular Spring messages.
 *
 * <p>Pass the option...
 *
 * <p><code class="code">-XmessageHandlerClass:org.springframework.aop.aspectj.AspectJWeaverMessageHandler</code>
 *
 * <p>to the weaver; for example, specifying the following in a
 * "{@code META-INF/aop.xml} file:
 *
 * <p><code class="code">&lt;weaver options="..."/&gt;</code>
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
public class AspectJWeaverMessageHandler implements IMessageHandler {

	private static final String AJ_ID = "[AspectJ] ";

	private static final Log logger = LogFactory.getLog("AspectJ Weaver");


	@Override
	public boolean handleMessage(IMessage message) throws AbortException {
		Kind messageKind = message.getKind();
		if (messageKind == IMessage.DEBUG) {
			if (logger.isDebugEnabled()) {
				logger.debug(makeMessageFor(message));
				return true;
			}
		}
		else if (messageKind == IMessage.INFO || messageKind == IMessage.WEAVEINFO) {
			if (logger.isInfoEnabled()) {
				logger.info(makeMessageFor(message));
				return true;
			}
		}
		else if (messageKind == IMessage.WARNING) {
			if (logger.isWarnEnabled()) {
				logger.warn(makeMessageFor(message));
				return true;
			}
		}
		else if (messageKind == IMessage.ERROR) {
			if (logger.isErrorEnabled()) {
				logger.error(makeMessageFor(message));
				return true;
			}
		}
		else if (messageKind == IMessage.ABORT) {
			if (logger.isFatalEnabled()) {
				logger.fatal(makeMessageFor(message));
				return true;
			}
		}
		return false;
	}

	private String makeMessageFor(IMessage aMessage) {
		return AJ_ID + aMessage.getMessage();
	}

	@Override
	public boolean isIgnoring(Kind messageKind) {
		// We want to see everything, and allow configuration of log levels dynamically.
		return false;
	}

	@Override
	public void dontIgnore(Kind messageKind) {
		// We weren't ignoring anything anyway...
	}

	@Override
	public void ignore(Kind kind) {
		// We weren't ignoring anything anyway...
	}

}

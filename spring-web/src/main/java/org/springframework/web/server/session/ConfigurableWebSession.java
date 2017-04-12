/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.server.session;

import java.time.Instant;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;

import org.springframework.web.server.WebSession;

/**
 * Extend {@link WebSession} with management operations meant for internal use
 * for example by implementations of {@link WebSessionManager}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ConfigurableWebSession extends WebSession {

	/**
	 * Update the last access time for user-related session activity.
	 * @param time the time of access
	 */
	void setLastAccessTime(Instant time);

	/**
	 * Set the operation to invoke when {@link WebSession#save()} is invoked.
	 * @param saveOperation the save operation
	 */
	void setSaveOperation(Supplier<Mono<Void>> saveOperation);

}

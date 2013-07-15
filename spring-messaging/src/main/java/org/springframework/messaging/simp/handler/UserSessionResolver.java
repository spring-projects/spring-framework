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

package org.springframework.messaging.simp.handler;

import java.util.Set;


/**
 * A strategy for resolving a user name to one or more session id's.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface UserSessionResolver {

	/**
	 * Retrieve the sessionId(s) associated with the given user.
	 *
	 * @param user the user name
	 * @return a Set with zero, one, or more, current session id's.
	 */
	Set<String> resolveUserSessionIds(String user);

}

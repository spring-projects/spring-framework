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

package org.springframework.web.messaging.support;

import org.springframework.web.messaging.SessionSubscriptionRegistry;


/**
 * Test fixture for {@link DefaultSessionSubscriptionRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultSessionSubscriptionRegistryTests extends AbstractSessionSubscriptionRegistryTests {


	@Override
	protected SessionSubscriptionRegistry createSessionSubscriptionRegistry() {
		return new DefaultSessionSubscriptionRegistry();
	}

}

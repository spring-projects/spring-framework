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

package org.springframework.jms.core;

import jakarta.jms.Session;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 06.01.2005
 */
class JmsTemplateTransactedTests extends JmsTemplateTests {

	private Session localSession = mock();


	@Override
	@BeforeEach
	void setupMocks() throws Exception {
		super.setupMocks();
		given(this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).willReturn(this.localSession);
	}

	@Override
	protected Session getLocalSession() {
		return this.localSession;
	}

	@Override
	protected boolean useTransactedSession() {
		return true;
	}

	@Override
	protected boolean useTransactedTemplate() {
		return true;
	}

}

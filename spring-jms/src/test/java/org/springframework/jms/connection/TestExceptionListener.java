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

package org.springframework.jms.connection;

import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;

/**
 * @author Juergen Hoeller
 */
public class TestExceptionListener implements ExceptionListener {

	private int count = 0;

	@Override
	public void onException(JMSException ex) {
		this.count++;
	}

	public int getCount() {
		return count;
	}

}

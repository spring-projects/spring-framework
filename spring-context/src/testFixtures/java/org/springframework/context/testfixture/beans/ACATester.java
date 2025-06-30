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

package org.springframework.context.testfixture.beans;

import java.util.Locale;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.NoSuchMessageException;

public class ACATester implements ApplicationContextAware {

	private ApplicationContext ac;

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws ApplicationContextException {
		// check re-initialization
		if (this.ac != null) {
			throw new IllegalStateException("Already initialized");
		}

		// check message source availability
		if (ctx != null) {
			try {
				ctx.getMessage("code1", null, Locale.getDefault());
			}
			catch (NoSuchMessageException ex) {
				// expected
			}
		}

		this.ac = ctx;
	}

	public ApplicationContext getApplicationContext() {
		return ac;
	}

}

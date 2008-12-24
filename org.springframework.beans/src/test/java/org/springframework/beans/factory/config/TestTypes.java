/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.beans.factory.ObjectFactory;

/**
 * Shared test types for this package.
 * 
 * @author Chris Beams
 */
final class TestTypes {}

/**
 * @author Juergen Hoeller
 */
class NoOpScope implements Scope {

	public Object get(String name, ObjectFactory<?> objectFactory) {
		throw new UnsupportedOperationException();
	}

	public Object remove(String name) {
		throw new UnsupportedOperationException();
	}

	public void registerDestructionCallback(String name, Runnable callback) {
	}

	public Object resolveContextualObject(String key) {
		return null;
	}

	public String getConversationId() {
		return null;
	}

}

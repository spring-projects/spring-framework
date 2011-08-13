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
package test.beans;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

/**
 * Simple scope implementation which creates object based on a flag.
 *
 * @author  Costin Leau
 * @author  Chris Beams
 */
public class CustomScope implements Scope {

	public boolean createNewScope = true;

	private Map<String, Object> beans = new HashMap<String, Object>();

	public Object get(String name, ObjectFactory<?> objectFactory) {
		if (createNewScope) {
			beans.clear();
			// reset the flag back
			createNewScope = false;
		}

		Object bean = beans.get(name);
		// if a new object is requested or none exists under the current
		// name, create one
		if (bean == null) {
			beans.put(name, objectFactory.getObject());
		}

		return beans.get(name);
	}

	public String getConversationId() {
		return null;
	}

	public void registerDestructionCallback(String name, Runnable callback) {
		// do nothing
	}

	public Object remove(String name) {
		return beans.remove(name);
	}

	public Object resolveContextualObject(String key) {
		return null;
	}

}

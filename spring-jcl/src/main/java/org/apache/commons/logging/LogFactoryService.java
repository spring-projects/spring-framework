/*
 * Copyright 2002-2023 the original author or authors.
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

package org.apache.commons.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A minimal subclass of the standard Apache Commons Logging's {@code LogFactory} class,
 * overriding the abstract {@code getInstance} lookup methods. This is just applied in
 * case of the standard {@code commons-logging} jar accidentally ending up on the classpath,
 * with the standard {@code LogFactory} class performing its META-INF service discovery.
 * This implementation simply delegates to Spring's common {@link Log} factory methods.
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @deprecated since it is only meant to be used in the above-mentioned fallback scenario
 */
@Deprecated
public class LogFactoryService extends LogFactory {

	private final Map<String, Object> attributes = new ConcurrentHashMap<>();


	@Override
	public Log getInstance(Class<?> clazz) {
		return getInstance(clazz.getName());
	}

	@Override
	public Log getInstance(String name) {
		return LogAdapter.createLog(name);
	}


	// Just in case some code happens to rely on Commons Logging attributes...

	@Override
	public void setAttribute(String name, Object value) {
		if (value != null) {
			this.attributes.put(name, value);
		}
		else {
			this.attributes.remove(name);
		}
	}

	@Override
	public void removeAttribute(String name) {
		this.attributes.remove(name);
	}

	@Override
	public Object getAttribute(String name) {
		return this.attributes.get(name);
	}

	@Override
	public String[] getAttributeNames() {
		return this.attributes.keySet().toArray(new String[0]);
	}

	@Override
	public void release() {
	}

}

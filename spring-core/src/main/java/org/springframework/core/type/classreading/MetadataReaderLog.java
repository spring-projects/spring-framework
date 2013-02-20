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

package org.springframework.core.type.classreading;

/**
 * Callback interface that can be used to collect non-fatal log messages from a
 * {@link MetadataReaderFactory} implementation. Allows callers to decide how
 * and when non-fatal log messages should be displayed.
 *
 * @author Phillip Webb
 */
public interface MetadataReaderLog {

	/**
	 * Called to log a non fatal error whilst reading meta-data.
	 * @param message the log message
	 * @param t the underlying cause (may be {@code null})
	 */
	void log(String message, Throwable t);

}

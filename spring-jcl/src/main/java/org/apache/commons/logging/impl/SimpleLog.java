/*
 * Copyright 2002-2017 the original author or authors.
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

package org.apache.commons.logging.impl;

/**
 * Originally a simple Commons Logging provider configured by system properties.
 * Deprecated in {@code spring-jcl}, effectively equivalent to {@link NoOpLog} now.
 *
 * <p>Instead of instantiating this directly, call {@code LogFactory#getLog(Class/String)}
 * which will fall back to {@code java.util.logging} if neither Log4j nor SLF4J are present.
 *
 * @author Juergen Hoeller (for the {@code spring-jcl} variant)
 * @since 5.0
 */
@Deprecated
@SuppressWarnings("serial")
public class SimpleLog extends NoOpLog {

	public SimpleLog(String name) {
		super(name);
		System.out.println(SimpleLog.class.getName() + " is deprecated and equivalent to NoOpLog in spring-jcl. " +
				"Use a standard LogFactory.getLog(Class/String) call instead.");
	}

}

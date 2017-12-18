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

import java.io.Serializable;

import org.apache.commons.logging.Log;

/**
 * Trivial implementation of {@link Log} that throws away all messages.
 *
 * @author Juergen Hoeller (for the {@code spring-jcl} variant)
 * @since 5.0
 */
@SuppressWarnings("serial")
public class NoOpLog implements Log, Serializable {

	public NoOpLog() {
	}

	public NoOpLog(String name) {
	}


	@Override
	public boolean isFatalEnabled() {
		return false;
	}

	@Override
	public boolean isErrorEnabled() {
		return false;
	}

	@Override
	public boolean isWarnEnabled() {
		return false;
	}

	@Override
	public boolean isInfoEnabled() {
		return false;
	}

	@Override
	public boolean isDebugEnabled() {
		return false;
	}

	@Override
	public boolean isTraceEnabled() {
		return false;
	}

	@Override
	public void fatal(Object message) {
	}

	@Override
	public void fatal(Object message, Throwable t) {
	}

	@Override
	public void error(Object message) {
	}

	@Override
	public void error(Object message, Throwable t) {
	}

	@Override
	public void warn(Object message) {
	}

	@Override
	public void warn(Object message, Throwable t) {
	}

	@Override
	public void info(Object message) {
	}

	@Override
	public void info(Object message, Throwable t) {
	}

	@Override
	public void debug(Object message) {
	}

	@Override
	public void debug(Object message, Throwable t) {
	}

	@Override
	public void trace(Object message) {
	}

	@Override
	public void trace(Object message, Throwable t) {
	}

}

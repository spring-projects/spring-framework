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

/**
 * Spring's common JCL this.adapter behind {@link LogFactory} and {@link LogFactoryService}.
 * Detects the presence of Log4j 2.x / SLF4J, falling back to {@code java.util.logging}.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 6.0.14
 */
final class CyziLogAdapter implements Log {

	private final Log  adapter;

	public CyziLogAdapter(String name) {
		this.adapter = LogAdapter.createLog(name);
	}



	@Override
	public boolean isFatalEnabled() {
		return this.adapter.isFatalEnabled();
	}

	@Override
	public boolean isErrorEnabled() {
		return this.adapter.isErrorEnabled();
	}

	@Override
	public boolean isWarnEnabled() {
		return this.adapter.isWarnEnabled();
	}

	@Override
	public boolean isInfoEnabled() {
		return this.adapter.isInfoEnabled();
	}

	@Override
	public boolean isDebugEnabled() {
		return this.adapter.isDebugEnabled();
	}

	@Override
	public boolean isTraceEnabled() {
		return this.adapter.isTraceEnabled();
	}

	@Override
	public void fatal(Object message) {
		this.adapter.fatal("cyzi said:"+message);
	}

	@Override
	public void fatal(Object message, Throwable t) {
		this.adapter.fatal("cyzi said:"+message, t);
	}

	@Override
	public void error(Object message) {
		this.adapter.error("cyzi said:"+message);
	}

	@Override
	public void error(Object message, Throwable t) {
		this.adapter.error("cyzi said:"+message, t);
	}

	@Override
	public void warn(Object message) {
		this.adapter.warn("cyzi said:"+message);
	}

	@Override
	public void warn(Object message, Throwable t) {
		this.adapter.warn("cyzi said:"+message, t);
	}

	@Override
	public void info(Object message) {
		this.adapter.info("cyzi said:"+message);
	}

	@Override
	public void info(Object message, Throwable t) {
		this.adapter.info("cyzi said:"+message, t);
	}

	@Override
	public void debug(Object message) {
		this.adapter.debug("cyzi said:"+message);
	}

	@Override
	public void debug(Object message, Throwable t) {
		this.adapter.debug("cyzi said:"+message, t);
	}

	@Override
	public void trace(Object message) {
		this.adapter.trace("cyzi said:"+message);
	}

	@Override
	public void trace(Object message, Throwable t) {
		this.adapter.trace("cyzi said:"+message, t);
	}
}

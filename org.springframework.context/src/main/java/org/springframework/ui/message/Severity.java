/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.ui.message;

/**
 * Enum exposing supported message severities.
 * 
 * @author Keith Donald
 * @author Jeremy Grelle
 * @see Message
 */
public enum Severity {

	/**
	 * The "Informational" severity. Used to indicate a successful operation or result.
	 */
	INFO,

	/**
	 * The "Warning" severity. Used to indicate there is a minor problem, or to inform the message receiver of possible
	 * misuse, or to indicate a problem may arise in the future.
	 */
	WARNING,

	/**
	 * The "Error" severity. Used to indicate a significant problem like a business rule violation.
	 */
	ERROR,

	/**
	 * The "Fatal" severity. Used to indicate a fatal problem like a system error.
	 */
	FATAL

}

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
 * Communicates information of interest to the user.
 * For example, a error message may inform a user of a web application a business rule was violated.
 * TODO - should we introduce summary/detail fields instead of just text
 * @author Keith Donald
 */
public interface Message {

	/**
	 * The severity of this message.
	 * The severity indicates the intensity or priority of the message.
	 * @return the message severity
	 */
	public Severity getSeverity();

	/**
	 * The message text.
	 * The text is the message's communication payload.
	 * @return the message text
	 */
	public String getText();

}

/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.TimeZone;

/**
 * Editor for <code>java.util.TimeZone</code>, translating timezone IDs into
 * TimeZone objects. Does not expose a text representation for TimeZone objects.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.util.TimeZone
 */
public class TimeZoneEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(TimeZone.getTimeZone(text));
	}

	/**
	 * This implementation returns <code>null</code> to indicate that
	 * there is no appropriate text representation.
	 */
	@Override
	public String getAsText() {
		return null;
	}

}

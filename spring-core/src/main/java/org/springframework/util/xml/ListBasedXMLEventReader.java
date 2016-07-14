/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.util.xml;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.stream.events.XMLEvent;

import org.springframework.util.Assert;

/**
 * Implementation of {@code XMLEventReader} based on a list of {@link XMLEvent}s.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class ListBasedXMLEventReader extends AbstractXMLEventReader {

	private final List<XMLEvent> events;

	private int cursor = 0;

	public ListBasedXMLEventReader(List<XMLEvent> events) {
		Assert.notNull(events, "'events' must not be null");
		this.events = Collections.unmodifiableList(events);
	}

	@Override
	public boolean hasNext() {
		return cursor != events.size();
	}

	@Override
	public XMLEvent nextEvent() {
		if (cursor < events.size()) {
			return events.get(cursor++);
		}
		else {
			throw new NoSuchElementException();
		}
	}

	@Override
	public XMLEvent peek() {
		if (cursor < events.size()) {
			return events.get(cursor);
		}
		else {
			return null;
		}
	}

	@Override
	public void close() {
		super.close();
		this.events.clear();
	}
}

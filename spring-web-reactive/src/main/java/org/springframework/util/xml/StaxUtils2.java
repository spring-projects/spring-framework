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

import java.util.List;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.XMLEvent;

/**
 * TODO: to be merged with {@link StaxUtils}.
 * @author Arjen Poutsma
 */
public abstract class StaxUtils2 {

	/**
	 * Create a {@link XMLEventReader} from the given list of {@link XMLEvent}.
	 * @param events the list of {@link XMLEvent}s.
	 * @return an {@code XMLEventReader} that reads from the given events
	 */
	public static XMLEventReader createXMLEventReader(List<XMLEvent> events) {
		return new ListBasedXMLEventReader(events);
	}

}

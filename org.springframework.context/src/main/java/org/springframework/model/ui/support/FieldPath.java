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
package org.springframework.model.ui.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class FieldPath implements Iterable<FieldPathElement> {

	private List<FieldPathElement> elements = new ArrayList<FieldPathElement>();

	public FieldPath(String propertyPath) {
		// a.b.c[i].d[key].e
		String[] props = propertyPath.split("\\.");
		if (props.length == 0) {
			props = new String[] { propertyPath };
		}
		for (String prop : props) {
			if (prop.contains("[")) {
				int start = prop.indexOf('[');
				int end = prop.indexOf(']', start);
				String index = prop.substring(start + 1, end);
				elements.add(new FieldPathElement(prop.substring(0, start), false));
				elements.add(new FieldPathElement(index, true));
			} else {
				elements.add(new FieldPathElement(prop, false));
			}
		}
	}

	public FieldPathElement getFirstElement() {
		return elements.get(0);
	}

	public List<FieldPathElement> getNestedElements() {
		if (elements.size() > 1) {
			return elements.subList(1, elements.size());
		} else {
			return Collections.emptyList();
		}
	}

	public Iterator<FieldPathElement> iterator() {
		return elements.iterator();
	}
	
	public String toString() {
		return elements.toString();
	}
}
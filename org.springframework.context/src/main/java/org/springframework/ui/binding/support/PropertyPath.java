/**
 * 
 */
package org.springframework.ui.binding.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PropertyPath implements Iterable<PropertyPathElement> {

	private List<PropertyPathElement> elements = new ArrayList<PropertyPathElement>();

	public PropertyPath(String propertyPath) {
		// a.b.c[i].d[key].e
		String[] props = propertyPath.split("\\.");
		if (props.length == 0) {
			props = new String[] { propertyPath };
		}
		for (String prop : props) {
			if (prop.startsWith("[")) {
				int end = prop.indexOf(']');
				String index = prop.substring(0, end);
				elements.add(new PropertyPathElement(index, true));
			} else {
				elements.add(new PropertyPathElement(prop, false));
			}
		}
	}

	public PropertyPathElement getFirstElement() {
		return elements.get(0);
	}

	public List<PropertyPathElement> getNestedElements() {
		if (elements.size() > 1) {
			return elements.subList(1, elements.size() - 1);
		} else {
			return Collections.emptyList();
		}
	}

	public Iterator<PropertyPathElement> iterator() {
		return elements.iterator();
	}

}
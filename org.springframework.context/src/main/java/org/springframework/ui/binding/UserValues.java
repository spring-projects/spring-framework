package org.springframework.ui.binding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A simpler holder for a list of UserValues.
 * 
 * @author Keith Donald
 * @see Binder#bind(UserValues)
 */
public class UserValues implements Iterable<UserValue> {
	
	private List<UserValue> values;
	
	/**
	 * Creates a new user values list of the default size.
	 */
	public UserValues() {
		values = new ArrayList<UserValue>();
	}

	/**
	 * Creates a new user values list of the size provided.
	 */
	public UserValues(int size) {
		values = new ArrayList<UserValue>(size);
	}

	// implementing Iterable
	
	public Iterator<UserValue> iterator() {
		return values.iterator();
	}
	
	/**
	 * The user values list.
	 * The returned list is not modifiable.
	 */
	public List<UserValue> asList() {
		return Collections.unmodifiableList(values);
	}
	
	/**
	 * Add a new user value.
	 * @param property the property the value should be bound to
	 * @param value the actual user-entered value
	 */
	public void add(String property, Object value) {
		values.add(new UserValue(property, value));
	}
	
	/**
	 * The number of user values in the list.
	 */
	public int size() {
		return values.size();
	}

	/**
	 * Creates a new UserValues list with a single element.
	 * @param property the property
	 * @param value the actual user-entered value
	 * @return the singleton user value list
	 */
	public static UserValues single(String property, Object value) {
		UserValues values = new UserValues(1);
		values.add(property, value);
		return values;
	}
	
}
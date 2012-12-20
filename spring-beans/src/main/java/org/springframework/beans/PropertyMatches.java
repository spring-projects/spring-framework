/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Helper class for calculating bean property matches, according to.
 * Used by BeanWrapperImpl to suggest alternatives for an invalid property name.
 *
 * @author Alef Arendsen
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 2.0
 * @see #forProperty(String, Class)
 */
final class PropertyMatches {

	//---------------------------------------------------------------------
	// Static section
	//---------------------------------------------------------------------

	/** Default maximum property distance: 2 */
	public static final int DEFAULT_MAX_DISTANCE = 2;


	/**
	 * Create PropertyMatches for the given bean property.
	 * @param propertyName the name of the property to find possible matches for
	 * @param beanClass the bean class to search for matches
	 */
	public static PropertyMatches forProperty(String propertyName, Class<?> beanClass) {
		return forProperty(propertyName, beanClass, DEFAULT_MAX_DISTANCE);
	}

	/**
	 * Create PropertyMatches for the given bean property.
	 * @param propertyName the name of the property to find possible matches for
	 * @param beanClass the bean class to search for matches
	 * @param maxDistance the maximum property distance allowed for matches
	 */
	public static PropertyMatches forProperty(String propertyName, Class<?> beanClass, int maxDistance) {
		return new PropertyMatches(propertyName, beanClass, maxDistance);
	}


	//---------------------------------------------------------------------
	// Instance section
	//---------------------------------------------------------------------

	private final String propertyName;

	private String[] possibleMatches;


	/**
	 * Create a new PropertyMatches instance for the given property.
	 */
	private PropertyMatches(String propertyName, Class<?> beanClass, int maxDistance) {
		this.propertyName = propertyName;
		this.possibleMatches = calculateMatches(BeanUtils.getPropertyDescriptors(beanClass), maxDistance);
	}


	/**
	 * Return the calculated possible matches.
	 */
	public String[] getPossibleMatches() {
		return possibleMatches;
	}

	/**
	 * Build an error message for the given invalid property name,
	 * indicating the possible property matches.
	 */
	public String buildErrorMessage() {
		StringBuilder msg = new StringBuilder();
		msg.append("Bean property '");
		msg.append(this.propertyName);
		msg.append("' is not writable or has an invalid setter method. ");

		if (ObjectUtils.isEmpty(this.possibleMatches)) {
			msg.append("Does the parameter type of the setter match the return type of the getter?");
		}
		else {
			msg.append("Did you mean ");
			for (int i = 0; i < this.possibleMatches.length; i++) {
				msg.append('\'');
				msg.append(this.possibleMatches[i]);
				if (i < this.possibleMatches.length - 2) {
					msg.append("', ");
				}
				else if (i == this.possibleMatches.length - 2){
					msg.append("', or ");
				}
	 		}
			msg.append("'?");
		}
		return msg.toString();
	}


	/**
	 * Generate possible property alternatives for the given property and
	 * class. Internally uses the <code>getStringDistance</code> method, which
	 * in turn uses the Levenshtein algorithm to determine the distance between
	 * two Strings.
	 * @param propertyDescriptors the JavaBeans property descriptors to search
	 * @param maxDistance the maximum distance to accept
	 */
	private String[] calculateMatches(PropertyDescriptor[] propertyDescriptors, int maxDistance) {
		List<String> candidates = new ArrayList<String>();
		for (PropertyDescriptor pd : propertyDescriptors) {
			if (pd.getWriteMethod() != null) {
				String possibleAlternative = pd.getName();
				if (calculateStringDistance(this.propertyName, possibleAlternative) <= maxDistance) {
					candidates.add(possibleAlternative);
				}
			}
		}
		Collections.sort(candidates);
		return StringUtils.toStringArray(candidates);
	}

	/**
	 * Calculate the distance between the given two Strings
	 * according to the Levenshtein algorithm.
	 * @param s1 the first String
	 * @param s2 the second String
	 * @return the distance value
	 */
	private int calculateStringDistance(String s1, String s2) {
		if (s1.length() == 0) {
			return s2.length();
		}
		if (s2.length() == 0) {
			return s1.length();
		}
		int d[][] = new int[s1.length() + 1][s2.length() + 1];

		for (int i = 0; i <= s1.length(); i++) {
			d[i][0] = i;
		}
		for (int j = 0; j <= s2.length(); j++) {
			d[0][j] = j;
		}

		for (int i = 1; i <= s1.length(); i++) {
			char s_i = s1.charAt(i - 1);
			for (int j = 1; j <= s2.length(); j++) {
				int cost;
				char t_j = s2.charAt(j - 1);
				if (s_i == t_j) {
					cost = 0;
				} else {
					cost = 1;
				}
				d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1),
						d[i - 1][j - 1] + cost);
			}
		}

		return d[s1.length()][s2.length()];
	}

}

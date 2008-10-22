/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.util;

/**
 * Utility class for diagnostic purposes, to analyze the
 * ClassLoader hierarchy for any given object or class loader.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 02 April 2001
 * @deprecated as of Spring 2.5, to be removed in Spring 3.0
 * @see java.lang.ClassLoader
 */
public abstract class ClassLoaderUtils {

	/**
	 * Show the class loader hierarchy for this class.
	 * Uses default line break and tab text characters.
	 * @param obj object to analyze loader hierarchy for
	 * @param role a description of the role of this class in the application
	 * (e.g., "servlet" or "EJB reference")
	 * @return a String showing the class loader hierarchy for this class
	 */
	public static String showClassLoaderHierarchy(Object obj, String role) {
		return showClassLoaderHierarchy(obj, role, "\n", "\t");
	}

	/**
	 * Show the class loader hierarchy for this class.
	 * @param obj object to analyze loader hierarchy for
	 * @param role a description of the role of this class in the application
	 * (e.g., "servlet" or "EJB reference")
	 * @param lineBreak line break
	 * @param tabText text to use to set tabs
	 * @return a String showing the class loader hierarchy for this class
	 */
	public static String showClassLoaderHierarchy(Object obj, String role, String lineBreak, String tabText) {
		String s = "object of " + obj.getClass() + ": role is " + role + lineBreak;
		return s + showClassLoaderHierarchy(obj.getClass().getClassLoader(), lineBreak, tabText, 0);
	}

	/**
	 * Show the class loader hierarchy for the given class loader.
	 * Uses default line break and tab text characters.
	 * @param cl class loader to analyze hierarchy for
	 * @return a String showing the class loader hierarchy for this class
	 */
	public static String showClassLoaderHierarchy(ClassLoader cl) {
		return showClassLoaderHierarchy(cl, "\n", "\t");
	}

	/**
	 * Show the class loader hierarchy for the given class loader.
	 * @param cl class loader to analyze hierarchy for
	 * @param lineBreak line break
	 * @param tabText text to use to set tabs
	 * @return a String showing the class loader hierarchy for this class
	 */
	public static String showClassLoaderHierarchy(ClassLoader cl, String lineBreak, String tabText) {
		return showClassLoaderHierarchy(cl, lineBreak, tabText, 0);
	}

	/**
	 * Show the class loader hierarchy for the given class loader.
	 * @param cl class loader to analyze hierarchy for
	 * @param lineBreak line break
	 * @param tabText text to use to set tabs
	 * @param indent nesting level (from 0) of this loader; used in pretty printing
	 * @return a String showing the class loader hierarchy for this class
	 */
	private static String showClassLoaderHierarchy(ClassLoader cl, String lineBreak, String tabText, int indent) {
		if (cl == null) {
			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			return "context class loader=[" + ccl + "] hashCode=" + ccl.hashCode();
		}
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < indent; i++) {
			buf.append(tabText);
		}
		buf.append("[").append(cl).append("] hashCode=").append(cl.hashCode()).append(lineBreak);
		ClassLoader parent = cl.getParent();
		return buf.toString() + showClassLoaderHierarchy(parent, lineBreak, tabText, indent + 1);
	}

}

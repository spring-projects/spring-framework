/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression;

import java.lang.reflect.Method;
import java.util.List;

/**
 * MethodFilter instances allow SpEL users to fine tune the behavior of the method
 * resolution process. Method resolution (which translates from a method name in an
 * expression to a real method to invoke) will normally retrieve candidate methods for
 * invocation via a simple call to 'Class.getMethods()' and will choose the first one that
 * is suitable for the input parameters. By registering a MethodFilter the user can
 * receive a callback and change the methods that will be considered suitable.
 *
 * @author Andy Clement
 * @since 3.0.1
 */
@FunctionalInterface
public interface MethodFilter {

	/**
	 * Called by the method resolver to allow the SpEL user to organize the list of
	 * candidate methods that may be invoked. The filter can remove methods that should
	 * not be considered candidates and it may sort the results. The resolver will then
	 * search through the methods as returned from the filter when looking for a suitable
	 * candidate to invoke.
	 * @param methods the full list of methods the resolver was going to choose from
	 * @return a possible subset of input methods that may be sorted by order of relevance
	 */
	List<Method> filter(List<Method> methods);

}

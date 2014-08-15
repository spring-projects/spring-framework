
/**
 *
 * Package allowing MVC Controller implementations to handle requests
 * at <i>method</i> rather than <i>class</i> level. This is useful when
 * we want to avoid having many trivial controller classes, as can
 * easily happen when using an MVC framework.
 *
 * <p>Typically a controller that handles multiple request types will
 * extend MultiActionController, and implement multiple request handling
 * methods that will be invoked by reflection if they follow this class'
 * naming convention. Classes are analyzed at startup and methods cached,
 * so the performance overhead of reflection in this approach is negligible.
 *
 */
package org.springframework.web.servlet.mvc.multiaction;


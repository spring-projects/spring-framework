
/**
 *
 * <p>
 *     Standard controller implementations for the servlet MVC framework that comes
 *     with Spring. Provides both abstract base classes and concrete implementations
 *     for often seen use cases.
 * </p>
 *
 * <p>
 *     A <code>Controller</code> - as defined in this package - is analogous to a Struts
 *     <code>Action</code>. Usually <code>Controllers</code> are JavaBeans
 *     to allow easy configuration. Controllers define the <code>C</code> from so-called
 *     MVC paradigm and can be used in conjunction with the
 *     {@link org.springframework.web.servlet.ModelAndView ModelAndView}
 *     to achieve interactive applications. The view might be represented by a
 *     HTML interface, but, because of model and the controller being completely
 *     independent of the view, PDF views are possible, as well as for instance Excel
 *     views.
 * </p>
 *
 * <p>
 *     How to actually set up a (web)application using the MVC framework Spring
 *     provides is explained in more detail in the
 *     <a href="../../../../../../MVC-step-by-step/Spring-MVC-step-by-step.html">MVC-Step-by-Step
 *     tutorial</a>, also provided in this package (or have a look
 *     <a href="http://www.springframework.org">here</a> for an online version).
 *     The classes contained by this package explain in more detail the actual
 *     workflow of some of the abstract and concrete controller and how to extend
 *     and fully use their functionality.
 * </p>
 *
 * <p>
 *     Especially useful to read, while getting into the Spring MVC framework
 *     are the following:
 *     <ul>
 *         <li><a href="Controller.html">Controller</a></li>
 *         <li><a href="SimpleFormController.html">BaseCommandController</a></li>
 *         <li><a href="ParameterizableViewController.html">ParameterizableViewController</a></li>
 *     </ul>
 * </p>
 *
 */
package org.springframework.web.servlet.mvc;


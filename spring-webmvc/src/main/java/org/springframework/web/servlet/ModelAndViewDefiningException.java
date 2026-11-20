/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet;

import jakarta.servlet.ServletException;

import org.springframework.util.Assert;

/**
 * Exception to be thrown on error conditions that should forward
 * to a specific view with a specific model.
 *
 * <p>Can be thrown at any time during handler processing.
 * This includes any template methods of pre-built controllers.
 * For example, a form controller might abort to a specific error page
 * if certain parameters do not allow to proceed with the normal workflow.
 *
 * @author Juergen Hoeller
 * @since 22.11.2003
 */
@SuppressWarnings("serial")
public class ModelAndViewDefiningException extends ServletException {

	private final ModelAndView modelAndView;


	/**
	 * Create new ModelAndViewDefiningException with the given ModelAndView,
	 * typically representing a specific error page.
	 * @param modelAndView the ModelAndView with view to forward to and model to expose
	 */
	public ModelAndViewDefiningException(ModelAndView modelAndView) {
		Assert.notNull(modelAndView, "ModelAndView must not be null in ModelAndViewDefiningException");
		this.modelAndView = modelAndView;
	}

	/**
	 * Return the ModelAndView that this exception contains for forwarding to.
	 */
	public ModelAndView getModelAndView() {
		return this.modelAndView;
	}

}

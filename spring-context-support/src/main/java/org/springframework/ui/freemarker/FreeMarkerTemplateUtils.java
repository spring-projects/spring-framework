/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.ui.freemarker;

import java.io.IOException;
import java.io.StringWriter;

import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Utility class for working with FreeMarker.
 *
 * <p>Provides convenience methods to process a FreeMarker template with a model.
 *
 * @author Juergen Hoeller
 * @since 14.03.2004
 */
public abstract class FreeMarkerTemplateUtils {

	/**
	 * Process the specified FreeMarker template with the given model and write
	 * the result to a String.
	 * <p>When using this method to prepare text for a mail to be sent with Spring's
	 * mail support, consider wrapping IO/TemplateException in MailPreparationException.
	 * @param model the model object, typically a Map that contains model names
	 * as keys and model objects as values
	 * @return the result as a String
	 * @throws IOException if the template wasn't found or couldn't be read
	 * @throws freemarker.template.TemplateException if rendering failed
	 * @see org.springframework.mail.MailPreparationException
	 */
	public static String processTemplateIntoString(Template template, Object model)
			throws IOException, TemplateException {

		StringWriter result = new StringWriter(1024);
		template.process(model, result);
		return result.toString();
	}

}

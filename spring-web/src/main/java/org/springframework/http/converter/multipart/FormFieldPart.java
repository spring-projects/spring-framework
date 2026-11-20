/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.multipart;


/**
 * Specialization of {@link Part} for a form field.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 7.1
 */
public interface FormFieldPart extends Part {

	/**
	 * Return the form field value.
	 */
	String value();

}

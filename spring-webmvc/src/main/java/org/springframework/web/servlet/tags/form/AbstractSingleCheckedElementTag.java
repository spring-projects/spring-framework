/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags.form;

import jakarta.servlet.jsp.JspException;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Abstract base class to provide common methods for implementing databinding-aware
 * JSP tags for rendering a <i>single</i> HTML '{@code input}' element with a
 * '{@code type}' of '{@code checkbox}' or '{@code radio}'.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
@SuppressWarnings("serial")
public abstract class AbstractSingleCheckedElementTag extends AbstractCheckedElementTag {

	/**
	 * The value of the '{@code value}' attribute.
	 */
	private @Nullable Object value;

	/**
	 * The value of the '{@code label}' attribute.
	 */
	private @Nullable Object label;


	/**
	 * Set the value of the '{@code value}' attribute.
	 * May be a runtime expression.
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * Get the value of the '{@code value}' attribute.
	 */
	protected @Nullable Object getValue() {
		return this.value;
	}

	/**
	 * Set the value of the '{@code label}' attribute.
	 * May be a runtime expression.
	 */
	public void setLabel(Object label) {
		this.label = label;
	}

	/**
	 * Get the value of the '{@code label}' attribute.
	 */
	protected @Nullable Object getLabel() {
		return this.label;
	}


	/**
	 * Renders the '{@code input(radio)}' element with the configured
	 * {@link #setValue(Object) value}. Marks the element as checked if the
	 * value matches the {@link #getValue bound value}.
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag("input");
		String id = resolveId();
		writeOptionalAttribute(tagWriter, "id", id);
		writeOptionalAttribute(tagWriter, "name", getName());
		writeOptionalAttributes(tagWriter);
		writeTagDetails(tagWriter);
		tagWriter.endTag();

		Object resolvedLabel = evaluate("label", getLabel());
		if (resolvedLabel != null) {
			Assert.state(id != null, "Label id is required");
			tagWriter.startTag("label");
			tagWriter.writeAttribute("for", id);
			tagWriter.appendValue(convertToDisplayString(resolvedLabel));
			tagWriter.endTag();
		}

		return SKIP_BODY;
	}

	/**
	 * Write the details for the given primary tag:
	 * i.e. special attributes and the tag's value.
	 */
	protected abstract void writeTagDetails(TagWriter tagWriter) throws JspException;

}

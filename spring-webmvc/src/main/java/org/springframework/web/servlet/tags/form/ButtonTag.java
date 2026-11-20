/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags.form;

import jakarta.servlet.jsp.JspException;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.web.servlet.support.RequestDataValueProcessor;

/**
 * The {@code <button>} tag renders a form field label in an HTML 'button' tag.
 * It is provided for completeness if the application relies on a
 * {@link RequestDataValueProcessor}.
 *
 * <h3>Attribute Summary</h3>
 * <table>
 * <thead>
 * <tr>
 * <th class="table-header col-first">Attribute</th>
 * <th class="table-header col-second">Required?</th>
 * <th class="table-header col-second">Runtime Expression?</th>
 * <th class="table-header col-last">Description</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="even-row-color">
 * <td><p>disabled</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Optional Attribute. Setting the value of this attribute to 'true'
 * will disable the HTML element.</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>id</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>HTML Standard Attribute</p></td>
 * </tr>
 * <tr class="even-row-color">
 * <td><p>name</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>The name attribute for the HTML button tag</p></td>
 * </tr>
 * <tr class="odd-row-color">
 * <td><p>value</p></td>
 * <td><p>false</p></td>
 * <td><p>true</p></td>
 * <td><p>The name attribute for the HTML button tag</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 3.1
 */
@SuppressWarnings("serial")
public class ButtonTag extends AbstractHtmlElementTag {

	/**
	 * The name of the '{@code disabled}' attribute.
	 */
	public static final String DISABLED_ATTRIBUTE = "disabled";


	private @Nullable TagWriter tagWriter;

	private @Nullable String name;

	private @Nullable String value;

	private boolean disabled;


	/**
	 * Get the value of the '{@code name}' attribute.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Set the value of the '{@code name}' attribute.
	 */
	@Override
	public @Nullable String getName() {
		return this.name;
	}

	/**
	 * Set the value of the '{@code value}' attribute.
	 */
	public void setValue(@Nullable String value) {
		this.value = value;
	}

	/**
	 * Get the value of the '{@code value}' attribute.
	 */
	public @Nullable String getValue() {
		return this.value;
	}

	/**
	 * Set the value of the '{@code disabled}' attribute.
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * Get the value of the '{@code disabled}' attribute.
	 */
	public boolean isDisabled() {
		return this.disabled;
	}


	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag("button");
		writeDefaultAttributes(tagWriter);
		tagWriter.writeAttribute("type", getType());
		writeValue(tagWriter);
		if (isDisabled()) {
			tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
		}
		tagWriter.forceBlock();
		this.tagWriter = tagWriter;
		return EVAL_BODY_INCLUDE;
	}

	/**
	 * Writes the '{@code value}' attribute to the supplied {@link TagWriter}.
	 * Subclasses may choose to override this implementation to control exactly
	 * when the value is written.
	 */
	protected void writeValue(TagWriter tagWriter) throws JspException {
		String valueToUse = (getValue() != null ? getValue() : getDefaultValue());
		tagWriter.writeAttribute("value",
				processFieldValue(getName(), getDisplayString(valueToUse), getType()));
	}

	/**
	 * Return the default value.
	 * @return the default value if none supplied
	 */
	protected String getDefaultValue() {
		return "Submit";
	}

	/**
	 * Get the value of the '{@code type}' attribute. Subclasses
	 * can override this to change the type of '{@code input}' element
	 * rendered. Default value is '{@code submit}'.
	 */
	protected String getType() {
		return "submit";
	}

	/**
	 * Closes the '{@code button}' block tag.
	 */
	@Override
	public int doEndTag() throws JspException {
		Assert.state(this.tagWriter != null, "No TagWriter set");
		this.tagWriter.endTag();
		return EVAL_PAGE;
	}

}

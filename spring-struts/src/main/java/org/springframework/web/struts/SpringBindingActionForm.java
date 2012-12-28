/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.web.struts;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.util.MessageResources;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

/**
 * A thin Struts ActionForm adapter that delegates to Spring's more complete
 * and advanced data binder and Errors object underneath the covers to bind
 * to POJOs and manage rejected values.
 *
 * <p>Exposes Spring-managed errors to the standard Struts view tags, through
 * exposing a corresponding Struts ActionMessages object as request attribute.
 * Also exposes current field values in a Struts-compliant fashion, including
 * rejected values (which Spring's binding keeps even for non-String fields).
 *
 * <p>Consequently, Struts views can be written in a completely traditional
 * fashion (with standard {@code html:form}, {@code html:errors}, etc),
 * seamlessly accessing a Spring-bound POJO form object underneath.
 *
 * <p>Note this ActionForm is designed explicitly for use in <i>request scope</i>.
 * It expects to receive an {@code expose} call from the Action, passing
 * in the Errors object to expose plus the current HttpServletRequest.
 *
 * <p>Example definition in {@code struts-config.xml}:
 *
 * <pre>
 * &lt;form-beans&gt;
 *   &lt;form-bean name="actionForm" type="org.springframework.web.struts.SpringBindingActionForm"/&gt;
 * &lt;/form-beans&gt;</pre>
 *
 * Example code in a custom Struts {@code Action}:
 *
 * <pre>
 * public ActionForward execute(ActionMapping actionMapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
 *   SpringBindingActionForm form = (SpringBindingActionForm) actionForm;
 *   MyPojoBean bean = ...;
 *   ServletRequestDataBinder binder = new ServletRequestDataBinder(bean, "myPojo");
 *   binder.bind(request);
 *   form.expose(binder.getBindingResult(), request);
 *   return actionMapping.findForward("success");
 * }</pre>
 *
 * This class is compatible with both Struts 1.2.x and Struts 1.1.
 * On Struts 1.2, default messages registered with Spring binding errors
 * are exposed when none of the error codes could be resolved.
 * On Struts 1.1, this is not possible due to a limitation in the Struts
 * message facility; hence, we expose the plain default error code there.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 * @see #expose(org.springframework.validation.Errors, javax.servlet.http.HttpServletRequest)
 * @deprecated as of Spring 3.0
 */
@Deprecated
@SuppressWarnings("serial")
public class SpringBindingActionForm extends ActionForm {

	private static final Log logger = LogFactory.getLog(SpringBindingActionForm.class);

	private static boolean defaultActionMessageAvailable = true;


	static {
		// Register special PropertyUtilsBean subclass that knows how to
		// extract field values from a SpringBindingActionForm.
		// As a consequence of the static nature of Commons BeanUtils,
		// we have to resort to this initialization hack here.
		ConvertUtilsBean convUtils = new ConvertUtilsBean();
		PropertyUtilsBean propUtils = new SpringBindingAwarePropertyUtilsBean();
		BeanUtilsBean beanUtils = new BeanUtilsBean(convUtils, propUtils);
		BeanUtilsBean.setInstance(beanUtils);

		// Determine whether the Struts 1.2 support for default messages
		// is available on ActionMessage: ActionMessage(String, boolean)
		// with "false" to be passed into the boolean flag.
		try {
			ActionMessage.class.getConstructor(new Class[] {String.class, boolean.class});
		}
		catch (NoSuchMethodException ex) {
			defaultActionMessageAvailable = false;
		}
	}


	private Errors errors;

	private Locale locale;

	private MessageResources messageResources;


	/**
	 * Set the Errors object that this SpringBindingActionForm is supposed
	 * to wrap. The contained field values and errors will be exposed
	 * to the view, accessible through Struts standard tags.
	 * @param errors the Spring Errors object to wrap, usually taken from
	 * a DataBinder that has been used for populating a POJO form object
	 * @param request the HttpServletRequest to retrieve the attributes from
	 * @see org.springframework.validation.DataBinder#getBindingResult()
	 */
	public void expose(Errors errors, HttpServletRequest request) {
		this.errors = errors;

		// Obtain the locale from Struts well-known location.
		this.locale = (Locale) request.getSession().getAttribute(Globals.LOCALE_KEY);

		// Obtain the MessageResources from Struts' well-known location.
		this.messageResources = (MessageResources) request.getAttribute(Globals.MESSAGES_KEY);

		if (errors != null && errors.hasErrors()) {
			// Add global ActionError instances from the Spring Errors object.
			ActionMessages actionMessages = (ActionMessages) request.getAttribute(Globals.ERROR_KEY);
			if (actionMessages == null) {
				request.setAttribute(Globals.ERROR_KEY, getActionMessages());
			}
			else {
				actionMessages.add(getActionMessages());
			}
		}
	}


	/**
	 * Return an ActionMessages representation of this SpringBindingActionForm,
	 * exposing all errors contained in the underlying Spring Errors object.
	 * @see org.springframework.validation.Errors#getAllErrors()
	 */
	private ActionMessages getActionMessages() {
		ActionMessages actionMessages = new ActionMessages();
		Iterator it = this.errors.getAllErrors().iterator();
		while (it.hasNext()) {
			ObjectError objectError = (ObjectError) it.next();
			String effectiveMessageKey = findEffectiveMessageKey(objectError);
			if (effectiveMessageKey == null && !defaultActionMessageAvailable) {
				// Need to specify default code despite it not being resolvable:
				// Struts 1.1 ActionMessage doesn't support default messages.
				effectiveMessageKey = objectError.getCode();
			}
			ActionMessage message = (effectiveMessageKey != null) ?
					new ActionMessage(effectiveMessageKey, resolveArguments(objectError.getArguments())) :
					new ActionMessage(objectError.getDefaultMessage(), false);
			if (objectError instanceof FieldError) {
				FieldError fieldError = (FieldError) objectError;
				actionMessages.add(fieldError.getField(), message);
			}
			else {
				actionMessages.add(ActionMessages.GLOBAL_MESSAGE, message);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Final ActionMessages used for binding: " + actionMessages);
		}
		return actionMessages;
	}

	private Object[] resolveArguments(Object[] arguments) {
		if (arguments == null || arguments.length == 0) {
			return arguments;
		}
		for (int i = 0; i < arguments.length; i++) {
			Object arg = arguments[i];
			if (arg instanceof MessageSourceResolvable) {
				MessageSourceResolvable resolvable = (MessageSourceResolvable)arg;
				String[] codes = resolvable.getCodes();
				boolean resolved = false;
				if (this.messageResources != null) {
					for (int j = 0; j < codes.length; j++) {
						String code = codes[j];
						if (this.messageResources.isPresent(this.locale, code)) {
							arguments[i] = this.messageResources.getMessage(
									this.locale, code, resolveArguments(resolvable.getArguments()));
							resolved = true;
							break;
						}
					}
				}
				if (!resolved) {
					arguments[i] = resolvable.getDefaultMessage();
				}
			}
		}
		return arguments;
	}

	/**
	 * Find the most specific message key for the given error.
	 * @param error the ObjectError to find a message key for
	 * @return the most specific message key found
	 */
	private String findEffectiveMessageKey(ObjectError error) {
		if (this.messageResources != null) {
			String[] possibleMatches = error.getCodes();
			for (int i = 0; i < possibleMatches.length; i++) {
				if (logger.isDebugEnabled()) {
					logger.debug("Looking for error code '" + possibleMatches[i] + "'");
				}
				if (this.messageResources.isPresent(this.locale, possibleMatches[i])) {
					if (logger.isDebugEnabled()) {
						logger.debug("Found error code '" + possibleMatches[i] + "' in resource bundle");
					}
					return possibleMatches[i];
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Could not find a suitable message error code, returning default message");
		}
		return null;
	}


	/**
	 * Get the formatted value for the property at the provided path.
	 * The formatted value is a string value for display, converted
	 * via a registered property editor.
	 * @param propertyPath the property path
	 * @return the formatted property value
	 * @throws NoSuchMethodException if called during Struts binding
	 * (without Spring Errors object being exposed), to indicate no
	 * available property to Struts
	 */
	private Object getFieldValue(String propertyPath) throws NoSuchMethodException {
		if (this.errors == null) {
			throw new NoSuchMethodException(
					"No bean properties exposed to Struts binding - performing Spring binding later on");
		}
		return this.errors.getFieldValue(propertyPath);
	}


	/**
	 * Special subclass of PropertyUtilsBean that it is aware of SpringBindingActionForm
	 * and uses it for retrieving field values. The field values will be taken from
	 * the underlying POJO form object that the Spring Errors object was created for.
	 */
	private static class SpringBindingAwarePropertyUtilsBean extends PropertyUtilsBean {

		@Override
		public Object getNestedProperty(Object bean, String propertyPath)
				throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

			// Extract Spring-managed field value in case of SpringBindingActionForm.
			if (bean instanceof SpringBindingActionForm) {
				SpringBindingActionForm form = (SpringBindingActionForm) bean;
				return form.getFieldValue(propertyPath);
			}

			// Else fall back to default PropertyUtils behavior.
			return super.getNestedProperty(bean, propertyPath);
		}
	}

}

/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.jmx.export.assembler;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.management.Descriptor;
import javax.management.JMException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.jmx.support.JmxUtils;

/**
 * Builds on the {@link AbstractMBeanInfoAssembler} superclass to
 * add a basic algorithm for building metadata based on the
 * reflective metadata of the MBean class.
 *
 * <p>The logic for creating MBean metadata from the reflective metadata
 * is contained in this class, but this class makes no decisions as to
 * which methods and properties are to be exposed. Instead it gives
 * subclasses a chance to 'vote' on each property or method through
 * the {@code includeXXX} methods.
 *
 * <p>Subclasses are also given the opportunity to populate attribute
 * and operation metadata with additional descriptors once the metadata
 * is assembled through the {@code populateXXXDescriptor} methods.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author David Boden
 * @since 1.2
 * @see #includeOperation
 * @see #includeReadAttribute
 * @see #includeWriteAttribute
 * @see #populateAttributeDescriptor
 * @see #populateOperationDescriptor
 */
public abstract class AbstractReflectiveMBeanInfoAssembler extends AbstractMBeanInfoAssembler {

	/**
	 * Identifies a getter method in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_GET_METHOD = "getMethod";

	/**
	 * Identifies a setter method in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_SET_METHOD = "setMethod";

	/**
	 * Constant identifier for the role field in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_ROLE = "role";

	/**
	 * Constant identifier for the getter role field value in a JMX {@link Descriptor}.
	 */
	protected static final String ROLE_GETTER = "getter";

	/**
	 * Constant identifier for the setter role field value in a JMX {@link Descriptor}.
	 */
	protected static final String ROLE_SETTER = "setter";

	/**
	 * Identifies an operation (method) in a JMX {@link Descriptor}.
	 */
	protected static final String ROLE_OPERATION = "operation";

	/**
	 * Constant identifier for the visibility field in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_VISIBILITY = "visibility";

	/**
	 * Lowest visibility, used for operations that correspond to
	 * accessors or mutators for attributes.
	 * @see #FIELD_VISIBILITY
	 */
	protected static final int ATTRIBUTE_OPERATION_VISIBILITY = 4;

	/**
	 * Constant identifier for the class field in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_CLASS = "class";
	/**
	 * Constant identifier for the log field in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_LOG = "log";

	/**
	 * Constant identifier for the logfile field in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_LOG_FILE = "logFile";

	/**
	 * Constant identifier for the currency time limit field in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_CURRENCY_TIME_LIMIT = "currencyTimeLimit";

	/**
	 * Constant identifier for the default field in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_DEFAULT = "default";

	/**
	 * Constant identifier for the persistPolicy field in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_PERSIST_POLICY = "persistPolicy";

	/**
	 * Constant identifier for the persistPeriod field in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_PERSIST_PERIOD = "persistPeriod";

	/**
	 * Constant identifier for the persistLocation field in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_PERSIST_LOCATION = "persistLocation";

	/**
	 * Constant identifier for the persistName field in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_PERSIST_NAME = "persistName";

	/**
	 * Constant identifier for the displayName field in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_DISPLAY_NAME = "displayName";

	/**
	 * Constant identifier for the units field in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_UNITS = "units";

	/**
	 * Constant identifier for the metricType field in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_METRIC_TYPE = "metricType";

	/**
	 * Constant identifier for the custom metricCategory field in a JMX {@link Descriptor}.
	 */
	protected static final String FIELD_METRIC_CATEGORY = "metricCategory";


	/**
	 * Default value for the JMX field "currencyTimeLimit".
	 */
	private Integer defaultCurrencyTimeLimit;

	/**
	 * Indicates whether or not strict casing is being used for attributes.
	 */
	private boolean useStrictCasing = true;

	private boolean exposeClassDescriptor = false;

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();


	/**
	 * Set the default for the JMX field "currencyTimeLimit".
	 * The default will usually indicate to never cache attribute values.
	 * <p>Default is none, not explicitly setting that field, as recommended by the
	 * JMX 1.2 specification. This should result in "never cache" behavior, always
	 * reading attribute values freshly (which corresponds to a "currencyTimeLimit"
	 * of {@code -1} in JMX 1.2).
	 * <p>However, some JMX implementations (that do not follow the JMX 1.2 spec
	 * in that respect) might require an explicit value to be set here to get
	 * "never cache" behavior: for example, JBoss 3.2.x.
	 * <p>Note that the "currencyTimeLimit" value can also be specified on a
	 * managed attribute or operation. The default value will apply if not
	 * overridden with a "currencyTimeLimit" value {@code >= 0} there:
	 * a metadata "currencyTimeLimit" value of {@code -1} indicates
	 * to use the default; a value of {@code 0} indicates to "always cache"
	 * and will be translated to {@code Integer.MAX_VALUE}; a positive
	 * value indicates the number of cache seconds.
	 * @see org.springframework.jmx.export.metadata.AbstractJmxAttribute#setCurrencyTimeLimit
	 * @see #applyCurrencyTimeLimit(javax.management.Descriptor, int)
	 */
	public void setDefaultCurrencyTimeLimit(Integer defaultCurrencyTimeLimit) {
		this.defaultCurrencyTimeLimit = defaultCurrencyTimeLimit;
	}

	/**
	 * Return default value for the JMX field "currencyTimeLimit", if any.
	 */
	protected Integer getDefaultCurrencyTimeLimit() {
		return this.defaultCurrencyTimeLimit;
	}

	/**
	 * Set whether to use strict casing for attributes. Enabled by default.
	 * <p>When using strict casing, a JavaBean property with a getter such as
	 * {@code getFoo()} translates to an attribute called {@code Foo}.
	 * With strict casing disabled, {@code getFoo()} would translate to just
	 * {@code foo}.
	 */
	public void setUseStrictCasing(boolean useStrictCasing) {
		this.useStrictCasing = useStrictCasing;
	}

	/**
	 * Return whether strict casing for attributes is enabled.
	 */
	protected boolean isUseStrictCasing() {
		return this.useStrictCasing;
	}

	/**
	 * Set whether to expose the JMX descriptor field "class" for managed operations.
	 * Default is "false", letting the JMX implementation determine the actual class
	 * through reflection.
	 * <p>Set this property to {@code true} for JMX implementations that
	 * require the "class" field to be specified, for example WebLogic's.
	 * In that case, Spring will expose the target class name there, in case of
	 * a plain bean instance or a CGLIB proxy. When encountering a JDK dynamic
	 * proxy, the <b>first</b> interface implemented by the proxy will be specified.
	 * <p><b>WARNING:</b> Review your proxy definitions when exposing a JDK dynamic
	 * proxy through JMX, in particular with this property turned to {@code true}:
	 * the specified interface list should start with your management interface in
	 * this case, with all other interfaces following. In general, consider exposing
	 * your target bean directly or a CGLIB proxy for it instead.
	 * @see #getClassForDescriptor(Object)
	 */
	public void setExposeClassDescriptor(boolean exposeClassDescriptor) {
		this.exposeClassDescriptor = exposeClassDescriptor;
	}

	/**
	 * Return whether to expose the JMX descriptor field "class" for managed operations.
	 */
	protected boolean isExposeClassDescriptor() {
		return this.exposeClassDescriptor;
	}

	/**
	 * Set the ParameterNameDiscoverer to use for resolving method parameter
	 * names if needed (e.g. for parameter names of MBean operation methods).
	 * <p>Default is a {@link DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * Return the ParameterNameDiscoverer to use for resolving method parameter
	 * names if needed (may be {@code null} in order to skip parameter detection).
	 */
	protected ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.parameterNameDiscoverer;
	}


	/**
	 * Iterate through all properties on the MBean class and gives subclasses
	 * the chance to vote on the inclusion of both the accessor and mutator.
	 * If a particular accessor or mutator is voted for inclusion, the appropriate
	 * metadata is assembled and passed to the subclass for descriptor population.
	 * @param managedBean the bean instance (might be an AOP proxy)
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @return the attribute metadata
	 * @throws JMException in case of errors
	 * @see #populateAttributeDescriptor
	 */
	@Override
	protected ModelMBeanAttributeInfo[] getAttributeInfo(Object managedBean, String beanKey) throws JMException {
		PropertyDescriptor[] props = BeanUtils.getPropertyDescriptors(getClassToExpose(managedBean));
		List<ModelMBeanAttributeInfo> infos = new ArrayList<ModelMBeanAttributeInfo>();

		for (PropertyDescriptor prop : props) {
			Method getter = prop.getReadMethod();
			if (getter != null && getter.getDeclaringClass() == Object.class) {
				continue;
			}
			if (getter != null && !includeReadAttribute(getter, beanKey)) {
				getter = null;
			}

			Method setter = prop.getWriteMethod();
			if (setter != null && !includeWriteAttribute(setter, beanKey)) {
				setter = null;
			}

			if (getter != null || setter != null) {
				// If both getter and setter are null, then this does not need exposing.
				String attrName = JmxUtils.getAttributeName(prop, isUseStrictCasing());
				String description = getAttributeDescription(prop, beanKey);
				ModelMBeanAttributeInfo info = new ModelMBeanAttributeInfo(attrName, description, getter, setter);

				Descriptor desc = info.getDescriptor();
				if (getter != null) {
					desc.setField(FIELD_GET_METHOD, getter.getName());
				}
				if (setter != null) {
					desc.setField(FIELD_SET_METHOD, setter.getName());
				}

				populateAttributeDescriptor(desc, getter, setter, beanKey);
				info.setDescriptor(desc);
				infos.add(info);
			}
		}

		return infos.toArray(new ModelMBeanAttributeInfo[infos.size()]);
	}

	/**
	 * Iterate through all methods on the MBean class and gives subclasses the chance
	 * to vote on their inclusion. If a particular method corresponds to the accessor
	 * or mutator of an attribute that is inclued in the managment interface, then
	 * the corresponding operation is exposed with the &quot;role&quot; descriptor
	 * field set to the appropriate value.
	 * @param managedBean the bean instance (might be an AOP proxy)
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @return the operation metadata
	 * @see #populateOperationDescriptor
	 */
	@Override
	protected ModelMBeanOperationInfo[] getOperationInfo(Object managedBean, String beanKey) {
		Method[] methods = getClassToExpose(managedBean).getMethods();
		List<ModelMBeanOperationInfo> infos = new ArrayList<ModelMBeanOperationInfo>();

		for (Method method : methods) {
			if (method.isSynthetic()) {
				continue;
			}
			if (method.getDeclaringClass().equals(Object.class)) {
				continue;
			}

			ModelMBeanOperationInfo info = null;
			PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
			if (pd != null) {
				if ((method.equals(pd.getReadMethod()) && includeReadAttribute(method, beanKey)) ||
						(method.equals(pd.getWriteMethod()) && includeWriteAttribute(method, beanKey))) {
					// Attributes need to have their methods exposed as
					// operations to the JMX server as well.
					info = createModelMBeanOperationInfo(method, pd.getName(), beanKey);
					Descriptor desc = info.getDescriptor();
					if (method.equals(pd.getReadMethod())) {
						desc.setField(FIELD_ROLE, ROLE_GETTER);
					}
					else {
						desc.setField(FIELD_ROLE, ROLE_SETTER);
					}
					desc.setField(FIELD_VISIBILITY, ATTRIBUTE_OPERATION_VISIBILITY);
					if (isExposeClassDescriptor()) {
						desc.setField(FIELD_CLASS, getClassForDescriptor(managedBean).getName());
					}
					info.setDescriptor(desc);
				}
			}

			// allow getters and setters to be marked as operations directly
			if (info == null && includeOperation(method, beanKey)) {
				info = createModelMBeanOperationInfo(method, method.getName(), beanKey);
				Descriptor desc = info.getDescriptor();
				desc.setField(FIELD_ROLE, ROLE_OPERATION);
				if (isExposeClassDescriptor()) {
					desc.setField(FIELD_CLASS, getClassForDescriptor(managedBean).getName());
				}
				populateOperationDescriptor(desc, method, beanKey);
				info.setDescriptor(desc);
			}

			if (info != null) {
				infos.add(info);
			}
		}

		return infos.toArray(new ModelMBeanOperationInfo[infos.size()]);
	}

	/**
	 * Creates an instance of {@code ModelMBeanOperationInfo} for the
	 * given method. Populates the parameter info for the operation.
	 * @param method the {@code Method} to create a {@code ModelMBeanOperationInfo} for
	 * @param name the logical name for the operation (method name or property name);
	 * not used by the default implementation but possibly by subclasses
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @return the {@code ModelMBeanOperationInfo}
	 */
	protected ModelMBeanOperationInfo createModelMBeanOperationInfo(Method method, String name, String beanKey) {
		MBeanParameterInfo[] params = getOperationParameters(method, beanKey);
		if (params.length == 0) {
			return new ModelMBeanOperationInfo(getOperationDescription(method, beanKey), method);
		}
		else {
			return new ModelMBeanOperationInfo(method.getName(),
				getOperationDescription(method, beanKey),
				getOperationParameters(method, beanKey),
				method.getReturnType().getName(),
				MBeanOperationInfo.UNKNOWN);
		}
	}

	/**
	 * Return the class to be used for the JMX descriptor field "class".
	 * Only applied when the "exposeClassDescriptor" property is "true".
	 * <p>The default implementation returns the first implemented interface
	 * for a JDK proxy, and the target class else.
	 * @param managedBean the bean instance (might be an AOP proxy)
	 * @return the class to expose in the descriptor field "class"
	 * @see #setExposeClassDescriptor
	 * @see #getClassToExpose(Class)
	 * @see org.springframework.aop.framework.AopProxyUtils#proxiedUserInterfaces(Object)
	 */
	protected Class getClassForDescriptor(Object managedBean) {
		if (AopUtils.isJdkDynamicProxy(managedBean)) {
			return AopProxyUtils.proxiedUserInterfaces(managedBean)[0];
		}
		return getClassToExpose(managedBean);
	}


	/**
	 * Allows subclasses to vote on the inclusion of a particular attribute accessor.
	 * @param method the accessor {@code Method}
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @return {@code true} if the accessor should be included in the management interface,
	 * otherwise {@code false}
	 */
	protected abstract boolean includeReadAttribute(Method method, String beanKey);

	/**
	 * Allows subclasses to vote on the inclusion of a particular attribute mutator.
	 * @param method the mutator {@code Method}.
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @return {@code true} if the mutator should be included in the management interface,
	 * otherwise {@code false}
	 */
	protected abstract boolean includeWriteAttribute(Method method, String beanKey);

	/**
	 * Allows subclasses to vote on the inclusion of a particular operation.
	 * @param method the operation method
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @return whether the operation should be included in the management interface
	 */
	protected abstract boolean includeOperation(Method method, String beanKey);

	/**
	 * Get the description for a particular attribute.
	 * <p>The default implementation returns a description for the operation
	 * that is the name of corresponding {@code Method}.
	 * @param propertyDescriptor the PropertyDescriptor for the attribute
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @return the description for the attribute
	 */
	protected String getAttributeDescription(PropertyDescriptor propertyDescriptor, String beanKey) {
		return propertyDescriptor.getDisplayName();
	}

	/**
	 * Get the description for a particular operation.
	 * <p>The default implementation returns a description for the operation
	 * that is the name of corresponding {@code Method}.
	 * @param method the operation method
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @return the description for the operation
	 */
	protected String getOperationDescription(Method method, String beanKey) {
		return method.getName();
	}

	/**
	 * Create parameter info for the given method.
	 * <p>The default implementation returns an empty array of {@code MBeanParameterInfo}.
	 * @param method the {@code Method} to get the parameter information for
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @return the {@code MBeanParameterInfo} array
	 */
	protected MBeanParameterInfo[] getOperationParameters(Method method, String beanKey) {
		ParameterNameDiscoverer paramNameDiscoverer = getParameterNameDiscoverer();
		String[] paramNames = (paramNameDiscoverer != null ? paramNameDiscoverer.getParameterNames(method) : null);
		if (paramNames == null) {
			return new MBeanParameterInfo[0];
		}

		MBeanParameterInfo[] info = new MBeanParameterInfo[paramNames.length];
		Class<?>[] typeParameters = method.getParameterTypes();
		for(int i = 0; i < info.length; i++) {
			info[i] = new MBeanParameterInfo(paramNames[i], typeParameters[i].getName(), paramNames[i]);
		}

		return info;
	}

	/**
	 * Allows subclasses to add extra fields to the {@code Descriptor} for an MBean.
	 * <p>The default implementation sets the {@code currencyTimeLimit} field to
	 * the specified "defaultCurrencyTimeLimit", if any (by default none).
	 * @param descriptor the {@code Descriptor} for the MBean resource.
	 * @param managedBean the bean instance (might be an AOP proxy)
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @see #setDefaultCurrencyTimeLimit(Integer)
	 * @see #applyDefaultCurrencyTimeLimit(javax.management.Descriptor)
	 */
	@Override
	protected void populateMBeanDescriptor(Descriptor descriptor, Object managedBean, String beanKey) {
		applyDefaultCurrencyTimeLimit(descriptor);
	}

	/**
	 * Allows subclasses to add extra fields to the {@code Descriptor} for a
	 * particular attribute.
	 * <p>The default implementation sets the {@code currencyTimeLimit} field to
	 * the specified "defaultCurrencyTimeLimit", if any (by default none).
	 * @param desc the attribute descriptor
	 * @param getter the accessor method for the attribute
	 * @param setter the mutator method for the attribute
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @see #setDefaultCurrencyTimeLimit(Integer)
	 * @see #applyDefaultCurrencyTimeLimit(javax.management.Descriptor)
	 */
	protected void populateAttributeDescriptor(Descriptor desc, Method getter, Method setter, String beanKey) {
		applyDefaultCurrencyTimeLimit(desc);
	}

	/**
	 * Allows subclasses to add extra fields to the {@code Descriptor} for a
	 * particular operation.
	 * <p>The default implementation sets the {@code currencyTimeLimit} field to
	 * the specified "defaultCurrencyTimeLimit", if any (by default none).
	 * @param desc the operation descriptor
	 * @param method the method corresponding to the operation
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @see #setDefaultCurrencyTimeLimit(Integer)
	 * @see #applyDefaultCurrencyTimeLimit(javax.management.Descriptor)
	 */
	protected void populateOperationDescriptor(Descriptor desc, Method method, String beanKey) {
		applyDefaultCurrencyTimeLimit(desc);
	}

	/**
	 * Set the {@code currencyTimeLimit} field to the specified
	 * "defaultCurrencyTimeLimit", if any (by default none).
	 * @param desc the JMX attribute or operation descriptor
	 * @see #setDefaultCurrencyTimeLimit(Integer)
	 */
	protected final void applyDefaultCurrencyTimeLimit(Descriptor desc) {
		if (getDefaultCurrencyTimeLimit() != null) {
			desc.setField(FIELD_CURRENCY_TIME_LIMIT, getDefaultCurrencyTimeLimit().toString());
		}
	}

	/**
	 * Apply the given JMX "currencyTimeLimit" value to the given descriptor.
	 * <p>The default implementation sets a value {@code >0} as-is (as number of cache seconds),
	 * turns a value of {@code 0} into {@code Integer.MAX_VALUE} ("always cache")
	 * and sets the "defaultCurrencyTimeLimit" (if any, indicating "never cache") in case of
	 * a value {@code <0}. This follows the recommendation in the JMX 1.2 specification.
	 * @param desc the JMX attribute or operation descriptor
	 * @param currencyTimeLimit the "currencyTimeLimit" value to apply
	 * @see #setDefaultCurrencyTimeLimit(Integer)
	 * @see #applyDefaultCurrencyTimeLimit(javax.management.Descriptor)
	 */
	protected void applyCurrencyTimeLimit(Descriptor desc, int currencyTimeLimit) {
		if (currencyTimeLimit > 0) {
			// number of cache seconds
			desc.setField(FIELD_CURRENCY_TIME_LIMIT, Integer.toString(currencyTimeLimit));
		}
		else if (currencyTimeLimit == 0) {
			// "always cache"
			desc.setField(FIELD_CURRENCY_TIME_LIMIT, Integer.toString(Integer.MAX_VALUE));
		}
		else {
			// "never cache"
			applyDefaultCurrencyTimeLimit(desc);
		}
	}

}

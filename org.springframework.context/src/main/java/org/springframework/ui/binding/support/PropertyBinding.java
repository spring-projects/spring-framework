package org.springframework.ui.binding.support;

import java.beans.PropertyDescriptor;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.ui.binding.Binding;
import org.springframework.ui.binding.support.GenericBinder.BindingContext;
import org.springframework.util.ReflectionUtils;

public class PropertyBinding extends AbstractBinding implements Binding {

	private PropertyDescriptor property;

	private Object object;

	public PropertyBinding(PropertyDescriptor property, Object object, BindingContext bindingContext) {
		super(bindingContext);
		this.property = property;
		this.object = object;
	}

	// Binding overrides
	
	@Override
	public boolean isEditable() {
		return isWriteable() && super.isEditable();
	}

	// implementing
	
	protected ValueModel getValueModel() {
		return new ValueModel() {
			public Object getValue() {
				return ReflectionUtils.invokeMethod(property.getReadMethod(), object);
			}

			public Class<?> getValueType() {
				return property.getPropertyType();
			}

			@SuppressWarnings("unchecked")
			public TypeDescriptor<?> getValueTypeDescriptor() {
				return new TypeDescriptor(new MethodParameter(property.getReadMethod(), -1));
			}

			public void setValue(Object value) {
				ReflectionUtils.invokeMethod(property.getWriteMethod(), object, value);
			}
		};
	}
	
	// internal helpers
	
	private boolean isWriteable() {
		return property.getWriteMethod() != null;
	}
}
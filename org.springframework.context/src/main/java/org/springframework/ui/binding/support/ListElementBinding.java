package org.springframework.ui.binding.support;

import java.util.List;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.ui.binding.support.GenericBinder.BindingContext;

public class ListElementBinding extends AbstractBinding {
	
	private List list;
	
	private int index;

	private Class<?> elementType;
	
	private BindingContext bindingContext;
	
	public ListElementBinding(int index, Class<?> elementType, List list, BindingContext bindingContext) {
		super(bindingContext);
		this.index = index;
		this.elementType = elementType;
		this.list = list;
		this.bindingContext = bindingContext;
	}
	
	@Override
	protected ValueModel getValueModel() {
		return new ValueModel() {
			public Object getValue() {
				return list.get(index);
			}

			public Class<?> getValueType() {
				if (elementType != null) {
					return elementType;
				} else {
					return getValue().getClass();
				}
			}

			public TypeDescriptor<?> getValueTypeDescriptor() {
				return TypeDescriptor.valueOf(getValueType());
			}

			public void setValue(Object value) {
				list.set(index, value);
			}
		};
	}

}
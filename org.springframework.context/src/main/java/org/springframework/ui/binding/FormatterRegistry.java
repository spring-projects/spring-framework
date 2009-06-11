package org.springframework.ui.binding;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.ui.format.AnnotationFormatterFactory;
import org.springframework.ui.format.Formatter;

/**
 * A centralized registry of Formatters indexed by property types.
 * 
 * @author Keith Donald
 */
public interface FormatterRegistry {

	/**
	 * Get the Formatter for the property type.
	 * @param propertyType the property type descriptor, which provides additional property metadata.
	 * @return the Formatter, or <code>null</code> if none is registered
	 */
	Formatter<?> getFormatter(TypeDescriptor<?> propertyType);
	
	/**
	 * Adds a Formatter that will format the values of properties of the provided type.
	 * The type should generally be a concrete class for a scalar value, such as BigDecimal, and not a collection value.
	 * The type may be an annotation type, which will have the Formatter format values of properties annotated with that annotation.
	 * Use {@link #add(AnnotationFormatterFactory)} when the format annotation defines configurable annotation instance values.
	 * <p>
	 * Note the Formatter's formatted object type does not have to equal the associated property type.
	 * When the property type differs from the formatted object type, the caller of the Formatter is expected to coerse a property value to the type expected by the Formatter.  
	 * @param formatter the formatter
	 * @param propertyType the type
	 */
	void add(Formatter<?> formatter, Class<?> propertyType);

	/**
	 * Adds a AnnotationFormatterFactory that will format values of properties annotated with a specific annotation.
	 * @param factory the annotation formatter factory
	 */
	void add(AnnotationFormatterFactory<?, ?> factory);

}

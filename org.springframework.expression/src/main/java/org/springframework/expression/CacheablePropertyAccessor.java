package org.springframework.expression;

/**
 * A CacheablePropertyAccessor is an optimized PropertyAccessor where the two parts of accessing the property are
 * separated: (1) resolving the property and (2) retrieving its value. In some cases there is a large cost to
 * discovering which property an expression refers to and once discovered it will always resolve to the same property.
 * In these situations a CacheablePropertyAccessor enables the resolution to be done once and a reusable object (an
 * executor) returned that can be called over and over to retrieve the property value without going through resolution
 * again.
 * <p>
 * 
 * @author Andy Clement
 */
public abstract class CacheablePropertyAccessor implements PropertyAccessor {

	/**
	 * Attempt to resolve the named property and return an executor that can be called to get the value of that
	 * property. Return null if the property cannot be resolved.
	 * 
	 * @param context the evaluation context
	 * @param target the target upon which the property is being accessed
	 * @param name the name of the property being accessed
	 * @return a reusable executor that can retrieve the property value
	 */
	public abstract PropertyReaderExecutor getReaderAccessor(EvaluationContext context, Object target, Object name);

	/**
	 * Attempt to resolve the named property and return an executor that can be called to set the value of that
	 * property. Return null if the property cannot be resolved.
	 * 
	 * @param context the evaluation context
	 * @param target the target upon which the property is being accessed
	 * @param name the name of the property to be set
	 * @return a reusable executor that can set the property value
	 */
	public abstract PropertyWriterExecutor getWriterAccessor(EvaluationContext context, Object target, Object name);

	// Implementation of PropertyAccessor follows, based on the resolver/executor model

	public final boolean canRead(EvaluationContext context, Object target, Object name) throws AccessException {
		return getReaderAccessor(context, target, name) != null;
	}

	public final boolean canWrite(EvaluationContext context, Object target, Object name) throws AccessException {
		return getWriterAccessor(context, target, name) != null;
	}

	public final Object read(EvaluationContext context, Object target, Object name) throws AccessException {
		return getReaderAccessor(context, target, name).execute(context, target);
	}

	public final void write(EvaluationContext context, Object target, Object name, Object newValue)
			throws AccessException {
		getWriterAccessor(context, target, name).execute(context, target, newValue);
	}

}

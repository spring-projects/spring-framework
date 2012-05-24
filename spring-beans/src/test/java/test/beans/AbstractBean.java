package test.beans;

import org.springframework.beans.factory.support.LookupMethodTests;

/**
 * A simple bean used for testing <code>lookup-method</code> constructors.
 * 
 * The actual test class which uses this bean is {@link LookupMethodTests}
 * @author kpietrzak
 *
 */
public abstract class AbstractBean {
	
	public abstract TestBean get();
	public abstract TestBean getOneArgument(String name);
	public abstract TestBean getTwoArguments(String name, int age);
	public abstract TestBean getThreeArguments(String name, int age, int anotherArg);

}

package org.springframework.expression.spel.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.MethodExecutor;

public class Spr7538Tests {

	@Ignore @Test
	public void repro() throws Exception {
		AlwaysTrueReleaseStrategy target = new AlwaysTrueReleaseStrategy();
		BeanFactoryTypeConverter converter = new BeanFactoryTypeConverter();

		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setTypeConverter(converter);

		List<Foo> arguments = new ArrayList<Foo>();

		// !!!! With the below line commented you'll get NPE. Uncomment and everything is OK!
		//arguments.add(new Foo());

		List<TypeDescriptor> paramDescriptors = new ArrayList<TypeDescriptor>();
		Method method = AlwaysTrueReleaseStrategy.class.getMethod("checkCompleteness", List.class);
		paramDescriptors.add(new TypeDescriptor(new MethodParameter(method, 0)));


		List<TypeDescriptor> argumentTypes = new ArrayList<TypeDescriptor>();
		argumentTypes.add(TypeDescriptor.forObject(arguments));
		ReflectiveMethodResolver resolver = new ReflectiveMethodResolver();
		MethodExecutor executor = resolver.resolve(context, target, "checkCompleteness", argumentTypes);

		Object result = executor.execute(context, target, arguments);
		System.out.println("Result: " + result);
	}

	public static class AlwaysTrueReleaseStrategy {
		public boolean checkCompleteness(List<Foo> messages) {
			return true;
		}
	}

	public static class Foo{}
}

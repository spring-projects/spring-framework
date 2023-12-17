package com.lxcecho.validator;

import com.lxcecho.validator.one.Person;
import com.lxcecho.validator.one.PersonValidator;
import com.lxcecho.validator.three.MyService;
import com.lxcecho.validator.two.MyValidation1;
import com.lxcecho.validator.two.MyValidation2;
import com.lxcecho.validator.two.User;
import com.lxcecho.validator.two.ValidationConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/12
 */
public class ValidatorTest {

	/**
	 * 校验测试
	 */
	@Test
	public void testPersonValidator() {
		// 创建 person 对象
		Person person = new Person();
		person.setName("lucy");
		person.setAge(250);

		// 创建 person 对应 dataBinder
		DataBinder binder = new DataBinder(person);

		// 设置校验器
		binder.setValidator(new PersonValidator());

		// 调用方法执行校验
		binder.validate();

		// 输出校验结果
		BindingResult result = binder.getBindingResult();
		System.out.println(result.getAllErrors());
	}

	@Test
	public void testValidationOne() {
		ApplicationContext context = new AnnotationConfigApplicationContext(ValidationConfig.class);
		MyValidation1 validation1 = context.getBean(MyValidation1.class);

		User user = new User();
		user.setName("lucy");
		user.setAge(20);
		boolean message = validation1.validatorByUserOne(user);
		System.out.println(message);
	}

	@Test
	public void testValidationTwo() {
		ApplicationContext context = new AnnotationConfigApplicationContext(ValidationConfig.class);
		MyValidation2 validation2 = context.getBean(MyValidation2.class);

		User user = new User();
		user.setName("lucy");
		user.setAge(200);

		boolean message = validation2.validatorByUserTwo(user);
		System.out.println(message);
	}

	@Test
	public void testValidationOnMethod() {
		ApplicationContext context = new AnnotationConfigApplicationContext(com.lxcecho.validator.three.ValidationConfig.class);
		MyService service = context.getBean(MyService.class);
		com.lxcecho.validator.three.User user = new com.lxcecho.validator.three.User();
		user.setName("lucy");
		user.setPhone("13566754321");
		user.setMessage("test echo");
		service.testMethod(user);
	}

}

package com.lxcecho.validator.four;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;

import java.lang.annotation.*;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {CannotBlankValidation.class})
public @interface CannotBlank {

	/**
	 * 默认错误信息
	 *
	 * @return
	 */
	String message() default "不能包含空格";

	/**
	 * 分组
	 *
	 * @return
	 */
	Class<?>[] groups() default {};

	/**
	 * 负载
	 *
	 * @return
	 */
	Class<? extends Payload>[] payload() default {};

	/**
	 * 指定多个时使用
	 */
	@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public @interface List {
		CannotBlank[] value();
	}

}

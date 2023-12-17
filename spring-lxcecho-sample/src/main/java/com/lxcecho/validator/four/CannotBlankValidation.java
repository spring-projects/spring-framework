package com.lxcecho.validator.four;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class CannotBlankValidation implements ConstraintValidator<CannotBlank, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		// null 时不进行校验
		if (value != null && value.contains(" ")) {
			// 获取默认提示信息
			String defaultConstraintMessageTemplate = context.getDefaultConstraintMessageTemplate();
			System.out.println("default message :" + defaultConstraintMessageTemplate);
			// 禁用默认提示信息
			context.disableDefaultConstraintViolation();
			// 设置提示语
			context.buildConstraintViolationWithTemplate("can not contains blank").addConstraintViolation();
			return false;
		}
		return false;
	}
}

package com.lxcecho.validator.three;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
@Service
@Validated
public class MyService {

	public String testMethod(@NotNull @Valid User user) {
		return user.toString();
	}

}

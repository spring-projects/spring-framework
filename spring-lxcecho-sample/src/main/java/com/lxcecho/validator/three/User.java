package com.lxcecho.validator.three;

import com.lxcecho.validator.four.CannotBlank;
import jakarta.validation.constraints.*;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class User {

	@NotNull
	private String name;

	@Min(0)
	@Max(150)
	private int age;

	@Pattern(regexp = "^1(3|4|5|7|8)\\d{9}$", message = "手机号码格式错误")
	@NotBlank(message = "手机号码不能为空")
	private String phone;

	@CannotBlank
	private String message;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

}

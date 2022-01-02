package cn.cxd.beans;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * TODO: 一句话简介
 *
 * @author ChenXiaoDong
 * @since 2022/1/2
 */
@Data
@Component
public class Car {
	private String name;
	private String brand;

	public Car() {
		name = "AE86";
		brand = "奔驰";
	}


}

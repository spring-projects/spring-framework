package com.gaok;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author : gaokang
 * @date : 2020/5/6
 */
@Component
public class CountyService {

	@Autowired
	private CityService cityService;

	public CountyService() {
		System.out.println("init countyService");
	}
}

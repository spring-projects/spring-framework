package org.springframework.test.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author : yining.gao@msxf.com
 * @Description :
 * @Date : Created in 15:09 2018/8/3
 * @Modify by :
 */
@Controller
public class TestController {

	private static final Log LOGGER =  LogFactory.getLog(TestController.class);
	@RequestMapping(value = "/test")
	public String testSession(HttpServletRequest request) {
		LOGGER.debug("Is dumber request ?"+request.getAttribute("isDumber"));
		LOGGER.debug("Handled time ?"+request.getAttribute("handledTime"));
		return "testTemplate";
	}
}

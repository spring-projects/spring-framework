package org.springframework.web.servlet.mvc.annotation;

import java.awt.Color;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class Spr7766Controller {

	@RequestMapping("/colors")
	public void handler(@RequestParam List<Color> colors) {
		Assert.isTrue(colors.size() == 2);
		Assert.isTrue(colors.get(0).equals(Color.WHITE));
		Assert.isTrue(colors.get(1).equals(Color.BLACK));
	}
}

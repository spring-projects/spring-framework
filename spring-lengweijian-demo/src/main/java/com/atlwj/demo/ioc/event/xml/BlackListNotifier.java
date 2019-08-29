package com.atlwj.demo.ioc.event.xml;

import org.springframework.context.ApplicationListener;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author lengweijian
 */
public class BlackListNotifier implements ApplicationListener<BlackListEvent> {

	@Override
	public void onApplicationEvent(BlackListEvent event) {
		// notify appropriate parties via notificationAddress..
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss SSS");
		String format = simpleDateFormat.format(new Date(event.getTimestamp()));
		System.out.printf("当前系统时间： %s\n",format);
		System.out.printf("当前邮件:%s  由于历史原因已经位列黑名单中，想永久解除黑名单请充值100¥",event.getAddress());
	}
}

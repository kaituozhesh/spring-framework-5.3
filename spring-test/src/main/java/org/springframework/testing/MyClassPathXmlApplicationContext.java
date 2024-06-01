package org.springframework.testing;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @Description:
 * @ClassName: MyClassPathXmlApplicationContext
 * @Auther: lin
 * @Date: 2024/6/1 14:46
 * @Version: 1.0
 */
public class MyClassPathXmlApplicationContext extends ClassPathXmlApplicationContext {

	public MyClassPathXmlApplicationContext(String... configLocations) {
		super(configLocations);
	}

	@Override
	protected void initPropertySources() {
		System.out.println("自定义initPropertySources");
//		getEnvironment().setRequiredProperties("abc");
	}
}

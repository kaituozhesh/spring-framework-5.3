package org.springframework.testing;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.testing.aop.AopTestServiceImpl;
import org.springframework.testing.bean.MyBean;
import org.springframework.testing.tag.User;

/**
 * @Description:
 * @ClassName: ClassPathTest
 * @Auther: lin
 * @Date: 2024/4/6 09:21
 * @Version: 1.0
 */
public class ClassPathTest {
	public static void main(String[] args) {
//		iocTest();
//		aopTest();
		tagTest();
	}

	private static void tagTest() {
		ApplicationContext ap = new ClassPathXmlApplicationContext("applicationContext.xml");
		// 从容器中获取 bean
		User dzg = (User) ap.getBean("dzg");

		System.out.println(dzg.getEmail());
	}

	public static void iocTest() {
		ApplicationContext ap = new MyClassPathXmlApplicationContext("applicationContext.xml");
		// 从容器中获取 bean
		MyBean myBean = ap.getBean(MyBean.class);

		System.out.println(myBean.getName());
	}

	public static void aopTest() {
		ApplicationContext ap = new ClassPathXmlApplicationContext("applicationContext.xml");
		// 从容器中获取 bean
		Object aopTestService = ap.getBean("aopTestService");
		if (aopTestService instanceof AopTestServiceImpl) {
			((AopTestServiceImpl) aopTestService).test();
		}
	}
}

/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}


	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// WARNING: Although it may appear that the body of this method can be easily
		// refactored to avoid the use of multiple loops and multiple lists, the use
		// of multiple lists and multiple passes over the names of processors is
		// intentional. We must ensure that we honor the contracts for PriorityOrdered
		// and Ordered processors. Specifically, we must NOT cause processors to be
		// instantiated (via getBean() invocations) or registered in the ApplicationContext
		// in the wrong order.
		//
		// Before submitting a pull request (PR) to change this method, please review the
		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
		// to ensure that your proposal does not result in a breaking change:
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		// 要执行回调方法和已执行回调方法的beanName集合，用于防止重复回调
		Set<String> processedBeans = new HashSet<>();

		/* beanFactory是否属于BeanDefinitionRegistry，一般都是，所有都会走这一个逻辑 */
		if (beanFactory instanceof BeanDefinitionRegistry) {
			// beanFactory强转为BeanDefinitionRegistry类型
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			// 这个集合保存BeanFactoryPostProcessor类型的后置处理器实例（用于最后执行postProcessBeanFactory回调方法）
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			// 这个集合保存BeanDefinitionRegistryPostProcessor类型的后置处理器实例（用于最后执行postProcessBeanFactory回调方法）
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			/*
			 * 1 首先遍历、处理beanFactoryPostProcessors集合
			 * 对于BeanDefinitionRegistryPostProcessor类型的对象回调postProcessBeanDefinitionRegistry方法
			 * 并加入registryProcessors集合中，非这个类型的BeanFactoryPostProcessor加入regularPostProcessors集合中
			 */
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				/* 1.1 如果属于BeanDefinitionRegistryPostProcessor类型 */
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					// postProcessor强转为BeanDefinitionRegistryPostProcessor类型
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					/* 按照遍历顺序，回调postProcessBeanDefinitionRegistry方法 */
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					// 加入到registryProcessors集合中(用于最后执行postProcessBeanFactory回调方法)
					registryProcessors.add(registryProcessor);
				}
				/* 1.2 如果不属于BeanDefinitionRegistryPostProcessor类型 */
				else {
					// 加入到regularPostProcessors集合中
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			// 这个集合临时保存当前准备创建并执行回调的BeanDefinitionRegistryPostProcessor类型的后置处理器实例，毁掉完毕即清理
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			/*
			 * 2 对于beanFactory中，所有实现了PriorityOrdered接口的BeanDefinitionRegistryPostProcessor类型的bean定义进行实例化
			 * 随后对这一批BeanDefinitionRegistryPostProcessor实例进行排序，最后按照排序顺序从前向后回调postProcessBeanDefinitionRegistry方法
			 */
			// 从beanFactory中获取所有BeanDefinitionRegistryPostProcessor类型的bean定义的名称数组
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			// 遍历beanFactory中的BeanDefinitionRegistryPostProcessor类型的bean定义的名称数组
			for (String ppName : postProcessorNames) {
				// 如果该名称的bean定义还实现了PriorityOrdered接口
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					// 当前beanName的BeanDefinitionRegistryPostProcessor实例加入到currentRegistryProcessors集合
					// 这个getBean方法实际上已经将bean实例创建出来了
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					// 加入到processedBeans集合
					processedBeans.add(ppName);
				}
			}
			//到这一步currentRegistryProcessors集合的元素都是实现了PriorityOrdered接口的类型实例

			//这里对currentRegistryProcessors集合的元素根据Ordered顺序进行排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			//currentRegistryProcessors整体加入到registryProcessors集合中(用于最后执行postProcessBeanFactory回调方法)
			registryProcessors.addAll(currentRegistryProcessors);
			//对于currentRegistryProcessors集合中的已排序的BeanDefinitionRegistryPostProcessor按照顺序从前向后回调postProcessBeanDefinitionRegistry方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			//回调完毕之后清空currentRegistryProcessors集合
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	/**
	 * PostProcessorRegistrationDelegate的静态方法
	 * <p>
	 * 实例化和注册所有的BeanPostProcessor，如果给出显式顺序，则按照顺序注册。
	 *
	 * @param beanFactory        bean工厂
	 * @param applicationContext 上下文容器
	 */
	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		// WARNING: Although it may appear that the body of this method can be easily
		// refactored to avoid the use of multiple loops and multiple lists, the use
		// of multiple lists and multiple passes over the names of processors is
		// intentional. We must ensure that we honor the contracts for PriorityOrdered
		// and Ordered processors. Specifically, we must NOT cause processors to be
		// instantiated (via getBean() invocations) or registered in the ApplicationContext
		// in the wrong order.
		//
		// Before submitting a pull request (PR) to change this method, please review the
		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
		// to ensure that your proposal does not result in a breaking change:
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		// 从beanFactory中获取所有BeanPostProcessor类型的bean定义的名称数组
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		//beanProcessor的目标计数器
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		//注册一个BeanPostProcessorChecker处理器用
		//会在Bean创建完后检查可在当前Bean上起作用的BeanPostProcessor个数与总的BeanPostProcessor个数，如果起作用的个数少于总数，则输出日志信息。
		beanFactory.addBeanPostProcessor(new PostProcessorRegistrationDelegate.BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		// 这个集合保存实现了PriorityOrdered接口的BeanPostProcessor实例
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		// 这个集合保存Spring内部的BeanPostProcessor实例
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		// 这个集合保存实现了Ordered接口的BeanPostProcessor的beanName
		List<String> orderedPostProcessorNames = new ArrayList<>();
		// 这个集合保存普通的BeanPostProcessor的beanName
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		// 遍历postProcessorNames数组
		for (String ppName : postProcessorNames) {
			/* 如果该名称的bean定义还实现了PriorityOrdered接口，那么初始化 */
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				// 获取该名称的bean定义的实例，这一步创建了BeanPostProcessor实例
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				// 该实例加入到priorityOrderedPostProcessors集合
				priorityOrderedPostProcessors.add(pp);
				// 如果该实例还实现了MergedBeanDefinitionPostProcessor接口
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					// 该实例加入到internalPostProcessors集合
					internalPostProcessors.add(pp);
				}
			}
			/* 否则，如果该名称的bean定义还实现了Ordered接口 */
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				//beanName加入到orderedPostProcessorNames集合
				orderedPostProcessorNames.add(ppName);
			}
			/* 否则，如果该名称的bean定义即没有实现PriorityOrdered接口也没有实现Ordered接口 */
			else {
				// beanName加入到nonOrderedPostProcessorNames集合
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		/*
		 * 对实现了PriorityOrdered接口的BeanPostProcessor实例进行排序，随后按照排序顺序从前向后注册BeanPostProcessor实例
		 */
		// 这里对currentRegistryProcessors集合的元素根据Ordered顺序进行排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 注册给定的BeanPostProcessor
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);


		// 这个集合保存实现了Ordered接口的BeanPostProcessor实例
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		// 遍历orderedPostProcessorNames数组
		for (String ppName : orderedPostProcessorNames) {
			// 获取该名称的bean定义的实例，这一步创建了BeanPostProcessor实例
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			// 该实例加入到orderedPostProcessors集合
			orderedPostProcessors.add(pp);
			// 如果该实例还实现了MergedBeanDefinitionPostProcessor接口
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				// 该实例加入到internalPostProcessors集合
				internalPostProcessors.add(pp);
			}
		}
		/*
		 * 对实现了Ordered接口的BeanPostProcessor实例进行排序，随后按照排序顺序从前向后注册BeanPostProcessor实例
		 */
		// 这里对orderedPostProcessors集合的元素根据Ordered顺序进行排序
		sortPostProcessors(orderedPostProcessors, beanFactory);
		// 注册给定的BeanPostProcessor
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);


		// 这个集合保存普通的BeanPostProcessor实例
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		// 遍历nonOrderedPostProcessorNames数组
		for (String ppName : nonOrderedPostProcessorNames) {
			// 获取该名称的bean定义的实例，这一步创建了BeanPostProcessor实例
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			// 该实例加入到nonOrderedPostProcessors集合
			nonOrderedPostProcessors.add(pp);
			// 如果该实例还实现了MergedBeanDefinitionPostProcessor接口
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				// 该实例加入到internalPostProcessors集合
				internalPostProcessors.add(pp);
			}
		}
		//注册给定的BeanPostProcessor，不需要排序
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		/*
		 * 对实现了MergedBeanDefinitionPostProcessor接口的Spring内部的BeanPostProcessor实例进行排序，随后按照排序顺序从前向后注册BeanPostProcessor实例
		 */
		// 这里对internalPostProcessors集合的元素根据Ordered顺序进行排序
		sortPostProcessors(internalPostProcessors, beanFactory);
		// 注册给定的BeanPostProcessor，相当于内部的BeanPostProcessor会被移动到处理器集合的尾部
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		/*
		 * 重新注册ApplicationListenerDetector，会被移到处理器集合的末尾
		 */
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		if (postProcessors.size() <= 1) {
			return;
		}
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry, ApplicationStartup applicationStartup) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanDefRegistry = applicationStartup.start("spring.context.beandef-registry.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanDefinitionRegistry(registry);
			postProcessBeanDefRegistry.end();
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanFactory = beanFactory.getApplicationStartup().start("spring.context.bean-factory.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanFactory(beanFactory);
			postProcessBeanFactory.end();
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		if (beanFactory instanceof AbstractBeanFactory) {
			// Bulk addition is more efficient against our CopyOnWriteArrayList there
			((AbstractBeanFactory) beanFactory).addBeanPostProcessors(postProcessors);
		}
		else {
			for (BeanPostProcessor postProcessor : postProcessors) {
				beanFactory.addBeanPostProcessor(postProcessor);
			}
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}

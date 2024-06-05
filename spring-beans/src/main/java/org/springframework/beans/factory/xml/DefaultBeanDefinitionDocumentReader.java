/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link BeanDefinitionDocumentReader} interface that
 * reads bean definitions according to the "spring-beans" DTD and XSD format
 * (Spring's default XML bean definition format).
 *
 * <p>The structure, elements, and attribute names of the required XML document
 * are hard-coded in this class. (Of course a transform could be run if necessary
 * to produce this format). {@code <beans>} does not need to be the root
 * element of the XML document: this class will parse all bean definition elements
 * in the XML file, regardless of the actual root element.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Erik Wiersma
 * @since 18.12.2003
 */
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

	public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;

	public static final String NESTED_BEANS_ELEMENT = "beans";

	public static final String ALIAS_ELEMENT = "alias";

	public static final String NAME_ATTRIBUTE = "name";

	public static final String ALIAS_ATTRIBUTE = "alias";

	public static final String IMPORT_ELEMENT = "import";

	public static final String RESOURCE_ATTRIBUTE = "resource";

	public static final String PROFILE_ATTRIBUTE = "profile";


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private XmlReaderContext readerContext;

	@Nullable
	private BeanDefinitionParserDelegate delegate;


	/**
	 * 从给定的DOM文档中读取bean定义信息，并在给定的读取器上下文中（readerContext）将它们注册到注册表中
	 */
	@Override
	public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
		this.readerContext = readerContext;
		// 解析doc的根节点，并将解析到的bean定义注册到注册表中
		doRegisterBeanDefinitions(doc.getDocumentElement());
	}

	/**
	 * Return the descriptor for the XML resource that this parser works on.
	 */
	protected final XmlReaderContext getReaderContext() {
		Assert.state(this.readerContext != null, "No XmlReaderContext available");
		return this.readerContext;
	}

	/**
	 * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor}
	 * to pull the source metadata from the supplied {@link Element}.
	 */
	@Nullable
	protected Object extractSource(Element ele) {
		return getReaderContext().extractSource(ele);
	}


	/**
	 * 从给定的文档根标签<beans />中查找、注册每个 bean 定义。
	 */
	@SuppressWarnings("deprecation")  // for Environment.acceptsProfiles(String...)
	protected void doRegisterBeanDefinitions(Element root) {
		// 获取bean定义的解析器对象delegate，首先获取目前的delegate作为parent
		// 因为 <beans/> 标签可以嵌套，也有父子关系，可能进行递归调用
		BeanDefinitionParserDelegate parent = this.delegate;
		/*
		 * 根据root和parent创建新的delegate，同时解析<beans/>根标签的属性
		 */
		this.delegate = createDelegate(getReaderContext(), root, parent);

		// 是否是默认命名空间 http://www.springframework.org/schema/beans的标签
		if (this.delegate.isDefaultNamespace(root)) {
			// 获取当前<beans/>标签的profile属性的值
			String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
			// 如果指定了profile属性
			if (StringUtils.hasText(profileSpec)) {
				String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
						profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
				// 如果环境变量中指定活动的profile没有包含当前<beans/>标签的profile属性值
				// 那么不加载这个<beans/>标签下的bean定义直接返回
				if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipped XML bean definition file due to specified profiles [" + profileSpec +
								"] not matching: " + getReaderContext().getResource());
					}
					return;
				}
			}
		}

		preProcessXml(root);
		// 真正的从根元素开始进行bean定义的解析
		parseBeanDefinitions(root, this.delegate);
		postProcessXml(root);

		this.delegate = parent;
	}

	/**
	 * 创建bean定义的解析器，解析<beans/>根节点元素的属性
	 *
	 * @param readerContext
	 * @param root           根节点元素
	 * @param parentDelegate 父解析器
	 * @return
	 */
	protected BeanDefinitionParserDelegate createDelegate(
			XmlReaderContext readerContext, Element root, @Nullable BeanDefinitionParserDelegate parentDelegate) {
		// 创建解析器
		BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
		// 设置解析器默认值，即解析<beans/>跟标签的属性
		delegate.initDefaults(root, parentDelegate);
		// 返回解析器
		return delegate;
	}

	/**
	 * Parse the elements at the root level in the document:
	 * "import", "alias", "bean".
	 *
	 * @param root the DOM root element of the document
	 */
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		// 如果属于命名空间下的标签 <import/>  <alias/>  <bean/>  <beans/>
		if (delegate.isDefaultNamespace(root)) {
			// 获取标签下的所有直接子节点
			NodeList nl = root.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				// 标签之间的空白换行符号/n也会算作一个Node节点 -> DeferredTextImpl，标签之间被注释的语句也会算作一个Node节点 -> DeferredCommentImpl
				// 这里需要筛选出真正需要被解析的标签元素节点，即Element -> DeferredElementNSImpl，因此XML中标签之间的注释在一定程度上也会增加遍历以及判断成本
				if (node instanceof Element) {
					Element ele = (Element) node;
					// 属于默认命名空间下的标签 <import/>、<alias/>、<bean/>、<beans/>
					if (delegate.isDefaultNamespace(ele)) {
						parseDefaultElement(ele, delegate);
					}
					// 其他扩展标签，<mvc/>、<task/>、<context/>、<aop/>等。
					else {
						delegate.parseCustomElement(ele);
					}
				}
			}
		}
		// 其他命名空间的扩展标签，<mvc/>、<task/>、<context/>、<aop/>等。
		else {
			delegate.parseCustomElement(root);
		}
	}

	private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
		// 如果是<import/>标签，表示引入其他XML文件
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			importBeanDefinitionResource(ele);
		}
		// 如果是<alias/>标签，表示bean别名
		else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			processAliasRegistration(ele);
		}
		// 如果是<bean/>标签，表示bean定义
		else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			processBeanDefinition(ele, delegate);
		}
		// 如果是<beans/>标签，表示嵌套的<beans/>标签
		else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
			// 递归调用doRegisterBeanDefinitions方法解析<bean/>标签
			doRegisterBeanDefinitions(ele);
		}
	}

	/**
	 * 解析<import/>标签，将给定资源的 bean 定义加载到 bean 工厂。
	 */
	protected void importBeanDefinitionResource(Element ele) {
		// 获取resource属性值
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		// 如果没有为null或者""或者空白字符，那么将抛出异常
		if (!StringUtils.hasText(location)) {
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}

		// 解析资源路径字符串: e.g. "${user.dir}"
		location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);

		Set<Resource> actualResources = new LinkedHashSet<>(4);

		// 判断该路径是绝对的还是相对的 URI
		boolean absoluteLocation = false;
		try {
			// 如果是以"classpath*:"、"classpath:"开头或者可以据此创立一个URL对象，或者此URI是一个绝对路径，那么就是绝对URI
			absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
		} catch (URISyntaxException ex) {
			// cannot convert to an URI, considering the location relative
			// unless it is the well-known Spring prefix "classpath*:"
		}

		// 绝对路径
		if (absoluteLocation) {
			try {
				// 加载指定路径下面的配置文件
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		}
		// 相对路径
		else {
			// No URL -> considering resource location as relative to the current file.
			try {
				int importCount;
				// 尝试转换为单个资源
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				// 此资源是否存在确定的一个文件，如果使用了Ant通配符，表示匹配多个资源文件，那么不会存在
				if (relativeResource.exists()) {
					// 如果是确定的一个文件，那么解析该文件
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					actualResources.add(relativeResource);
				}
				else {
					String baseLocation = getReaderContext().getResource().getURL().toString();
					// 根据当前<import/>标签所在文件路径将相对路径转为绝对路径然后再进行解析
					importCount = getReaderContext().getReader().loadBeanDefinitions(
							StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			} catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from relative location [" + location + "]", ele, ex);
			}
		}
		// 过去解析的所有Resource资源的数组
		Resource[] actResArray = actualResources.toArray(new Resource[0]);
		// 发布事件
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
	}

	/**
	 * 解析给定的<alias/>标签，在注册表中注册别名映射。
	 */
	protected void processAliasRegistration(Element ele) {
		// 获取name和alias属性
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
		boolean valid = true;
		// 为空则抛出异常
		if (!StringUtils.hasText(name)) {
			getReaderContext().error("Name must not be empty", ele);
			valid = false;
		}
		if (!StringUtils.hasText(alias)) {
			getReaderContext().error("Alias must not be empty", ele);
			valid = false;
		}
		if (valid) {
			try {
				// 调用registerAlias方法，将beanName和aliases中的每一个别名注册到registry的缓存中。
				getReaderContext().getRegistry().registerAlias(name, alias);
			}
			catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}

	/**
	 * 处理给定的<bean/>标签，解析bean定义并将其注册到注册表
	 * Process the given bean element, parsing the bean definition
	 * and registering it with the registry.
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		// 1. 核心：解析<bean/>标签对象，解析完之后转换为一个BeanDefinition（BeanDefinitionHolder）对象
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		if (bdHolder != null) {
			// 2. 对于<bean/>标签下的自定义属性和子标签进行解析
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				// Register the final decorated instance.
				// 3. 核心：将解析之后的BeanDefinition注册到register中，这里的register就是bean工厂
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" +
						bdHolder.getBeanName() + "'", ele, ex);
			}
			// Send registration event.
			// 4. 最后发布注册事件，通知相关的监听器，这个Bean已经加载完成了
			getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}


	/**
	 * Allow the XML to be extensible by processing any custom element types first,
	 * before we start to process the bean definitions. This method is a natural
	 * extension point for any other custom pre-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 *
	 * @see #getReaderContext()
	 */
	protected void preProcessXml(Element root) {
	}

	/**
	 * Allow the XML to be extensible by processing any custom element types last,
	 * after we finished processing the bean definitions. This method is a natural
	 * extension point for any other custom post-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 *
	 * @see #getReaderContext()
	 */
	protected void postProcessXml(Element root) {
	}

}

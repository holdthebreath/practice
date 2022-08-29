# Spring Boot

## 特性

1. 自动装配/配置 -Auto Configuration
2. 起步依赖-Starter Dependency
3. 命令行界面-Spring Boot CLI
4. 运行监控-Actuator

### 自动装配

Spring Boot只是在Spring基础上，通过SPI的方式，做了进一步优化。
**SpringBoot 定义了一套接口规范，这套规范规定：SpringBoot 在启动时会扫描外部引用 jar
包中的META-INF/spring.factories文件，将文件中配置的类型信息加载到 Spring 容器（此处涉及到 JVM 类加载机制与 Spring
的容器知识），并执行类中定义的各种操作。对于外部 jar 来说，只需要按照 SpringBoot 定义的标准，就能将自己的功能装置进
SpringBoot。**
没有 Spring Boot 的情况下，如果我们需要引入第三方依赖，需要手动配置，非常麻烦。但是，Spring Boot 中，我们直接引入一个 starter
即可。
通过注解或者一些简单的配置就能在 Spring Boot 的帮助下实现某块功能。

#### Java中SPI机制

SPI全称Service Provider Interface，是Java提供的一套用来被第三方实现或者扩展的API，它可以用来启用框架扩展和替换组件。
![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/202208241656565.png)
Java SPI 实际上是“基于接口的编程＋策略模式＋配置文件”组合实现的动态加载机制。
Java SPI就是提供这样的一个机制：为某个接口寻找服务实现的机制。有点类似IOC的思想，就是将装配的控制权移到程序之外，在模块化设计中这个机制尤其重要。所以SPI的核心思想就是解耦。

#### SPI其他待补充

和dubbo spi的区别

#### 如何实现自动装配

SpringBoot 的核心注解 @SpringBootApplication
大概可以把 @SpringBootApplication看作是 @Configuration、@EnableAutoConfiguration、@ComponentScan 注解的集合。

- @EnableAutoConfiguration：启用 SpringBoot 的自动配置机制
- @Configuration：允许在上下文中注册额外的 bean 或导入其他配置类
- @ComponentScan： 扫描被@Component (@Service,@Controller)注解的 bean，注解默认会扫描**启动类所在的包**下所有的类
  ，可以自定义不扫描某些bean。如下图所示，容器中将排除TypeExcludeFilter和AutoConfigurationExcludeFilter。

##### @EnableAutoConfiguration

@EnableAutoConfiguration只是一个简单地注解，自动装配核心功能的实现实际是通过**AutoConfigurationImportSelector**类。

##### AutoConfigurationImportSelector加载自动装配类

```java
public class AutoConfigurationImportSelector implements DeferredImportSelector, BeanClassLoaderAware, ResourceLoaderAware, BeanFactoryAware, EnvironmentAware, Ordered {

}

public interface DeferredImportSelector extends ImportSelector {

}

public interface ImportSelector {
    String[] selectImports(AnnotationMetadata var1);
}
```

AutoConfigurationImportSelector 类实现了 ImportSelector接口，也就实现了这个接口中的
selectImports方法，该方法主要用于**获取所有符合条件的类的全限定类名，这些类需要被加载到 IoC 容器中**。

```java
public class AutoConfigurationImportSelector implements DeferredImportSelector, BeanClassLoaderAware, ResourceLoaderAware, BeanFactoryAware, EnvironmentAware, Ordered {
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        if (!this.isEnabled(annotationMetadata)) {
            // <1>.判断自动装配开关是否打开
            return NO_IMPORTS;
        } else {
            //<2>.获取所有需要装配的bean
            AutoConfigurationEntry autoConfigurationEntry = this.getAutoConfigurationEntry(annotationMetadata);
            return StringUtils.toStringArray(autoConfigurationEntry.getConfigurations());
        }
    }

    protected AutoConfigurationEntry getAutoConfigurationEntry(AnnotationMetadata annotationMetadata) {
        if (!isEnabled(annotationMetadata)) {
            return EMPTY_ENTRY;
        }
//      获取@EnableAutoConfiguration注解属性 exclude 和 excludeName
        AnnotationAttributes attributes = getAttributes(annotationMetadata);
//      获取需要自动装配的所有配置类，读取META-INF/spring.factories
        List<String> configurations = getCandidateConfigurations(annotationMetadata, attributes);
//      移除重复的依赖
        configurations = removeDuplicates(configurations);
        Set<String> exclusions = getExclusions(annotationMetadata, attributes);
        checkExcludedClasses(configurations, exclusions);
//      移除全部@EnableAutoConfiguration注解属性 exclude 和 excludeName的设置的依赖      
        configurations.removeAll(exclusions);
//      过滤没有全部满足@ConditionalOnXX 中的所有条件的类。      
        configurations = getConfigurationClassFilter().filter(configurations);
        fireAutoConfigurationImportEvents(configurations, exclusions);
        return new AutoConfigurationEntry(configurations, exclusions);
    }
}
```

**XXXAutoConfiguration的作用就是按需加载组件**
不光是当前依赖下的META-INF/spring.factories被读取到，**所有Spring Boot Starter下的META-INF/spring.factories都会被读取到**

定义bean满足的条件的注解

- @ConditionalOnBean：当容器里有指定 Bean 的条件下
- @ConditionalOnMissingBean：当容器里没有指定 Bean 的情况下
- @ConditionalOnSingleCandidate：当指定 Bean 在容器中只有一个，或者虽然有多个但是指定首选 Bean
- @ConditionalOnClass：当类路径下有指定类的条件下
- @ConditionalOnMissingClass：当类路径下没有指定类的条件下
- @ConditionalOnProperty：指定的属性是否有指定的值
- @ConditionalOnResource：类路径是否有指定的值
- @ConditionalOnExpression：基于 SpEL 表达式作为判断条件
- @ConditionalOnJava：基于 Java 版本作为判断条件
- @ConditionalOnJndi：在 JNDI 存在的条件下差在指定的位置
- @ConditionalOnNotWebApplication：当前项目不是 Web 项目的条件下
- @ConditionalOnWebApplication：当前项目是 Web 项 目的条件下







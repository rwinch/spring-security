package org.springframework.security.config.http

import org.aopalliance.aop.Advice
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.springframework.aop.Advisor
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException
import org.springframework.security.web.access.AccessDeniedHandlerImpl
import org.springframework.security.web.access.ExceptionTranslationFilter

/**
 *
 * @author Luke Taylor
 */
class FilterChainConfigTests extends AbstractHttpConfigTests {
    def 'SEC-2821: Filter has interceptor'() {
        when:
        bean("myInterceptor", MyAdvisor)
        createAppContext("""
<b:bean id="fcp" class="org.springframework.security.web.FilterChainProxy">
  <b:constructor-arg>
    <util:list>
      <filter-chain pattern="/**" filters="filter1,filter2"/>
    </util:list>
  </b:constructor-arg>
</b:bean>

<b:bean id="filter1" class="org.springframework.aop.framework.ProxyFactoryBean"
      p:proxyInterfaces="javax.servlet.Filter">
  <b:property name="target">
    <b:bean class="org.springframework.security.web.context.SecurityContextPersistenceFilter"/>
  </b:property>
  <b:property name="interceptorNames">
    <b:list>
      <b:value>myInterceptor</b:value>
    </b:list>
  </b:property>
</b:bean>

<b:bean id="filter2" class="org.springframework.aop.framework.ProxyFactoryBean"
      p:proxyInterfaces="javax.servlet.Filter">
  <b:property name="target">
    <b:bean class="org.springframework.security.web.context.SecurityContextPersistenceFilter"/>
  </b:property>
  <b:property name="interceptorNames">
    <b:list>
    </b:list>
  </b:property>
</b:bean>
""")
        then:
        !appContext.getBean("fcp").getFilters('/').find { it == null }

    }

    static class MyAdvisor implements Advisor {

        @Override
        Advice getAdvice() {
             def interceptor = new MethodInterceptor() {
                 @Override
                 Object invoke(MethodInvocation invocation) throws Throwable {
                     invocation.proceed()
                 }
             }
            interceptor
        }

        @Override
        boolean isPerInstance() {
            false
        }
    }
}

package org.springframework.security.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class MockBeanPostProcessor implements BeanPostProcessor {
    private Set<String> postProcessBeforeInitializationBeanNames = new HashSet<String>();

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        System.out.println(beanName);
        postProcessBeforeInitializationBeanNames.add(beanName);
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    public Set<String> getPostProcessBeforeInitializationBeanNames() {
        return postProcessBeforeInitializationBeanNames;
    }
}

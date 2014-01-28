/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.config.annotation.authentication.configuration;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

class AuthenticationManagerBuilderPostProcessor implements BeanDefinitionRegistryPostProcessor, BeanClassLoaderAware {
    private ClassLoader beanClassloader;
    private Set<String> beanNamesToInitialize = new HashSet<String>();

    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) throws BeansException {
        for(String beanName : beanNamesToInitialize) {
            beanFactory.getBean(beanName);
        }
    }

    private boolean shouldInitializeEarly(Class<?> beanClass) {
        return shouldSearchMethods(beanClass) && hasAuowiredAuthenticationManagerBuilderMethod(beanClass);
    }

    private boolean shouldSearchMethods(Class<?> beanClass) {
        while(beanClass != null) {
            Annotation[] beanAnnotations = beanClass.getAnnotations();
            for(Annotation a : beanAnnotations) {
                String annotationName = a.annotationType().getName();
                if(annotationName.startsWith("org.springframework.") && annotationName.contains("Configuration")) {
                    return true;
                }
            }
            beanClass = beanClass.getSuperclass();
        }
        return false;
    }

    private boolean hasAuowiredAuthenticationManagerBuilderMethod(Class<?> beanClass) {
        Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(beanClass);
        for(Method method : methods) {
            Autowired autowired = AnnotationUtils.findAnnotation(method, Autowired.class);
            if(autowired == null) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            for(Class<?> paramType : parameterTypes) {
                if(AuthenticationManagerBuilder.class.equals(paramType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void postProcessBeanDefinitionRegistry(
            BeanDefinitionRegistry registry) throws BeansException {
        String[] beanNames = registry.getBeanDefinitionNames();
        for(String beanName : beanNames) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            String beanClassName = beanDefinition.getBeanClassName();
            if(beanClassName == null) {
                continue;
            }
            Class<?> beanClass = ClassUtils.resolveClassName(beanClassName, beanClassloader);
            if(shouldInitializeEarly(beanClass)) {
                beanNamesToInitialize.add(beanName);
            }
        }
    }

    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassloader = classLoader;
    }

}

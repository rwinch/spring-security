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
package org.springframework.security.access.annotation.sec2150;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;

import javax.annotation.security.RolesAllowed;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.annotation.Jsr250MethodSecurityMetadataSource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.annotation.SecuredAnnotationSecurityMetadataSource;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.ExpressionBasedAnnotationAttributeFactory;
import org.springframework.security.access.intercept.method.MockMethodInvocation;
import org.springframework.security.access.method.MethodSecurityMetadataSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PrePostAnnotationSecurityMetadataSource;

public class Sec2150SecuredTests {

    MethodSecurityMetadataSource source;

    PersonRepository repository;

    MethodInvocation object;

    @Before
    public void setup() throws Exception {
        ProxyFactory factory = new ProxyFactory(new Class[] {PersonRepository.class});
        factory.setTargetClass(CrudRepository.class);
        repository = (PersonRepository) factory.getProxy();
        object = new MockMethodInvocation(repository, PersonRepository.class , "findAll");
    }

    @Test
    public void secured() throws Exception {
        source = new SecuredAnnotationSecurityMetadataSource();
        Collection<ConfigAttribute> attributes = source.getAttributes(object);
        assertThat(attributes).isNotEmpty();
    }

    @Test
    public void jsr() throws Exception {
        source = new Jsr250MethodSecurityMetadataSource();
        Collection<ConfigAttribute> attributes = source.getAttributes(object);
        assertThat(attributes).isNotEmpty();
    }

    @Test
    public void prePost() throws Exception {
        source = new PrePostAnnotationSecurityMetadataSource(new ExpressionBasedAnnotationAttributeFactory(new DefaultMethodSecurityExpressionHandler()));
        Collection<ConfigAttribute> attributes = source.getAttributes(object);
        assertThat(attributes).isNotEmpty();
    }

    interface CrudRepository {

        Iterable<Object> findAll();
    }

    @Secured("ROLE_PERSON")
    @PreAuthorize("hasRole('ROLE_PERSON')")
    @RolesAllowed("ROLE_PERSON")
    public interface PersonRepository extends CrudRepository {

    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.knox.gateway.svcregfunc.impl;

import org.apache.knox.gateway.filter.rewrite.api.UrlRewriteEnvironment;
import org.apache.knox.gateway.filter.rewrite.spi.UrlRewriteContext;
import org.apache.knox.gateway.filter.rewrite.spi.UrlRewriteFunctionProcessor;
import org.apache.knox.gateway.ha.provider.HaProvider;
import org.apache.knox.gateway.ha.provider.HaServletContextListener;
import org.apache.knox.gateway.services.ServiceType;
import org.apache.knox.gateway.services.GatewayServices;
import org.apache.knox.gateway.services.registry.ServiceRegistry;
import org.apache.knox.gateway.svcregfunc.api.ServiceAddressFunctionDescriptor;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.ServiceLoader;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.fail;

class ServiceAddressFunctionProcessorTest {
  private ServiceRegistry reg;
  private GatewayServices svc;
  private UrlRewriteEnvironment env;
  private UrlRewriteContext ctx;
  private ServiceAddressFunctionDescriptor desc;

  @BeforeEach
  void setUp() {
    reg = EasyMock.createNiceMock( ServiceRegistry.class );
    EasyMock.expect( reg.lookupServiceURL( "test-cluster", "test-service" ) ).andReturn( "test-scheme://test-host:777/test-path" ).anyTimes();

    svc = EasyMock.createNiceMock( GatewayServices.class );
    EasyMock.expect( svc.getService( ServiceType.SERVICE_REGISTRY_SERVICE ) ).andReturn( reg ).anyTimes();

    env = EasyMock.createNiceMock( UrlRewriteEnvironment.class );
    EasyMock.expect( env.getAttribute( GatewayServices.GATEWAY_SERVICES_ATTRIBUTE ) ).andReturn( svc ).anyTimes();
    EasyMock.expect( env.getAttribute( GatewayServices.GATEWAY_CLUSTER_ATTRIBUTE ) ).andReturn( "test-cluster" ).anyTimes();

    ctx = EasyMock.createNiceMock( UrlRewriteContext.class );

    desc = EasyMock.createNiceMock( ServiceAddressFunctionDescriptor.class );

    HaProvider haProvider = EasyMock.createNiceMock( HaProvider.class );

    EasyMock.expect(env.getAttribute(HaServletContextListener.PROVIDER_ATTRIBUTE_NAME)).andReturn(haProvider).anyTimes();

    EasyMock.expect(haProvider.isHaEnabled(EasyMock.anyObject(String.class))).andReturn(Boolean.FALSE).anyTimes();

    EasyMock.replay( reg, svc, env, desc, ctx, haProvider );
  }

  @Test
  void testServiceLoader() {
    ServiceLoader loader = ServiceLoader.load( UrlRewriteFunctionProcessor.class );
    Iterator iterator = loader.iterator();
    assertThat( "Service iterator empty.", iterator.hasNext() );
    while( iterator.hasNext() ) {
      Object object = iterator.next();
      if( object instanceof ServiceAddressFunctionProcessor ) {
        return;
      }
    }
    fail( "Failed to find " + ServiceAddressFunctionProcessor.class.getName() + " via service loader." );
  }

  @Test
  void testName() {
    ServiceAddressFunctionProcessor func = new ServiceAddressFunctionProcessor();
    assertThat( func.name(), is( "serviceAddr" ) );
  }

  @Test
  void testInitialize() throws Exception {
    ServiceAddressFunctionProcessor func = new ServiceAddressFunctionProcessor();
    try {
      func.initialize( null, desc );
      fail( "Should have thrown an IllegalArgumentException" );
    } catch( IllegalArgumentException e ) {
      assertThat( e.getMessage(), containsString( "environment" ) );
    }

    func = new ServiceAddressFunctionProcessor();
    try {
      func.initialize( env, null );
    } catch( Exception e ) {
      e.printStackTrace();
      fail( "Should not have thrown an exception" );
    }

    func.initialize( env, desc );

    assertThat( func.cluster(), is( "test-cluster" ) );
    assertThat( func.registry(), sameInstance( reg ) );
  }

  @Test
  void testDestroy() throws Exception {
    ServiceAddressFunctionProcessor func = new ServiceAddressFunctionProcessor();
    func.initialize( env, desc );
    func.destroy();

    assertThat( func.cluster(), nullValue() );
    assertThat( func.registry(), nullValue() );
  }

  @Test
  void testResolve() throws Exception {
    ServiceAddressFunctionProcessor func = new ServiceAddressFunctionProcessor();
    func.initialize( env, desc );

    assertThat( func.resolve( ctx, Collections.singletonList("test-service")), contains( "test-host:777" ) );
    assertThat( func.resolve( ctx, Collections.singletonList("invalid-test-service")), contains( "invalid-test-service" ) );
    assertThat( func.resolve( ctx, null ), nullValue() );

    func.destroy();
  }
}

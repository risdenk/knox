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
package org.apache.knox.gateway.hadoopauth;

import org.apache.knox.gateway.deploy.DeploymentContext;
import org.apache.knox.gateway.deploy.ProviderDeploymentContributor;
import org.apache.knox.gateway.descriptor.FilterDescriptor;
import org.apache.knox.gateway.descriptor.FilterParamDescriptor;
import org.apache.knox.gateway.descriptor.GatewayDescriptor;
import org.apache.knox.gateway.descriptor.ResourceDescriptor;
import org.apache.knox.gateway.descriptor.impl.GatewayDescriptorImpl;
import org.apache.knox.gateway.hadoopauth.deploy.HadoopAuthDeploymentContributor;
import org.apache.knox.gateway.topology.Provider;
import org.apache.knox.gateway.topology.Topology;
import org.easymock.EasyMock;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class HadoopAuthDeploymentContributorTest {
  @SuppressWarnings("rawtypes")
  @Test
  void testServiceLoader() {
    ServiceLoader loader = ServiceLoader.load( ProviderDeploymentContributor.class );
    Iterator iterator = loader.iterator();
    assertThat( "Service iterator empty.", iterator.hasNext() );
    while( iterator.hasNext() ) {
      Object object = iterator.next();
      if( object instanceof HadoopAuthDeploymentContributor) {
        return;
      }
    }
    fail( "Failed to find " + HadoopAuthDeploymentContributor.class.getName() + " via service loader." );
  }

  @Test
  void testDeployment() {
    String aliasKey = "signature.secret";
    String aliasValue = "${ALIAS=signature.secret}";
    String normalKey = "type";
    String normalValue = "simple";

    WebArchive webArchive = ShrinkWrap.create( WebArchive.class, "test-archive" );

    Provider provider = new Provider();
    provider.setEnabled( true );
    provider.setName( HadoopAuthDeploymentContributor.NAME );
    // Keep order of params in map for testing
    Map<String, String> params = new TreeMap<>();
    params.put(aliasKey, aliasValue);
    params.put(normalKey, normalValue);
    provider.setParams(params);

    Topology topology = new Topology();
    topology.setName( "Sample" );

    DeploymentContext context = EasyMock.createNiceMock( DeploymentContext.class );
    EasyMock.expect( context.getWebArchive() ).andReturn( webArchive ).anyTimes();
    EasyMock.expect( context.getTopology() ).andReturn( topology ).anyTimes();
    EasyMock.replay( context );

    GatewayDescriptor gatewayDescriptor = new GatewayDescriptorImpl();
    ResourceDescriptor resource = gatewayDescriptor.createResource();

    HadoopAuthDeploymentContributor contributor = new HadoopAuthDeploymentContributor();

    assertThat( contributor.getRole(), is( HadoopAuthDeploymentContributor.ROLE ) );
    assertThat( contributor.getName(), is( HadoopAuthDeploymentContributor.NAME ) );

    contributor.initializeContribution( context );
    contributor.contributeFilter(context, provider, null, resource, null);
    contributor.finalizeContribution( context );

    // Check that the params are properly setup
    FilterDescriptor hadoopAuthFilterDescriptor = resource.filters().get(0);
    assertNotNull(hadoopAuthFilterDescriptor);
    assertEquals(HadoopAuthDeploymentContributor.NAME, hadoopAuthFilterDescriptor.name());
    List<FilterParamDescriptor> hadoopAuthFilterParams = hadoopAuthFilterDescriptor.params();
    assertEquals(3, hadoopAuthFilterParams.size());

    FilterParamDescriptor paramDescriptor = hadoopAuthFilterParams.get(0);
    assertEquals(aliasKey, paramDescriptor.name());
    assertEquals(aliasValue, paramDescriptor.value());

    FilterParamDescriptor paramDescriptor2 = hadoopAuthFilterParams.get(1);
    assertEquals(normalKey, paramDescriptor2.name());
    assertEquals(normalValue, paramDescriptor2.value());
  }
}

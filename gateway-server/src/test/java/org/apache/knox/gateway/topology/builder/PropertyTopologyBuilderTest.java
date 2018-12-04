/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.knox.gateway.topology.builder;

import org.apache.knox.gateway.topology.Topology;
import org.apache.knox.gateway.topology.builder.property.Property;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

class PropertyTopologyBuilderTest {
  @Test
  void testBuildFailedForWrongProperty() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "miss_prop", "value" ) );
    Assertions.assertThrows(IllegalArgumentException.class, propertyTopologyBuilder::build);
  }

  @Test
  void testBuildSuccessfulForTopologyProperty() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.name", "topology" ) );
    Topology topology = propertyTopologyBuilder.build();

    assertThat( topology, notNullValue() );
  }

  @Test
  void testBuildFailedForWrongTopologyProperty() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.miss_prop", "value" ) );
    Assertions.assertThrows(IllegalArgumentException.class, propertyTopologyBuilder::build);
  }

  @Test
  void testBuildFailedForWrongGatewayToken() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.gateway.miss_prop", "value" ) );
    Assertions.assertThrows(IllegalArgumentException.class, propertyTopologyBuilder::build);
  }

  @Test
  void testBuildFailedForWrongProviderToken1() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.gateway.provider", "value" ) );
    Assertions.assertThrows(IllegalArgumentException.class, propertyTopologyBuilder::build);
  }

  @Test
  void testBuildFailedForWrongProviderToken2() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.gateway.provider.authentication", "value" ) );
    Assertions.assertThrows(IllegalArgumentException.class, propertyTopologyBuilder::build);
  }

  @Test
  void testBuildFailedForWrongProviderToken3() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.gateway.provider.authentication.ShiroProvider", "value" ) );
    Assertions.assertThrows(IllegalArgumentException.class, propertyTopologyBuilder::build);
  }

  @Test
  void testBuildSuccessfulForProviderProperty() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.gateway.provider.authentication.ShiroProvider.enabled", "value" ) );
    Topology topology = propertyTopologyBuilder.build();

    assertThat( topology, notNullValue() );
    assertThat( topology.getProviders().size(), is( 1 ) );
    assertThat( topology.getProviders().iterator().next().isEnabled(), is( false ) );
  }

  @Test
  void testBuildFailedForWrongProviderProperty() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.gateway.provider.authentication.ShiroProvider.miss_prop", "value" ) );
    Assertions.assertThrows(IllegalArgumentException.class, propertyTopologyBuilder::build);
  }

  @Test
  void testBuildFailedForWrongProviderParamToken1() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.gateway.provider.authentication.ShiroProvider.param", "value" ) );
    Assertions.assertThrows(IllegalArgumentException.class, propertyTopologyBuilder::build);
  }

  @Test
  void testBuildFailedForEmptyProviderParamName() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.gateway.provider.authentication.ShiroProvider.param.", "value" ) );
    Assertions.assertThrows(IllegalArgumentException.class, propertyTopologyBuilder::build);
  }

  @Test
  void testBuildFailedForEmptyProviderParamValue() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.gateway.provider.authentication.ShiroProvider.param.name1", "" ) );
    Assertions.assertThrows(IllegalArgumentException.class, propertyTopologyBuilder::build);
  }

  @Test
  void testBuildFailedForWrongServiceToken1() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.gateway.service", "value" ) );
    Assertions.assertThrows(IllegalArgumentException.class, propertyTopologyBuilder::build);
  }

  @Test
  void testBuildFailedForWrongServiceToken2() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.gateway.service.WEBHDFS", "value" ) );
    Assertions.assertThrows(IllegalArgumentException.class, propertyTopologyBuilder::build);
  }

  @Test
  void testBuildFailedForWrongServiceToken3() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.gateway.service.WEBHDFS.", "value" ) );
    Assertions.assertThrows(IllegalArgumentException.class, propertyTopologyBuilder::build);
  }

  @Test
  void testBuildSuccessfulForServiceProperty() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.gateway.service.WEBHDFS..url", "http://host:50070/webhdfs" ) );
    Assertions.assertThrows(IllegalArgumentException.class, propertyTopologyBuilder::build);
  }

  @Test
  void testBuildFailedForWrongServiceProperty() {
    PropertyTopologyBuilder propertyTopologyBuilder = new PropertyTopologyBuilder();
    propertyTopologyBuilder.addProperty( new Property( "topology.gateway.service.WEBHDFS..miss_prop", "value" ) );
    Assertions.assertThrows(IllegalArgumentException.class, propertyTopologyBuilder::build);
  }
}

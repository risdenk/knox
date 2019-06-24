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
package org.apache.knox.gateway.config.impl;

import org.apache.knox.gateway.config.ConfigurationAdapter;
import org.apache.knox.gateway.config.spi.ConfigurationAdapterDescriptor;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.ServiceLoader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.fail;

class BeanConfigurationAdapterDescriptorTest {
  @Test
  void testServiceLoader() {
    ServiceLoader<ConfigurationAdapterDescriptor> loader = ServiceLoader.load( ConfigurationAdapterDescriptor.class );
    for (ConfigurationAdapterDescriptor configurationAdapterDescriptor : loader) {
      if (configurationAdapterDescriptor instanceof BeanConfigurationAdapterDescriptor) {
        return;
      }
    }
    fail( "Failed to load BeanConfigurationAdapterDescriptor" );
  }

  @Test
  void testDescriptor() {
    ConfigurationAdapterDescriptor descriptor = new BeanConfigurationAdapterDescriptor();
    Map<Class<?>,Class<? extends ConfigurationAdapter>> map = descriptor.providedConfigurationAdapters();
    assertThat( map, hasKey( (Class)Object.class ) );
    Class<? extends ConfigurationAdapter> type = map.get( Object.class );
    assertThat(
        "Descriptor didn't return " + BeanConfigurationAdapter.class.getName(),
        type == BeanConfigurationAdapter.class );
  }
}

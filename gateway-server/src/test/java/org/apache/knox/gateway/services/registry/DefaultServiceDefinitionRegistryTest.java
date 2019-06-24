/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.knox.gateway.services.registry;

import org.apache.knox.gateway.config.GatewayConfig;
import org.apache.knox.gateway.services.registry.impl.DefaultServiceDefinitionRegistry;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class DefaultServiceDefinitionRegistryTest {
  @Test
  void matchSimplePattern() throws Exception {
    DefaultServiceDefinitionRegistry registry = new DefaultServiceDefinitionRegistry();
    GatewayConfig config = EasyMock.createNiceMock(GatewayConfig.class);
    URL url = ClassLoader.getSystemResource("services");
    EasyMock.expect(config.getGatewayServicesDir()).andReturn(new File(url.getFile()).getAbsolutePath()).anyTimes();
    EasyMock.replay(config);
    registry.init(config, null);
    ServiceDefEntry entry = registry.getMatchingService("/foo/somepath");
    assertThat( entry.getRole(), is("FOO"));
    entry = registry.getMatchingService("/bar/?somepath");
    assertThat( entry.getRole(), is("BAR"));
  }
}

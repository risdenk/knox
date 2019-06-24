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
package org.apache.knox.gateway.services.security.impl;

import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingCluster;
import org.apache.knox.gateway.config.GatewayConfig;
import org.apache.knox.gateway.service.config.remote.zk.ZooKeeperClientService;
import org.apache.knox.gateway.service.config.remote.zk.ZooKeeperClientServiceProvider;
import org.apache.knox.gateway.services.config.client.RemoteConfigurationRegistryClientService;
import org.apache.knox.gateway.services.security.AliasService;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.capture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for {@link ZookeeperRemoteAliasService} backed by Zookeeper.
 */
class ZookeeperRemoteAliasServiceTest {
  private static TestingCluster zkNodes;
  private static GatewayConfig gc;

  @BeforeAll
  static void setupSuite() throws Exception {
    String configMonitorName = "remoteConfigMonitorClient";
    configureAndStartZKCluster();

    // Setup the base GatewayConfig mock
    gc = EasyMock.createNiceMock(GatewayConfig.class);
    EasyMock.expect(gc.getRemoteRegistryConfigurationNames())
        .andReturn(Collections.singletonList(configMonitorName)).anyTimes();

    final String registryConfig =
        GatewayConfig.REMOTE_CONFIG_REGISTRY_TYPE + "="
            + ZooKeeperClientService.TYPE + ";"
            + GatewayConfig.REMOTE_CONFIG_REGISTRY_ADDRESS + "=" + zkNodes
            .getConnectString();

    EasyMock.expect(gc.getRemoteRegistryConfiguration(configMonitorName))
        .andReturn(registryConfig).anyTimes();

    EasyMock.expect(gc.getRemoteConfigurationMonitorClientName())
        .andReturn(configMonitorName).anyTimes();

    EasyMock.expect(gc.getAlgorithm()).andReturn("AES").anyTimes();

    EasyMock.expect(gc.isRemoteAliasServiceEnabled())
        .andReturn(true).anyTimes();

    EasyMock.replay(gc);
  }

  private static void configureAndStartZKCluster() throws Exception {
    // Configure security for the ZK cluster instances
    Map<String, Object> customInstanceSpecProps = new HashMap<>();
    customInstanceSpecProps.put("authProvider.1",
        "org.apache.zookeeper.server.auth.SASLAuthenticationProvider");
    customInstanceSpecProps.put("requireClientAuthScheme", "sasl");

    // Define the test cluster
    List<InstanceSpec> instanceSpecs = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      InstanceSpec is = new InstanceSpec(null, -1, -1, -1, false, (i + 1), -1,
          -1, customInstanceSpecProps);
      instanceSpecs.add(is);
    }
    zkNodes = new TestingCluster(instanceSpecs);

    // Start the cluster
    zkNodes.start();
  }

  @AfterAll
  static void tearDownSuite() throws Exception {
    // Shutdown the ZK cluster
    zkNodes.close();
  }

  @Test
  void testAliasForCluster() throws Exception {
    final String expectedClusterName = "sandbox";
    final String expectedAlias = "knox.test.alias";
    final String expectedPassword = "dummyPassword";

    final String expectedClusterNameDev = "development";
    final String expectedAliasDev = "knox.test.alias.dev";
    final String expectedPasswordDev = "otherDummyPassword";

    // Mock Alias Service
    final DefaultAliasService defaultAlias = EasyMock
        .createNiceMock(DefaultAliasService.class);
    // Captures for validating the alias creation for a generated topology
    final Capture<String> capturedCluster = EasyMock.newCapture();
    final Capture<String> capturedAlias = EasyMock.newCapture();
    final Capture<String> capturedPwd = EasyMock.newCapture();

    defaultAlias
        .addAliasForCluster(capture(capturedCluster), capture(capturedAlias),
            capture(capturedPwd));
    EasyMock.expectLastCall().anyTimes();

    /* defaultAlias.getAliasesForCluster() never returns null */
    EasyMock.expect(defaultAlias.getAliasesForCluster(expectedClusterName))
        .andReturn(new ArrayList<>()).anyTimes();
    EasyMock.expect(defaultAlias.getAliasesForCluster(expectedClusterNameDev))
        .andReturn(new ArrayList<>()).anyTimes();

    EasyMock.replay(defaultAlias);

    final DefaultMasterService ms = EasyMock
        .createNiceMock(DefaultMasterService.class);
    EasyMock.expect(ms.getMasterSecret()).andReturn("knox".toCharArray())
        .anyTimes();
    EasyMock.replay(ms);

    RemoteConfigurationRegistryClientService clientService = (new ZooKeeperClientServiceProvider())
        .newInstance();
    clientService.setAliasService(defaultAlias);
    clientService.init(gc, Collections.emptyMap());

    final ZookeeperRemoteAliasService zkAlias = new ZookeeperRemoteAliasService(defaultAlias, ms,
        clientService);
    zkAlias.init(gc, Collections.emptyMap());
    zkAlias.start();

    /* Put */
    zkAlias.addAliasForCluster(expectedClusterName, expectedAlias,
        expectedPassword);
    zkAlias.addAliasForCluster(expectedClusterNameDev, expectedAliasDev,
        expectedPasswordDev);

    /* GET all Aliases */
    List<String> aliases = zkAlias.getAliasesForCluster(expectedClusterName);
    List<String> aliasesDev = zkAlias
        .getAliasesForCluster(expectedClusterNameDev);

    assertEquals(aliases.size(), 1);
    assertEquals(aliasesDev.size(), 1);

    assertTrue(aliases.contains(expectedAlias),
        "Expected alias 'knox.test.alias' not found ");
    assertTrue(aliasesDev.contains(expectedAliasDev),
        "Expected alias 'knox.test.alias.dev' not found ");

    final char[] result = zkAlias
        .getPasswordFromAliasForCluster(expectedClusterName, expectedAlias);
    final char[] result1 = zkAlias
        .getPasswordFromAliasForCluster(expectedClusterNameDev,
            expectedAliasDev);

    assertEquals(expectedPassword, new String(result));
    assertEquals(expectedPasswordDev, new String(result1));

    /* Remove */
    zkAlias.removeAliasForCluster(expectedClusterNameDev, expectedAliasDev);

    /* Make sure expectedAliasDev is removed*/
    aliases = zkAlias.getAliasesForCluster(expectedClusterName);
    aliasesDev = zkAlias.getAliasesForCluster(expectedClusterNameDev);

    assertEquals(aliasesDev.size(), 0);
    assertFalse(aliasesDev.contains(expectedAliasDev),
        "Expected alias 'knox.test.alias.dev' to be removed but found.");

    assertEquals(aliases.size(), 1);
    assertTrue(aliases.contains(expectedAlias),
        "Expected alias 'knox.test.alias' not found ");

    /* Test auto-generate password for alias */
    final String testAutoGeneratedpasswordAlias = "knox.test.alias.auto";

    final char[] autoGeneratedPassword = zkAlias
        .getPasswordFromAliasForCluster(expectedClusterName,
            testAutoGeneratedpasswordAlias, true);
    aliases = zkAlias.getAliasesForCluster(expectedClusterName);

    assertNotNull(autoGeneratedPassword);
    assertEquals(aliases.size(), 2);
    assertTrue(aliases.contains(testAutoGeneratedpasswordAlias),
        "Expected alias 'knox.test.alias' not found ");
  }

  @Test
  void testEncryptDecrypt() throws Exception {
    final String testPassword = "ApacheKnoxPassword123";

    final AliasService defaultAlias = EasyMock
        .createNiceMock(AliasService.class);
    EasyMock.replay(defaultAlias);

    final DefaultMasterService ms = EasyMock
        .createNiceMock(DefaultMasterService.class);
    EasyMock.expect(ms.getMasterSecret()).andReturn("knox".toCharArray())
        .anyTimes();
    EasyMock.replay(ms);

    RemoteConfigurationRegistryClientService clientService = (new ZooKeeperClientServiceProvider())
                                                                 .newInstance();
    clientService.setAliasService(defaultAlias);
    clientService.init(gc, Collections.emptyMap());

    final ZookeeperRemoteAliasService zkAlias = new ZookeeperRemoteAliasService(defaultAlias, ms,
        clientService);
    zkAlias.init(gc, Collections.emptyMap());

    final String encrypted = zkAlias.encrypt(testPassword);
    assertNotNull(encrypted);
    final String clear = zkAlias.decrypt(encrypted);
    assertEquals(testPassword, clear);

    try {
      // Match default data that is put into a newly created znode
      final byte[] badData = new byte[0];
      zkAlias.decrypt(new String(badData, StandardCharsets.UTF_8));
      fail("Should have failed to decrypt.");
    } catch (IllegalArgumentException e) {
      assertEquals("Data should have 3 parts split by ::", e.getMessage());
    }
  }
}

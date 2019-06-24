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
package org.apache.knox.gateway.ha.provider.impl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingCluster;
import org.apache.knox.gateway.ha.provider.HaServiceConfig;
import org.apache.knox.gateway.ha.provider.URLManager;
import org.apache.knox.gateway.ha.provider.URLManagerLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple unit tests for KafkaZookeeperURLManager.
 *
 * @see KafkaZookeeperURLManager
 */
class KafkaZookeeperURLManagerTest {
  private TestingCluster cluster;

  @BeforeEach
  void setUp() throws Exception {
    cluster = new TestingCluster(3);
    cluster.start();

    try (CuratorFramework zooKeeperClient = CuratorFrameworkFactory.builder()
        .connectString(cluster.getConnectString())
        .retryPolicy(new ExponentialBackoffRetry(1000, 3)).build()) {

      zooKeeperClient.start();
      assertTrue(zooKeeperClient.blockUntilConnected(10, TimeUnit.SECONDS));
      zooKeeperClient.create().forPath("/brokers");
      zooKeeperClient.create().forPath("/brokers/ids");
    }
  }

  @AfterEach
  void tearDown() throws IOException {
    if(cluster != null) {
      cluster.close();
    }
  }

  @Test
  void testHBaseZookeeperURLManagerLoading() {
    HaServiceConfig config = new DefaultHaServiceConfig("KAFKA");
    config.setEnabled(true);
    config.setZookeeperEnsemble(cluster.getConnectString());
    URLManager manager = URLManagerLoader.loadURLManager(config);
    assertNotNull(manager);
    assertTrue(manager instanceof KafkaZookeeperURLManager);
  }
}

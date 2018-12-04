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
package org.apache.knox.gateway.services.metrics.impl;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import org.apache.knox.gateway.config.impl.GatewayConfigImpl;
import org.apache.knox.gateway.services.metrics.InstrumentationProvider;
import org.apache.knox.gateway.services.metrics.MetricsReporter;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultMetricsServiceTest {
  @Test
  void lifecycle() throws Exception {
    DefaultMetricsService service = new DefaultMetricsService();
    service.init(new GatewayConfigImpl(), null);
    assertNotNull(service.getContext());
    assertNotNull(service.getMetricRegistry());
    assertNotNull(service.getMetricsReporters());
    assertNotNull(service.getInstrumentationProviders());
    service.start();
    assertNotNull(service.getContext().getMetricsService());
    MetricRegistry metricRegistry = (MetricRegistry) service.getContext().getProperty(DefaultMetricsService.METRICS_REGISTRY);
    assertNotNull(metricRegistry);
    service.stop();
    service.getMetricRegistry().removeMatching(MetricFilter.ALL);
  }

  @Test
  public void instrumentationProvidersLoadingDefaultIsEmpty() throws Exception {
    DefaultMetricsService service = new DefaultMetricsService();
    service.init(new GatewayConfigImpl(), null);
    Map<Class<?>, InstrumentationProvider> map = service.getInstrumentationProviders();
    assertTrue(map.entrySet().isEmpty());
    assertNull(service.getInstrumented(HttpClientBuilder.class));
    service.getMetricRegistry().removeMatching(MetricFilter.ALL);
  }

  @Test
  public void instrumentationProvidersLoading() throws Exception {
    DefaultMetricsService service = new DefaultMetricsService();
    GatewayConfigImpl config = new GatewayConfigImpl();
    config.set(GatewayConfigImpl.METRICS_ENABLED, "true");
    service.init(config, null);
    Map<Class<?>, InstrumentationProvider> map = service.getInstrumentationProviders();
    assertTrue(map.entrySet().size() >= 2);
    assertNotNull(service.getInstrumented(HttpClientBuilder.class));
    service.getMetricRegistry().removeMatching(MetricFilter.ALL);
  }

  @Test
  public void reportersLoadingDisabled() throws Exception {
    DefaultMetricsService service = new DefaultMetricsService();
    GatewayConfigImpl config = new GatewayConfigImpl();
    config.set(GatewayConfigImpl.METRICS_ENABLED, "true");
    config.set(GatewayConfigImpl.JMX_METRICS_REPORTING_ENABLED, "false");
    service.init(config, null);
    List<MetricsReporter> reporters = service.getMetricsReporters();
    assertTrue(reporters.size() >= 2);
    for (MetricsReporter reporter : reporters) {
      assertFalse(reporter.isEnabled());
    }
    service.getMetricRegistry().removeMatching(MetricFilter.ALL);
    config.set(GatewayConfigImpl.JMX_METRICS_REPORTING_ENABLED, "true");
    config.set(GatewayConfigImpl.GRAPHITE_METRICS_REPORTING_ENABLED, "true");
    service.init(config, null);
    reporters = service.getMetricsReporters();
    for (MetricsReporter reporter : reporters) {
      assertTrue(reporter.isEnabled());
    }
    service.start();
    service.stop();
    service.getMetricRegistry().removeMatching(MetricFilter.ALL);
  }
}

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
package org.apache.knox.gateway.services.metrics.impl.instr;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.httpclient.InstrumentedHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.knox.gateway.dispatch.ConnectionManagerBuilder;
import org.apache.knox.gateway.dispatch.DefaultConnectionManagerBuilder;
import org.apache.knox.gateway.services.metrics.InstrumentationProvider;
import org.apache.knox.gateway.services.metrics.MetricsContext;
import org.apache.knox.gateway.services.metrics.impl.DefaultMetricsService;

import java.util.concurrent.TimeUnit;

public class InstrHttpClientConnectionManagerProvider implements
    InstrumentationProvider<ConnectionManagerBuilder> {

  @Override
  public ConnectionManagerBuilder getInstrumented(MetricsContext metricsContext) {
    MetricRegistry registry = (MetricRegistry) metricsContext.getProperty(DefaultMetricsService.METRICS_REGISTRY);
    return new InstrumentedConnectionManagerBuilder(registry);
  }

  @Override
  public ConnectionManagerBuilder getInstrumented(ConnectionManagerBuilder connectionManager,
                                                            MetricsContext metricsContext) {
    throw new UnsupportedOperationException();
  }

  private static class InstrumentedConnectionManagerBuilder extends DefaultConnectionManagerBuilder {
    private final MetricRegistry registry;

    InstrumentedConnectionManagerBuilder(MetricRegistry registry) {
      this.registry = registry;
    }

    @Override
    public PoolingHttpClientConnectionManager build() {
      return new InstrumentedHttpClientConnectionManager(registry, getConnectionSocketFactoryRegistry(),
          null, null, SystemDefaultDnsResolver.INSTANCE, -1, TimeUnit.MILLISECONDS, getName());
    }
  }
}

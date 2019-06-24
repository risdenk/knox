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
package org.apache.knox.gateway.ha.dispatch;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.net.URIAuthority;
import org.apache.knox.gateway.config.Configure;
import org.apache.knox.gateway.config.Optional;
import org.apache.knox.gateway.dispatch.DefaultDispatch;
import org.apache.knox.gateway.filter.AbstractGatewayFilter;
import org.apache.knox.gateway.ha.dispatch.i18n.HaDispatchMessages;
import org.apache.knox.gateway.ha.provider.HaProvider;
import org.apache.knox.gateway.ha.provider.HaServiceConfig;
import org.apache.knox.gateway.ha.provider.impl.HaServiceConfigConstants;
import org.apache.knox.gateway.i18n.messages.MessagesFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default HA dispatch class that has a very basic failover mechanism
 */
public class DefaultHaDispatch extends DefaultDispatch {

  protected static final String FAILOVER_COUNTER_ATTRIBUTE = "dispatch.ha.failover.counter";

  protected static final HaDispatchMessages LOG = MessagesFactory.get(HaDispatchMessages.class);

  private int maxFailoverAttempts = HaServiceConfigConstants.DEFAULT_MAX_FAILOVER_ATTEMPTS;

  private int failoverSleep = HaServiceConfigConstants.DEFAULT_FAILOVER_SLEEP;

  private HaProvider haProvider;

  @Optional
  @Configure
  private String serviceRole;

  @Override
  public void init() {
    super.init();
    LOG.initializingForResourceRole(getServiceRole());
    if ( haProvider != null ) {
      HaServiceConfig serviceConfig = haProvider.getHaDescriptor().getServiceConfig(getServiceRole());
      maxFailoverAttempts = serviceConfig.getMaxFailoverAttempts();
      failoverSleep = serviceConfig.getFailoverSleep();
    }
  }

  public String getServiceRole() {
    return serviceRole;
  }

  public void setServiceRole(String serviceRole) {
    this.serviceRole = serviceRole;
  }

  public HaProvider getHaProvider() {
    return haProvider;
  }

  @Configure
  public void setHaProvider(HaProvider haProvider) {
    this.haProvider = haProvider;
  }

  @Override
  protected void executeRequest(ClassicHttpRequest outboundRequest, HttpServletRequest inboundRequest, HttpServletResponse outboundResponse) throws IOException {
    ClassicHttpResponse inboundResponse = null;
    try {
      inboundResponse = executeOutboundRequest(outboundRequest);
      writeOutboundResponse(outboundRequest, inboundRequest, outboundResponse, inboundResponse);
    } catch ( IOException e ) {
      LOG.errorConnectingToServer(outboundRequest.getRequestUri(), e);
      failoverRequest(outboundRequest, inboundRequest, outboundResponse, inboundResponse, e);
    }
  }


  protected void failoverRequest(ClassicHttpRequest outboundRequest, HttpServletRequest inboundRequest, HttpServletResponse outboundResponse, ClassicHttpResponse inboundResponse, Exception exception) throws IOException {
    String auditUri;
    try {
      auditUri = outboundRequest.getUri().toString();
    } catch (URISyntaxException e) {
      auditUri = null;
    }
    LOG.failingOverRequest(auditUri);
    AtomicInteger counter = (AtomicInteger) inboundRequest.getAttribute(FAILOVER_COUNTER_ATTRIBUTE);
    if ( counter == null ) {
      counter = new AtomicInteger(0);
    }
    inboundRequest.setAttribute(FAILOVER_COUNTER_ATTRIBUTE, counter);
    if ( counter.incrementAndGet() <= maxFailoverAttempts ) {
      haProvider.markFailedURL(getServiceRole(), auditUri);
      //null out target url so that rewriters run again
      inboundRequest.setAttribute(AbstractGatewayFilter.TARGET_REQUEST_URL_ATTRIBUTE_NAME, null);
      URI uri = getDispatchUrl(inboundRequest);
      try {
        outboundRequest.setAuthority(URIAuthority.create(uri.getAuthority()));
      } catch (URISyntaxException e) {
        throw new IOException(e);
      }
      outboundRequest.setScheme(uri.getScheme());
      outboundRequest.setPath(uri.getPath());
      if ( failoverSleep > 0 ) {
        try {
          Thread.sleep(failoverSleep);
        } catch ( InterruptedException e ) {
          LOG.failoverSleepFailed(getServiceRole(), e);
        }
      }
      executeRequest(outboundRequest, inboundRequest, outboundResponse);
    } else {
      LOG.maxFailoverAttemptsReached(maxFailoverAttempts, getServiceRole());
      if ( inboundResponse != null ) {
        writeOutboundResponse(outboundRequest, inboundRequest, outboundResponse, inboundResponse);
      } else {
        throw new IOException(exception);
      }
    }
  }

}

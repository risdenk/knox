/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.knox.gateway.hdfs.dispatch;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.BufferedHttpEntity;
import org.apache.hc.core5.net.URIAuthority;
import org.apache.knox.gateway.config.Configure;
import org.apache.knox.gateway.filter.AbstractGatewayFilter;
import org.apache.knox.gateway.ha.provider.HaProvider;
import org.apache.knox.gateway.ha.provider.HaServiceConfig;
import org.apache.knox.gateway.ha.provider.impl.HaServiceConfigConstants;
import org.apache.knox.gateway.hdfs.i18n.WebHdfsMessages;
import org.apache.knox.gateway.i18n.messages.MessagesFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractHdfsHaDispatch extends HdfsHttpClientDispatch {

  private static final String FAILOVER_COUNTER_ATTRIBUTE = "dispatch.ha.failover.counter";
  private static final WebHdfsMessages LOG = MessagesFactory.get(WebHdfsMessages.class);
  private int maxFailoverAttempts = HaServiceConfigConstants.DEFAULT_MAX_FAILOVER_ATTEMPTS;
  private int failoverSleep = HaServiceConfigConstants.DEFAULT_FAILOVER_SLEEP;
  private HaProvider haProvider;

  public AbstractHdfsHaDispatch() throws ServletException {
    super();
  }

  @Override
  public void init() {
     super.init();
     if (haProvider != null) {
       HaServiceConfig serviceConfig = haProvider.getHaDescriptor().getServiceConfig(getResourceRole());
       maxFailoverAttempts = serviceConfig.getMaxFailoverAttempts();
       failoverSleep = serviceConfig.getFailoverSleep();
     }
   }

  public HaProvider getHaProvider() {
    return haProvider;
  }

  abstract String getResourceRole();

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
      } catch (StandbyException e) {
         LOG.errorReceivedFromStandbyNode(e);
         failoverRequest(outboundRequest, inboundRequest, outboundResponse, inboundResponse, e);
      } catch (SafeModeException e) {
         LOG.errorReceivedFromSafeModeNode(e);
         failoverRequest(outboundRequest, inboundRequest, outboundResponse, inboundResponse, e);
      } catch (IOException e) {
        String auditUri;
        try {
          auditUri = outboundRequest.getUri().toString();
        } catch (URISyntaxException e1) {
          auditUri = null;
        }
        LOG.errorConnectingToServer(auditUri, e);
        failoverRequest(outboundRequest, inboundRequest, outboundResponse, inboundResponse, e);
      }
   }

  /**
    * Checks for specific outbound response codes/content to trigger a retry or failover
    */
  @Override
  protected void writeOutboundResponse(ClassicHttpRequest outboundRequest, HttpServletRequest inboundRequest, HttpServletResponse outboundResponse, ClassicHttpResponse inboundResponse) throws IOException {
      if (inboundResponse.getCode() == 403) {
         BufferedHttpEntity entity = new BufferedHttpEntity(inboundResponse.getEntity());
         inboundResponse.setEntity(entity);
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         inboundResponse.getEntity().writeTo(outputStream);
         String body = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
         if (body.contains("StandbyException")) {
            throw new StandbyException();
         }
         if (body.contains("SafeModeException") || body.contains("RetriableException")) {
            throw new SafeModeException();
         }
      }
      super.writeOutboundResponse(outboundRequest, inboundRequest, outboundResponse, inboundResponse);
   }

  private void failoverRequest(ClassicHttpRequest outboundRequest, HttpServletRequest inboundRequest, HttpServletResponse outboundResponse, ClassicHttpResponse inboundResponse, Exception exception) throws IOException {
      String auditUri;
      try {
        auditUri = outboundRequest.getUri().toString();
      } catch (URISyntaxException e1) {
        auditUri = null;
      }
      LOG.failedToConnectTo(auditUri);
      AtomicInteger counter = (AtomicInteger) inboundRequest.getAttribute(FAILOVER_COUNTER_ATTRIBUTE);
      if (counter == null) {
         counter = new AtomicInteger(0);
      }
      inboundRequest.setAttribute(FAILOVER_COUNTER_ATTRIBUTE, counter);
      if (counter.incrementAndGet() <= maxFailoverAttempts) {
         haProvider.markFailedURL(getResourceRole(), auditUri);
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
         if (failoverSleep > 0) {
            try {
               Thread.sleep(failoverSleep);
            } catch (InterruptedException e) {
               LOG.failoverSleepFailed(getResourceRole(), e);
            }
         }
         LOG.failingOverRequest(auditUri);
         executeRequest(outboundRequest, inboundRequest, outboundResponse);
      } else {
         LOG.maxFailoverAttemptsReached(maxFailoverAttempts, getResourceRole());
         if (inboundResponse != null) {
            writeOutboundResponse(outboundRequest, inboundRequest, outboundResponse, inboundResponse);
         } else {
            throw new IOException(exception);
         }
      }
   }
}
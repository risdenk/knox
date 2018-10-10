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
package org.apache.knox.gateway.dispatch;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.knox.gateway.ha.dispatch.DefaultHaDispatch;
import org.apache.knox.gateway.util.MimeTypes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

public class NiFiHaDispatch extends DefaultHaDispatch {

  public NiFiHaDispatch() {
    setServiceRole("NIFI");
  }

  @Override
  protected void executeRequest(ClassicHttpRequest outboundRequest, HttpServletRequest inboundRequest, HttpServletResponse outboundResponse) throws IOException {
    ClassicHttpResponse inboundResponse = null;
    try {
      outboundRequest = NiFiRequestUtil.modifyOutboundRequest(outboundRequest, inboundRequest);
      inboundResponse = executeOutboundRequest(outboundRequest);
      writeOutboundResponse(outboundRequest, inboundRequest, outboundResponse, inboundResponse);
    } catch (IOException e) {
      LOG.errorConnectingToServer(outboundRequest.getRequestUri(), e);
      failoverRequest(outboundRequest, inboundRequest, outboundResponse, inboundResponse, e);
    }
  }

  /*
   * Overridden to provide a spot to modify the outbound response before its stream is closed.
   */
  @Override
  protected void writeOutboundResponse(ClassicHttpRequest outboundRequest, HttpServletRequest inboundRequest, HttpServletResponse outboundResponse, ClassicHttpResponse inboundResponse) throws IOException {
    // Copy the client respond header to the server respond.
    outboundResponse.setStatus(inboundResponse.getCode());
    Header[] headers = inboundResponse.getHeaders();
    Set<String> excludeHeaders = getOutboundResponseExcludeHeaders();
    boolean hasExcludeHeaders = false;
    if ((excludeHeaders != null) && !(excludeHeaders.isEmpty())) {
      hasExcludeHeaders = true;
    }
    for ( Header header : headers ) {
      String name = header.getName();
      if (hasExcludeHeaders && excludeHeaders.contains(name.toUpperCase(Locale.ROOT))) {
        continue;
      }
      String value = header.getValue();
      outboundResponse.addHeader(name, value);
    }

    HttpEntity entity = inboundResponse.getEntity();
    if( entity != null ) {
      outboundResponse.setContentType( getInboundResponseContentType( entity ) );
      InputStream stream = entity.getContent();
      try {
        NiFiResponseUtil.modifyOutboundResponse(inboundRequest, outboundResponse, inboundResponse);
        writeResponse( inboundRequest, outboundResponse, stream );
      } finally {
        closeInboundResponse( inboundResponse, stream );
      }
    }
  }

  /**
   * Overriden due to DefaultDispatch#getInboundResponseContentType(HttpEntity) having private access, and the method is used by
   * {@link #writeOutboundResponse(ClassicHttpRequest, HttpServletRequest, HttpServletResponse, ClassicHttpResponse)}}
   */
  private String getInboundResponseContentType( final HttpEntity entity ) {
    String fullContentType = null;
    if( entity != null ) {
      ContentType entityContentType = EntityUtils.getContentType(entity);
      if( entityContentType != null ) {
        if( entityContentType.getCharset() == null ) {
          final String entityMimeType = entityContentType.getMimeType();
          final String defaultCharset = MimeTypes.getDefaultCharsetForMimeType( entityMimeType );
          if( defaultCharset != null ) {
            DefaultDispatch.LOG.usingDefaultCharsetForEntity( entityMimeType, defaultCharset );
            entityContentType = entityContentType.withCharset( defaultCharset );
          }
        } else {
          DefaultDispatch.LOG.usingExplicitCharsetForEntity( entityContentType.getMimeType(), entityContentType.getCharset() );
        }
        fullContentType = entityContentType.toString();
      }
    }
    if( fullContentType == null ) {
      DefaultDispatch.LOG.unknownResponseEntityContentType();
    } else {
      DefaultDispatch.LOG.inboundResponseEntityContentType( fullContentType );
    }
    return fullContentType;
  }
}

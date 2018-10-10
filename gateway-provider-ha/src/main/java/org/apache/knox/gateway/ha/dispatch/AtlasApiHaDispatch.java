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
import org.apache.hc.core5.http.Header;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AtlasApiHaDispatch extends DefaultHaDispatch {
  private static Set<String> REQUEST_EXCLUDE_HEADERS = new HashSet<>();

  static {
    REQUEST_EXCLUDE_HEADERS.add("Content-Length");
  }

  public AtlasApiHaDispatch() {
    setServiceRole("ATLAS-API");
  }

  @Override
  public Set<String> getOutboundResponseExcludeHeaders() {
    return Collections.emptySet();
  }

  @Override
  public Set<String> getOutboundRequestExcludeHeaders() {
    return REQUEST_EXCLUDE_HEADERS;
  }

  @Override
  protected void executeRequest(ClassicHttpRequest outboundRequest, HttpServletRequest inboundRequest, HttpServletResponse outboundResponse) throws IOException {
    ClassicHttpResponse inboundResponse = null;
    try {
      inboundResponse = executeOutboundRequest(outboundRequest);
      int statusCode = inboundResponse.getCode();
      Header originalLocationHeader = inboundResponse.getFirstHeader("Location");

      if ((statusCode == HttpServletResponse.SC_MOVED_TEMPORARILY || statusCode == HttpServletResponse.SC_TEMPORARY_REDIRECT) && originalLocationHeader != null) {
        inboundResponse.removeHeaders("Location");
        failoverRequest(outboundRequest, inboundRequest, outboundResponse, inboundResponse, new Exception("Atlas HA redirection"));
      }

      writeOutboundResponse(outboundRequest, inboundRequest, outboundResponse, inboundResponse);

    } catch (IOException e) {
      LOG.errorConnectingToServer(outboundRequest.getRequestUri(), e);
      failoverRequest(outboundRequest, inboundRequest, outboundResponse, inboundResponse, e);
    }
  }
}

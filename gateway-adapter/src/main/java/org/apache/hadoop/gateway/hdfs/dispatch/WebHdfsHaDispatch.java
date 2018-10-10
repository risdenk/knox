/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.hadoop.gateway.hdfs.dispatch;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.knox.gateway.ha.provider.HaProvider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Deprecated
public class WebHdfsHaDispatch extends org.apache.knox.gateway.hdfs.dispatch.WebHdfsHaDispatch {

  public WebHdfsHaDispatch() throws ServletException {
    super();
  }

  @Override
  public void init() {
    super.init();
  }

  @Override
  public HaProvider getHaProvider() {
    return super.getHaProvider();
  }

  @Override
  public void setHaProvider(HaProvider haProvider) {
    super.setHaProvider(haProvider);
  }

  @Override
  protected void executeRequest(ClassicHttpRequest outboundRequest,
                                HttpServletRequest inboundRequest, HttpServletResponse outboundResponse)
      throws IOException {
    super.executeRequest(outboundRequest, inboundRequest, outboundResponse);
  }

  @Override
  protected void writeOutboundResponse(ClassicHttpRequest outboundRequest,
      HttpServletRequest inboundRequest, HttpServletResponse outboundResponse,
      ClassicHttpResponse inboundResponse) throws IOException {
    super.writeOutboundResponse(outboundRequest, inboundRequest,
        outboundResponse, inboundResponse);
  }
}

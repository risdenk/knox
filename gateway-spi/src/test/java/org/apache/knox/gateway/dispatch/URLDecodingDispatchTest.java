/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.  The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.knox.gateway.dispatch;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test NoURLEncodingDispatch dispatch.
 *
 * @since 1.1.0
 */
class URLDecodingDispatchTest {
  private final Dispatch dispatch = new URLDecodingDispatch();

  /*
   * Test whether the encoded url is decoded properly.
   */
  @Test
  void testGetDispatchUrl() {
    final String path = "https://localhost:8443/gateway/sandbox/datanode/datanode.html?host=http%3A%2F%2Flocalhost%3A9864";
    URI uri;

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getRequestURI()).andReturn(path).anyTimes();
    EasyMock.expect(request.getRequestURL()).andReturn(new StringBuffer(path))
        .anyTimes();
    EasyMock.expect(request.getQueryString()).andReturn(null).anyTimes();
    EasyMock.replay(request);
    uri = dispatch.getDispatchUrl(request);
    assertThat(uri.toASCIIString(),
        is("https://localhost:8443/gateway/sandbox/datanode/datanode.html?host=http://localhost:9864"));
  }
}

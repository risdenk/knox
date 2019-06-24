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
package org.apache.knox.gateway.webappsec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.knox.gateway.webappsec.filter.XFrameOptionsFilter;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

class XFrameOptionsFilterTest {
  private static final String X_FRAME_OPTIONS = "X-Frame-Options";
  private String options;
  private Collection<String> headers;

  @Test
  void testDefaultOptionsValue() throws Exception {
    try {
      XFrameOptionsFilter filter = new XFrameOptionsFilter();
      Properties props = new Properties();
      props.put("xframe.options.enabled", "true");
      filter.init(new TestFilterConfig(props));

      HttpServletRequest request = EasyMock.createNiceMock(
          HttpServletRequest.class);
      HttpServletResponse response = EasyMock.createNiceMock(
          HttpServletResponse.class);
      EasyMock.replay(request);
      EasyMock.replay(response);

      TestFilterChain chain = new TestFilterChain();
      filter.doFilter(request, response, chain);
      assertTrue(chain.doFilterCalled, "doFilterCalled should not be false.");
      assertEquals("DENY", options,
          "Options value incorrect should be DENY but is: " + options);

      assertEquals(1, headers.size(),
          "X-Frame-Options count not equal to 1.");
    } catch (ServletException se) {
      fail("Should NOT have thrown a ServletException.");
    }
  }

  @Test
  void testConfiguredOptionsValue() throws Exception {
    try {
      XFrameOptionsFilter filter = new XFrameOptionsFilter();
      Properties props = new Properties();
      props.put("xframe.options.enabled", "true");
      props.put("xframe.options", "SAMEORIGIN");
      filter.init(new TestFilterConfig(props));

      HttpServletRequest request = EasyMock.createNiceMock(
          HttpServletRequest.class);
      HttpServletResponse response = EasyMock.createNiceMock(
          HttpServletResponse.class);
      EasyMock.replay(request);
      EasyMock.replay(response);

      TestFilterChain chain = new TestFilterChain();
      filter.doFilter(request, response, chain);
      assertTrue(chain.doFilterCalled, "doFilterCalled should not be false.");
      assertEquals("SAMEORIGIN", options,
          "Options value incorrect should be SAMEORIGIN but is: " + options);

      assertEquals(1, headers.size(), "X-Frame-Options count not equal to 1.");
    } catch (ServletException se) {
      fail("Should NOT have thrown a ServletException.");
    }
  }

  class TestFilterConfig implements FilterConfig {
    Properties props;

    TestFilterConfig(Properties props) {
      this.props = props;
    }

    @Override
    public String getFilterName() {
      return null;
    }

    @Override
    public ServletContext getServletContext() {
      return null;
    }

    @Override
    public String getInitParameter(String name) {
      return props.getProperty(name, null);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
      return null;
    }
  }

  class TestFilterChain implements FilterChain {
    boolean doFilterCalled;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) {
      doFilterCalled = true;
      options = ((HttpServletResponse)response).getHeader(X_FRAME_OPTIONS);
      headers = ((HttpServletResponse)response).getHeaders(X_FRAME_OPTIONS);
    }
  }
}

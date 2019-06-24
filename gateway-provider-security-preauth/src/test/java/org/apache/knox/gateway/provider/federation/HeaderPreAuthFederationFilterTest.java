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
package org.apache.knox.gateway.provider.federation;

import org.apache.knox.gateway.preauth.filter.DefaultValidator;
import org.apache.knox.gateway.preauth.filter.HeaderPreAuthFederationFilter;
import org.apache.knox.gateway.preauth.filter.IPValidator;
import org.apache.knox.gateway.preauth.filter.PreAuthService;
import org.apache.knox.gateway.preauth.filter.PreAuthValidator;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HeaderPreAuthFederationFilterTest {
  @Test
  void testDefaultValidator() throws ServletException {
    HeaderPreAuthFederationFilter hpaff = new HeaderPreAuthFederationFilter();
    final HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    final FilterConfig filterConfig = EasyMock.createNiceMock(FilterConfig.class);
    EasyMock.expect(filterConfig.getInitParameter(PreAuthService.VALIDATION_METHOD_PARAM)).andReturn
        (DefaultValidator.DEFAULT_VALIDATION_METHOD_VALUE);
    EasyMock.replay(filterConfig);
    EasyMock.replay(request);

    hpaff.init(filterConfig);
    List<PreAuthValidator> validators = hpaff.getValidators();
    assertEquals(validators.size(), 1);
    assertEquals(validators.get(0).getName(), DefaultValidator.DEFAULT_VALIDATION_METHOD_VALUE);
    assertTrue(PreAuthService.validate(request, filterConfig, validators));
  }

  @Test
  void testIPValidator() throws ServletException {
    HeaderPreAuthFederationFilter hpaff = new HeaderPreAuthFederationFilter();

    final FilterConfig filterConfig = EasyMock.createNiceMock(FilterConfig.class);
    EasyMock.expect(filterConfig.getInitParameter(IPValidator.IP_ADDRESSES_PARAM))
        .andReturn("5.4.3.2,10.1.23.42").anyTimes();
    EasyMock.expect(filterConfig.getInitParameter(PreAuthService.VALIDATION_METHOD_PARAM))
        .andReturn(IPValidator.IP_VALIDATION_METHOD_VALUE).anyTimes();
    EasyMock.replay(filterConfig);

    final HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getRemoteAddr()).andReturn("10.1.23.42");
    EasyMock.replay(request);

    hpaff.init(filterConfig);
    List<PreAuthValidator> validators = hpaff.getValidators();
    assertEquals(validators.size(), 1);
    assertEquals(validators.get(0).getName(), IPValidator.IP_VALIDATION_METHOD_VALUE);
    assertTrue(PreAuthService.validate(request, filterConfig, validators));

    //Negative testing
    EasyMock.reset(request);
    EasyMock.expect(request.getRemoteAddr()).andReturn("10.10.22.33");
    EasyMock.replay(request);
    assertFalse(PreAuthService.validate(request, filterConfig, validators));
  }

  @Test
  void testCustomValidatorPositive() throws ServletException {
    HeaderPreAuthFederationFilter hpaff = new HeaderPreAuthFederationFilter();
    final HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    final FilterConfig filterConfig = EasyMock.createNiceMock(FilterConfig.class);
    EasyMock.expect(filterConfig.getInitParameter(PreAuthService.VALIDATION_METHOD_PARAM)).andReturn
        (DummyValidator.NAME);
    EasyMock.replay(request);
    EasyMock.replay(filterConfig);

    hpaff.init(filterConfig);
    List<PreAuthValidator> validators = hpaff.getValidators();
    assertEquals(validators.size(), 1);
    assertEquals(validators.get(0).getName(), DummyValidator.NAME);
    //Positive test
    EasyMock.reset(request);
    EasyMock.expect(request.getHeader("CUSTOM_TOKEN")).andReturn("HelloWorld");
    EasyMock.replay(request);
    assertTrue(PreAuthService.validate(request, filterConfig, validators));

  }

  @Test
  void testCustomValidatorNegative() throws ServletException {
    HeaderPreAuthFederationFilter hpaff = new HeaderPreAuthFederationFilter();
    final HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    final FilterConfig filterConfig = EasyMock.createNiceMock(FilterConfig.class);
    EasyMock.expect(filterConfig.getInitParameter(PreAuthService.VALIDATION_METHOD_PARAM)).andReturn
        (DummyValidator.NAME);
    EasyMock.replay(request);
    EasyMock.replay(filterConfig);

    hpaff.init(filterConfig);
    List<PreAuthValidator> validators = hpaff.getValidators();
    assertEquals(validators.size(), 1);
    assertEquals(validators.get(0).getName(), DummyValidator.NAME);

    EasyMock.reset(request);
    EasyMock.expect(request.getHeader("CUSTOM_TOKEN")).andReturn("NOTHelloWorld");
    EasyMock.replay(request);
    assertFalse(PreAuthService.validate(request, filterConfig, validators));

  }

  public static class DummyValidator implements PreAuthValidator {
    static final String NAME = "DummyValidator";

    public DummyValidator() {
    }

    @Override
    public boolean validate(HttpServletRequest httpRequest, FilterConfig filterConfig) {
      String token = httpRequest.getHeader("CUSTOM_TOKEN");
      return "HelloWorld".equalsIgnoreCase(token);
    }

    @Override
    public String getName() {
      return NAME;
    }
  }
}

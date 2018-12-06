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
package org.apache.knox.gateway.identityasserter.common.filter;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.knox.gateway.SpiGatewayMessages;
import org.apache.knox.gateway.config.GatewayConfig;
import org.apache.knox.gateway.i18n.messages.MessagesFactory;
import org.apache.knox.gateway.security.PrimaryPrincipal;
import org.apache.knox.gateway.servlet.SynchronousServletInputStreamAdapter;
import org.eclipse.jetty.http.HttpMethod;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

public class IdentityAsserterHttpServletRequestWrapper extends HttpServletRequestWrapper {

  private static final SpiGatewayMessages LOG = MessagesFactory.get( SpiGatewayMessages.class );
  private static final String PRINCIPAL_PARAM = "user.name";
  private static final String DOAS_PRINCIPAL_PARAM = "doAs";

  private final String username;

  public IdentityAsserterHttpServletRequestWrapper( HttpServletRequest request, String principal ) {
    super(request);
    username = principal;
  }

  @Override
  public Principal getUserPrincipal() {
    return new PrimaryPrincipal(username);
  }

  @Override
  public String getParameter(String name) {
    if (name.equals(PRINCIPAL_PARAM)) {
      return username;
    }
    return super.getParameter(name);
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    return convertValuesToStringArrays(getParams());
  }

  private Map<String, String[]> convertValuesToStringArrays(List<NameValuePair> params) {
    Map<String, List<String>> tempMap = new LinkedHashMap<>();
    for(NameValuePair param : params) {
      String paramKey = param.getName();
      if(!tempMap.containsKey(paramKey)) {
        tempMap.put(paramKey, new ArrayList<>());
      }
      tempMap.get(paramKey).add(param.getValue());
    }

    Map<String, String[]> returnMap = new LinkedHashMap<>();
    for(Map.Entry<String, List<String>> entry : tempMap.entrySet()) {
      returnMap.put(entry.getKey(), entry.getValue().toArray(new String[0]));
    }
    return returnMap;
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return Collections.enumeration(getParameterMap().keySet());
  }

  @Override
  public String[] getParameterValues(String name) {
    return getParameterMap().get(name);
  }

  private List<NameValuePair> getParams(String qString ) {
    if (getMethod().equals(HttpMethod.GET.name())) {
      if (qString != null && !qString.isEmpty()) {
        return URLEncodedUtils.parse(qString, StandardCharsets.UTF_8);
      } else {
        return new ArrayList<>();
      }
    } else {
      if (qString == null || qString.isEmpty()) {
        return new ArrayList<>();
      } else {
        return URLEncodedUtils.parse(qString, StandardCharsets.UTF_8);
      }
    }
  }

  private List<NameValuePair> getParams() {
    return getParams( super.getQueryString() );
  }

  @Override
  public String getQueryString() {
    List<NameValuePair> params = getParams();
    List<String> principalParamNames = getImpersonationParamNames();
    List<NameValuePair> scrubbedParams = scrubOfExistingPrincipalParams(params, principalParamNames);

    if (Boolean.parseBoolean(System.getProperty(GatewayConfig.HADOOP_KERBEROS_SECURED))) {
      scrubbedParams.add(new BasicNameValuePair(DOAS_PRINCIPAL_PARAM, username));
    } else {
      scrubbedParams.add(new BasicNameValuePair(PRINCIPAL_PARAM, username));
    }

    String encoding = getCharacterEncoding();
    if (encoding == null) {
      encoding = Charset.defaultCharset().name();
    }

    return URLEncodedUtils.format(params, encoding);
  }

  private List<String> getImpersonationParamNames() {
    // TODO: let's have service definitions register their impersonation
    // params in a future release and get this list from a central registry.
    // This will provide better coverage of protection by removing any
    // prepopulated impersonation params.
    List<String> principalParamNames = new ArrayList<>();
    principalParamNames.add(DOAS_PRINCIPAL_PARAM);
    principalParamNames.add(PRINCIPAL_PARAM);
    return principalParamNames;
  }

  private List<NameValuePair> scrubOfExistingPrincipalParams(
      List<NameValuePair> params, List<String> principalParamNames) {
    Iterator<NameValuePair> iterator = params.iterator();
    while(iterator.hasNext()) {
      NameValuePair param = iterator.next();
      for (String p : principalParamNames) {
        String paramKey = param.getName();
        if (p.equalsIgnoreCase(paramKey)) {
          iterator.remove();
          LOG.possibleIdentitySpoofingAttempt(paramKey);
        }
      }
    }
    return params;
  }

  @Override
  public int getContentLength() {
    int len;
    String contentType = getContentType();
    // If the content type is a form we might rewrite the body so default it to -1.
    if( contentType != null && contentType.startsWith( "application/x-www-form-urlencoded" ) ) {
      len = -1;
    } else {
      len = super.getContentLength();
    }
    return len;
  }

  @Override
  public ServletInputStream getInputStream() throws java.io.IOException {
    String contentType = getContentType();
    if( contentType != null && contentType.startsWith( "application/x-www-form-urlencoded" ) ) {
      String encoding = getCharacterEncoding();
      if( encoding == null ) {
        encoding = Charset.defaultCharset().name();
      }
      String body = IOUtils.toString( super.getInputStream(), encoding );
      List<NameValuePair> params = getParams( body );
      body = URLEncodedUtils.format( params, encoding );
      // ASCII is OK here because body should have already been escaped
      return new ServletInputStreamWrapper( new ByteArrayInputStream( body.getBytes(StandardCharsets.US_ASCII.name()) ) );
    } else {
      return super.getInputStream();
    }
  }

  private static class ServletInputStreamWrapper extends SynchronousServletInputStreamAdapter {
    private final InputStream stream;

    ServletInputStreamWrapper( InputStream stream ) {
      this.stream = stream;
    }

    @Override
    public int read() throws IOException {
      return stream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
      return stream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      return stream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
      return stream.skip(n);
    }

    @Override
    public int available() throws IOException {
      return stream.available();
    }

    @Override
    public void close() throws IOException {
      stream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
      stream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
      stream.reset();
    }

    @Override
    public boolean markSupported() {
      return stream.markSupported();
    }
  }
}

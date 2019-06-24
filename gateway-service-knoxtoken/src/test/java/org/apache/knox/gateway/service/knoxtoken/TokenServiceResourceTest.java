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
package org.apache.knox.gateway.service.knoxtoken;

import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import org.apache.knox.gateway.security.PrimaryPrincipal;
import org.apache.knox.gateway.services.ServiceType;
import org.apache.knox.gateway.services.GatewayServices;
import org.apache.knox.gateway.services.security.token.JWTokenAuthority;
import org.apache.knox.gateway.services.security.token.impl.JWT;
import org.apache.knox.gateway.services.security.token.impl.JWTToken;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.security.auth.Subject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Some tests for the token service
 */
class TokenServiceResourceTest {
  private static RSAPublicKey publicKey;
  private static RSAPrivateKey privateKey;

  @BeforeAll
  static void setUpBeforeClass() throws Exception {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048);
    KeyPair KPair = kpg.generateKeyPair();

    publicKey = (RSAPublicKey) KPair.getPublic();
    privateKey = (RSAPrivateKey) KPair.getPrivate();
  }

  @Test
  void testClientData() {
    TokenResource tr = new TokenResource();

    Map<String,Object> clientDataMap = new HashMap<>();
    tr.addClientDataToMap("cookie.name=hadoop-jwt,test=value".split(","), clientDataMap);
    assertEquals(2, clientDataMap.size());

    clientDataMap = new HashMap<>();
    tr.addClientDataToMap("cookie.name=hadoop-jwt".split(","), clientDataMap);
    assertEquals(1, clientDataMap.size());

    clientDataMap = new HashMap<>();
    tr.addClientDataToMap("".split(","), clientDataMap);
    assertEquals(0, clientDataMap.size());
  }

  @Test
  void testGetToken() throws Exception {
    ServletContext context = EasyMock.createNiceMock(ServletContext.class);

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getServletContext()).andReturn(context).anyTimes();
    Principal principal = EasyMock.createNiceMock(Principal.class);
    EasyMock.expect(principal.getName()).andReturn("alice").anyTimes();
    EasyMock.expect(request.getUserPrincipal()).andReturn(principal).anyTimes();

    GatewayServices services = EasyMock.createNiceMock(GatewayServices.class);
    EasyMock.expect(context.getAttribute(GatewayServices.GATEWAY_SERVICES_ATTRIBUTE)).andReturn(services);

    JWTokenAuthority authority = new TestJWTokenAuthority(publicKey, privateKey);
    EasyMock.expect(services.getService(ServiceType.TOKEN_SERVICE)).andReturn(authority);

    EasyMock.replay(principal, services, context, request);

    TokenResource tr = new TokenResource();
    tr.request = request;

    // Issue a token
    Response retResponse = tr.doGet();

    assertEquals(200, retResponse.getStatus());

    // Parse the response
    String retString = retResponse.getEntity().toString();
    String accessToken = getTagValue(retString, "access_token");
    assertNotNull(accessToken);
    String expiry = getTagValue(retString, "expires_in");
    assertNotNull(expiry);

    // Verify the token
    JWT parsedToken = new JWTToken(accessToken);
    assertEquals("alice", parsedToken.getSubject());
    assertTrue(authority.verifyToken(parsedToken));
  }

  @Test
  void testAudiences() throws Exception {
    ServletContext context = EasyMock.createNiceMock(ServletContext.class);
    EasyMock.expect(context.getInitParameter("knox.token.audiences")).andReturn("recipient1,recipient2");
    EasyMock.expect(context.getInitParameter("knox.token.ttl")).andReturn(null);
    EasyMock.expect(context.getInitParameter("knox.token.target.url")).andReturn(null);
    EasyMock.expect(context.getInitParameter("knox.token.client.data")).andReturn(null);

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getServletContext()).andReturn(context).anyTimes();
    Principal principal = EasyMock.createNiceMock(Principal.class);
    EasyMock.expect(principal.getName()).andReturn("alice").anyTimes();
    EasyMock.expect(request.getUserPrincipal()).andReturn(principal).anyTimes();

    GatewayServices services = EasyMock.createNiceMock(GatewayServices.class);
    EasyMock.expect(context.getAttribute(GatewayServices.GATEWAY_SERVICES_ATTRIBUTE)).andReturn(services);

    JWTokenAuthority authority = new TestJWTokenAuthority(publicKey, privateKey);
    EasyMock.expect(services.getService(ServiceType.TOKEN_SERVICE)).andReturn(authority);

    EasyMock.replay(principal, services, context, request);

    TokenResource tr = new TokenResource();
    tr.request = request;
    tr.context = context;
    tr.init();

    // Issue a token
    Response retResponse = tr.doGet();

    assertEquals(200, retResponse.getStatus());

    // Parse the response
    String retString = retResponse.getEntity().toString();
    String accessToken = getTagValue(retString, "access_token");
    assertNotNull(accessToken);
    String expiry = getTagValue(retString, "expires_in");
    assertNotNull(expiry);

    // Verify the token
    JWT parsedToken = new JWTToken(accessToken);
    assertEquals("alice", parsedToken.getSubject());
    assertTrue(authority.verifyToken(parsedToken));

    // Verify the audiences
    List<String> audiences = Arrays.asList(parsedToken.getAudienceClaims());
    assertEquals(2, audiences.size());
    assertTrue(audiences.contains("recipient1"));
    assertTrue(audiences.contains("recipient2"));
  }

  @Test
  void testAudiencesWhitespace() throws Exception {
    ServletContext context = EasyMock.createNiceMock(ServletContext.class);
    EasyMock.expect(context.getInitParameter("knox.token.audiences")).andReturn(" recipient1, recipient2 ");
    EasyMock.expect(context.getInitParameter("knox.token.ttl")).andReturn(null);
    EasyMock.expect(context.getInitParameter("knox.token.target.url")).andReturn(null);
    EasyMock.expect(context.getInitParameter("knox.token.client.data")).andReturn(null);

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getServletContext()).andReturn(context).anyTimes();
    Principal principal = EasyMock.createNiceMock(Principal.class);
    EasyMock.expect(principal.getName()).andReturn("alice").anyTimes();
    EasyMock.expect(request.getUserPrincipal()).andReturn(principal).anyTimes();

    GatewayServices services = EasyMock.createNiceMock(GatewayServices.class);
    EasyMock.expect(context.getAttribute(GatewayServices.GATEWAY_SERVICES_ATTRIBUTE)).andReturn(services);

    JWTokenAuthority authority = new TestJWTokenAuthority(publicKey, privateKey);
    EasyMock.expect(services.getService(ServiceType.TOKEN_SERVICE)).andReturn(authority);

    EasyMock.replay(principal, services, context, request);

    TokenResource tr = new TokenResource();
    tr.request = request;
    tr.context = context;
    tr.init();

    // Issue a token
    Response retResponse = tr.doGet();

    assertEquals(200, retResponse.getStatus());

    // Parse the response
    String retString = retResponse.getEntity().toString();
    String accessToken = getTagValue(retString, "access_token");
    assertNotNull(accessToken);
    String expiry = getTagValue(retString, "expires_in");
    assertNotNull(expiry);

    // Verify the token
    JWT parsedToken = new JWTToken(accessToken);
    assertEquals("alice", parsedToken.getSubject());
    assertTrue(authority.verifyToken(parsedToken));

    // Verify the audiences
    List<String> audiences = Arrays.asList(parsedToken.getAudienceClaims());
    assertEquals(2, audiences.size());
    assertTrue(audiences.contains("recipient1"));
    assertTrue(audiences.contains("recipient2"));
  }

  @Test
  void testValidClientCert() throws Exception {
    ServletContext context = EasyMock.createNiceMock(ServletContext.class);
    EasyMock.expect(context.getInitParameter("knox.token.client.cert.required")).andReturn("true");
    EasyMock.expect(context.getInitParameter("knox.token.allowed.principals")).andReturn("CN=localhost, OU=Test, O=Hadoop, L=Test, ST=Test, C=US");

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getServletContext()).andReturn(context).anyTimes();
    X509Certificate trustedCertMock = EasyMock.createMock(X509Certificate.class);
    EasyMock.expect(trustedCertMock.getSubjectDN()).andReturn(new PrimaryPrincipal("CN=localhost,OU=Test, O=Hadoop, L=Test, ST=Test, C=US")).anyTimes();
    ArrayList<X509Certificate> certArrayList = new ArrayList<>();
    certArrayList.add(trustedCertMock);
    X509Certificate[] certs = {};
    EasyMock.expect(request.getAttribute("javax.servlet.request.X509Certificate")).andReturn(certArrayList.toArray(certs)).anyTimes();

    Principal principal = EasyMock.createNiceMock(Principal.class);
    EasyMock.expect(principal.getName()).andReturn("alice").anyTimes();
    EasyMock.expect(request.getUserPrincipal()).andReturn(principal).anyTimes();

    GatewayServices services = EasyMock.createNiceMock(GatewayServices.class);
    EasyMock.expect(context.getAttribute(GatewayServices.GATEWAY_SERVICES_ATTRIBUTE)).andReturn(services);

    JWTokenAuthority authority = new TestJWTokenAuthority(publicKey, privateKey);
    EasyMock.expect(services.getService(ServiceType.TOKEN_SERVICE)).andReturn(authority);

    EasyMock.replay(principal, services, context, request, trustedCertMock);

    TokenResource tr = new TokenResource();
    tr.request = request;
    tr.context = context;
    tr.init();

    // Issue a token
    Response retResponse = tr.doGet();

    assertEquals(200, retResponse.getStatus());

    // Parse the response
    String retString = retResponse.getEntity().toString();
    String accessToken = getTagValue(retString, "access_token");
    assertNotNull(accessToken);
    String expiry = getTagValue(retString, "expires_in");
    assertNotNull(expiry);

    // Verify the token
    JWT parsedToken = new JWTToken(accessToken);
    assertEquals("alice", parsedToken.getSubject());
    assertTrue(authority.verifyToken(parsedToken));
  }

  @Test
  void testValidClientCertWrongUser() {
    ServletContext context = EasyMock.createNiceMock(ServletContext.class);
    EasyMock.expect(context.getInitParameter("knox.token.client.cert.required")).andReturn("true");
    EasyMock.expect(context.getInitParameter("knox.token.allowed.principals")).andReturn("CN=remotehost, OU=Test, O=Hadoop, L=Test, ST=Test, C=US");

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getServletContext()).andReturn(context).anyTimes();
    X509Certificate trustedCertMock = EasyMock.createMock(X509Certificate.class);
    EasyMock.expect(trustedCertMock.getSubjectDN()).andReturn(new PrimaryPrincipal("CN=localhost, OU=Test, O=Hadoop, L=Test, ST=Test, C=US")).anyTimes();
    ArrayList<X509Certificate> certArrayList = new ArrayList<>();
    certArrayList.add(trustedCertMock);
    X509Certificate[] certs = {};
    EasyMock.expect(request.getAttribute("javax.servlet.request.X509Certificate")).andReturn(certArrayList.toArray(certs)).anyTimes();

    Principal principal = EasyMock.createNiceMock(Principal.class);
    EasyMock.expect(principal.getName()).andReturn("alice").anyTimes();
    EasyMock.expect(request.getUserPrincipal()).andReturn(principal).anyTimes();

    GatewayServices services = EasyMock.createNiceMock(GatewayServices.class);
    EasyMock.expect(context.getAttribute(GatewayServices.GATEWAY_SERVICES_ATTRIBUTE)).andReturn(services);

    JWTokenAuthority authority = new TestJWTokenAuthority(publicKey, privateKey);
    EasyMock.expect(services.getService(ServiceType.TOKEN_SERVICE)).andReturn(authority);

    EasyMock.replay(principal, services, context, request, trustedCertMock);

    TokenResource tr = new TokenResource();
    tr.request = request;
    tr.context = context;
    tr.init();

    // Issue a token
    Response retResponse = tr.doGet();

    assertEquals(403, retResponse.getStatus());
  }

  @Test
  void testMissingClientCert() {
    ServletContext context = EasyMock.createNiceMock(ServletContext.class);
    EasyMock.expect(context.getInitParameter("knox.token.client.cert.required")).andReturn("true");
    EasyMock.expect(context.getInitParameter("knox.token.allowed.principals")).andReturn("CN=remotehost, OU=Test, O=Hadoop, L=Test, ST=Test, C=US");

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getServletContext()).andReturn(context).anyTimes();
    EasyMock.expect(request.getAttribute("javax.servlet.request.X509Certificate")).andReturn(null).anyTimes();

    Principal principal = EasyMock.createNiceMock(Principal.class);
    EasyMock.expect(principal.getName()).andReturn("alice").anyTimes();
    EasyMock.expect(request.getUserPrincipal()).andReturn(principal).anyTimes();

    GatewayServices services = EasyMock.createNiceMock(GatewayServices.class);
    EasyMock.expect(context.getAttribute(GatewayServices.GATEWAY_SERVICES_ATTRIBUTE)).andReturn(services);

    JWTokenAuthority authority = new TestJWTokenAuthority(publicKey, privateKey);
    EasyMock.expect(services.getService(ServiceType.TOKEN_SERVICE)).andReturn(authority);

    EasyMock.replay(principal, services, context, request);

    TokenResource tr = new TokenResource();
    tr.request = request;
    tr.context = context;
    tr.init();

    // Issue a token
    Response retResponse = tr.doGet();

    assertEquals(403, retResponse.getStatus());
  }

  @Test
  void testSignatureAlgorithm() throws Exception {
    ServletContext context = EasyMock.createNiceMock(ServletContext.class);
    EasyMock.expect(context.getInitParameter("knox.token.audiences")).andReturn("recipient1,recipient2");
    EasyMock.expect(context.getInitParameter("knox.token.ttl")).andReturn(null);
    EasyMock.expect(context.getInitParameter("knox.token.target.url")).andReturn(null);
    EasyMock.expect(context.getInitParameter("knox.token.client.data")).andReturn(null);
    EasyMock.expect(context.getInitParameter("knox.token.sigalg")).andReturn("RS512");

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getServletContext()).andReturn(context).anyTimes();
    Principal principal = EasyMock.createNiceMock(Principal.class);
    EasyMock.expect(principal.getName()).andReturn("alice").anyTimes();
    EasyMock.expect(request.getUserPrincipal()).andReturn(principal).anyTimes();

    GatewayServices services = EasyMock.createNiceMock(GatewayServices.class);
    EasyMock.expect(context.getAttribute(GatewayServices.GATEWAY_SERVICES_ATTRIBUTE)).andReturn(services);

    JWTokenAuthority authority = new TestJWTokenAuthority(publicKey, privateKey);
    EasyMock.expect(services.getService(ServiceType.TOKEN_SERVICE)).andReturn(authority);

    EasyMock.replay(principal, services, context, request);

    TokenResource tr = new TokenResource();
    tr.request = request;
    tr.context = context;
    tr.init();

    // Issue a token
    Response retResponse = tr.doGet();

    assertEquals(200, retResponse.getStatus());

    // Parse the response
    String retString = retResponse.getEntity().toString();
    String accessToken = getTagValue(retString, "access_token");
    assertNotNull(accessToken);
    String expiry = getTagValue(retString, "expires_in");
    assertNotNull(expiry);

    // Verify the token
    JWT parsedToken = new JWTToken(accessToken);
    assertEquals("alice", parsedToken.getSubject());
    assertTrue(authority.verifyToken(parsedToken));
    assertTrue(parsedToken.getHeader().contains("RS512"));
  }

  @Test
  void testDefaultTTL() throws Exception {
    ServletContext context = EasyMock.createNiceMock(ServletContext.class);
    EasyMock.expect(context.getInitParameter("knox.token.audiences")).andReturn("recipient1,recipient2");
    EasyMock.expect(context.getInitParameter("knox.token.ttl")).andReturn(null);
    EasyMock.expect(context.getInitParameter("knox.token.target.url")).andReturn(null);
    EasyMock.expect(context.getInitParameter("knox.token.client.data")).andReturn(null);

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getServletContext()).andReturn(context).anyTimes();
    Principal principal = EasyMock.createNiceMock(Principal.class);
    EasyMock.expect(principal.getName()).andReturn("alice").anyTimes();
    EasyMock.expect(request.getUserPrincipal()).andReturn(principal).anyTimes();

    GatewayServices services = EasyMock.createNiceMock(GatewayServices.class);
    EasyMock.expect(context.getAttribute(GatewayServices.GATEWAY_SERVICES_ATTRIBUTE)).andReturn(services);

    JWTokenAuthority authority = new TestJWTokenAuthority(publicKey, privateKey);
    EasyMock.expect(services.getService(ServiceType.TOKEN_SERVICE)).andReturn(authority);

    EasyMock.replay(principal, services, context, request);

    TokenResource tr = new TokenResource();
    tr.request = request;
    tr.context = context;
    tr.init();

    // Issue a token
    Response retResponse = tr.doGet();

    assertEquals(200, retResponse.getStatus());

    // Parse the response
    String retString = retResponse.getEntity().toString();
    String accessToken = getTagValue(retString, "access_token");
    assertNotNull(accessToken);
    String expiry = getTagValue(retString, "expires_in");
    assertNotNull(expiry);

    // Verify the token
    JWT parsedToken = new JWTToken(accessToken);
    assertEquals("alice", parsedToken.getSubject());
    assertTrue(authority.verifyToken(parsedToken));

    Date expiresDate = parsedToken.getExpiresDate();
    Date now = new Date();
    assertTrue(expiresDate.after(now));
    assertTrue((expiresDate.getTime() - now.getTime()) < 30000L);
  }

  @Test
  void testCustomTTL() throws Exception {
    ServletContext context = EasyMock.createNiceMock(ServletContext.class);
    EasyMock.expect(context.getInitParameter("knox.token.audiences")).andReturn("recipient1,recipient2");
    EasyMock.expect(context.getInitParameter("knox.token.ttl")).andReturn("60000");
    EasyMock.expect(context.getInitParameter("knox.token.target.url")).andReturn(null);
    EasyMock.expect(context.getInitParameter("knox.token.client.data")).andReturn(null);

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getServletContext()).andReturn(context).anyTimes();
    Principal principal = EasyMock.createNiceMock(Principal.class);
    EasyMock.expect(principal.getName()).andReturn("alice").anyTimes();
    EasyMock.expect(request.getUserPrincipal()).andReturn(principal).anyTimes();

    GatewayServices services = EasyMock.createNiceMock(GatewayServices.class);
    EasyMock.expect(context.getAttribute(GatewayServices.GATEWAY_SERVICES_ATTRIBUTE)).andReturn(services);

    JWTokenAuthority authority = new TestJWTokenAuthority(publicKey, privateKey);
    EasyMock.expect(services.getService(ServiceType.TOKEN_SERVICE)).andReturn(authority);

    EasyMock.replay(principal, services, context, request);

    TokenResource tr = new TokenResource();
    tr.request = request;
    tr.context = context;
    tr.init();

    // Issue a token
    Response retResponse = tr.doGet();

    assertEquals(200, retResponse.getStatus());

    // Parse the response
    String retString = retResponse.getEntity().toString();
    String accessToken = getTagValue(retString, "access_token");
    assertNotNull(accessToken);
    String expiry = getTagValue(retString, "expires_in");
    assertNotNull(expiry);

    // Verify the token
    JWT parsedToken = new JWTToken(accessToken);
    assertEquals("alice", parsedToken.getSubject());
    assertTrue(authority.verifyToken(parsedToken));

    Date expiresDate = parsedToken.getExpiresDate();
    Date now = new Date();
    assertTrue(expiresDate.after(now));
    long diff = expiresDate.getTime() - now.getTime();
    assertTrue(diff < 60000L && diff > 30000L);
  }

  @Test
  void testNegativeTTL() throws Exception {
    ServletContext context = EasyMock.createNiceMock(ServletContext.class);
    EasyMock.expect(context.getInitParameter("knox.token.audiences")).andReturn("recipient1,recipient2");
    EasyMock.expect(context.getInitParameter("knox.token.ttl")).andReturn("-60000");
    EasyMock.expect(context.getInitParameter("knox.token.target.url")).andReturn(null);
    EasyMock.expect(context.getInitParameter("knox.token.client.data")).andReturn(null);

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getServletContext()).andReturn(context).anyTimes();
    Principal principal = EasyMock.createNiceMock(Principal.class);
    EasyMock.expect(principal.getName()).andReturn("alice").anyTimes();
    EasyMock.expect(request.getUserPrincipal()).andReturn(principal).anyTimes();

    GatewayServices services = EasyMock.createNiceMock(GatewayServices.class);
    EasyMock.expect(context.getAttribute(GatewayServices.GATEWAY_SERVICES_ATTRIBUTE)).andReturn(services);

    JWTokenAuthority authority = new TestJWTokenAuthority(publicKey, privateKey);
    EasyMock.expect(services.getService(ServiceType.TOKEN_SERVICE)).andReturn(authority);

    EasyMock.replay(principal, services, context, request);

    TokenResource tr = new TokenResource();
    tr.request = request;
    tr.context = context;
    tr.init();

    // Issue a token
    Response retResponse = tr.doGet();

    assertEquals(200, retResponse.getStatus());

    // Parse the response
    String retString = retResponse.getEntity().toString();
    String accessToken = getTagValue(retString, "access_token");
    assertNotNull(accessToken);
    String expiry = getTagValue(retString, "expires_in");
    assertNotNull(expiry);

    // Verify the token
    JWT parsedToken = new JWTToken(accessToken);
    assertEquals("alice", parsedToken.getSubject());
    assertTrue(authority.verifyToken(parsedToken));

    Date expiresDate = parsedToken.getExpiresDate();
    Date now = new Date();
    assertTrue(expiresDate.after(now));
    assertTrue((expiresDate.getTime() - now.getTime()) < 30000L);
  }

  @Test
  void testOverflowTTL() throws Exception {
    ServletContext context = EasyMock.createNiceMock(ServletContext.class);
    EasyMock.expect(context.getInitParameter("knox.token.audiences")).andReturn("recipient1,recipient2");
    EasyMock.expect(context.getInitParameter("knox.token.ttl")).andReturn(String.valueOf(Long.MAX_VALUE));
    EasyMock.expect(context.getInitParameter("knox.token.target.url")).andReturn(null);
    EasyMock.expect(context.getInitParameter("knox.token.client.data")).andReturn(null);

    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getServletContext()).andReturn(context).anyTimes();
    Principal principal = EasyMock.createNiceMock(Principal.class);
    EasyMock.expect(principal.getName()).andReturn("alice").anyTimes();
    EasyMock.expect(request.getUserPrincipal()).andReturn(principal).anyTimes();

    GatewayServices services = EasyMock.createNiceMock(GatewayServices.class);
    EasyMock.expect(context.getAttribute(GatewayServices.GATEWAY_SERVICES_ATTRIBUTE)).andReturn(services);

    JWTokenAuthority authority = new TestJWTokenAuthority(publicKey, privateKey);
    EasyMock.expect(services.getService(ServiceType.TOKEN_SERVICE)).andReturn(authority);

    EasyMock.replay(principal, services, context, request);

    TokenResource tr = new TokenResource();
    tr.request = request;
    tr.context = context;
    tr.init();

    // Issue a token
    Response retResponse = tr.doGet();

    assertEquals(200, retResponse.getStatus());

    // Parse the response
    String retString = retResponse.getEntity().toString();
    String accessToken = getTagValue(retString, "access_token");
    assertNotNull(accessToken);
    String expiry = getTagValue(retString, "expires_in");
    assertNotNull(expiry);

    // Verify the token
    JWT parsedToken = new JWTToken(accessToken);
    assertEquals("alice", parsedToken.getSubject());
    assertTrue(authority.verifyToken(parsedToken));

    Date expiresDate = parsedToken.getExpiresDate();
    Date now = new Date();
    assertTrue(expiresDate.after(now));
    assertTrue((expiresDate.getTime() - now.getTime()) < 30000L);
  }

  private String getTagValue(String token, String tagName) {
    String searchString = tagName + "\":";
    String value = token.substring(token.indexOf(searchString) + searchString.length());
    if (value.startsWith("\"")) {
      value = value.substring(1);
    }
    if (value.contains("\"")) {
      return value.substring(0, value.indexOf('\"'));
    } else if (value.contains(",")) {
      return value.substring(0, value.indexOf(','));
    } else {
      return value.substring(0, value.length() - 1);
    }
  }

  private static class TestJWTokenAuthority implements JWTokenAuthority {
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    TestJWTokenAuthority(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
      this.publicKey = publicKey;
      this.privateKey = privateKey;
    }

    @Override
    public JWT issueToken(Subject subject, String algorithm) {
      Principal p = (Principal) subject.getPrincipals().toArray()[0];
      return issueToken(p, algorithm);
    }

    @Override
    public JWT issueToken(Principal p, String algorithm) {
      return issueToken(p, null, algorithm);
    }

    @Override
    public JWT issueToken(Principal p, String audience, String algorithm) {
      return issueToken(p, audience, algorithm, -1);
    }

    @Override
    public boolean verifyToken(JWT token) {
      JWSVerifier verifier = new RSASSAVerifier(publicKey);
      return token.verify(verifier);
    }

    @Override
    public JWT issueToken(Principal p, String audience, String algorithm,
                               long expires) {
      ArrayList<String> audiences = null;
      if (audience != null) {
        audiences = new ArrayList<>();
        audiences.add(audience);
      }
      return issueToken(p, audiences, algorithm, expires);
    }

    @Override
    public JWT issueToken(Principal p, List<String> audiences, String algorithm, long expires,
                          String signingkeyName, String signingkeyAlias, char[] signingkeyPassphrase) {
      return issueToken(p, audiences, algorithm, expires);
    }

    @Override
    public JWT issueToken(Principal p, List<String> audiences, String algorithm, long expires) {
      String[] claimArray = new String[4];
      claimArray[0] = "KNOXSSO";
      claimArray[1] = p.getName();
      claimArray[2] = null;
      if (expires == -1) {
        claimArray[3] = null;
      } else {
        claimArray[3] = String.valueOf(expires);
      }

      JWT token = new JWTToken(algorithm, claimArray, audiences);
      JWSSigner signer = new RSASSASigner(privateKey);
      token.sign(signer);

      return token;
    }

    @Override
    public JWT issueToken(Principal p, String algorithm, long expiry) {
      return issueToken(p, Collections.emptyList(), algorithm, expiry);
    }

    @Override
    public boolean verifyToken(JWT token, RSAPublicKey publicKey) {
      JWSVerifier verifier = new RSASSAVerifier(publicKey);
      return token.verify(verifier);
    }
  }
}

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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.knox.gateway.config.GatewayConfig;
import org.apache.knox.gateway.servlet.SynchronousServletOutputStreamAdapter;
import org.apache.knox.test.TestUtils;
import org.apache.knox.test.category.FastTests;
import org.apache.knox.test.category.UnitTests;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category( { UnitTests.class, FastTests.class } )
public class DefaultDispatchTest {

  // Make sure Hadoop cluster topology isn't exposed to client when there is a connectivity issue.
  @Test
  public void testJiraKnox58() throws IOException, URISyntaxException {
    URI uri = new URI( "http://unreachable-host.invalid" );
    ClassicHttpRequest outboundRequest = EasyMock.createNiceMock( ClassicHttpRequest.class );
    EasyMock.expect( outboundRequest.getMethod() ).andReturn( "GET" ).anyTimes();
    EasyMock.expect( outboundRequest.getUri() ).andReturn( uri  ).anyTimes();

    HttpServletRequest inboundRequest = EasyMock.createNiceMock( HttpServletRequest.class );

    HttpServletResponse outboundResponse = EasyMock.createNiceMock( HttpServletResponse.class );
    EasyMock.expect( outboundResponse.getOutputStream() ).andAnswer( new IAnswer<SynchronousServletOutputStreamAdapter>() {
      @Override
      public SynchronousServletOutputStreamAdapter answer() {
        return new SynchronousServletOutputStreamAdapter() {
          @Override
          public void write( int b ) throws IOException {
            throw new IOException( "unreachable-host.invalid" );
          }
        };
      }
    });

    EasyMock.replay( outboundRequest, inboundRequest, outboundResponse );

    DefaultDispatch dispatch = new DefaultDispatch();
    HttpClientBuilder builder = HttpClientBuilder.create();
    CloseableHttpClient client = builder.build();
    dispatch.setHttpClient(client);
    try {
      dispatch.executeRequest( outboundRequest, inboundRequest, outboundResponse );
      fail( "Should have thrown IOException" );
    } catch( IOException e ) {
      assertThat( e.getMessage(), not( containsString( "unreachable-host.invalid" ) ) );
      assertThat( e, not( instanceOf( UnknownHostException.class ) ) ) ;
      assertThat( "Message needs meaningful content.", e.getMessage().trim().length(), greaterThan( 12 ) );
    }
  }

  @Test
  public void testCallToSecureClusterWithDelegationToken() throws URISyntaxException, IOException {
    DefaultDispatch defaultDispatch = new DefaultDispatch();
    ServletContext servletContext = EasyMock.createNiceMock( ServletContext.class );
    GatewayConfig gatewayConfig = EasyMock.createNiceMock( GatewayConfig.class );
    EasyMock.expect(gatewayConfig.isHadoopKerberosSecured()).andReturn(true).anyTimes();
    EasyMock.expect( servletContext.getAttribute( GatewayConfig.GATEWAY_CONFIG_ATTRIBUTE ) ).andReturn( gatewayConfig ).anyTimes();
    ServletInputStream inputStream = EasyMock.createNiceMock( ServletInputStream.class );
    HttpServletRequest inboundRequest = EasyMock.createNiceMock( HttpServletRequest.class );
    EasyMock.expect(inboundRequest.getQueryString()).andReturn( "delegation=123").anyTimes();
    EasyMock.expect(inboundRequest.getInputStream()).andReturn( inputStream).anyTimes();
    EasyMock.expect(inboundRequest.getServletContext()).andReturn( servletContext ).anyTimes();
    EasyMock.replay( gatewayConfig, servletContext, inboundRequest );
    HttpEntity httpEntity = defaultDispatch.createRequestEntity(inboundRequest);
    assertFalse("buffering in the presence of delegation token",
        (httpEntity instanceof PartiallyRepeatableHttpEntity));
  }

  @Test
  public void testCallToNonSecureClusterWithoutDelegationToken() throws URISyntaxException, IOException {
    DefaultDispatch defaultDispatch = new DefaultDispatch();
    ServletContext servletContext = EasyMock.createNiceMock( ServletContext.class );
    GatewayConfig gatewayConfig = EasyMock.createNiceMock( GatewayConfig.class );
    EasyMock.expect(gatewayConfig.isHadoopKerberosSecured()).andReturn(false).anyTimes();
    EasyMock.expect( servletContext.getAttribute( GatewayConfig.GATEWAY_CONFIG_ATTRIBUTE ) ).andReturn( gatewayConfig ).anyTimes();
    ServletInputStream inputStream = EasyMock.createNiceMock( ServletInputStream.class );
    HttpServletRequest inboundRequest = EasyMock.createNiceMock( HttpServletRequest.class );
    EasyMock.expect(inboundRequest.getInputStream()).andReturn( inputStream).anyTimes();
    EasyMock.expect(inboundRequest.getQueryString()).andReturn( "a=123").anyTimes();
    EasyMock.expect(inboundRequest.getServletContext()).andReturn( servletContext ).anyTimes();
    EasyMock.replay( gatewayConfig, servletContext, inboundRequest );
    HttpEntity httpEntity = defaultDispatch.createRequestEntity(inboundRequest);
    assertFalse("buffering in non secure cluster",
        (httpEntity instanceof PartiallyRepeatableHttpEntity));
  }

  @Test
  public void testCallToSecureClusterWithoutDelegationToken() throws URISyntaxException, IOException {
    DefaultDispatch defaultDispatch = new DefaultDispatch();
    defaultDispatch.setReplayBufferSize(10);
    ServletContext servletContext = EasyMock.createNiceMock( ServletContext.class );
    GatewayConfig gatewayConfig = EasyMock.createNiceMock( GatewayConfig.class );
    EasyMock.expect(gatewayConfig.isHadoopKerberosSecured()).andReturn( Boolean.TRUE ).anyTimes();
    EasyMock.expect( servletContext.getAttribute( GatewayConfig.GATEWAY_CONFIG_ATTRIBUTE ) ).andReturn( gatewayConfig ).anyTimes();
    ServletInputStream inputStream = EasyMock.createNiceMock( ServletInputStream.class );
    HttpServletRequest inboundRequest = EasyMock.createNiceMock( HttpServletRequest.class );
    EasyMock.expect(inboundRequest.getQueryString()).andReturn( "a=123").anyTimes();
    EasyMock.expect(inboundRequest.getInputStream()).andReturn( inputStream).anyTimes();
    EasyMock.expect(inboundRequest.getServletContext()).andReturn( servletContext ).anyTimes();
    EasyMock.replay( gatewayConfig, servletContext, inboundRequest );
    HttpEntity httpEntity = defaultDispatch.createRequestEntity(inboundRequest);
    assertTrue("not buffering in the absence of delegation token",
        (httpEntity instanceof PartiallyRepeatableHttpEntity));
  }

  @Test
  public void testUsingDefaultBufferSize() throws URISyntaxException, IOException {
    DefaultDispatch defaultDispatch = new DefaultDispatch();
    ServletContext servletContext = EasyMock.createNiceMock( ServletContext.class );
    GatewayConfig gatewayConfig = EasyMock.createNiceMock( GatewayConfig.class );
    EasyMock.expect(gatewayConfig.isHadoopKerberosSecured()).andReturn( Boolean.TRUE ).anyTimes();
    EasyMock.expect(gatewayConfig.getHttpServerRequestBuffer()).andReturn( 16384 ).anyTimes();
    EasyMock.expect( servletContext.getAttribute( GatewayConfig.GATEWAY_CONFIG_ATTRIBUTE ) ).andReturn( gatewayConfig ).anyTimes();
    ServletInputStream inputStream = EasyMock.createNiceMock( ServletInputStream.class );
    HttpServletRequest inboundRequest = EasyMock.createNiceMock( HttpServletRequest.class );
    EasyMock.expect(inboundRequest.getQueryString()).andReturn( "a=123").anyTimes();
    EasyMock.expect(inboundRequest.getInputStream()).andReturn( inputStream).anyTimes();
    EasyMock.expect(inboundRequest.getServletContext()).andReturn( servletContext ).anyTimes();
    EasyMock.replay( gatewayConfig, servletContext, inboundRequest );
    HttpEntity httpEntity = defaultDispatch.createRequestEntity(inboundRequest);
    assertTrue("not buffering in the absence of delegation token",
        (httpEntity instanceof PartiallyRepeatableHttpEntity));
    assertEquals(defaultDispatch.getReplayBufferSize(), 16);
    assertEquals(defaultDispatch.getReplayBufferSizeInBytes(), 16384);

    //also test normal setter and getters
    defaultDispatch.setReplayBufferSize(-1);
    assertEquals(defaultDispatch.getReplayBufferSizeInBytes(), -1);
    assertEquals(defaultDispatch.getReplayBufferSize(), -1);

    defaultDispatch.setReplayBufferSize(16);
    assertEquals(defaultDispatch.getReplayBufferSizeInBytes(), 16384);
    assertEquals(defaultDispatch.getReplayBufferSize(), 16);

  }

  @Test( timeout = TestUtils.SHORT_TIMEOUT )
  public void testGetDispatchUrl() throws Exception {
    HttpServletRequest request;
    Dispatch dispatch;
    String path;
    String query;
    URI uri;

    dispatch = new DefaultDispatch();

    path = "http://test-host:42/test-path";
    request = EasyMock.createNiceMock( HttpServletRequest.class );
    EasyMock.expect( request.getRequestURI() ).andReturn( path ).anyTimes();
    EasyMock.expect( request.getRequestURL() ).andReturn( new StringBuffer( path ) ).anyTimes();
    EasyMock.expect( request.getQueryString() ).andReturn( null ).anyTimes();
    EasyMock.replay( request );
    uri = dispatch.getDispatchUrl( request );
    assertThat( uri.toASCIIString(), is( "http://test-host:42/test-path" ) );

    path = "http://test-host:42/test,path";
    request = EasyMock.createNiceMock( HttpServletRequest.class );
    EasyMock.expect( request.getRequestURI() ).andReturn( path ).anyTimes();
    EasyMock.expect( request.getRequestURL() ).andReturn( new StringBuffer( path ) ).anyTimes();
    EasyMock.expect( request.getQueryString() ).andReturn( null ).anyTimes();
    EasyMock.replay( request );
    uri = dispatch.getDispatchUrl( request );
    assertThat( uri.toASCIIString(), is( "http://test-host:42/test,path" ) );

    path = "http://test-host:42/test%2Cpath";
    request = EasyMock.createNiceMock( HttpServletRequest.class );
    EasyMock.expect( request.getRequestURI() ).andReturn( path ).anyTimes();
    EasyMock.expect( request.getRequestURL() ).andReturn( new StringBuffer( path ) ).anyTimes();
    EasyMock.expect( request.getQueryString() ).andReturn( null ).anyTimes();
    EasyMock.replay( request );
    uri = dispatch.getDispatchUrl( request );
    assertThat( uri.toASCIIString(), is( "http://test-host:42/test%2Cpath" ) );

    path = "http://test-host:42/test%2Cpath";
    query = "test%26name=test%3Dvalue";
    request = EasyMock.createNiceMock( HttpServletRequest.class );
    EasyMock.expect( request.getRequestURI() ).andReturn( path ).anyTimes();
    EasyMock.expect( request.getRequestURL() ).andReturn( new StringBuffer( path ) ).anyTimes();
    EasyMock.expect( request.getQueryString() ).andReturn( query ).anyTimes();
    EasyMock.replay( request );
    uri = dispatch.getDispatchUrl( request );
    assertThat( uri.toASCIIString(), is( "http://test-host:42/test%2Cpath?test%26name=test%3Dvalue" ) );
  }
}

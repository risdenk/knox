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
package org.apache.knox.gateway.identityasserter.function;

import org.apache.knox.gateway.filter.AbstractGatewayFilter;
import org.apache.knox.gateway.filter.rewrite.api.UrlRewriteServletContextListener;
import org.apache.knox.gateway.filter.rewrite.api.UrlRewriteServletFilter;
import org.apache.knox.gateway.filter.rewrite.spi.UrlRewriteFunctionProcessor;
import org.apache.knox.gateway.identityasserter.common.function.UsernameFunctionProcessor;
import org.apache.knox.gateway.security.PrimaryPrincipal;
import org.apache.knox.gateway.util.urltemplate.Parser;
import org.apache.knox.test.TestUtils;
import org.apache.knox.test.log.NoOpLogger;
import org.apache.knox.test.mock.MockInteraction;
import org.apache.knox.test.mock.MockServlet;
import org.apache.http.auth.BasicUserPrincipal;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.servlet.ServletTester;
import org.eclipse.jetty.util.log.Log;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.security.auth.Subject;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.ServiceLoader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.fail;

class UsernameFunctionProcessorTest {
  private ServletTester server;
  private HttpTester.Request request;
  private HttpTester.Response response;
  private Queue<MockInteraction> interactions;
  private MockInteraction interaction;

  private static URL getTestResource( String name ) {
    name = UsernameFunctionProcessorTest.class.getName().replaceAll( "\\.", "/" ) + "/" + name;
    return ClassLoader.getSystemResource( name );
  }

  private void testSetup(String username, Map<String,String> initParams ) throws Exception {
    String descriptorUrl = getTestResource( "rewrite.xml" ).toExternalForm();

    Log.setLog( new NoOpLogger() );

    server = new ServletTester();
    server.setContextPath( "/" );
    server.getContext().addEventListener( new UrlRewriteServletContextListener() );
    server.getContext().setInitParameter(
        UrlRewriteServletContextListener.DESCRIPTOR_LOCATION_INIT_PARAM_NAME, descriptorUrl );

    FilterHolder setupFilter = server.addFilter( SetupFilter.class, "/*", EnumSet.of( DispatcherType.REQUEST ) );
    setupFilter.setFilter( new SetupFilter( username ) );
    FilterHolder rewriteFilter = server.addFilter( UrlRewriteServletFilter.class, "/*", EnumSet.of( DispatcherType.REQUEST ) );
    if( initParams != null ) {
      for( Map.Entry<String,String> entry : initParams.entrySet() ) {
        rewriteFilter.setInitParameter( entry.getKey(), entry.getValue() );
      }
    }
    rewriteFilter.setFilter( new UrlRewriteServletFilter() );

    interactions = new ArrayDeque<>();

    ServletHolder servlet = server.addServlet( MockServlet.class, "/" );
    servlet.setServlet( new MockServlet( "mock-servlet", interactions ) );

    server.start();

    interaction = new MockInteraction();
    request = HttpTester.newRequest();
    response = null;
  }

  @AfterEach
  void tearDown() throws Exception {
    if( server != null ) {
      server.stop();
    }
  }

  @Test
  void testInitialize() throws Exception {
    UsernameFunctionProcessor processor = new UsernameFunctionProcessor();
    // Shouldn't fail.
    processor.initialize( null, null );
  }

  @Test
  void testDestroy() throws Exception {
    UsernameFunctionProcessor processor = new UsernameFunctionProcessor();
    // Shouldn't fail.
    processor.destroy();
  }

  @Test
  void testResolve() throws Exception {
    final UsernameFunctionProcessor processor = new UsernameFunctionProcessor();
    assertThat( processor.resolve( null, null ), nullValue() );
    assertThat( processor.resolve( null, Collections.singletonList("test-input")), contains( "test-input" ) );
    Subject subject = new Subject();
    subject.getPrincipals().add( new PrimaryPrincipal( "test-username" ) );
    subject.setReadOnly();
    Subject.doAs( subject, new PrivilegedExceptionAction<Object>() {
      @Override
      public Object run() throws Exception {
        assertThat( processor.resolve( null, null ), contains( "test-username" ) );
        assertThat( processor.resolve( null, Collections.singletonList("test-ignored")), contains( "test-username" ) );
        return null;
      }
    } );
  }

  @Test
  void testServiceLoader() {
    ServiceLoader loader = ServiceLoader.load( UrlRewriteFunctionProcessor.class );
    for (Object object : loader) {
      if (object instanceof UsernameFunctionProcessor) {
        return;
      }
    }
    fail( "Failed to find UsernameFunctionProcessor via service loader." );
  }

  @Test
  void testRequestUrlRewriteOfUsernameViaRewriteRule() throws Exception {
    Map<String,String> initParams = new HashMap<>();
    initParams.put( "request.url", "test-rule-username" );
    testSetup( "test-user", initParams );

    String input = "<root/>";
    String expect = "<root/>";

    // Setup the server side request/response interaction.
    interaction.expect()
        .method( "PUT" )
        .requestUrl( "test-output-scheme://test-input-host:777/test-output-path/test-input-path" )
        .queryParam( "user.name", "test-user" )
        .queryParam( "test-query-input-name", "test-query-input-value" )
        .queryParam( "test-query-output-name", "test-query-output-value" )
        .contentType( "text/xml" )
        .content( expect, StandardCharsets.UTF_8 );
    interaction.respond().status( 200 );
    interactions.add( interaction );
    request.setMethod( "PUT" );
    request.setURI( "/test-input-path?test-query-input-name=test-query-input-value" );
    //request.setVersion( "HTTP/1.1" );
    request.setHeader( "Host", "test-input-host:777" );
    request.setHeader( "Content-Type", "text/xml; charset=UTF-8" );
    request.setContent( input );

    response = TestUtils.execute( server, request );

    // Test the results.
    assertThat( response.getStatus(), Is.is( 200 ) );
  }

  private static class SetupFilter implements Filter {
    private Subject subject;

    SetupFilter( String userName ) {
      subject = new Subject();
      subject.getPrincipals().add( new BasicUserPrincipal( userName ) );
    }

    @Override
    public void init( FilterConfig filterConfig ) {
    }

    @Override
    public void doFilter( final ServletRequest request, final ServletResponse response, final FilterChain chain ) throws ServletException {
      HttpServletRequest httpRequest = ((HttpServletRequest)request);
      StringBuffer sourceUrl = httpRequest.getRequestURL();
      String queryString = httpRequest.getQueryString();
      if( queryString != null ) {
        sourceUrl.append( '?' );
        sourceUrl.append( queryString );
      }
      try {
        request.setAttribute(
            AbstractGatewayFilter.SOURCE_REQUEST_URL_ATTRIBUTE_NAME,
            Parser.parseLiteral( sourceUrl.toString() ) );
      } catch( URISyntaxException e ) {
        throw new ServletException( e );
      }
      try {
        Subject.doAs( subject, new PrivilegedExceptionAction<Void>() {
          @Override
          public Void run() throws Exception {
              chain.doFilter( request, response );
              return null;
          }
        } );
      } catch( PrivilegedActionException e ) {
        throw new ServletException( e );
      }
    }

    @Override
    public void destroy() {
    }
  }
}

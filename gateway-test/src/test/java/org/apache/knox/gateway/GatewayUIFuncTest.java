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
package org.apache.knox.gateway;

import java.nio.charset.StandardCharsets;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.apache.knox.test.TestUtils.LOG_ENTER;
import static org.apache.knox.test.TestUtils.LOG_EXIT;

@Tag("verify")
class GatewayUIFuncTest {
  private static GatewayTestDriver driver = new GatewayTestDriver();

  // Controls the host name to which the gateway dispatch requests.  This may be the name of a sandbox VM
  // or an EC2 instance.  Currently only a single host is supported.
  private static final String TEST_HOST = "vm.local";

  // Specifies if the test requests should go through the gateway or directly to the services.
  // This is frequently used to verify the behavior of the test both with and without the gateway.
  private static final boolean USE_GATEWAY = true;

  // Specifies if the test requests should be sent to mock services or the real services.
  // This is frequently used to verify the behavior of the test both with and without mock services.
  private static final boolean USE_MOCK_SERVICES = true;

  // Specifies if the GATEWAY_HOME created for the test should be deleted when the test suite is complete.
  // This is frequently used during debugging to keep the GATEWAY_HOME around for inspection.
  private static final boolean CLEANUP_TEST = true;

  /**
   * Creates a deployment of a gateway instance that all test methods will share.  This method also creates a
   * registry of sorts for all of the services that will be used by the test methods.
   * The createTopology method is used to create the topology file that would normally be read from disk.
   * The driver.setupGateway invocation is where the creation of GATEWAY_HOME occurs.
   * @throws Exception Thrown if any failure occurs.
   */
  @BeforeAll
  static void setUpBeforeClass() throws Exception {
    LOG_ENTER();
    GatewayTestConfig config = new GatewayTestConfig();
    driver.setResourceBase(GatewayUIFuncTest.class);
    driver.setupLdap(0);
    driver.setupService( "OOZIEUI", "http://" + TEST_HOST + ":11000/oozie", "/cluster/oozie", USE_MOCK_SERVICES );
    driver.setupGateway( config, "cluster", createTopology(), USE_GATEWAY );
    LOG_EXIT();
  }

  @AfterAll
  static void tearDownAfterClass() throws Exception {
    LOG_ENTER();
    if( CLEANUP_TEST ) {
      driver.cleanup();
    }
    LOG_EXIT();
  }

  @AfterEach
  void cleanupTest() {
    driver.reset();
  }

  /**
   * Creates a topology that is deployed to the gateway instance for the test suite.
   * Note that this topology is shared by all of the test methods in this suite.
   * @return A populated XML structure for a topology file.
   */
  private static XMLTag createTopology() {
    return XMLDoc.newDocument( true )
        .addRoot( "topology" )
          .addTag( "gateway" )
            .addTag( "provider" )
              .addTag( "role" ).addText( "webappsec" )
              .addTag("name").addText("WebAppSec")
              .addTag("enabled").addText("true")
              .addTag( "param" )
                .addTag("name").addText("csrf.enabled")
                .addTag("value").addText("true").gotoParent().gotoParent()
            .addTag("provider")
              .addTag("role").addText("authentication")
              .addTag("name").addText("ShiroProvider")
              .addTag("enabled").addText("true")
              .addTag( "param" )
                .addTag("name").addText("main.ldapRealm")
                .addTag("value").addText("org.apache.knox.gateway.shirorealm.KnoxLdapRealm").gotoParent()
              .addTag( "param" )
                .addTag( "name" ).addText( "main.ldapRealm.userDnTemplate" )
                .addTag( "value" ).addText( "uid={0},ou=people,dc=hadoop,dc=apache,dc=org" ).gotoParent()
              .addTag( "param" )
                .addTag( "name" ).addText( "main.ldapRealm.contextFactory.url" )
                .addTag( "value" ).addText( driver.getLdapUrl() ).gotoParent()
              .addTag( "param" )
                .addTag( "name" ).addText( "main.ldapRealm.contextFactory.authenticationMechanism" )
                .addTag( "value" ).addText( "simple" ).gotoParent()
              .addTag( "param" )
                .addTag( "name" ).addText( "urls./**" )
                .addTag( "value" ).addText( "authcBasic" ).gotoParent().gotoParent()
            .addTag("provider")
              .addTag("role").addText("identity-assertion")
              .addTag("enabled").addText("true")
              .addTag("name").addText("Default").gotoParent()
          .gotoRoot()
          .addTag( "service" )
            .addTag( "role" ).addText( "OOZIEUI" )
            .addTag( "url" ).addText( driver.getRealUrl( "OOZIEUI" ) ).gotoParent();
  }

  @Test
  void testOozieUIRoutesAndRewriteRules() {
    LOG_ENTER();
    String username = "guest";
    String password = "guest-password";
    String path;

    path = "/oozie-console.css";
    driver.getMock("OOZIEUI")
        .expect().method("GET")
        .pathInfo(path)
        .queryParam( "user.name", username )
        .respond()
        .status(HttpStatus.SC_OK)
        .contentType("application/json")
        .characterEncoding(StandardCharsets.UTF_8.name());
    given()
        .auth().preemptive().basic( username, password )
        .header( "X-XSRF-Header", "test-xsrf-header-value" )
        .then()
        .statusCode( 200 )
        .when().get( driver.getUrl( "OOZIEUI" ) + path + ( driver.isUseGateway() ? "" : "?user.name=" + username ) ).asString();

    path = "/ext-2.2/resources/css/ext-all.css";
    driver.getMock("OOZIEUI")
        .expect().method("GET")
        .pathInfo(path)
        .queryParam( "user.name", username )
        .respond()
        .status(HttpStatus.SC_OK)
        .contentType("application/json")
        .characterEncoding(StandardCharsets.UTF_8.name());
    given()
        .auth().preemptive().basic( username, password )
        .header( "X-XSRF-Header", "test-xsrf-header-value" )
        .then()
        .statusCode( 200 )
        .when().get( driver.getUrl( "OOZIEUI" ) + path + ( driver.isUseGateway() ? "" : "?user.name=" + username ) ).asString();

    path = "/ext-2.2/adapter/ext/ext-base.js";
    driver.getMock("OOZIEUI")
        .expect().method("GET")
        .pathInfo(path)
        .queryParam( "user.name", username )
        .respond()
        .status(HttpStatus.SC_OK)
        .contentType("application/json")
        .characterEncoding(StandardCharsets.UTF_8.name());
    given()
        .auth().preemptive().basic( username, password )
        .header( "X-XSRF-Header", "test-xsrf-header-value" )
        .then()
        .statusCode( 200 )
        .when().get( driver.getUrl( "OOZIEUI" ) + path + ( driver.isUseGateway() ? "" : "?user.name=" + username ) ).asString();

    path = "/console/sla/js/table/jquery-1.8.3.min.js";
    driver.getMock("OOZIEUI")
        .expect().method("GET")
        .pathInfo(path)
        .queryParam( "user.name", username )
        .respond()
        .status(HttpStatus.SC_OK)
        .contentType("application/json")
        .characterEncoding(StandardCharsets.UTF_8.name());
    given()
        .auth().preemptive().basic( username, password )
        .header( "X-XSRF-Header", "test-xsrf-header-value" )
        .then()
        .statusCode( 200 )
        .when().get( driver.getUrl( "OOZIEUI" ) + path + ( driver.isUseGateway() ? "" : "?user.name=" + username ) ).asString();

    path = "/console/sla/css/jquery.dataTables.css";
    driver.getMock("OOZIEUI")
        .expect().method("GET")
        .pathInfo(path)
        .queryParam( "user.name", username )
        .respond()
        .status(HttpStatus.SC_OK)
        .contentType("application/json")
        .characterEncoding(StandardCharsets.UTF_8.name());
    given()
        .auth().preemptive().basic( username, password )
        .header( "X-XSRF-Header", "test-xsrf-header-value" )
        .then()
        .statusCode( 200 )
        .when().get( driver.getUrl( "OOZIEUI" ) + path + ( driver.isUseGateway() ? "" : "?user.name=" + username ) ).asString();

    path = "/console/sla/js/oozie-sla.js";
    driver.getMock("OOZIEUI")
        .expect().method("GET")
        .pathInfo(path)
        .queryParam( "user.name", username )
        .respond()
        .status(HttpStatus.SC_OK)
        .contentType("application/json")
        .characterEncoding(StandardCharsets.UTF_8.name());
    given()
        .auth().preemptive().basic( username, password )
        .header( "X-XSRF-Header", "test-xsrf-header-value" )
        .then()
        .statusCode( 200 )
        .when().get( driver.getUrl( "OOZIEUI" ) + path + ( driver.isUseGateway() ? "" : "?user.name=" + username ) ).asString();

    path = "/console/sla/js/graph/jquery.flot.min.js";
    driver.getMock("OOZIEUI")
        .expect().method("GET")
        .pathInfo(path)
        .queryParam( "user.name", username )
        .respond()
        .status(HttpStatus.SC_OK)
        .contentType("application/json")
        .characterEncoding(StandardCharsets.UTF_8.name());
    given()
        .auth().preemptive().basic( username, password )
        .header( "X-XSRF-Header", "test-xsrf-header-value" )
        .then()
        .statusCode( 200 )
        .when().get( driver.getUrl( "OOZIEUI" ) + path + ( driver.isUseGateway() ? "" : "?user.name=" + username ) ).asString();

    driver.getMock("OOZIEUI")
        .expect().method("GET")
        .pathInfo("/")
        .queryParam( "user.name", username )
        .respond()
        .status(HttpStatus.SC_OK)
        .contentType("application/json")
        .characterEncoding(StandardCharsets.UTF_8.name());
    given()
        .auth().preemptive().basic( username, password )
        .header( "X-XSRF-Header", "test-xsrf-header-value" )
        .then()
        .statusCode( 200 )
        .when().get( driver.getUrl( "OOZIEUI" ) + ( driver.isUseGateway() ? "" : "?user.name=" + username ) ).asString();

    driver.assertComplete();
    LOG_EXIT();
  }
}

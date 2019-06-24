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
package org.apache.knox.gateway.services.security;

import org.apache.knox.gateway.config.GatewayConfig;
import org.apache.knox.gateway.services.security.impl.ConfigurableEncryptor;
import org.apache.knox.gateway.services.security.impl.DefaultCryptoService;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CryptoServiceTest {
  private static CryptoService cs;
  private static AliasService as;

  @BeforeAll
  static void setupSuite() {
    as = new AliasService() {
      @Override
      public void init(GatewayConfig config, Map<String, String> options) {
      }

      @Override
      public void start() {
      }

      @Override
      public void stop() {
      }

      @Override
      public void addAliasForCluster(String clusterName, String alias,
          String value) {
      }

      @Override
      public char[] getPasswordFromAliasForCluster(String clusterName,
          String alias) {
        return "password".toCharArray();
      }

      @Override
      public char[] getPasswordFromAliasForCluster(String clusterName,
          String alias, boolean generate) {
        return null;
      }

      @Override
      public void generateAliasForCluster(String clusterName, String alias) {
      }

      @Override
      public char[] getPasswordFromAliasForGateway(String alias) {
        return null;
      }

      @Override
      public void generateAliasForGateway(String alias) {
      }

      @Override
      public Certificate getCertificateForGateway(String alias) {
        return null;
      }

      @Override
      public void removeAliasForCluster(String clusterName, String alias) {
      }

      @Override
      public List<String> getAliasesForCluster(String clusterName) {
        return null;
      }

      @Override
      public char[] getGatewayIdentityPassphrase() {
        return null;
      }

      @Override
      public char[] getGatewayIdentityKeystorePassword() throws AliasServiceException {
        return null;
      }

      @Override
      public char[] getSigningKeyPassphrase() throws AliasServiceException {
        return new char[0];
      }

      @Override
      public char[] getSigningKeystorePassword() throws AliasServiceException {
        return new char[0];
      }
    };
    cs = new DefaultCryptoService();
    ((DefaultCryptoService)cs).setAliasService(as);
  }

  @Test
  void testCryptoServiceAES() throws Exception {
    GatewayConfig config = EasyMock.createNiceMock( GatewayConfig.class );
    EasyMock.expect(config.getAlgorithm()).andReturn("AES");
    EasyMock.expect(config.getPBEAlgorithm()).andReturn("PBKDF2WithHmacSHA1");
    EasyMock.expect(config.getSaltSize()).andReturn("16");
    EasyMock.expect(config.getIterationCount()).andReturn("65536");
    EasyMock.expect(config.getKeyLength()).andReturn("128");
    EasyMock.expect(config.getTransformation()).andReturn("AES/CBC/PKCS5Padding");
    EasyMock.replay(config);

    // password to create key - same Encryptor
    String queryString = "url=http://localhost:50070/api/v1/blahblah";
    ConfigurableEncryptor aes0 = new ConfigurableEncryptor("password");
    aes0.init(config);
    cs.init(config, new HashMap<>());
    EncryptionResult result0 = cs.encryptForCluster("Test", "encrypt_url", queryString.getBytes(StandardCharsets.UTF_8));
    byte[] decrypted0 = cs.decryptForCluster("Test", "encrypt_url", result0.cipher, result0.iv, result0.salt);
    assertEquals(queryString, new String(decrypted0, StandardCharsets.UTF_8));
    assertEquals(queryString.getBytes(StandardCharsets.UTF_8).length, decrypted0.length);
    assertEquals(queryString.getBytes(StandardCharsets.UTF_8).length, new String(decrypted0, StandardCharsets.UTF_8).toCharArray().length);
  }

  @Test
  void testCryptoServiceDES() throws Exception {
    GatewayConfig config = EasyMock.createNiceMock( GatewayConfig.class );
    EasyMock.expect(config.getAlgorithm()).andReturn("DES");
    EasyMock.expect(config.getPBEAlgorithm()).andReturn("PBKDF2WithHmacSHA1");
    EasyMock.expect(config.getSaltSize()).andReturn("16");
    EasyMock.expect(config.getIterationCount()).andReturn("65536");
    EasyMock.expect(config.getKeyLength()).andReturn("128");
    EasyMock.expect(config.getTransformation()).andReturn("DES");
    EasyMock.replay(config);

    // password to create key - same Encryptor
    String queryString = "url=http://localhost:50070/api/v1/blahblah";
    ConfigurableEncryptor aes0 = new ConfigurableEncryptor("password");
    aes0.init(config);
    cs.init(config, new HashMap<>());
    EncryptionResult result0 = cs.encryptForCluster("Test", "encrypt_url", queryString.getBytes(StandardCharsets.UTF_8));
    byte[] decrypted0 = cs.decryptForCluster("Test", "encrypt_url", result0.cipher, result0.iv, result0.salt);
    assertEquals(queryString, new String(decrypted0, StandardCharsets.UTF_8));
    assertEquals(queryString.getBytes(StandardCharsets.UTF_8).length, decrypted0.length);
    assertEquals(queryString.getBytes(StandardCharsets.UTF_8).length, new String(decrypted0, StandardCharsets.UTF_8).toCharArray().length);
  }

  @Test
  void testConfigurableEncryptor() throws Exception {
    GatewayConfig config = EasyMock.createNiceMock( GatewayConfig.class );
    EasyMock.expect(config.getAlgorithm()).andReturn("AES");
    EasyMock.expect(config.getPBEAlgorithm()).andReturn("PBKDF2WithHmacSHA1");
    EasyMock.expect(config.getSaltSize()).andReturn("16");
    EasyMock.expect(config.getIterationCount()).andReturn("65536");
    EasyMock.expect(config.getKeyLength()).andReturn("128");
    EasyMock.expect(config.getTransformation()).andReturn("AES/CBC/PKCS5Padding");
    EasyMock.replay(config);

    // password to create key - same Encryptor
    ConfigurableEncryptor aes = new ConfigurableEncryptor("Test");
    aes.init(config);
    EncryptionResult result = aes.encrypt("larry".getBytes(StandardCharsets.UTF_8));
    byte[] decrypted = aes.decrypt(result.salt, result.iv, result.cipher);
    assertEquals(new String(decrypted, StandardCharsets.UTF_8), "larry");

    // password to create key - different Encryptor
    ConfigurableEncryptor aes2 = new ConfigurableEncryptor("Test");
    aes2.init(config);
    decrypted = aes2.decrypt(result.salt, result.iv, result.cipher);
    assertEquals(new String(decrypted, StandardCharsets.UTF_8), "larry");

    // password to create key resolved from alias - same Encryptor
    ConfigurableEncryptor aes3 = new ConfigurableEncryptor(new String(as.getPasswordFromAliasForCluster("test", "encrypt_url")));
    aes3.init(config);
    result = aes3.encrypt("larry".getBytes(StandardCharsets.UTF_8));
    decrypted = aes3.decrypt(result.salt, result.iv, result.cipher);
    assertEquals(new String(decrypted, StandardCharsets.UTF_8), "larry");

    // password to create key resolved from alias - different Encryptor
    ConfigurableEncryptor aes4 = new ConfigurableEncryptor(new String(as.getPasswordFromAliasForCluster("test", "encrypt_url")));
    aes4.init(config);
    decrypted = aes4.decrypt(result.salt, result.iv, result.cipher);
    assertEquals(new String(decrypted, StandardCharsets.UTF_8), "larry");
  }

  @Test
  void testEncryptionOfQueryStrings() {
    String alias = "encrypt-url";
    String queryString = "url=http://localhost:50070/api/v1/blahblah";

    EncryptionResult result = cs.encryptForCluster("Test", alias, queryString.getBytes(StandardCharsets.UTF_8));
    assertEquals(0, result.cipher.length % 16,
        "Resulted cipertext length should be a multiple of 16");
    byte[] decryptedQueryString = cs.decryptForCluster("Test", alias, result.cipher, result.iv, result.salt);
    assertEquals(queryString.getBytes(StandardCharsets.UTF_8).length, decryptedQueryString.length);
  }
}

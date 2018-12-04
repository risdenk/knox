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
package org.apache.knox.gateway.audit;

import org.apache.knox.test.log.CollectAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class StoreAndForwardAppenderTest {
  @BeforeEach
  void setUp() {
    tearDown();
  }

  @AfterEach
  void tearDown() {
    LogManager.shutdown();
    String absolutePath = "target/audit";
    File db = new File( absolutePath + ".db" );
    if( db.exists() ) {
      assertThat( "Failed to delete audit store db file.", db.delete(), is( true ) );
    }
    File lg = new File( absolutePath + ".lg" );
    if( lg.exists() ) {
      assertThat( "Failed to delete audit store lg file.", lg.delete(), is( true ) );
    }
    PropertyConfigurator.configure( ClassLoader.getSystemResourceAsStream( "audit-log4j.properties" ) );
  }

  @Test
  void testAppender() {
    Assertions.assertTimeout(Duration.ofMillis(500000), () -> {
      System.out.println("Running " + Thread.currentThread().getStackTrace()[1].getClassName() + "#" + Thread.currentThread().getStackTrace()[1].getMethodName());

      int iterations = 1000;
      Logger logger = Logger.getLogger("audit.store");
      for (int i = 1; i <= iterations; i++) {
        logger.info(Integer.toString(i));
      }
      while (CollectAppender.queue.size() < iterations) {
        Thread.sleep(20);
      }
      assertThat(CollectAppender.queue.size(), is(iterations));
    });
  }
}
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
package org.apache.knox.gateway.shell.hbase.table.scanner.stateless;

import java.util.concurrent.Callable;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.knox.gateway.shell.AbstractRequest;
import org.apache.knox.gateway.shell.EmptyResponse;
import org.apache.knox.gateway.shell.KnoxSession;
import org.apache.knox.gateway.shell.hbase.HBase;

public class CreateStatelessScanner {
  public static class Request extends AbstractRequest<Response> {
    private String tableName;
    private String writerString;

    public Request(KnoxSession session, String tableName, String writerString) {
      super(session);
      this.tableName = tableName;
      this.writerString = writerString;
    }

    @Override
    protected Callable<Response> callable() {
      return new Callable<Response>() {
        @Override
        public Response call() throws Exception {
          URIBuilder uri = uri(HBase.SERVICE_PATH, "/", tableName, "/scanner");
          HttpPut request = new HttpPut(uri.build());
          HttpEntity entity = new StringEntity(writerString, ContentType.create("text/xml", "UTF-8"));
          request.setEntity(entity);

          return new Response(execute(request));
        }
      };
    }
  }

  public static class Response extends EmptyResponse {
    Response(HttpResponse response) {
      super(response);
    }

    public String getScannerId() {
      Header locationHeader = response().getFirstHeader("Location");
      if (locationHeader != null && locationHeader.getValue() != null && !locationHeader.getValue().isEmpty()) {
        String location = locationHeader.getValue();
        int position = location.lastIndexOf('/');
        if (position != -1) {
          return location.substring(position + 1);
        }
      }
      return null;
    }
  }
}

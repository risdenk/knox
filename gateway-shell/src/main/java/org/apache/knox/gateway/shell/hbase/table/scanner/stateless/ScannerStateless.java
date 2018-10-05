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

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.apache.commons.codec.binary.Base64;
import org.apache.knox.gateway.shell.HadoopException;
import org.apache.knox.gateway.shell.KnoxSession;
import org.apache.knox.gateway.shell.hbase.HBase;
import org.apache.knox.gateway.shell.hbase.table.row.Column;
import org.apache.knox.gateway.shell.hbase.table.scanner.Scanner;
import org.apache.knox.gateway.util.XmlUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ScannerStateless extends Scanner {
  private static final String ELEMENT_SCANNER = "Scanner";
  private static final String ELEMENT_COLUMN = "column";
  private static final String ELEMENT_FILTER = "filter";
  private static final String ATTRIBUTE_START_ROW = "startRow";
  private static final String ATTRIBUTE_END_ROW = "endRow";
  private static final String ATTRIBUTE_BATCH = "batch";
  private static final String ATTRIBUTE_START_TIME = "startTime";
  private static final String ATTRIBUTE_END_TIME = "endTime";
  private static final String ATTRIBUTE_MAX_VERSIONS = "maxVersions";

  private KnoxSession session;

  private String tableName;
  private String startRow;
  private String endRow;
  private List<Column> columns = new ArrayList<Column>();
  private Integer batch;
  private Long startTime;
  private Long endTime;
  private String filter;
  private Integer maxVersions;

  private String writerString;


  public ScannerStateless(KnoxSession session, String tableName) {
    this.session = session;
    this.tableName = tableName;
  }

  public ScannerStateless rows(String startRow, String endRow) {
    this.startRow = startRow;
    this.endRow = endRow;
    return this;
  }

  public ScannerStateless startRow(String startRow) {
    this.startRow = startRow;
    return this;
  }

  public ScannerStateless endRow(String endRow) {
    this.endRow = endRow;
    return this;
  }

  public ScannerStateless column(String family, String qualifier) {
    Column column = new Column(family, qualifier);
    columns.add(column);
    return this;
  }

  public ScannerStateless column(String family) {
    //return column( family, null );
    Column column = new Column(family, "");
    columns.add(column);
    return this;
  }

  public ScannerStateless batch(Integer batch) {
    this.batch = batch;
    return this;
  }

  public ScannerStateless times(Long startTime, Long endTime) {
    this.startTime = startTime;
    this.endTime = endTime;
    return this;
  }

  public ScannerStateless startTime(Long startTime) {
    this.startTime = startTime;
    return this;
  }

  public ScannerStateless endTime(Long endTime) {
    this.endTime = endTime;
    return this;
  }

  public ScannerStateless filter(String filter) {
    this.filter = filter;
    return this;
  }

  public ScannerStateless maxVersions(Integer maxVersions) {
    this.maxVersions = maxVersions;
    return this;
  }

  public String get() throws Exception {
    try {
      this.writerString = this.getScannerWriterString();
    } catch (DOMException | UnsupportedEncodingException
                 | ParserConfigurationException | TransformerException e) {
      e.printStackTrace();
    }

    String tempScannerId = new CreateStatelessScanner.Request(this.session, this.tableName, this.writerString).callable().call().getScannerId();

    return getStatelessScannerData(session, tableName, tempScannerId);
  }

  private String getScannerWriterString() throws DOMException, ParserConfigurationException, UnsupportedEncodingException, TransformerException {
    Document document = XmlUtils.createDocument();

    Element root = document.createElement(ELEMENT_SCANNER);
    if (startRow != null) {
      root.setAttribute(ATTRIBUTE_START_ROW, Base64.encodeBase64String(startRow.getBytes(StandardCharsets.UTF_8)));
    }
    if (endRow != null) {
      root.setAttribute(ATTRIBUTE_END_ROW, Base64.encodeBase64String(endRow.getBytes(StandardCharsets.UTF_8)));
    }
    if (batch != null) {
      root.setAttribute(ATTRIBUTE_BATCH, batch.toString());
    }
    if (startTime != null) {
      root.setAttribute(ATTRIBUTE_START_TIME, startTime.toString());
    }
    if (endTime != null) {
      root.setAttribute(ATTRIBUTE_END_TIME, endTime.toString());
    }
    if (maxVersions != null) {
      root.setAttribute(ATTRIBUTE_MAX_VERSIONS, maxVersions.toString());
    }
    document.appendChild(root);

    for (Column column : columns) {
      Element columnElement = document.createElement(ELEMENT_COLUMN);
      columnElement.setTextContent(Base64.encodeBase64String(column.toURIPart().getBytes(StandardCharsets.UTF_8)));
      root.appendChild(columnElement);
    }

    if (filter != null && !filter.isEmpty()) {
      Element filterElement = document.createElement(ELEMENT_FILTER);
      filterElement.setTextContent(filter);
      root.appendChild(filterElement);
    }

    StringWriter writer = new StringWriter();
    Transformer t = XmlUtils.getTransformer(true, false, 0, false);
    XmlUtils.writeXml(document, writer, t);

    return writer.toString();
  }


  private String getStatelessScannerData(KnoxSession session, String tableName, String scannerId) {
    StringBuilder sb = new StringBuilder();
    String tempValue = "";

    try {
      while (!(tempValue = HBase.session(session).table(tableName).scanner(scannerId).getNext().now().getString()).isEmpty()) {

        if (sb.toString().isEmpty()) {
          sb.append(tempValue);
        } else {
          sb.append(',').append(tempValue);
        }
      }
    } catch (HadoopException | IOException e) {
      e.printStackTrace();
    }

    return sb.toString().replaceAll("}]}]},\\{\"Row\":\\[\\{", "}]},\\{");
  }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.knox.gateway.rm.dispatch;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.knox.gateway.ha.provider.HaDescriptor;
import org.apache.knox.gateway.ha.provider.HaProvider;
import org.apache.knox.gateway.ha.provider.HaServletContextListener;
import org.apache.knox.gateway.ha.provider.impl.DefaultHaProvider;
import org.apache.knox.gateway.ha.provider.impl.HaDescriptorFactory;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.WriteListener;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RMHaDispatchTest {
    private static final String LOCATION = "Location";

    @Test
    public void testConnectivityFailure() throws Exception {
        String serviceName = "RESOURCEMANAGER";
        HaDescriptor descriptor = HaDescriptorFactory.createDescriptor();
        descriptor.addServiceConfig(HaDescriptorFactory.createServiceConfig(serviceName, "true", "1", "1000", "2", "1000", null, null));
        HaProvider provider = new DefaultHaProvider(descriptor);
        URI uri1 = new URI("http://unreachable-host.invalid");
        URI uri2 = new URI("http://reachable-host.invalid");
        ArrayList<String> urlList = new ArrayList<>();
        urlList.add(uri1.toString());
        urlList.add(uri2.toString());
        provider.addHaService(serviceName, urlList);
        FilterConfig filterConfig = EasyMock.createNiceMock(FilterConfig.class);
        ServletContext servletContext = EasyMock.createNiceMock(ServletContext.class);

        EasyMock.expect(filterConfig.getServletContext()).andReturn(servletContext).anyTimes();
        EasyMock.expect(servletContext.getAttribute(HaServletContextListener.PROVIDER_ATTRIBUTE_NAME)).andReturn(provider).anyTimes();

        ClassicHttpRequest outboundRequest = EasyMock.createNiceMock(ClassicHttpRequest.class);
        EasyMock.expect(outboundRequest.getMethod()).andReturn("GET").anyTimes();
        EasyMock.expect(outboundRequest.getUri()).andReturn(uri1).anyTimes();

        HttpServletRequest inboundRequest = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.expect(inboundRequest.getRequestURL()).andReturn(new StringBuffer(uri2.toString())).once();
        EasyMock.expect(inboundRequest.getAttribute("dispatch.ha.failover.counter")).andReturn(new AtomicInteger(0)).once();
        EasyMock.expect(inboundRequest.getAttribute("dispatch.ha.failover.counter")).andReturn(new AtomicInteger(1)).once();

        HttpServletResponse outboundResponse = EasyMock.createNiceMock(HttpServletResponse.class);
        EasyMock.expect(outboundResponse.getOutputStream()).andAnswer(new IAnswer<ServletOutputStream>() {
            @Override
            public ServletOutputStream answer() throws Throwable {
                return new ServletOutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        throw new IOException("unreachable-host.invalid");
                    }

                    @Override
                    public void setWriteListener(WriteListener arg0) {
                    }

                    @Override
                    public boolean isReady() {
                        return false;
                    }
                };
            }
        }).once();
        EasyMock.replay(filterConfig, servletContext, outboundRequest, inboundRequest, outboundResponse);
        Assert.assertEquals(uri1.toString(), provider.getActiveURL(serviceName));
        RMHaDispatch dispatch = new RMHaDispatch();
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        dispatch.setHttpClient(client);
        dispatch.setHaProvider(provider);
        dispatch.init();
        long startTime = System.currentTimeMillis();
        try {
            dispatch.executeRequest(outboundRequest, inboundRequest, outboundResponse);
        } catch (IOException e) {
            //this is expected after the failover limit is reached
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        Assert.assertEquals(uri2.toString(), provider.getActiveURL(serviceName));
        //test to make sure the sleep took place
        Assert.assertTrue(elapsedTime > 1000);
    }

    @Test
    public void testConnectivityFailover() throws Exception {
        String serviceName = "RESOURCEMANAGER";
        HaDescriptor descriptor = HaDescriptorFactory.createDescriptor();
        descriptor.addServiceConfig(HaDescriptorFactory.createServiceConfig(serviceName, "true", "1", "1000", "2", "1000", null, null));
        HaProvider provider = new DefaultHaProvider(descriptor);
        URI uri1 = new URI("http://passive-host.invalid");
        URI uri2 = new URI("http://other-host.invalid");
        URI uri3 = new URI("http://active-host.invalid");
        ArrayList<String> urlList = new ArrayList<>();
        urlList.add(uri1.toString());
        urlList.add(uri2.toString());
        urlList.add(uri3.toString());
        provider.addHaService(serviceName, urlList);
        FilterConfig filterConfig = EasyMock.createNiceMock(FilterConfig.class);
        ServletContext servletContext = EasyMock.createNiceMock(ServletContext.class);

        EasyMock.expect(filterConfig.getServletContext()).andReturn(servletContext).anyTimes();
        EasyMock.expect(servletContext.getAttribute(HaServletContextListener.PROVIDER_ATTRIBUTE_NAME)).andReturn(provider).anyTimes();


        BasicClassicHttpResponse inboundResponse = EasyMock.createNiceMock(BasicClassicHttpResponse.class);
        EasyMock.expect(inboundResponse.getEntity()).andReturn(getResponseEntity()).anyTimes();
        EasyMock.expect(inboundResponse.getFirstHeader(LOCATION)).andReturn(getFirstHeader(uri3.toString())).anyTimes();


        ClassicHttpRequest outboundRequest = EasyMock.createNiceMock(ClassicHttpRequest.class);
        EasyMock.expect(outboundRequest.getMethod()).andReturn("GET").anyTimes();
        EasyMock.expect(outboundRequest.getUri()).andReturn(uri1).anyTimes();

        HttpServletRequest inboundRequest = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.expect(inboundRequest.getRequestURL()).andReturn(new StringBuffer(uri2.toString())).once();
        EasyMock.expect(inboundRequest.getAttribute("dispatch.ha.failover.counter")).andReturn(new AtomicInteger(0)).once();
        EasyMock.expect(inboundRequest.getAttribute("dispatch.ha.failover.counter")).andReturn(new AtomicInteger(1)).once();

        HttpServletResponse outboundResponse = EasyMock.createNiceMock(HttpServletResponse.class);
        EasyMock.expect(outboundResponse.getOutputStream()).andAnswer(new IAnswer<ServletOutputStream>() {
            @Override
            public ServletOutputStream answer() throws Throwable {
                return new ServletOutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        throw new IOException("unreachable-host.invalid");
                    }

                    @Override
                    public void setWriteListener(WriteListener arg0) {
                    }

                    @Override
                    public boolean isReady() {
                        return false;
                    }
                };
            }
        }).once();
        Assert.assertEquals(uri1.toString(), provider.getActiveURL(serviceName));
        EasyMock.replay(filterConfig, servletContext, inboundResponse, outboundRequest, inboundRequest, outboundResponse);

        RMHaDispatch dispatch = new RMHaDispatch();
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        dispatch.setHttpClient(client);
        dispatch.setHaProvider(provider);
        dispatch.init();
        long startTime = System.currentTimeMillis();
        try {
            dispatch.setInboundResponse(inboundResponse);
            dispatch.executeRequest(outboundRequest, inboundRequest, outboundResponse);
        } catch (IOException e) {
            //this is expected after the failover limit is reached
        }
        Assert.assertEquals(uri3.toString(), dispatch.getUriFromInbound(inboundRequest, inboundResponse, null).toString());
        long elapsedTime = System.currentTimeMillis() - startTime;
        Assert.assertEquals(uri3.toString(), provider.getActiveURL(serviceName));
        //test to make sure the sleep took place
        Assert.assertTrue(elapsedTime > 1000);
    }

    private StringEntity getResponseEntity() {
        String body = "This is standby RM";
        return new StringEntity(body, ContentType.TEXT_HTML);
    }

    private Header getFirstHeader(String host) {
        return new BasicHeader(LOCATION, host);
    }

}

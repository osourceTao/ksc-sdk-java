/*
 * Copyright 2016-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://ksyun.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.ksc.http.apache;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.protocol.HttpContext;

import com.ksc.annotation.SdkInternalApi;
import com.ksc.http.apache.client.impl.ApacheHttpClientFactory;
import com.ksc.util.StringUtils;

/**
 * SdkProxyRoutePlanner delegates a Proxy Route Planner from the settings instead of the
 * system properties. It will use the proxy created from proxyHost and proxyPort and
 * filter the hosts who matches nonProxyHosts pattern.
 *
 * @see ApacheHttpClientFactory#create(com.ksc.http.settings.HttpClientSettings)
 */
@SdkInternalApi
public class SdkProxyRoutePlanner extends DefaultRoutePlanner {
    private HttpHost proxy;
    private String[] hostPatterns;

    public SdkProxyRoutePlanner(String proxyHost, int proxyPort, String nonProxyHosts) {
        super(DefaultSchemePortResolver.INSTANCE);
        proxy = new HttpHost(proxyHost, proxyPort);
        parseNonProxyHosts(nonProxyHosts);
    }

    private void parseNonProxyHosts(String nonProxyHosts) {
        if (!StringUtils.isNullOrEmpty(nonProxyHosts)) {
            String[] hosts = nonProxyHosts.split("\\|");
            hostPatterns = new String[hosts.length];
            for (int i = 0; i < hosts.length; ++i) {
                hostPatterns[i] = hosts[i].toLowerCase().replace("*", ".*?");
            }
        }
    }

    boolean doesTargetMatchNonProxyHosts(HttpHost target) {
        if (hostPatterns == null) {
            return false;
        }
        String targetHost = target.getHostName().toLowerCase();
        for (String pattern : hostPatterns) {
            if (targetHost.matches(pattern)) return true;
        }
        return false;
    }

    @Override
    protected HttpHost determineProxy(
        final HttpHost target,
        final HttpRequest request,
        final HttpContext context) throws HttpException {

        return doesTargetMatchNonProxyHosts(target) ? null : proxy;
    }
}

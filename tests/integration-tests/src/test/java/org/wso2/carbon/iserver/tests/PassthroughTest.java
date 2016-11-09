/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.iserver.tests;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.testng.listener.PaxExam;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.wso2.carbon.container.CarbonContainerFactory;
import org.wso2.carbon.container.options.CarbonDistributionOption;
import org.wso2.carbon.kernel.utils.CarbonServerInfo;
import org.wso2.carbon.protocol.emulator.dsl.Emulator;
import org.wso2.carbon.protocol.emulator.http.client.contexts.HttpClientConfigBuilderContext;
import org.wso2.carbon.protocol.emulator.http.client.contexts.HttpClientRequestBuilderContext;
import org.wso2.carbon.protocol.emulator.http.client.contexts.HttpClientResponseBuilderContext;
import org.wso2.carbon.protocol.emulator.http.client.contexts.HttpClientResponseProcessorContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Inject;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.wso2.carbon.container.options.CarbonDistributionOption.copyDropinsBundle;
import static org.wso2.carbon.container.options.CarbonDistributionOption.copyFile;
import static org.wso2.carbon.protocol.emulator.http.server.contexts.HttpServerConfigBuilderContext.configure;
import static org.wso2.carbon.protocol.emulator.http.server.contexts.HttpServerRequestBuilderContext.request;
import static org.wso2.carbon.protocol.emulator.http.server.contexts.HttpServerResponseBuilderContext.response;

@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@ExamFactory(CarbonContainerFactory.class)
/**
 * Test class to test end to end passthrough sample.
 */
public class PassthroughTest {

    private static final Logger log = LoggerFactory.getLogger(PassthroughTest.class);

    @Inject
    private BundleContext bundleContext;

    @Inject
    private CarbonServerInfo carbonServerInfo;

    @Configuration
    public Option[] createConfiguration() {
        return new Option[]{
                copyPassthroughSampleOption(),
                copyDropinsBundle(maven().artifactId("emulator").groupId("org.wso2.carbon.protocol.emulator")
                        .versionAsInProject()),
                CarbonDistributionOption.keepDirectory()
        };
    }

    @Test
    public void testSuccessEndtoEndPassthroughScenario() throws Exception {
        Emulator.getHttpEmulator().server().given(configure().host("localhost").port(8080).context("/stockquote"))
                .when(request().withMethod(HttpMethod.GET).withPath("/all"))
                .then(response().withBody("Retrieving all Stocks....").withStatusCode(HttpResponseStatus.OK)
                        .withHeader("Header1", "value1")).operation().start();

        HttpClientResponseProcessorContext response = Emulator.getHttpEmulator().client()
                .given(HttpClientConfigBuilderContext.configure().host("localhost").port(9090))
                .when(HttpClientRequestBuilderContext.request().withPath("/stocks/getStocks")
                        .withMethod(HttpMethod.GET))
                .then(HttpClientResponseBuilderContext.response().assertionIgnore()).operation().send();

        Assert.assertEquals(response.getReceivedResponseContext().getResponseStatus().toString(), "200 OK");
    }


    /**
     * Deploy the simple passThrough sample
     */
    private Option copyPassthroughSampleOption() {
        Path passthroughSamplePath;

        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = Paths.get(".").toString();
        }
        passthroughSamplePath = Paths.get("samples", "basic-routing", "passthrough.ballerina");
        return copyFile(passthroughSamplePath, Paths.get("deployment", "integration-flows", "passthrough.ballerina"));
    }

}

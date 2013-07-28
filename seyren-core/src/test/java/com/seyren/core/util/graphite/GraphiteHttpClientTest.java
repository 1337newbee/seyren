/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.seyren.core.util.graphite;

import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriverRule;
import com.seyren.core.util.config.SeyrenConfig;

public class GraphiteHttpClientTest {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    @Rule
    public ClientDriverRule clientDriver = new ClientDriverRule();
    
    private GraphiteHttpClient graphiteHttpClient;
    
    @Before
    public void before() {
        graphiteHttpClient = new GraphiteHttpClient(seyrenConfig(clientDriver.getBaseUrl()));
    }

    @After
    public void after() {
        System.clearProperty("GRAPHITE_URL");
    }
    
    @Test
    public void requestingJsonCallsThroughToGraphiteCorrectly() throws Exception {
        String response = "[{\"target\": \"service.error.1MinuteRate\", \"datapoints\": [[0.06, 1337453460]]}]";
        
        clientDriver.addExpectation(
                onRequestTo("/render/")
                        .withParam("from", "-11minutes")
                        .withParam("until", "-1minutes")
                        .withParam("uniq", Pattern.compile("[0-9]+"))
                        .withParam("format", "json")
                        .withParam("target", "service.error.1MinuteRate"),
                giveResponse(response, "application/json"));
        
        JsonNode node = graphiteHttpClient.getTargetJson("service.error.1MinuteRate");
        
        assertThat(node, is(MAPPER.readTree(response)));
    }

    @Test
    public void exceptionGettingDataFromGraphiteIsHandled() throws Exception {
        thrown.expect(GraphiteReadException.class);
        
        graphiteHttpClient = new GraphiteHttpClient(seyrenConfig("http://unknown"));
        graphiteHttpClient.getTargetJson("service.*.1MinuteRate");
    }
    
    @Test
    public void authIsAddedWhenUsernameAndPasswordAreProvided() throws Exception {
        System.setProperty("GRAPHITE_USERNAME", "seyren");
        System.setProperty("GRAPHITE_PASSWORD", "s3yr3N");
        graphiteHttpClient = new GraphiteHttpClient(seyrenConfig(clientDriver.getBaseUrl()));
        
        String response = "[{\"target\": \"service.error.1MinuteRate\", \"datapoints\": [[0.20, 1337453460],[0.01, 1337453463]]}]";
        
        clientDriver.addExpectation(
                onRequestTo("/render/")
                        .withParam("from", "-11minutes")
                        .withParam("until", "-1minutes")
                        .withParam("uniq", Pattern.compile("[0-9]+"))
                        .withParam("format", "json")
                        .withParam("target", "service.error.1MinuteRate")
                        .withHeader("Authorization", "Basic c2V5cmVuOnMzeXIzTg=="),
                giveResponse(response, "application/json"));
        
        graphiteHttpClient.getTargetJson("service.error.1MinuteRate");
        
        System.clearProperty("GRAPHITE_USERNAME");
        System.clearProperty("GRAPHITE_PASSWORD");
    }
    
    private SeyrenConfig seyrenConfig(String graphiteUrl) {
        System.setProperty("GRAPHITE_URL", graphiteUrl);
        return new SeyrenConfig();
    }
    
}

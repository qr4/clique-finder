package cliquefinder.datafetcher;

import cliquefinder.model.TwitterAccount;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Flowable;
import javafx.util.Pair;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.function.Predicate;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Unit tests for our twitter api adapter. Note that real integration tests will be done in CoreTest (with real json responses)
 * Created by qr4 on 30.07.17.
 */
public class TwitterAPITest {
    private WireMockServer mockServer;
    private WireMock wireMock;
    private TwitterAPI sut;

    @BeforeMethod
    public void mockDummyResponses() {
        mockServer = new WireMockServer(wireMockConfig().port(8089)); // No-args constructor will start on port 8080, no HTTPS
        mockServer.start();
        this.wireMock = new WireMock(mockServer.port());

        // only one cursor page
        mockOkIdsResponse(wireMock, "-1", "foo", "{\"next_cursor\":0, \"ids\":[\"123\", \"42\"]}");

        // multiple pages
        mockOkIdsResponse(wireMock, "-1", "bar","{\"next_cursor\":\"1\", \"ids\":[\"1\"]}");
        mockOkIdsResponse(wireMock, "1", "bar","{\"next_cursor\":\"0\", \"ids\":[\"2\",\"3\"]}");

        // mock lookup responses
        mockOkUserLookupResponse(wireMock, "name1,name2", "[{\"id_str\":\"1\", \"screen_name\":\"name1\"},{\"id_str\":\"2\", \"screen_name\":\"name2\"}]");

        this.sut = new TwitterAPI("http://localhost:8089");
    }

    // exposed for other tests
    public static void mockOkIdsResponse(final WireMock wiremock, final String cursor, final String name, final String json) {
        wiremock.register(WireMock.get(WireMock.urlPathEqualTo("/followers/ids.json"))
                .withQueryParam("screen_name", WireMock.equalTo(name))
                .withQueryParam("cursor", WireMock.equalTo(cursor))
                .willReturn(WireMock.okJson(json)));
    }

    // exposed for other tests
    public static void mockOkUserLookupResponse(final WireMock wiremock, final String json) {
        wiremock.register(WireMock.post(WireMock.urlPathEqualTo("/users/lookup.json"))
                .willReturn(WireMock.okJson(json)));
    }

    private static void mockOkUserLookupResponse(final WireMock wiremock, final String names, final String json) {
        wiremock.register(WireMock.post(WireMock.urlPathEqualTo("/users/lookup.json"))
                .withQueryParam("screen_name", WireMock.equalTo(names))
                .willReturn(WireMock.okJson(json)));
    }

    @AfterMethod
    public void cleanUp() {
        wireMock.removeMappings();
        mockServer.stop();
    }

    @Test
    public void constructsLookupRequestsCorrectly() {
       final List<Flowable<Pair<String,String>>> flowables = sut.fetchIdsForNames(ImmutableSet.of("name1", "name2"));
       Assert.assertEquals(flowables.get(0).test().assertComplete().values(),
               ImmutableList.of(new Pair<>("name1","1"), new Pair<>("name2", "2")));
    }

    @Test
    public void constructsFollowersRequestsCorrectly() {
        final List<Flowable<TwitterAccount>> flowables = sut.fetchFollowersForNames(ImmutableSet.of("foo", "bar"));

        assertEmittedTwitterAccount(flowables.get(0).test().assertComplete().values(),
                account -> {
                    Assert.assertEquals(account.getName(), "foo");
                    Assert.assertEquals(account.getFollowersIds().length, 2);
                    Assert.assertEquals(account.getFollowersIds()[0],"123");
                    Assert.assertEquals(account.getFollowersIds()[1],"42");
                    return true;
                });

        // we expect 2 emissions
        final List<TwitterAccount> emittedValues = flowables.get(1).test().assertComplete().values();
        Assert.assertEquals(emittedValues.size(), 2);

        Assert.assertEquals(emittedValues.get(0).getName(), "bar");
        Assert.assertEquals(emittedValues.get(0).getFollowersIds().length, 1);
        Assert.assertEquals(emittedValues.get(0).getFollowersIds()[0], "1");

        Assert.assertEquals(emittedValues.get(1).getName(), "bar");
        Assert.assertEquals(emittedValues.get(1).getFollowersIds().length, 2);
        Assert.assertEquals(emittedValues.get(1).getFollowersIds()[0], "2");
        Assert.assertEquals(emittedValues.get(1).getFollowersIds()[1], "3");
    }

    @Test
    public void ignoresNonSuccessfulFollowersResponses() {
        final List<Flowable<TwitterAccount>> observables = sut.fetchFollowersForNames(ImmutableSet.of("faulty"));

        wireMock.register(WireMock.get(WireMock.urlPathEqualTo("/followers/list.json"))
                .withQueryParam("cursor", WireMock.equalTo("-1"))
                .withQueryParam("screen_name", WireMock.equalTo("faulty"))
                .willReturn(WireMock.badRequest())) ;

        Assert.assertEquals(observables.get(0).test().assertComplete().valueCount(), 0);
    }

    private void assertEmittedTwitterAccount(
            final List<TwitterAccount> emittedValues, final Predicate<TwitterAccount> validator
    ) {
        Assert.assertEquals(emittedValues.size(), 1);
        validator.test(emittedValues.get(0));
    }
}

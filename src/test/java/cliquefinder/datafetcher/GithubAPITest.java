package cliquefinder.datafetcher;

import cliquefinder.model.GithubAccount;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Flowable;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.function.Predicate;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Unit tests for our github api adapter. Note that real integration tests will be done in CoreTest (with real json responses)
 * Created by qr4 on 30.07.17.
 */
public class GithubAPITest {
    private WireMockServer mockServer;
    private WireMock wireMock;
    private GithubAPI sut;

    @BeforeMethod
    public void mockDummyResponses() {
        mockServer = new WireMockServer(wireMockConfig().port(8089)); // No-args constructor will start on port 8080, no HTTPS
        mockServer.start();
        this.wireMock = new WireMock(mockServer.port());
        mockOkOrgsResponse(wireMock, "foo", "[{\"id\":\"fooId1\"}, {\"id\":\"fooId2\"}]");
        mockOkOrgsResponse(wireMock, "bar","[{\"id\":\"barId\"}]");

        this.sut = new GithubAPI("http://localhost:8089");
    }

    public static void mockOkOrgsResponse(final WireMock wiremock, final String name, final String json) {
        wiremock.register(WireMock.get(WireMock.urlMatching("/users/" + name + "/orgs"))
                .willReturn(WireMock.okJson(json)));
    }

    @AfterMethod
    public void cleanUp() {
        wireMock.removeMappings();
        mockServer.stop();
    }

    @Test
    public void constructsRequestsCorrectly() {
        final List<Flowable<GithubAccount>> observables = sut.fetchOrganizationsForNames(ImmutableSet.of("foo", "bar"));

        assertOnlyOneEmittedGithubAccountsEquals(observables.get(0).test().assertComplete().values(),
                account -> {
                    Assert.assertEquals(account.getName(), "foo");
                    Assert.assertEquals(account.getGithubOrganizations().length, 2);
                    Assert.assertEquals(account.getGithubOrganizations()[0].getId(), "fooId1");
                    Assert.assertEquals(account.getGithubOrganizations()[1].getId(), "fooId2");
                    return true;
                });

        assertOnlyOneEmittedGithubAccountsEquals(observables.get(1).test().assertComplete().values(),
                account -> {
                    Assert.assertEquals(account.getName(), "bar");
                    Assert.assertEquals(account.getGithubOrganizations().length, 1);
                    Assert.assertEquals(account.getGithubOrganizations()[0].getId(), "barId");
                    return true;
                });
    }

    @Test
    public void ignoresNonSuccessfulResponses() {
        final List<Flowable<GithubAccount>> observables = sut.fetchOrganizationsForNames(ImmutableSet.of("foo", "faulty"));
        wireMock.register(WireMock.get(WireMock.urlEqualTo("/users/faulty/orgs"))
                .willReturn(WireMock.badRequest()));

        assertOnlyOneEmittedGithubAccountsEquals(observables.get(0).test().assertComplete().values(),
                account -> {
                    Assert.assertEquals(account.getName(), "foo");
                    Assert.assertEquals(account.getGithubOrganizations().length, 2);
                    Assert.assertEquals(account.getGithubOrganizations()[0].getId(), "fooId1");
                    Assert.assertEquals(account.getGithubOrganizations()[1].getId(), "fooId2");
                    return true;
                });

        Assert.assertEquals(observables.get(1).test().valueCount(), 0);
    }

    private void assertOnlyOneEmittedGithubAccountsEquals(
            final List<GithubAccount> emittedValues, final Predicate<GithubAccount> validator
    ) {
        Assert.assertEquals(emittedValues.size(), 1);
        validator.test(emittedValues.get(0));
    }
}

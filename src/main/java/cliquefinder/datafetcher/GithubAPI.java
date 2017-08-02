package cliquefinder.datafetcher;

import cliquefinder.model.GithubAccount;
import cliquefinder.model.GithubOrganization;
import com.google.gson.Gson;
import io.reactivex.Flowable;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.util.HttpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class queries the github api for given names and returns a list ob rxjava2 flowables.
 * Each of this observable represents a github user with a name and a list of joined organizations.
 * Created by qr4 on 25.07.17.
 */
@ParametersAreNonnullByDefault
public class GithubAPI {
    private static final Logger LOG = LoggerFactory.getLogger(GithubAPI.class);

    private static final Gson gson = new Gson();
    private final String baseUrl;
    private final AsyncHttpClient client;

    public GithubAPI(final String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new DefaultAsyncHttpClient();
    }

    public List<Flowable<GithubAccount>> fetchOrganizationsForNames(final Set<String> names) {
        return names.stream().map(
                name -> {
                    final String url = getUrlForOrgsRequest(name);
                    return Flowable.fromFuture(client.prepareGet( url ).execute()).retry( 5 )
                            .map(resp -> {
                                if (resp.getStatusCode() != HttpConstants.ResponseStatusCodes.OK_200) {
                                    // Log an error and ignore this name
                                    LOG.error("Could not fetch github data for name: '{}'. Got Error {} {}", name, resp.getStatusCode(), resp.getStatusText());
                                    return null;
                                }
                                final GithubOrganization[] organizations = gson.fromJson(resp.getResponseBody(), GithubOrganization[].class);
                                return new GithubAccount(name, organizations);
                            });
                }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public void close() throws IOException {
        client.close();
    }

    private String getUrlForOrgsRequest(final String name) {
        return this.baseUrl + "/users/" + name + "/orgs";
    }
}

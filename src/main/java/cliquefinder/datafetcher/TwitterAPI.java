package cliquefinder.datafetcher;

import cliquefinder.model.TwitterAccount;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import javafx.util.Pair;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.asynchttpclient.util.HttpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This utility class queries the twitter api for a list of names, returning a list of observables.
 * Each observable represents a TwitterAPI account with a name and a list of followers.
 * Created by qr4 on 30.07.17.
 */
@ParametersAreNonnullByDefault
public class TwitterAPI {
    private static final Logger LOG = LoggerFactory.getLogger(TwitterAPI.class);

    private static final Gson gson = new Gson();
    private static final JsonParser parser = new JsonParser();
    private static final String BEARER_CODE = System.getenv("BEARER_CODE");

    private final String baseUrl;
    private final AsyncHttpClient client;

    public TwitterAPI(String baseUrl) {
        this.baseUrl = baseUrl;
        client = new DefaultAsyncHttpClient();
    }

    private List<Pair<String,String>> parseUserData(final JsonArray jsonUsers) {
        return Streams.stream(jsonUsers).map(jsonElement -> {
            final JsonObject user = jsonElement.getAsJsonObject();
            return new Pair<>(user.get("screen_name").getAsString(), user.get("id_str").getAsString());
        }).collect(Collectors.toList());
    }

    public List<Flowable<Pair<String, String>>> fetchIdsForNames(final Set<String> names) {
        return Lists.partition(names.stream().collect(Collectors.toList()), 100)
                .stream()
                .map(nameBatch -> String.join(",", nameBatch))
                .map(nameBatchStr ->
                        Flowable.fromFuture(client.preparePost(getUrlForLookupRequest(nameBatchStr)).execute()).retry( 5 )
                                .flatMap(response -> {
                                    if (response.getStatusCode() != HttpConstants.ResponseStatusCodes.OK_200) {
                                        LOG.error("Could not fetch ids for names: '{}'. Got Error: {} {}",
                                                nameBatchStr, response.getStatusCode(), response.getStatusText());
                                        return Flowable.fromIterable(ImmutableList.of());
                                    }

                                    final JsonArray users = parser.parse(response.getResponseBody()).getAsJsonArray();
                                    return Flowable.fromIterable(parseUserData(users));
                                }))
                .collect(Collectors.toList());
    }

    public List<Flowable<TwitterAccount>> fetchFollowersForNames(final Set<String> names) {
        return names.stream().map(
                name -> Flowable.<TwitterAccount>create(emitter -> {
                    try {
                        long cursor = -1;
                        while (cursor != 0) {
                            final String url = getUrlForFollowersRequest(name, cursor);
                            final ListenableFuture<Response> future = client.prepareGet( url )
                                    .setHeader("Authorization", "Bearer " + BEARER_CODE)
                                    .execute();

                            final Response response = future.toCompletableFuture().get();
                            if (response.getStatusCode() != HttpConstants.ResponseStatusCodes.OK_200) {
                                // Log error and ignore this request.
                                LOG.error("Could not fetch twitter follower ids ids for name: '{}'. Got Error: {} {}",
                                        name, response.getStatusCode(), response.getStatusText());
                                break;
                            }
                            final JsonObject tree = parser.parse(response.getResponseBody()).getAsJsonObject();
                            final JsonArray users = tree.getAsJsonArray("ids");
                            final String[] followersIds = gson.fromJson(users.toString(), String[].class);

                            emitter.onNext(new TwitterAccount(name, followersIds));
                            cursor = tree.get("next_cursor").getAsLong();
                        }
                        emitter.onComplete();
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                }, BackpressureStrategy.BUFFER).cache()).collect(Collectors.toList());
    }

    public void close() throws IOException {
        client.close();
    }

    private String getUrlForFollowersRequest(final String name, final long cursor) {
        return baseUrl + "/followers/ids.json?" +
                "cursor=" + cursor +
                "&screen_name=" + name;
    }

    private String getUrlForLookupRequest(final String names) {
         return baseUrl + "/users/lookup.json?" + "screen_name=" + names;
    }
}

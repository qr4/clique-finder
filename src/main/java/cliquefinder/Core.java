package cliquefinder;

import cliquefinder.datafetcher.GithubAPI;
import cliquefinder.datafetcher.TwitterAPI;
import cliquefinder.model.GithubAccount;
import cliquefinder.model.GithubOrganization;
import cliquefinder.model.TwitterAccount;
import cliquefinder.solver.BronKerbosch;
import cliquefinder.solver.Clique;
import cliquefinder.solver.Graph;
import cliquefinder.solver.Node;
import io.reactivex.Flowable;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class holds the main program / logic flow.
 * Created by qr4 on 30.07.17.
 */
@ParametersAreNonnullByDefault
class Core {
    private static final Logger LOG = LoggerFactory.getLogger(Core.class);

    private final static Integer MAX_CONCURRENCY = 2;
    private final IOHandler ioHandler;
    private final Graph graph;
    private final GithubAPI githubAPI;
    private final TwitterAPI twitterAPI;

    Core(final String inputFilename, final String outputFilename,
         final String baseGithubUrl, final String baseTwitterUrl) throws IOException {
        this.ioHandler = new IOHandler(inputFilename, outputFilename);
        this.graph = new Graph();
        this.githubAPI = new GithubAPI(baseGithubUrl);
        this.twitterAPI = new TwitterAPI(baseTwitterUrl);
    }

    void run() throws IOException {
        final Set<String> names = ioHandler.parseInput();
        addEdgesFromGithub(names);
        addEdgesFromTwitter(names);

        LOG.info("Fetched all the data. Starting computation...");
        // at this point, we should have our graph built. Call our solver with the passed names.
        final BronKerbosch solver = new BronKerbosch(graph);
        final Set<Clique> cliques = solver.compute(names.stream().map(Node::new).collect(Collectors.toSet()));
        LOG.info("Writing result to file...");

        ioHandler.writeOutput(cliques);

        this.twitterAPI.close();
        this.githubAPI.close();
    }

    private void addEdgesFromGithub(final Set<String> names) {
        final List<Flowable<GithubAccount>> flowables = githubAPI.fetchOrganizationsForNames(names);
        final HashMap<String, List<String>> incidenceData = new HashMap<>();

        Flowable.merge(flowables, MAX_CONCURRENCY).map(obs ->
                new Pair<>(obs.getName(),
                        Arrays.stream(obs.getGithubOrganizations())
                                .map(GithubOrganization::getId)
                                .collect(Collectors.toList()))
        ).forEach(data -> {
            final String name = data.getKey();
            final List<String> commonGroup = data.getValue();
            commonGroup.forEach(common -> {
                incidenceData.putIfAbsent(common, new ArrayList<>());
                incidenceData.get(common).forEach(c -> graph.addEdge(new Node(name), new Node(c)));
                incidenceData.get(common).add(name);
            });
        });
    }

    private void addEdgesFromTwitter(final Set<String> names) {
        // first, map the names to ids
        final List<Flowable<Pair<String,String>>> lookupFlowables = twitterAPI.fetchIdsForNames(names);
        final HashMap<String, String> twitterIdNameLookup = new HashMap<>();
        Flowable.merge(lookupFlowables, MAX_CONCURRENCY)
                .forEach(nameIdPair -> twitterIdNameLookup.put(nameIdPair.getValue(), nameIdPair.getKey()));

        final List<Flowable<TwitterAccount>> idsFlowables = twitterAPI.fetchFollowersForNames(names);
        final HashMap<String, Set<String>> adjacencyData = new HashMap<>();
        Flowable.merge(idsFlowables, MAX_CONCURRENCY)
                .map(obs ->
                        new Pair<>(obs.getName(), Arrays.stream(obs.getFollowersIds())
                                .map(twitterIdNameLookup::get)
                                .collect(Collectors.toList())))

                .forEach(data -> {
                    final String name = data.getKey();
                    final List<String> followers = data.getValue();
                    followers.forEach(follower -> {
                        adjacencyData.putIfAbsent(name, new HashSet<>());
                        adjacencyData.get(name).add(follower);
                        // add the edges only of both names are adjacent to each other
                        if (adjacencyData.containsKey(follower) &&
                            adjacencyData.get(follower).contains(name)) {
                            graph.addEdge(new Node(name), new Node(follower));
                        }
                    });
                });
    }
}

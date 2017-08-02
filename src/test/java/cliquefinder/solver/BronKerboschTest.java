package cliquefinder.solver;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Unit tests for the Bron-Kerbosch algorithm.
 * Created by qr4 on 24.07.17.
 */
public class BronKerboschTest {
    private void assertResultEquals(final Set<Clique> result, final Set<Clique> expected) {
        Assert.assertTrue(Sets.symmetricDifference(result, expected).isEmpty());
    }

    /**
     * This graph is a simple edge.
     */
    @Test
    public void testSimpleGraph() {
        final BronKerbosch sut = new BronKerbosch(getGraphFromFile("simple_graph.txt"));
        final Set<Clique> result = sut.compute(ImmutableSet.of(
                new Node("v1"), new Node("v2"))
        );

        assertResultEquals(result, ImmutableSet.of(
                new Clique(ImmutableSet.of(new Node("v2"), new Node("v1")))
        ));
    }

    /**
     * This graph is the example graph from wikipedia's article on the bron-kerbosch algorithm.
     */
    @Test
    public void testWikiExampleGraph() {
        final BronKerbosch sut = new BronKerbosch(getGraphFromFile("wikipedia_example_graph.txt"));
        final Set<Clique> result = sut.compute(Stream.of("v1", "v2", "v3", "v4", "v5", "v6")
                .map(Node::new)
                .collect(Collectors.toSet()));

        assertResultEquals(result, ImmutableSet.of(
                new Clique(ImmutableSet.of(new Node("v6"), new Node("v4"))),
                new Clique(ImmutableSet.of(new Node("v2"), new Node("v3"))),
                new Clique(ImmutableSet.of(new Node("v2"), new Node("v5"), new Node("v1"))),
                new Clique(ImmutableSet.of(new Node("v4"), new Node("v3"))),
                new Clique(ImmutableSet.of(new Node("v4"), new Node("v5"))
        )));
    }

    @Test
    public void testWikiExampleGraphEmptyInputNodes() {
        final BronKerbosch sut = new BronKerbosch(getGraphFromFile("wikipedia_example_graph.txt"));
        final Set<Clique> result = sut.compute(ImmutableSet.of());

        assertResultEquals(result, ImmutableSet.of());
    }

    @Test
    public void testWikiExampleGraphSingleInputNode() {
        final BronKerbosch sut = new BronKerbosch(getGraphFromFile("wikipedia_example_graph.txt"));
        final Set<Clique> result = sut.compute(ImmutableSet.of(new Node("v1")));

        assertResultEquals(result, ImmutableSet.of(
                new Clique(ImmutableSet.of(new Node("v1")))));
    }

    @Test
    public void testWikiExampleGraphTriangleNodes() {
        final BronKerbosch sut = new BronKerbosch(getGraphFromFile("wikipedia_example_graph.txt"));
        final Set<Clique> result = sut.compute(ImmutableSet.of(
                new Node("v2"), new Node("v5"), new Node("v1")));

        assertResultEquals(result, ImmutableSet.of(
                new Clique(ImmutableSet.of(
                        new Node("v1"), new Node("v2"), new Node("v5")))));
    }

    /**
     * This graph consists of two disconnected components (both of which are triangles)
     */
    @Test
    public void testDisconnectedSCCGraph() {
        final BronKerbosch sut = new BronKerbosch(getGraphFromFile("two_scc_graph.txt"));
        final Set<Clique> result = sut.compute(ImmutableSet.of(
                new Node("v1"),
                new Node("v2"),
                new Node("v3"),
                new Node("v4"),
                new Node("v5"),
                new Node("v6")));

        assertResultEquals(result, ImmutableSet.of(
                new Clique(ImmutableSet.of(new Node("v1"), new Node("v2"), new Node("v3"))),
                new Clique(ImmutableSet.of(new Node("v4"), new Node("v5"), new Node("v6")))
        ));
    }

    private Graph getGraphFromFile(final String fileName) {
        try {
            final File file = new File( this.getClass().getResource( fileName ).getFile() );
            final Graph graph = new Graph();

            Files.lines(file.toPath())
                    .map(line -> line.split("\\s"))
                    .forEach(edge -> {
                        graph.addEdge( new Node(edge[0]), new Node(edge[1]));
                    });
            return graph;
        } catch (final IOException ioe) {
            Assert.fail("Could not parseInput graph.");
            return null;
        }
    }
}


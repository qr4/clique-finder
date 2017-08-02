package cliquefinder.solver;

import com.google.common.collect.ImmutableSet;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for our graph representation.
 * Created by qr4 on 30.07.17.
 */
public class GraphTest {
    @Test
    public void selfLoopsAreIgnored() {
        final Graph g = new Graph();
        final Node n1 = new Node("node1");
        g.addEdge(n1, n1);
        Assert.assertEquals(g.getAdjacentNodes(n1), ImmutableSet.of());
    }

    @Test
    public void returnsNodeWithMaxDegree() {
        final Graph g = new Graph();
        final Node n1 = new Node("node1");
        final Node n2 = new Node("node2");
        final Node n3 = new Node("node3");
        final Node n4 = new Node("node3");
        // n1 - n2 - n3
        //       |
        //       n4
        g.addEdge(n1, n2);
        g.addEdge(n2, n3);
        g.addEdge(n2, n4);

        Assert.assertEquals(g.getNodeWithMaxDegree(ImmutableSet.of(n1, n2, n3, n4)), n2);

    }

    @Test
    public void returnsCorrectAdjacentNodes() {
        final Graph g = new Graph();
        final Node n1 = new Node("node1");
        final Node n2 = new Node("node2");
        final Node n3 = new Node("node3");

        // n1 - n2 - n3
        g.addEdge(n1, n2);
        g.addEdge(n2, n3);
        Assert.assertEquals(g.getAdjacentNodes(n2), ImmutableSet.of(n1, n3));
    }

    @Test
    public void returnsCorrectAdjacentNodesForInvalidNode() {
        final Graph g = new Graph();
        Assert.assertEquals(g.getAdjacentNodes(new Node("testNode")), ImmutableSet.of());
    }
}

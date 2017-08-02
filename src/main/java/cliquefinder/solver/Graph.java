package cliquefinder.solver;

import com.google.common.collect.ImmutableSet;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents an undirected graph.
 * Created by qr4 on 25.07.17.
 */
@ParametersAreNonnullByDefault
public class Graph {
    private HashMap<Node, Set<Node>> graph;

    public Graph() {
        this.graph = new HashMap<>();
    }

    public void addEdge(final Node n1, final Node n2) {
        // dont allow self-loops
        if (n1.equals(n2)) return;

        graph.putIfAbsent(n1, new HashSet<>());
        graph.putIfAbsent(n2, new HashSet<>());

        graph.get(n1).add(n2);
        graph.get(n2).add(n1);
    }

    Set<Node> getAdjacentNodes(final Node node) {
        return graph.getOrDefault(node, ImmutableSet.of());
    }

    Node getNodeWithMaxDegree(final Set<Node> nodes) {
        return Collections.max(nodes, Comparator.comparingInt(node -> getAdjacentNodes(node).size()));
    }

}

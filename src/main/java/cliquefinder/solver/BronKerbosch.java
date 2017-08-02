package cliquefinder.solver;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

/**
 * This class implements the Bron-Kerbosch algorithm, which finds maximal cliques.
 *
 *  Starting point: R and X empty set and P is the vertex set of the nodes
 *
 *  BronKerbosch2(R,P,X):
 *      if P and X are both empty:
 *          report R as a maximal clique
 *      choose a pivot vertex u in P ⋃ X
 *      for each vertex v in P \ N(u):
 *          BronKerbosch2(R ⋃ {v}, P ⋂ N(v), X ⋂ N(v))
 *          P := P \ {v}
 *          X := X ⋃ {v}
 *
 * Created by qr4 on 24.07.17.
 */
@ParametersAreNonnullByDefault
public class BronKerbosch {
    private Graph graph;

    public BronKerbosch(final Graph graph) {
        this.graph = graph;
    }

    public Set<Clique> compute(final Set<Node> nodes) {
        // If the input nodes are empty, we have no maximal cliques.
        if (nodes.isEmpty()) {
            return ImmutableSet.of();
        }

        return recursionHelper(ImmutableSet.of(), ImmutableSet.copyOf(nodes), ImmutableSet.of());
    }

    private Set<Clique> recursionHelper(final ImmutableSet<Node> R,
                                        final ImmutableSet<Node> P,
                                        final ImmutableSet<Node> X) {
        if (P.isEmpty()) {
            if (X.isEmpty()) {
                return ImmutableSet.of(new Clique(R));
            }
            return ImmutableSet.of();
        }

        ImmutableSet.Builder<Clique> resultBuilder = new ImmutableSet.Builder<>();

        final Node pivotNode = graph.getNodeWithMaxDegree(P);

        final Set<Node> recursionX = new HashSet<>(X);
        final Set<Node> recursionP = new HashSet<>(P);

        for (final Node node : Sets.difference(P, graph.getAdjacentNodes(pivotNode))) {
            resultBuilder.addAll(recursionHelper(
                    ImmutableSet.<Node>builder().addAll(R).add(node).build(),
                    Sets.intersection(recursionP, graph.getAdjacentNodes(node)).immutableCopy(),
                    Sets.intersection(recursionX, graph.getAdjacentNodes(node)).immutableCopy()
            ));

            recursionP.remove(node);
            recursionX.add(node);
        }

        return resultBuilder.build();
    }
}

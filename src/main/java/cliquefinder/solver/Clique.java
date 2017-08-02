package cliquefinder.solver;

import com.google.common.collect.Sets;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A clique is a set of nodes which are interconnected. This class is just a typed
 * wrapper around a set of nodes.
 * Created by qr4 on 25.07.17.
 */
@ParametersAreNonnullByDefault
public class Clique {
    private TreeSet<Node> nodes;

    Clique(final Set<Node> nodes) {
        this.nodes = new TreeSet<>(nodes);
    }

    public SortedSet<Node> getNodes() {
        return nodes;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Clique)) {
            return false;
        }
        return Sets.symmetricDifference(((Clique) other).nodes, this.nodes).isEmpty();
    }

    @Override
    public int hashCode() {
        return nodes.hashCode();
    }
}

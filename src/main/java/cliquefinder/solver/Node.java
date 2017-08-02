package cliquefinder.solver;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This class represents a node in a graph and is just a typed wrapper around a string.
 * Created by qr4 on 25.07.17.
 */
@ParametersAreNonnullByDefault
public class Node implements Comparable<Node> {
    private String name;

    public Node(final String name) {
        this.name = name;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Node)) {
            return false;
        }
        return ((Node) other).name.equals(this.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public String getName() {
        return this.name;
    }

    @Override
    public int compareTo(final Node o) {
        return this.name.compareTo(o.name);
    }
}

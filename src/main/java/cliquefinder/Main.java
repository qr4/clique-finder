package cliquefinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            LOG.error("You need to pass 2 filenames: <input> <output>");
            return;
        }
        final Core c = new Core(args[0], args[1],
                "https://api.github.com",
                "https://api.twitter.com/1.1"
        );
        c.run();
    }
}

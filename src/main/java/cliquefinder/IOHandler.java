package cliquefinder;

import cliquefinder.solver.Clique;
import cliquefinder.solver.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class handles the IOHandler logic, i.e. parsing the input and writing the output.
 * Created by qr4 on 30.07.17.
 */
@ParametersAreNonnullByDefault
class IOHandler {
    private static final Logger LOG = LoggerFactory.getLogger(IOHandler.class);

    private final Path inputPath;
    private final Path outputPath;

    IOHandler(final String inputFilename, final String outputFilename) throws IOException {
        inputPath = Paths.get(inputFilename);
        if (!Files.exists(inputPath)) {
            throw new IllegalArgumentException("input file does not exist");
        }

        outputPath = Paths.get(outputFilename);
        if (Files.exists(outputPath)) {
            LOG.info("File already exists at output path. Will overwrite it.");
            Files.delete(outputPath);
        }

        Files.createFile(outputPath);
    }

    Set<String> parseInput() throws IOException {
        // Note that I dont do much input validation here. I assume that the passed file contains the correct
        // data, i.e. one name per line.
        return Files.readAllLines(this.inputPath).stream().collect(Collectors.toSet());
    }

    void writeOutput(final Set<Clique> cliques) {
        cliques.stream().sorted(Comparator.comparing(clique -> clique.getNodes().first()))
                .filter(c -> c.getNodes().size() >= 2)
                .forEach(clique -> {
                    try {
                        Files.write(this.outputPath, (String.join(" ",
                                clique.getNodes().stream().map(Node::getName).collect(Collectors.toList())) + "\n").getBytes(),
                                StandardOpenOption.APPEND);
                    } catch (final IOException e) {
                        LOG.error("could not write clique to file!", e);
                    }
                });
    }
}

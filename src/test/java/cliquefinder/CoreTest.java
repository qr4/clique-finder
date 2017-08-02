package cliquefinder;

import cliquefinder.datafetcher.GithubAPITest;
import cliquefinder.datafetcher.TwitterAPITest;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * This class performs an integration tests: it returns real recorded json and verifies the resulting cliques.
 * Created by qr4 on 30.07.17.
 */
public class CoreTest {
    private WireMockServer mockServer;
    private WireMock wireMock;
    private File inputFile;
    private File outputFile;

    private Core sut;

    @BeforeMethod
    public void setup() throws IOException {
        mockServer = new WireMockServer(wireMockConfig().port(8089)); // No-args constructor will start on port 8080, no HTTPS
        mockServer.start();
        this.wireMock = new WireMock(mockServer.port());

        inputFile = File.createTempFile("inputNames", ".txt");
        inputFile.deleteOnExit();
        // write input names
        Files.write(inputFile.toPath(), "alex\nbob\nalice\neve\ngrunt\nsly".getBytes());
        outputFile = File.createTempFile("outputNames", ".txt");
        outputFile.deleteOnExit();

        // setup the responses
        GithubAPITest.mockOkOrgsResponse(wireMock, "alex", getResponse("GithubResponseAlex"));
        GithubAPITest.mockOkOrgsResponse(wireMock, "alice", getResponse("GithubResponseAlice"));
        GithubAPITest.mockOkOrgsResponse(wireMock, "bob", getResponse("GithubResponseBob"));
        GithubAPITest.mockOkOrgsResponse(wireMock, "eve", getResponse("GithubResponseEve"));
        GithubAPITest.mockOkOrgsResponse(wireMock, "grunt", getResponse("GithubResponseGrunt"));
        GithubAPITest.mockOkOrgsResponse(wireMock, "sly", getResponse("GithubResponseSly"));

        TwitterAPITest.mockOkIdsResponse(wireMock, "-1", "alex", getResponse("TwitterResponseAlex"));
        TwitterAPITest.mockOkIdsResponse(wireMock, "-1",  "alice", getResponse("TwitterResponseAlice"));
        TwitterAPITest.mockOkIdsResponse(wireMock, "-1",  "bob", getResponse("TwitterResponseBob"));
        TwitterAPITest.mockOkIdsResponse(wireMock, "-1",  "eve", getResponse("TwitterResponseEve"));
        TwitterAPITest.mockOkIdsResponse(wireMock, "-1",  "grunt", getResponse("TwitterResponseGrunt"));
        TwitterAPITest.mockOkIdsResponse(wireMock, "-1",  "sly", getResponse("TwitterResponseSly"));

        TwitterAPITest.mockOkUserLookupResponse(wireMock, getResponse("TwitterResponseLookup"));

        this.sut = new Core(inputFile.getAbsolutePath(),outputFile.getAbsolutePath(),
                "http://localhost:8089",
                "http://localhost:8089");
    }

    @AfterMethod
    public void cleanUp() {
        wireMock.removeMappings();
        mockServer.stop();
    }

    @Test
    public void computesCorrectCliques() throws IOException {
        sut.run();

        final List<String> output = Files.readAllLines(outputFile.toPath());
        Assert.assertEquals(output.size(), 5);
        Assert.assertEquals(output.get(0), "alex alice grunt");
        Assert.assertEquals(output.get(1), "alice bob");
        Assert.assertEquals(output.get(2), "bob eve");
        Assert.assertEquals(output.get(3), "eve grunt");
        Assert.assertEquals(output.get(4), "eve sly");
    }


    private String getResponse(final String fileName) {
        try {
            final File file = new File( this.getClass().getResource(fileName).getFile() );
            return Files.lines(file.toPath()).collect(Collectors.joining("\n"));
        } catch (final IOException ioe) {
            Assert.fail("Could not parse mocked response.");
            return null;
        }
    }
}

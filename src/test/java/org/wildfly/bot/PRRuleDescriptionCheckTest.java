package org.wildfly.bot;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.wildfly.bot.utils.PullRequestJson;
import org.wildfly.bot.utils.Util;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.utils.TestConstants;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;

/**
 * Tests for the Wildfly -> Rules -> Body checks.
 */
@QuarkusTest
@GitHubAppTest
public class PRRuleDescriptionCheckTest {

    private static String wildflyConfigFile;
    private static PullRequestJson pullRequestJson;

    @Test
    void testSuccessfulBodyCheck() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Description"
                      body: "issues.redhat.com"
                      notify: [7125767235]
                """;

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @7125767235");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void testFailedBodyCheck() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON)
                .description(TestConstants.INVALID_DESCRIPTION)
                .build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Description"
                      body: "issues.redhat.com"
                      notify: [7125767235]
                """;

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR, Mockito.never()).comment("/cc @7125767235");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
                });
    }

    @Test
    void testTitleBodyCheckForBodyCaseInsensitive() throws IOException {
        pullRequestJson = PullRequestJson.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
        wildflyConfigFile = """
                wildfly:
                  rules:
                    - id: "Description"
                      body: "ISSUES.REDHAT.COM"
                      notify: [7125767235]
                """;

        given().github(mocks -> Util.mockRepo(mocks, wildflyConfigFile, pullRequestJson))
                .when().payloadFromString(pullRequestJson.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    GHPullRequest mockedPR = mocks.pullRequest(pullRequestJson.id());
                    Mockito.verify(mockedPR).comment("/cc @7125767235");
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    Util.verifyFormatSuccess(repo, pullRequestJson);
                });
    }
}

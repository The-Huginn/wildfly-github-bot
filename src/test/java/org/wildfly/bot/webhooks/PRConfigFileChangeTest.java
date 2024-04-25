package org.wildfly.bot.webhooks;

import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;
import org.wildfly.bot.model.RuntimeConstants;
import org.wildfly.bot.utils.TestConstants;
import org.wildfly.bot.utils.mocking.MockedGHPullRequest;
import org.wildfly.bot.utils.model.SsePullRequestPayload;

import java.io.IOException;
import java.util.List;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.wildfly.bot.model.RuntimeConstants.FAILED_CONFIGFILE_COMMENT;

/**
 * Tests for PRs changing the configuration file.
 */
@QuarkusTest
@GitHubAppTest
public class PRConfigFileChangeTest {

    private static SsePullRequestPayload ssePullRequestPayload;

    @BeforeAll
    static void setUpGitHubJson() throws IOException {
        ssePullRequestPayload = SsePullRequestPayload.builder(TestConstants.VALID_PR_TEMPLATE_JSON).build();
    }

    @Test
    void testUpdateIncorrectlyIndentedFile() throws IOException {
        given().github(mocks -> {
            GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
            GHContent mockGHContent = mock(GHContent.class);
            when(repo.getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, ssePullRequestPayload.commitSHA()))
                    .thenReturn(mockGHContent);
            when(mockGHContent.read()).thenReturn(IOUtils.toInputStream("""
                    wildfly:
                      rules:
                        - title: "test"
                        - body: "test"
                           notify: [The-non-existing-user]
                      emails:
                        - foo@bar.baz
                        - address@email.com""",
                    "UTF-8"));

            MockedGHPullRequest.builder(ssePullRequestPayload.id())
                    .files(".github/wildfly-bot.yml")
                    .mock(mocks);
        })
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(ssePullRequestPayload.id())).listFiles();
                    verify(mocks.pullRequest(ssePullRequestPayload.id())).listComments();
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    verify(repo).getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, ssePullRequestPayload.commitSHA());
                    Mockito.verify(repo).createCommitStatus(ssePullRequestPayload.commitSHA(),
                            GHCommitState.ERROR, "", "Unable to parse the configuration file. " +
                                    "Make sure it can be loaded to model at https://github.com/wildfly/wildfly-github-bot/blob/main/CONFIGURATION.yml",
                            "Configuration File");

                    verifyNoMoreInteractions(mocks.pullRequest(ssePullRequestPayload.id()));
                });
    }

    @Test
    void testUpdateToCorrectFile() throws IOException {
        given().github(mocks -> {
            GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
            GHContent mockGHContent = mock(GHContent.class);
            when(repo.getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, ssePullRequestPayload.commitSHA()))
                    .thenReturn(mockGHContent);
            when(mockGHContent.read()).thenReturn(IOUtils.toInputStream("""
                    wildfly:
                      rules:
                        - title: "test"
                          id: "some test id"
                        - id: "another great id"
                          body: "test"
                          notify: [The-non-existing-user]
                      emails:
                        - foo@bar.baz
                        - address@email.com""",
                    "UTF-8"));

            MockedGHPullRequest.builder(ssePullRequestPayload.id())
                    .files(".github/wildfly-bot.yml")
                    .mock(mocks);
        })
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(ssePullRequestPayload.id())).listFiles();
                    verify(mocks.pullRequest(ssePullRequestPayload.id()), times(2)).listComments();
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    verify(repo).getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, ssePullRequestPayload.commitSHA());
                    Mockito.verify(repo).createCommitStatus(ssePullRequestPayload.commitSHA(),
                            GHCommitState.SUCCESS, "", "Valid", "Configuration File");

                    verifyNoMoreInteractions(mocks.pullRequest(ssePullRequestPayload.id()));
                });
    }

    @Test
    void testUpdateWithIncorrectRulesFile() throws IOException {
        given().github(mocks -> {
            GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
            GHContent mockGHContent = mock(GHContent.class);
            when(repo.getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, ssePullRequestPayload.commitSHA()))
                    .thenReturn(mockGHContent);
            when(mockGHContent.read()).thenReturn(IOUtils.toInputStream("""
                    wildfly:
                      rules:
                        - title: "test"
                        - body: "test"
                          notify: [The-non-existing-user]
                        - id: "directory-error"
                          directories: [non-existing]
                      emails:
                        - foo@bar.baz
                        - address@email.com""",
                    "UTF-8"));

            MockedGHPullRequest.builder(ssePullRequestPayload.id())
                    .files(".github/wildfly-bot.yml")
                    .mock(mocks);
        })
                .when().payloadFromString(ssePullRequestPayload.jsonString())
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
                    verify(mocks.pullRequest(ssePullRequestPayload.id())).listFiles();
                    verify(mocks.pullRequest(ssePullRequestPayload.id()), times(2)).listComments();
                    verify(mocks.pullRequest(ssePullRequestPayload.id())).comment(FAILED_CONFIGFILE_COMMENT.formatted(
                            String.join("\n\n",
                                    List.of(
                                            "- [WARN] - Rule [title=test] is missing an id",
                                            "- [WARN] - Rule [body=test, notify=[The-non-existing-user]] is missing an id",
                                            "- [ERROR] - Rule [id=directory-error, directories=[non-existing]] has the following non-existing directory specified: non-existing"))));
                    GHRepository repo = mocks.repository(TestConstants.TEST_REPO);
                    verify(repo).getFileContent(".github/" + RuntimeConstants.CONFIG_FILE_NAME, ssePullRequestPayload.commitSHA());
                    Mockito.verify(repo).createCommitStatus(ssePullRequestPayload.commitSHA(),
                            GHCommitState.ERROR, "",
                            "One or multiple rules are invalid, please see the comment stating the problems",
                            "Configuration File");
                });
    }
}

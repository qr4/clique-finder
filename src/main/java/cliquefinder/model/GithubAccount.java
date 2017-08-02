package cliquefinder.model;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * In our usecase a github account consists only of a name and an array of joined organizations.
 * Created by qr4 on 26.07.17.
 */
@ParametersAreNonnullByDefault
public class GithubAccount {
    private final String name;
    private final GithubOrganization[] githubOrganizations;

    public GithubAccount(final String name, final GithubOrganization[] githubOrganizations) {
        this.name = name;
        this.githubOrganizations = githubOrganizations;
    }

    public String getName() {
        return name;
    }

    public GithubOrganization[] getGithubOrganizations() {
        return githubOrganizations;
    }
}

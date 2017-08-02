package cliquefinder.model;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A small class which is used for deserialization of the github result json.
 * Created by qr4 on 30.07.17.
 */
@ParametersAreNonnullByDefault
public class GithubOrganization {
    private String id;

    public GithubOrganization(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}

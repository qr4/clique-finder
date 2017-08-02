package cliquefinder.model;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * In our usecase a twitter account only consists of a name and a list of followersIds.
 * Created by qr4 on 30.07.17.
 */
@ParametersAreNonnullByDefault
public class TwitterAccount {
    private final String name;
    private final String[] followersIds;

    public TwitterAccount(String name, String[] followersIds) {
        this.name = name;
        this.followersIds = followersIds;
    }

    public String getName() {
        return name;
    }

    public String[] getFollowersIds() {
        return followersIds;
    }
}

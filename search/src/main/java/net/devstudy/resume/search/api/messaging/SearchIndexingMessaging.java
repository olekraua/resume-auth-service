package net.devstudy.resume.search.api.messaging;

public final class SearchIndexingMessaging {
    public static final String EXCHANGE = "resume.profile.search";
    public static final String QUEUE = "resume.search.indexing";
    public static final String ROUTING_KEY_INDEX = "profile.index";
    public static final String ROUTING_KEY_REMOVE = "profile.remove";

    private SearchIndexingMessaging() {
    }
}

package net.devstudy.resume.auth.api.service;

import java.util.List;

public interface UidSuggestionService {

    List<String> suggest(String baseUid);
}

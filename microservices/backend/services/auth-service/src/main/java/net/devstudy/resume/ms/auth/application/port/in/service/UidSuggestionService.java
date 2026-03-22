package net.devstudy.resume.ms.auth.application.port.in.service;

import java.util.List;

public interface UidSuggestionService {

    List<String> suggest(String baseUid);
}

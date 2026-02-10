package net.devstudy.resume.search.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import net.devstudy.resume.search.internal.document.ProfileSearchDocument;

public interface SearchQueryService {

    Page<ProfileSearchDocument> search(String query, Pageable pageable);
}

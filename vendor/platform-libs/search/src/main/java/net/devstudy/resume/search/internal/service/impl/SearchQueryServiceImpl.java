package net.devstudy.resume.search.internal.service.impl;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import lombok.RequiredArgsConstructor;
import net.devstudy.resume.search.api.service.SearchQueryService;
import net.devstudy.resume.search.internal.document.ProfileSearchDocument;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class SearchQueryServiceImpl implements SearchQueryService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public Page<ProfileSearchDocument> search(String query, Pageable pageable) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            return Page.empty(pageable);
        }
        NativeQuery esQuery = NativeQuery.builder()
                .withQuery(b -> b.multiMatch(mm -> mm
                        .query(q)
                        .fields("fullName^3", "firstName^3", "lastName^3",
                                "summary.en", "summary.uk",
                                "objective.en", "objective.uk",
                                "info.en", "info.uk",
                                "skills.en", "skills.uk")
                        .type(TextQueryType.BestFields)))
                .withPageable(pageable)
                .build();

        SearchHits<ProfileSearchDocument> hits = elasticsearchOperations.search(esQuery, ProfileSearchDocument.class);
        if (hits.isEmpty()) {
            return Page.empty(pageable);
        }
        List<ProfileSearchDocument> docs = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
        return new PageImpl<>(docs, pageable, hits.getTotalHits());
    }
}

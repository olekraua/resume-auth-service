package net.devstudy.resume.search.internal.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import lombok.RequiredArgsConstructor;
import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.search.internal.repository.search.ProfileSearchRepository;
import net.devstudy.resume.profile.api.service.ProfileReadService;
import net.devstudy.resume.search.internal.document.ProfileSearchDocument;
import net.devstudy.resume.search.internal.mapper.ProfileSearchMapper;
import net.devstudy.resume.profile.api.service.ProfileSearchService;

@Service
@ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(name = "app.search.profile-db.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ProfileSearchServiceImpl implements ProfileSearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileSearchServiceImpl.class);

    private final ProfileReadService profileReadService;
    private final ProfileSearchRepository profileSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ProfileSearchMapper profileSearchMapper;

    @Override
    public Page<Profile> search(String query, Pageable pageable) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            // занадто короткий запит: повертаємо всі профілі без фільтра
            return profileReadService.findAll(pageable);
        }
        // multi_match по основних текстових полях
        NativeQuery esQuery = NativeQuery.builder()
                .withQuery(b -> b.multiMatch(mm -> mm
                        .query(q)
                        .fields("fullName^3", "firstName^3", "lastName^3",
                                "summary.en", "summary.uk",
                                "objective.en", "objective.uk",
                                "info.en", "info.uk",
                                "skills.en", "skills.uk")
                        .type(TextQueryType.PhrasePrefix)))
                .withPageable(pageable)
                .build();

        SearchHits<ProfileSearchDocument> hits = elasticsearchOperations.search(esQuery, ProfileSearchDocument.class);
        if (hits.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> ids = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(ProfileSearchDocument::getId)
                .toList();

        List<Profile> profiles = profileReadService.findAllById(ids);
        Map<Long, Profile> byId = profiles.stream()
                .collect(Collectors.toMap(Profile::getId, p -> p));

        List<Profile> ordered = ids.stream()
                .map(byId::get)
                .filter(p -> p != null)
                .toList();

        return new PageImpl<>(ordered, pageable, hits.getTotalHits());
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexAll() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(ProfileSearchDocument.class);
        if (indexOperations.exists()) {
            indexOperations.delete();
        }
        indexOperations.createWithMapping();
        List<Profile> profiles = profileReadService.findAllForIndexing();
        indexProfiles(profiles);
    }

    @Override
    public void indexProfiles(List<Profile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return;
        }
        List<ProfileSearchDocument> docs = profiles.stream()
                .map(profileSearchMapper::toDocument)
                .toList();
        profileSearchRepository.saveAll(docs);
    }

    @Override
    public void removeProfile(Long profileId) {
        if (profileId == null) {
            return;
        }
        try {
            profileSearchRepository.deleteById(profileId);
        } catch (Exception ex) {
            LOGGER.warn("Elasticsearch delete failed: {}", ex.getMessage());
        }
    }
}

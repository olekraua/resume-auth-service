package net.devstudy.resume.search.internal.repository.search;

import net.devstudy.resume.search.internal.document.ProfileSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProfileSearchRepository extends ElasticsearchRepository<ProfileSearchDocument, Long> {
}

package net.devstudy.resume.search.internal.web;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.search.api.dto.PageResponse;
import net.devstudy.resume.search.api.dto.ProfileSummary;
import net.devstudy.resume.search.api.service.SearchQueryService;
import net.devstudy.resume.search.internal.document.ProfileSearchDocument;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.search.query-api.enabled", havingValue = "true")
public class SearchQueryApiController {

    private static final int MAX_PAGE_SIZE = 50;

    private final SearchQueryService searchQueryService;

    @GetMapping("/api/search")
    public PageResponse<ProfileSummary> search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", required = false) Integer size) {
        int safePage = Math.max(0, page);
        int safeSize = normalizeSize(size);
        String normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty()) {
            return new PageResponse<>(List.of(), safePage, safeSize, 0, 0, false);
        }
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);
        Page<ProfileSearchDocument> result = searchQueryService.search(normalized, pageRequest);
        List<ProfileSummary> items = result.getContent().stream()
                .map(ProfileSummary::from)
                .toList();
        return new PageResponse<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    @GetMapping("/api/suggest")
    public List<SuggestItem> suggest(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "limit", defaultValue = "5") int limit) {

        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        int size = Math.max(1, Math.min(limit, 50));
        Page<ProfileSearchDocument> result = searchQueryService.search(query.trim(), PageRequest.of(0, size));
        return result.getContent().stream()
                .map(doc -> new SuggestItem(doc.getUid(),
                        normalizeFullName(doc.getFullName(), doc.getFirstName(), doc.getLastName())))
                .toList();
    }

    private int normalizeSize(Integer size) {
        int effective = size == null ? 20 : size;
        return Math.max(1, Math.min(effective, MAX_PAGE_SIZE));
    }

    private String normalizeFullName(String fullName, String firstName, String lastName) {
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim();
        }
        String combined = String.format("%s %s",
                firstName == null ? "" : firstName.trim(),
                lastName == null ? "" : lastName.trim()).trim();
        return combined;
    }

    public record SuggestItem(String uid, String fullName) {
    }
}

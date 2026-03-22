package net.devstudy.resume.web.controller.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.auth.api.security.CurrentProfileProvider;
import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.profile.api.model.ProfileConnection;
import net.devstudy.resume.profile.api.model.ProfileConnectionState;
import net.devstudy.resume.profile.api.model.ProfileConnectionStatus;
import net.devstudy.resume.profile.api.service.ProfileConnectionService;
import net.devstudy.resume.profile.api.service.ProfileReadService;
import net.devstudy.resume.web.api.ApiErrorUtils;

@RestController
@RequestMapping("/api/profile/connections")
@RequiredArgsConstructor
public class ProfileConnectionApiController {

    private final ProfileConnectionService profileConnectionService;
    private final ProfileReadService profileReadService;
    private final CurrentProfileProvider currentProfileProvider;
    private final Executor connectionExecutor;

    @PostMapping
    public ResponseEntity<?> requestConnectionBody(@RequestBody(required = false) ConnectionActionRequest payload,
            HttpServletRequest request) {
        String uid = payload == null ? null : payload.uid();
        String action = normalizeAction(payload == null ? null : payload.action());
        if (!action.isEmpty() && !isRequestAction(action)) {
            return ApiErrorUtils.error(HttpStatus.BAD_REQUEST, "Unsupported action", request);
        }
        return requestConnectionInternal(uid, request);
    }

    @PostMapping("/{uid}")
    public ResponseEntity<?> requestConnectionLegacy(@PathVariable String uid, HttpServletRequest request) {
        return requestConnectionInternal(uid, request);
    }

    @PostMapping("/requests/{uid}")
    public ResponseEntity<?> requestConnection(@PathVariable String uid, HttpServletRequest request) {
        return requestConnectionInternal(uid, request);
    }

    @PutMapping
    public ResponseEntity<?> acceptRequestBody(@RequestBody(required = false) ConnectionActionRequest payload,
            HttpServletRequest request) {
        String uid = payload == null ? null : payload.uid();
        String action = normalizeAction(payload == null ? null : payload.action());
        if (!action.isEmpty() && !isAcceptAction(action)) {
            return ApiErrorUtils.error(HttpStatus.BAD_REQUEST, "Unsupported action", request);
        }
        return acceptRequestInternal(uid, request);
    }

    @PutMapping("/{uid}")
    public ResponseEntity<?> acceptRequestLegacy(@PathVariable String uid, HttpServletRequest request) {
        return acceptRequestInternal(uid, request);
    }

    @PostMapping("/requests/{uid}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable String uid, HttpServletRequest request) {
        return acceptRequestInternal(uid, request);
    }

    @PostMapping("/requests/{uid}/decline")
    public ResponseEntity<?> declineRequest(@PathVariable String uid, HttpServletRequest request) {
        return declineRequestInternal(uid, request);
    }

    @DeleteMapping("/requests/{uid}")
    public ResponseEntity<?> withdrawRequest(@PathVariable String uid, HttpServletRequest request) {
        return withdrawRequestInternal(uid, request);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteConnection(@RequestBody(required = false) ConnectionActionRequest payload,
            HttpServletRequest request) {
        String uid = payload == null ? null : payload.uid();
        String action = normalizeAction(payload == null ? null : payload.action());
        if (action.isEmpty()) {
            return ApiErrorUtils.error(HttpStatus.BAD_REQUEST, "Action is required", request);
        }
        if (isDeclineAction(action)) {
            return declineRequestInternal(uid, request);
        }
        if (isWithdrawAction(action)) {
            return withdrawRequestInternal(uid, request);
        }
        if (isRemoveAction(action)) {
            return removeConnectionInternal(uid, request);
        }
        return ApiErrorUtils.error(HttpStatus.BAD_REQUEST, "Unsupported action", request);
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<?> removeConnection(@PathVariable String uid, HttpServletRequest request) {
        return removeConnectionInternal(uid, request);
    }

    @GetMapping
    public ResponseEntity<?> listConnections(HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        List<ProfileConnection> connections = profileConnectionService.listConnections(currentId);
        return ResponseEntity.ok(toConnectionItems(connections, currentId, ProfileConnectionState.CONNECTED));
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<?> listIncoming(HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        List<ProfileConnection> connections = profileConnectionService.listIncomingRequests(currentId);
        return ResponseEntity.ok(toConnectionItems(connections, currentId, ProfileConnectionState.INCOMING_REQUEST));
    }

    @GetMapping("/requests/outgoing")
    public ResponseEntity<?> listOutgoing(HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        List<ProfileConnection> connections = profileConnectionService.listOutgoingRequests(currentId);
        return ResponseEntity.ok(toConnectionItems(connections, currentId, ProfileConnectionState.OUTGOING_REQUEST));
    }

    @GetMapping("/overview")
    public ResponseEntity<?> overview(HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        CompletableFuture<List<ConnectionItem>> incomingFuture = CompletableFuture.supplyAsync(
                () -> toConnectionItems(profileConnectionService.listIncomingRequests(currentId),
                        currentId,
                        ProfileConnectionState.INCOMING_REQUEST),
                connectionExecutor
        );
        CompletableFuture<List<ConnectionItem>> outgoingFuture = CompletableFuture.supplyAsync(
                () -> toConnectionItems(profileConnectionService.listOutgoingRequests(currentId),
                        currentId,
                        ProfileConnectionState.OUTGOING_REQUEST),
                connectionExecutor
        );
        CompletableFuture<List<ConnectionItem>> connectionsFuture = CompletableFuture.supplyAsync(
                () -> toConnectionItems(profileConnectionService.listConnections(currentId),
                        currentId,
                        ProfileConnectionState.CONNECTED),
                connectionExecutor
        );
        CompletableFuture.allOf(incomingFuture, outgoingFuture, connectionsFuture).join();
        ConnectionOverview overview = new ConnectionOverview(
                safeJoin(connectionsFuture),
                safeJoin(incomingFuture),
                safeJoin(outgoingFuture)
        );
        return ResponseEntity.ok(overview);
    }

    private Profile findProfileOrThrow(String uid) {
        if (uid == null || uid.isBlank()) {
            throw new IllegalArgumentException("Profile uid is required");
        }
        return profileReadService.findByUid(uid.trim())
                .orElseThrow(() -> new EntityNotFoundException("Profile not found: " + uid));
    }

    private List<ConnectionItem> toConnectionItems(List<ProfileConnection> connections,
            Long currentId,
            ProfileConnectionState state) {
        if (connections == null || connections.isEmpty()) {
            return List.of();
        }
        Set<Long> otherIds = connections.stream()
                .map(connection -> resolveOtherId(connection, currentId, state))
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Profile> profilesById = profileReadService.findAllById(new ArrayList<>(otherIds)).stream()
                .collect(Collectors.toMap(Profile::getId, Function.identity()));
        List<ConnectionItem> items = new ArrayList<>(connections.size());
        for (ProfileConnection connection : connections) {
            ConnectionItem item = toConnectionItem(connection, currentId, state, profilesById);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private ConnectionItem toConnectionItem(ProfileConnection connection,
            Long currentId,
            ProfileConnectionState state) {
        return toConnectionItem(connection, currentId, state, null);
    }

    private ConnectionItem toConnectionItem(ProfileConnection connection,
            Long currentId,
            ProfileConnectionState state,
            Map<Long, Profile> profilesById) {
        if (connection == null) {
            return null;
        }
        Long otherId = resolveOtherId(connection, currentId, state);
        Profile profile = profilesById == null ? profileReadService.findById(otherId).orElse(null)
                : profilesById.get(otherId);
        if (profile == null) {
            return null;
        }
        String fullName = profile.getFullName();
        if (fullName == null) {
            fullName = "";
        } else {
            fullName = fullName.trim();
        }
        return new ConnectionItem(
                profile.getUid(),
                fullName,
                profile.getSmallPhoto(),
                profile.getObjective(),
                profile.getCity(),
                profile.getCountry(),
                state,
                connection.getCreated(),
                connection.getResponded()
        );
    }

    private Long resolveOtherId(ProfileConnection connection, Long currentId, ProfileConnectionState state) {
        if (connection == null || currentId == null) {
            return null;
        }
        Long requesterId = safeProfileId(connection.getRequester());
        Long addresseeId = safeProfileId(connection.getAddressee());
        return switch (state) {
            case INCOMING_REQUEST -> requesterId;
            case OUTGOING_REQUEST -> addresseeId;
            case CONNECTED -> currentId.equals(requesterId) ? addresseeId : requesterId;
            default -> null;
        };
    }

    private ProfileConnectionState resolveRequestState(ProfileConnection connection, Long currentId) {
        if (connection == null || currentId == null) {
            return ProfileConnectionState.NONE;
        }
        if (connection.getStatus() == ProfileConnectionStatus.ACCEPTED) {
            return ProfileConnectionState.CONNECTED;
        }
        Long requesterId = safeProfileId(connection.getRequester());
        if (requesterId != null && requesterId.equals(currentId)) {
            return ProfileConnectionState.OUTGOING_REQUEST;
        }
        return ProfileConnectionState.INCOMING_REQUEST;
    }

    private Long safeProfileId(Profile profile) {
        return profile == null ? null : profile.getId();
    }

    private <T> T safeJoin(CompletableFuture<T> future) {
        return future == null ? null : future.join();
    }

    private ResponseEntity<?> requestConnectionInternal(String uid, HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        Profile target = findProfileOrThrow(uid);
        ProfileConnection connection = profileConnectionService.requestConnection(currentId, target.getId());
        ConnectionItem item = toConnectionItem(connection, currentId, resolveRequestState(connection, currentId));
        return ResponseEntity.ok(item);
    }

    private ResponseEntity<?> acceptRequestInternal(String uid, HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        Profile requester = findProfileOrThrow(uid);
        ProfileConnection connection = profileConnectionService.acceptRequest(currentId, requester.getId());
        ConnectionItem item = toConnectionItem(connection, currentId, ProfileConnectionState.CONNECTED);
        return ResponseEntity.ok(item);
    }

    private ResponseEntity<?> declineRequestInternal(String uid, HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        Profile requester = findProfileOrThrow(uid);
        profileConnectionService.declineRequest(currentId, requester.getId());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> withdrawRequestInternal(String uid, HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        Profile addressee = findProfileOrThrow(uid);
        profileConnectionService.withdrawRequest(currentId, addressee.getId());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> removeConnectionInternal(String uid, HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        Profile other = findProfileOrThrow(uid);
        profileConnectionService.removeConnection(currentId, other.getId());
        return ResponseEntity.noContent().build();
    }

    private String normalizeAction(String action) {
        if (action == null) {
            return "";
        }
        return action.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isRequestAction(String action) {
        return "connect".equals(action) || "request".equals(action) || "add".equals(action);
    }

    private boolean isAcceptAction(String action) {
        return "accept".equals(action);
    }

    private boolean isDeclineAction(String action) {
        return "decline".equals(action) || "ignore".equals(action) || "reject".equals(action);
    }

    private boolean isWithdrawAction(String action) {
        return "withdraw".equals(action) || "cancel".equals(action);
    }

    private boolean isRemoveAction(String action) {
        return "remove".equals(action) || "disconnect".equals(action);
    }

    public record ConnectionItem(
            String uid,
            String fullName,
            String smallPhoto,
            String objective,
            String city,
            String country,
            ProfileConnectionState state,
            Instant requestedAt,
            Instant respondedAt
    ) {
    }

    public record ConnectionActionRequest(String uid, String action) {
    }

    public record ConnectionOverview(
            List<ConnectionItem> connections,
            List<ConnectionItem> incomingRequests,
            List<ConnectionItem> outgoingRequests
    ) {
    }
}

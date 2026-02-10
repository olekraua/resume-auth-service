package net.devstudy.resume.profile.internal.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.profile.api.model.ProfileConnection;
import net.devstudy.resume.profile.api.model.ProfileConnectionState;
import net.devstudy.resume.profile.api.model.ProfileConnectionStatus;
import net.devstudy.resume.profile.api.service.ProfileConnectionService;
import net.devstudy.resume.profile.internal.repository.storage.ProfileConnectionRepository;
import net.devstudy.resume.profile.internal.repository.storage.ProfileRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileConnectionServiceImpl implements ProfileConnectionService {

    private final ProfileConnectionRepository connectionRepository;
    private final ProfileRepository profileRepository;

    @Override
    public Optional<ProfileConnection> findByPair(Long firstProfileId, Long secondProfileId) {
        if (firstProfileId == null || secondProfileId == null) {
            return Optional.empty();
        }
        if (firstProfileId.equals(secondProfileId)) {
            return Optional.empty();
        }
        return connectionRepository.findByPairKey(toPairKey(firstProfileId, secondProfileId));
    }

    @Override
    public ProfileConnectionState getConnectionState(Long currentProfileId, Long otherProfileId) {
        if (currentProfileId == null || otherProfileId == null) {
            return ProfileConnectionState.NONE;
        }
        if (currentProfileId.equals(otherProfileId)) {
            return ProfileConnectionState.SELF;
        }
        Optional<ProfileConnection> connection = connectionRepository
                .findByPairKey(toPairKey(currentProfileId, otherProfileId));
        if (connection.isEmpty()) {
            return ProfileConnectionState.NONE;
        }
        ProfileConnection existing = connection.get();
        if (existing.getStatus() == ProfileConnectionStatus.ACCEPTED) {
            return ProfileConnectionState.CONNECTED;
        }
        Long requesterId = safeProfileId(existing.getRequester());
        if (requesterId != null && requesterId.equals(currentProfileId)) {
            return ProfileConnectionState.OUTGOING_REQUEST;
        }
        return ProfileConnectionState.INCOMING_REQUEST;
    }

    @Override
    @Transactional
    public ProfileConnection requestConnection(Long requesterId, Long addresseeId) {
        validateIds(requesterId, addresseeId);
        Profile requester = getProfileOrThrow(requesterId);
        Profile addressee = getProfileOrThrow(addresseeId);
        String pairKey = toPairKey(requesterId, addresseeId);
        Optional<ProfileConnection> existingOpt = connectionRepository.findByPairKeyForUpdate(pairKey);
        if (existingOpt.isPresent()) {
            ProfileConnection existing = existingOpt.get();
            if (existing.getStatus() == ProfileConnectionStatus.ACCEPTED) {
                return existing;
            }
            Long existingRequesterId = safeProfileId(existing.getRequester());
            if (existingRequesterId != null && existingRequesterId.equals(requesterId)) {
                return existing;
            }
            throw new IllegalStateException("Incoming request already exists");
        }
        ProfileConnection connection = new ProfileConnection(requester, addressee,
                ProfileConnectionStatus.PENDING, Instant.now());
        connection.setPairKey(pairKey);
        try {
            return connectionRepository.save(connection);
        } catch (DataIntegrityViolationException ex) {
            return connectionRepository.findByPairKey(pairKey).orElseThrow(() -> ex);
        }
    }

    @Override
    @Transactional
    public ProfileConnection acceptRequest(Long currentProfileId, Long requesterId) {
        validateIds(currentProfileId, requesterId);
        String pairKey = toPairKey(currentProfileId, requesterId);
        ProfileConnection connection = connectionRepository.findByPairKeyForUpdate(pairKey)
                .orElseThrow(() -> new EntityNotFoundException("Connection request not found"));
        ensurePending(connection);
        Long connectionRequesterId = safeProfileId(connection.getRequester());
        Long connectionAddresseeId = safeProfileId(connection.getAddressee());
        if (!requesterId.equals(connectionRequesterId) || !currentProfileId.equals(connectionAddresseeId)) {
            throw new EntityNotFoundException("Connection request not found");
        }
        connection.setStatus(ProfileConnectionStatus.ACCEPTED);
        connection.setResponded(Instant.now());
        return connectionRepository.save(connection);
    }

    @Override
    @Transactional
    public void declineRequest(Long currentProfileId, Long requesterId) {
        validateIds(currentProfileId, requesterId);
        String pairKey = toPairKey(currentProfileId, requesterId);
        ProfileConnection connection = connectionRepository.findByPairKeyForUpdate(pairKey)
                .orElseThrow(() -> new EntityNotFoundException("Connection request not found"));
        ensurePending(connection);
        Long connectionRequesterId = safeProfileId(connection.getRequester());
        Long connectionAddresseeId = safeProfileId(connection.getAddressee());
        if (!requesterId.equals(connectionRequesterId) || !currentProfileId.equals(connectionAddresseeId)) {
            throw new EntityNotFoundException("Connection request not found");
        }
        connectionRepository.delete(connection);
    }

    @Override
    @Transactional
    public void withdrawRequest(Long currentProfileId, Long addresseeId) {
        validateIds(currentProfileId, addresseeId);
        String pairKey = toPairKey(currentProfileId, addresseeId);
        ProfileConnection connection = connectionRepository.findByPairKeyForUpdate(pairKey)
                .orElseThrow(() -> new EntityNotFoundException("Connection request not found"));
        ensurePending(connection);
        Long connectionRequesterId = safeProfileId(connection.getRequester());
        Long connectionAddresseeId = safeProfileId(connection.getAddressee());
        if (!currentProfileId.equals(connectionRequesterId) || !addresseeId.equals(connectionAddresseeId)) {
            throw new EntityNotFoundException("Connection request not found");
        }
        connectionRepository.delete(connection);
    }

    @Override
    @Transactional
    public void removeConnection(Long currentProfileId, Long otherProfileId) {
        validateIds(currentProfileId, otherProfileId);
        String pairKey = toPairKey(currentProfileId, otherProfileId);
        ProfileConnection connection = connectionRepository.findByPairKeyForUpdate(pairKey)
                .orElseThrow(() -> new EntityNotFoundException("Connection not found"));
        if (connection.getStatus() != ProfileConnectionStatus.ACCEPTED) {
            throw new EntityNotFoundException("Connection not found");
        }
        connectionRepository.delete(connection);
    }

    @Override
    public List<ProfileConnection> listIncomingRequests(Long profileId) {
        if (profileId == null) {
            return List.of();
        }
        return connectionRepository.findIncomingByStatus(profileId, ProfileConnectionStatus.PENDING);
    }

    @Override
    public List<ProfileConnection> listOutgoingRequests(Long profileId) {
        if (profileId == null) {
            return List.of();
        }
        return connectionRepository.findOutgoingByStatus(profileId, ProfileConnectionStatus.PENDING);
    }

    @Override
    public List<ProfileConnection> listConnections(Long profileId) {
        if (profileId == null) {
            return List.of();
        }
        return connectionRepository.findAcceptedByProfile(profileId, ProfileConnectionStatus.ACCEPTED);
    }

    private void ensurePending(ProfileConnection connection) {
        if (connection.getStatus() != ProfileConnectionStatus.PENDING) {
            throw new EntityNotFoundException("Connection request not found");
        }
    }

    private void validateIds(Long firstId, Long secondId) {
        if (firstId == null || secondId == null) {
            throw new IllegalArgumentException("Profile id is required");
        }
        if (firstId.equals(secondId)) {
            throw new IllegalArgumentException("Cannot connect to yourself");
        }
    }

    private Profile getProfileOrThrow(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found: " + profileId));
    }

    private String toPairKey(Long firstId, Long secondId) {
        long min = Math.min(firstId, secondId);
        long max = Math.max(firstId, secondId);
        return min + ":" + max;
    }

    private Long safeProfileId(Profile profile) {
        return profile == null ? null : profile.getId();
    }
}

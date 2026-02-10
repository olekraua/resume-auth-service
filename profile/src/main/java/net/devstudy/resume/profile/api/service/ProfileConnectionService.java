package net.devstudy.resume.profile.api.service;

import java.util.List;
import java.util.Optional;

import net.devstudy.resume.profile.api.model.ProfileConnection;
import net.devstudy.resume.profile.api.model.ProfileConnectionState;

public interface ProfileConnectionService {

    Optional<ProfileConnection> findByPair(Long firstProfileId, Long secondProfileId);

    ProfileConnectionState getConnectionState(Long currentProfileId, Long otherProfileId);

    ProfileConnection requestConnection(Long requesterId, Long addresseeId);

    ProfileConnection acceptRequest(Long currentProfileId, Long requesterId);

    void declineRequest(Long currentProfileId, Long requesterId);

    void withdrawRequest(Long currentProfileId, Long addresseeId);

    void removeConnection(Long currentProfileId, Long otherProfileId);

    List<ProfileConnection> listIncomingRequests(Long profileId);

    List<ProfileConnection> listOutgoingRequests(Long profileId);

    List<ProfileConnection> listConnections(Long profileId);
}

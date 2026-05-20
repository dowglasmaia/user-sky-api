package com.luciodowglas.userapi.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.luciodowglas.userapi.entity.UserProjectLink;
import com.luciodowglas.userapi.entity.UserProjectLinkId;

public interface UserProjectLinkRepository extends JpaRepository<UserProjectLink, UserProjectLinkId> {

    List<UserProjectLink> findAllByIdUserId(UUID userId);

    boolean existsByIdUserIdAndIdProjectId(UUID userId, UUID projectId);

    void deleteByIdUserIdAndIdProjectId(UUID userId, UUID projectId);
}

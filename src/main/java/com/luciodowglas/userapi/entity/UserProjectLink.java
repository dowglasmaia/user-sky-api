package com.luciodowglas.userapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Snapshot of an external project linked to a user.
 *
 * <p>The project itself lives in the projects-api service; this row records
 * the association plus a local copy of the project's display fields so
 * lookups by the user (GET /users/{id}/projects) don't require a network call.
 * Treat the snapshot as eventually consistent: refresh on link creation only.</p>
 */
@Entity
@Table(name = "tb_user_external_project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProjectLink {

    @EmbeddedId
    private UserProjectLinkId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}

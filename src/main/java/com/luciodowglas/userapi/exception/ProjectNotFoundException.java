package com.luciodowglas.userapi.exception;

import java.util.UUID;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(UUID projectId) {
        super("Project not found in external catalog: " + projectId);
    }

    public ProjectNotFoundException(UUID projectId, UUID userId) {
        super("Project '" + projectId + "' not linked to user: " + userId);
    }
}

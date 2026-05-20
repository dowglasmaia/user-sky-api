package com.luciodowglas.userapi.exception;

import java.util.UUID;

public class ProjectAlreadyExistsException extends RuntimeException {

    public ProjectAlreadyExistsException(UUID projectId, UUID userId) {
        super("Project '" + projectId + "' is already linked to user: " + userId);
    }
}

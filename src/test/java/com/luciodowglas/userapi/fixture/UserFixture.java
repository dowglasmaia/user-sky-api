package com.luciodowglas.userapi.fixture;

import com.luciodowglas.userapi.entity.User;
import com.luciodowglas.userapi.entity.UserProjectLink;
import com.luciodowglas.userapi.entity.UserProjectLinkId;
import com.luciodowglas.userapi.integration.project.dto.ExternalProjectDto;
import com.luciodowglas.userapi.security.RoleEnum;

import br.com.luciodowglas.openapi.model.CreateUserRequest;
import br.com.luciodowglas.openapi.model.ExternalProjectResponse;
import br.com.luciodowglas.openapi.model.LinkProjectRequest;
import br.com.luciodowglas.openapi.model.LoginRequest;
import br.com.luciodowglas.openapi.model.UpdateUserRequest;
import br.com.luciodowglas.openapi.model.UserPageResponse;
import br.com.luciodowglas.openapi.model.UserProjectsResponse;
import br.com.luciodowglas.openapi.model.UserResponse;
import br.com.luciodowglas.openapi.model.UserRole;
import br.com.luciodowglas.openapi.model.UserRole;

import java.util.List;
import java.util.UUID;

public final class UserFixture {

    public static final UUID USER_ID    = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    public static final UUID PROJECT_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
    public static final String USER_EMAIL   = "alice@test.com";
    public static final String ADMIN_EMAIL  = "admin@test.com";
    public static final String RAW_PASSWORD = "Secret123!";

    private UserFixture() {}

    public static User aUser() {
        return User.builder()
                .id(USER_ID)
                .email(USER_EMAIL)
                .name("Alice")
                .password("$2a$12$encodedpasswordhash")
                .role(RoleEnum.ROLE_USER)
                .build();
    }

    public static User anAdminUser() {
        return User.builder()
                .id(UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001"))
                .email(ADMIN_EMAIL)
                .name("Admin")
                .password("$2a$12$encodedpasswordhash")
                .role(RoleEnum.ROLE_ADMIN)
                .build();
    }

    public static UserProjectLink aUserProjectLink() {
        return UserProjectLink.builder()
                .id(new UserProjectLinkId(USER_ID, PROJECT_ID))
                .user(aUser())
                .name("Atlas")
                .description("Customer platform")
                .build();
    }

    public static CreateUserRequest aCreateUserRequest() {
        return new CreateUserRequest()
                .name("Alice")
                .email(USER_EMAIL)
                .password(RAW_PASSWORD);
    }

    public static CreateUserRequest aCreateUserRequestWithRole(UserRole role) {
        return new CreateUserRequest()
                .name("Alice")
                .email(USER_EMAIL)
                .password(RAW_PASSWORD)
                .role(role);
    }

    public static UpdateUserRequest anUpdateUserRequest() {
        return new UpdateUserRequest()
                .email("updated@test.com")
                .name("Alice Updated");
    }

    public static LinkProjectRequest aLinkProjectRequest() {
        return new LinkProjectRequest()
                .description("Custom override desc");
    }

    public static UserResponse aUserResponse() {
        return new UserResponse()
                .id(USER_ID)
                .name("Alice")
                .email(USER_EMAIL);
    }

    public static UserPageResponse aUserPageResponse() {
        return new UserPageResponse()
                .content(List.of(aUserResponse()))
                .totalElements(1L)
                .totalPages(1)
                .page(0)
                .size(20);
    }

    public static ExternalProjectDto anExternalProjectDto() {
        return new ExternalProjectDto(PROJECT_ID, "Atlas", "Customer platform");
    }

    public static ExternalProjectResponse anExternalProjectResponse() {
        return new ExternalProjectResponse()
                .id(PROJECT_ID)
                .name("Atlas")
                .description("Customer platform");
    }

    public static UserProjectsResponse aUserProjectsResponse() {
        return new UserProjectsResponse()
                .user("Alice")
                .projects(List.of(anExternalProjectResponse()));
    }

    public static LoginRequest aLoginRequest() {
        return new LoginRequest()
                .email(USER_EMAIL)
                .password(RAW_PASSWORD);
    }
}

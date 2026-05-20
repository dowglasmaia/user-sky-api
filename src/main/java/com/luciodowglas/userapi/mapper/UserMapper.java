package com.luciodowglas.userapi.mapper;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.luciodowglas.userapi.entity.User;

import br.com.luciodowglas.openapi.model.UserPageResponse;
import br.com.luciodowglas.openapi.model.UserResponse;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail());
    }

    public UserPageResponse toPageResponse(Page<User> page) {
        return new UserPageResponse()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize());
    }
}

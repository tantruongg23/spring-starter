package com.example.springstarter.converter;

import com.example.springstarter.domain.enumerate.Role;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter extends PostgreSQLEnumConverter<Role> {
    public RoleConverter() {
        super(Role.class);
    }
}

package com.icesi.cuidapetproyect.api;

import com.icesi.cuidapetproyect.api.dto.*;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiDtoValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void rejectsInvalidEmailShortPasswordAndEmptyRoles() {
        var errors = validator.validate(new UserCreateRequest("incorrecto", "123", List.of()));
        assertEquals(3, errors.size());
    }

    @Test
    void rejectsNullAndNonpositiveRelationshipIds() {
        assertFalse(validator.validate(new UserCreateRequest(
                "juan@example.com", "password123", Arrays.asList(1L, null))).isEmpty());
        assertFalse(validator.validate(new RoleCreateRequest("OWNER", null, List.of(0L))).isEmpty());
        assertFalse(validator.validate(new UserRoleRequest(null, -1L)).isEmpty());
        assertFalse(validator.validate(new RolePermissionRequest(1L, 0L)).isEmpty());
    }

    @Test
    void validatesUpdateStatusAndOptionalPassword() {
        assertTrue(validator.validate(new UserUpdateRequest(
                "juan@example.com", null, "ACTIVE")).isEmpty());
        assertFalse(validator.validate(new UserUpdateRequest(
                "juan@example.com", "", "ACTIVE")).isEmpty());
        assertFalse(validator.validate(new UserUpdateRequest(
                "juan@example.com", null, "ADMIN")).isEmpty());
        assertFalse(validator.validate(new RoleUpdateRequest("OWNER", null, "yes")).isEmpty());
    }

    @Test
    void respectsDatabaseTextLimits() {
        assertFalse(validator.validate(new RoleCreateRequest("R".repeat(61), null, List.of(1L))).isEmpty());
        assertFalse(validator.validate(new PermissionCreateRequest(
                "P".repeat(101), "Permiso", null)).isEmpty());
        assertFalse(validator.validate(new PermissionUpdateRequest(
                "Permiso", "D".repeat(301))).isEmpty());
    }

    @Test
    void acceptsValidRequestsForAllFiveEntities() {
        List<Object> requests = List.of(
                new UserCreateRequest("juan@example.com", "password123", List.of(1L)),
                new RoleCreateRequest("OWNER", null, List.of(1L)),
                new RoleUpdateRequest("OWNER", "Dueño", "Y"),
                new PermissionCreateRequest("READ_PETS", "Ver mascotas", null),
                new PermissionUpdateRequest("Ver mascotas", null),
                new UserRoleRequest(1L, 2L),
                new RolePermissionRequest(2L, 3L));
        for (Object request : requests) {
            assertTrue(validator.validate(request).isEmpty(), request.getClass().getSimpleName());
        }
    }
}

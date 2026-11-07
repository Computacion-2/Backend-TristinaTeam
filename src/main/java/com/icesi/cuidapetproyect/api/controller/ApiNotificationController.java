package com.icesi.cuidapetproyect.api.controller;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.service.AppNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notificaciones")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso sobre el recurso"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado"),
        @ApiResponse(responseCode = "409", description = "Destinatario inactivo")
})
public class ApiNotificationController {
    private final AppNotificationService service;

    @GetMapping
    @Operation(summary = "Listar notificaciones propias visibles para el usuario")
    public List<NotificationResponse> findAll(Authentication auth) {
        return service.findAll(auth);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una notificación por su identificador")
    public NotificationResponse findById(@PathVariable @Positive Long id, Authentication auth) {
        return service.findById(id, auth);
    }

    @PostMapping
    @Operation(summary = "Crear una notificación para un usuario (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Recurso creado; URL en Location")
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationCreateRequest dto, Authentication auth) {
        var response = service.create(dto, auth);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Marcar como leída o no leída; ADMIN también corrige el contenido")
    public NotificationResponse update(@PathVariable @Positive Long id, @Valid @RequestBody NotificationUpdateRequest dto,
                             Authentication auth) {
        return service.update(id, dto, auth);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una notificación")
    @ApiResponse(responseCode = "204", description = "Recurso eliminado")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id, Authentication auth) {
        service.delete(id, auth);
        return ResponseEntity.noContent().build();
    }
}

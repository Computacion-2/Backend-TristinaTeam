package com.icesi.cuidapetproyect.api.controller;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.service.BookingStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.List;

@RestController
@RequestMapping("/api/v1/booking-statuses")
@RequiredArgsConstructor
@Tag(name = "Estados de reserva")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso sobre el recurso"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado"),
        @ApiResponse(responseCode = "409", description = "Estado duplicado o en uso")
})
public class ApiBookingStatusController {
    private final BookingStatusService service;

    @GetMapping
    @Operation(summary = "Listar los estados de reserva del catálogo")
    public List<BookingStatusResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{code}")
    @Operation(summary = "Consultar un estado de reserva por su código")
    public BookingStatusResponse findById(@PathVariable @Pattern(regexp = "[A-Z_]{1,30}") String code) {
        return service.findById(code);
    }

    @PostMapping
    @Operation(summary = "Crear un estado de reserva (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Recurso creado; URL en Location")
    public ResponseEntity<BookingStatusResponse> create(@Valid @RequestBody BookingStatusCreateRequest dto, Authentication auth) {
        var response = service.create(dto, auth);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{code}")
                .buildAndExpand(response.statusCode()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{code}")
    @Operation(summary = "Actualizar el nombre de un estado de reserva (ADMIN)")
    public BookingStatusResponse update(@PathVariable @Pattern(regexp = "[A-Z_]{1,30}") String code, @Valid @RequestBody BookingStatusUpdateRequest dto,
                             Authentication auth) {
        return service.update(code, dto, auth);
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Eliminar un estado de reserva sin uso (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Recurso eliminado")
    public ResponseEntity<Void> delete(@PathVariable @Pattern(regexp = "[A-Z_]{1,30}") String code, Authentication auth) {
        service.delete(code, auth);
        return ResponseEntity.noContent().build();
    }
}

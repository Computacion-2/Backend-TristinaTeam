package com.icesi.cuidapetproyect.api.controller;

import com.icesi.cuidapetproyect.api.dto.*;
import com.icesi.cuidapetproyect.api.service.IncidentReportService;
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
@RequestMapping("/api/v1/incident-reports")
@RequiredArgsConstructor
@Tag(name = "Reportes de incidentes")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso sobre el recurso"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado"),
        @ApiResponse(responseCode = "409", description = "Participantes o reserva no válidos")
})
public class ApiIncidentReportController {
    private final IncidentReportService service;

    @GetMapping
    @Operation(summary = "Listar reportes de incidentes visibles para el usuario")
    public List<IncidentReportResponse> findAll(Authentication auth) {
        return service.findAll(auth);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un reporte de incidente por su identificador")
    public IncidentReportResponse findById(@PathVariable @Positive Long id, Authentication auth) {
        return service.findById(id, auth);
    }

    @PostMapping
    @Operation(summary = "Crear un reporte de incidente")
    @ApiResponse(responseCode = "201", description = "Recurso creado; URL en Location")
    public ResponseEntity<IncidentReportResponse> create(@Valid @RequestBody IncidentReportCreateRequest dto, Authentication auth) {
        var response = service.create(dto, auth);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un reporte de incidente conservando su identificador")
    public IncidentReportResponse update(@PathVariable @Positive Long id, @Valid @RequestBody IncidentReportUpdateRequest dto,
                             Authentication auth) {
        return service.update(id, dto, auth);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un reporte de incidente")
    @ApiResponse(responseCode = "204", description = "Recurso eliminado")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id, Authentication auth) {
        service.delete(id, auth);
        return ResponseEntity.noContent().build();
    }
}

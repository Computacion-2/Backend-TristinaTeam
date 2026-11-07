package com.icesi.cuidapetproyect.api.care;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CareRequestApplicationIntegrationTest extends CareFixture {

    @Test
    void bookingStatusCatalogIsReadableAndAdminManaged() throws Exception {
        send(get("/api/v1/booking-statuses"), ownerToken, null).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].statusCode", hasItem("SCHEDULED")));
        send(get("/api/v1/booking-statuses/SCHEDULED"), caregiverToken, null).andExpect(status().isOk());
        send(post("/api/v1/booking-statuses"), ownerToken, "{\"statusCode\":\"COMPLETED\",\"statusName\":\"X\"}")
                .andExpect(status().isForbidden());
        send(delete("/api/v1/booking-statuses/CANCELLED"), adminToken, null).andExpect(status().isNoContent());
        send(get("/api/v1/booking-statuses/CANCELLED"), adminToken, null).andExpect(status().isNotFound());
        send(post("/api/v1/booking-statuses"), adminToken, "{\"statusCode\":\"CANCELLED\",\"statusName\":\"Cancelada " + suffix + "\"}")
                .andExpect(status().isCreated()).andExpect(header().string("Location", endsWith("/api/v1/booking-statuses/CANCELLED")));
        send(post("/api/v1/booking-statuses"), adminToken, "{\"statusCode\":\"CANCELLED\",\"statusName\":\"Otra\"}")
                .andExpect(status().isConflict());
        send(post("/api/v1/booking-statuses"), adminToken, "{\"statusCode\":\"PAUSED\",\"statusName\":\"Pausada\"}")
                .andExpect(status().isBadRequest());
        send(put("/api/v1/booking-statuses/CANCELLED"), adminToken, "{\"statusName\":\"Cancelada por las partes " + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusName", startsWith("Cancelada por las partes")));
        var scheduledName = statuses.findById("SCHEDULED").orElseThrow().getStatusName();
        send(put("/api/v1/booking-statuses/CANCELLED"), adminToken, "{\"statusName\":\"" + scheduledName + "\"}")
                .andExpect(status().isConflict());
        send(get("/api/v1/booking-statuses/scheduled!"), adminToken, null).andExpect(status().isBadRequest());
    }

    @Test
    void ownerManagesRequestAndCaregiversSeeOnlyOpenOnes() throws Exception {
        long id = createRequest();
        String path = "/api/v1/care-requests/" + id;
        send(get(path), ownerToken, null).andExpect(status().isOk()).andExpect(jsonPath("$.petId").value(pet.getId()));
        send(get(path), caregiverToken, null).andExpect(status().isOk());
        send(get(path), otherToken, null).andExpect(status().isForbidden());
        send(get("/api/v1/care-requests"), otherToken, null).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
        send(get("/api/v1/care-requests"), caregiverToken, null).andExpect(jsonPath("$[*].id", hasItem((int) id)));
        send(put(path), ownerToken, updateBody(start.plusDays(1), "OPEN", null))
                .andExpect(status().isOk()).andExpect(jsonPath("$.city").value("Palmira"))
                .andExpect(jsonPath("$.ownerId").value(owner.getId()));
        send(put(path), otherToken, updateBody(start, "OPEN", null)).andExpect(status().isForbidden());
        send(put(path), ownerToken, updateBody(start, "CANCELLED", null)).andExpect(status().isBadRequest());
        send(put(path), ownerToken, updateBody(start, "CANCELLED", "Ya no viajo"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requestStatus").value("CANCELLED"));
        send(get(path), caregiverToken, null).andExpect(status().isForbidden());
        send(put(path), ownerToken, updateBody(start, "OPEN", null)).andExpect(status().isConflict());
        send(delete(path), otherToken, null).andExpect(status().isForbidden());
        send(delete(path), ownerToken, null).andExpect(status().isNoContent());
        send(get(path), adminToken, null).andExpect(status().isNotFound());
    }

    @Test
    void requestValidatesDatesBudgetPetOwnershipAndStatus() throws Exception {
        send(post("/api/v1/care-requests"), ownerToken, requestBody(start, start.minusHours(1), "50000"))
                .andExpect(status().isBadRequest());
        send(post("/api/v1/care-requests"), ownerToken, requestBody(start.minusDays(30), start.minusDays(29), "50000"))
                .andExpect(status().isBadRequest());
        send(post("/api/v1/care-requests"), ownerToken, requestBody(start, start.plusHours(2), "0"))
                .andExpect(status().isBadRequest());
        send(post("/api/v1/care-requests"), otherToken, requestBody(start, start.plusHours(2), "null"))
                .andExpect(status().isForbidden());
        pet.setPetStatus("INACTIVE");
        pets.saveAndFlush(pet);
        send(post("/api/v1/care-requests"), ownerToken, requestBody(start, start.plusHours(2), "null"))
                .andExpect(status().isConflict());
        send(post("/api/v1/care-requests"), adminToken, requestBody(start, start.plusHours(2), "null")
                .replace("\"petId\":" + pet.getId(), "\"petId\":999999999")).andExpect(status().isNotFound());
        long id = createRequestWithActivePet();
        send(put("/api/v1/care-requests/" + id), ownerToken, updateBody(start, "ASSIGNED", null))
                .andExpect(status().isBadRequest());
    }

    @Test
    void caregiverAppliesOnceWithinBudgetForOfferedService() throws Exception {
        long requestId = createRequest();
        long id = apply(requestId, "45000");
        String path = "/api/v1/care-applications/" + id;
        send(get(path), ownerToken, null).andExpect(status().isOk()).andExpect(jsonPath("$.caregiverId").value(caregiver.getId()));
        send(get(path), otherToken, null).andExpect(status().isForbidden());
        send(get("/api/v1/care-applications"), ownerToken, null).andExpect(jsonPath("$[*].id", hasItem((int) id)));
        send(get("/api/v1/care-applications"), secondCaregiverToken, null).andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
        send(post("/api/v1/care-applications"), caregiverToken, applicationBody(requestId, caregiver.getId(), "40000"))
                .andExpect(status().isConflict());
        send(post("/api/v1/care-applications"), secondCaregiverToken, applicationBody(requestId, secondCaregiver.getId(), "40000"))
                .andExpect(status().isConflict());
        send(post("/api/v1/care-applications"), caregiverToken, applicationBody(requestId, secondCaregiver.getId(), "40000"))
                .andExpect(status().isForbidden());
        send(post("/api/v1/care-applications"), ownerToken, applicationBody(requestId, owner.getId(), "40000"))
                .andExpect(status().isNotFound());
        send(put(path), caregiverToken, applicationUpdate("60000", "PENDING", null)).andExpect(status().isConflict());
        send(put(path), caregiverToken, applicationUpdate("42000", "PENDING", null))
                .andExpect(status().isOk()).andExpect(jsonPath("$.proposedRate").value(42000));
        send(put(path), caregiverToken, applicationUpdate("42000", "REJECTED", "No")).andExpect(status().isForbidden());
        send(put(path), ownerToken, applicationUpdate("1", "PENDING", null)).andExpect(status().isForbidden());
        send(put(path), ownerToken, applicationUpdate("1", "REJECTED", "Elegí otra fecha"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.applicationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.proposedRate").value(42000));
        send(put(path), caregiverToken, applicationUpdate("42000", "WITHDRAWN", null)).andExpect(status().isConflict());
        send(delete(path), ownerToken, null).andExpect(status().isForbidden());
        send(delete(path), caregiverToken, null).andExpect(status().isNoContent());
        send(get(path), adminToken, null).andExpect(status().isNotFound());
    }

    @Test
    void closingRequestRejectsPendingApplicationsAndBlocksNewOnes() throws Exception {
        long requestId = createRequest();
        long id = apply(requestId, "45000");
        send(put("/api/v1/care-applications/" + id), caregiverToken, applicationUpdate("45000", "WITHDRAWN", null))
                .andExpect(status().isOk()).andExpect(jsonPath("$.applicationStatus").value("WITHDRAWN"));
        long requestTwo = createRequest();
        long pending = apply(requestTwo, "30000");
        send(put("/api/v1/care-requests/" + requestTwo), ownerToken, updateBody(start, "CLOSED", null))
                .andExpect(status().isOk());
        send(get("/api/v1/care-applications/" + pending), caregiverToken, null)
                .andExpect(jsonPath("$.applicationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.responseReason").value("La solicitud fue cerrada"));
        send(post("/api/v1/care-applications"), secondCaregiverToken, applicationBody(requestTwo, secondCaregiver.getId(), "1"))
                .andExpect(status().isConflict());
        send(delete("/api/v1/care-requests/" + requestId), ownerToken, null).andExpect(status().isNoContent());
        send(get("/api/v1/care-applications/" + id), adminToken, null).andExpect(status().isNotFound());
    }

    @Test
    void swaggerDocumentsAllOperationsAndRoutesRequireToken() throws Exception {
        var docs = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var paths = JsonMapper.builder().build().readTree(docs.getResponse().getContentAsString()).get("paths");
        String[] collections = {"booking-statuses", "care-requests", "care-applications"};
        String[] parameters = {"/{code}", "/{id}", "/{id}"};
        for (int i = 0; i < collections.length; i++) {
            String path = "/api/v1/" + collections[i];
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            for (String method : List.of("get", "post")) assertTrue(paths.get(path).has(method));
            for (String method : List.of("get", "put", "delete")) assertTrue(paths.get(path + parameters[i]).has(method));
        }
        send(get("/api/v1/care-requests/0"), adminToken, null).andExpect(status().isBadRequest());
        send(get("/api/v1/care-applications/9223372036854775807"), adminToken, null).andExpect(status().isNotFound());
    }

    private long createRequestWithActivePet() throws Exception {
        pet.setPetStatus("ACTIVE");
        pets.saveAndFlush(pet);
        return createRequest();
    }

    private String updateBody(java.time.LocalDateTime from, String status, String reason) {
        return """
                {"petId":%d,"serviceTypeId":%d,"startAt":"%s","endAt":"%s","city":"Palmira",
                 "maxBudget":50000,"requestStatus":"%s","cancellationReason":%s}
                """.formatted(pet.getId(), type.getId(), from, from.plusHours(4), status,
                reason == null ? "null" : "\"" + reason + "\"");
    }

    private String applicationBody(long requestId, long caregiverId, String rate) {
        return "{\"requestId\":%d,\"caregiverId\":%d,\"proposedRate\":%s}".formatted(requestId, caregiverId, rate);
    }

    private String applicationUpdate(String rate, String status, String reason) {
        return "{\"proposedRate\":%s,\"applicationStatus\":\"%s\",\"responseReason\":%s}"
                .formatted(rate, status, reason == null ? "null" : "\"" + reason + "\"");
    }
}

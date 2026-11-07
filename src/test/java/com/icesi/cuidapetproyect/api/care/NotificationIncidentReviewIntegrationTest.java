package com.icesi.cuidapetproyect.api.care;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationIncidentReviewIntegrationTest extends CareFixture {

    private long requestOf(long booking) throws Exception {
        String json = send(get("/api/v1/care-bookings/" + booking), adminToken, null).andReturn().getResponse().getContentAsString();
        return JsonMapper.builder().build().readTree(json).get("requestId").asLong();
    }

    @Test
    void adminSendsNotificationsAndRecipientOnlyMarksThemRead() throws Exception {
        String body = """
                {"userId":%d,"notificationType":"SYSTEM_NOTICE","title":"Mantenimiento","messageText":"El sábado no hay servicio",
                 "referenceEntity":null,"referenceId":null}
                """.formatted(owner.getId());
        send(post("/api/v1/notifications"), ownerToken, body).andExpect(status().isForbidden());
        long id = id(send(post("/api/v1/notifications"), adminToken, body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.readFlag").value("N")).andExpect(header().exists("Location")));
        String path = "/api/v1/notifications/" + id;
        send(get(path), ownerToken, null).andExpect(status().isOk());
        send(get(path), otherToken, null).andExpect(status().isForbidden());
        send(get("/api/v1/notifications"), otherToken, null).andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
        send(get("/api/v1/notifications"), ownerToken, null).andExpect(jsonPath("$[*].userId", everyItem(is(owner.getId().intValue()))));
        send(put(path), ownerToken, "{\"readFlag\":\"Y\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.readFlag").value("Y")).andExpect(jsonPath("$.readAt").isNotEmpty());
        send(put(path), ownerToken, "{\"readFlag\":\"Y\",\"title\":\"Otro título\"}").andExpect(status().isForbidden());
        send(put(path), adminToken, "{\"readFlag\":\"N\",\"title\":\"Mantenimiento programado\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Mantenimiento programado")).andExpect(jsonPath("$.readAt").isEmpty());
        send(put(path), ownerToken, "{\"readFlag\":\"X\"}").andExpect(status().isBadRequest());
        send(post("/api/v1/notifications"), adminToken, body.replace("SYSTEM_NOTICE", "aviso libre")).andExpect(status().isBadRequest());
        send(post("/api/v1/notifications"), adminToken, body.replace("\"referenceId\":null", "\"referenceId\":5"))
                .andExpect(status().isBadRequest());
        other.setAccountStatus("SUSPENDED");
        users.saveAndFlush(other);
        send(post("/api/v1/notifications"), adminToken, body.replace("\"userId\":" + owner.getId(), "\"userId\":" + other.getId()))
                .andExpect(status().isConflict());
        send(delete(path), otherToken, null).andExpect(status().isForbidden());
        send(delete(path), ownerToken, null).andExpect(status().isNoContent());
        send(get(path), adminToken, null).andExpect(status().isNotFound());
    }

    @Test
    void participantsReportIncidentsAndAdminResolvesThem() throws Exception {
        long booking = startedBooking();
        long request = requestOf(booking);
        String body = """
                {"reporterId":%d,"reportedUserId":%d,"requestId":%d,"bookingId":%d,"reportReason":"Llegó tarde",
                 "reportDetails":"El cuidador llegó una hora tarde","evidenceUrl":"https://evidencias.example.com/1.jpg"}
                """;
        send(post("/api/v1/incident-reports"), ownerToken, body.formatted(owner.getId(), owner.getId(), request, booking))
                .andExpect(status().isConflict());
        send(post("/api/v1/incident-reports"), ownerToken, body.formatted(owner.getId(), other.getId(), request, booking))
                .andExpect(status().isConflict());
        send(post("/api/v1/incident-reports"), otherToken, body.formatted(other.getId(), owner.getId(), request, booking))
                .andExpect(status().isConflict());
        send(post("/api/v1/incident-reports"), otherToken, body.formatted(owner.getId(), caregiver.getId(), request, booking))
                .andExpect(status().isForbidden());
        long otherRequest = createRequest();
        send(post("/api/v1/incident-reports"), ownerToken, body.formatted(owner.getId(), caregiver.getId(), otherRequest, booking))
                .andExpect(status().isConflict());
        long id = id(send(post("/api/v1/incident-reports"), ownerToken, body.formatted(owner.getId(), caregiver.getId(), request, booking))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.reportStatus").value("OPEN")));
        String path = "/api/v1/incident-reports/" + id;
        send(get(path), ownerToken, null).andExpect(status().isOk());
        send(get(path), caregiverToken, null).andExpect(status().isForbidden());
        send(get("/api/v1/incident-reports"), caregiverToken, null).andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
        String update = """
                {"reportReason":"Llegó tarde","reportDetails":"%s","evidenceUrl":null,"reportStatus":"%s","resolutionNotes":%s}
                """;
        send(put(path), ownerToken, update.formatted("Una hora y media tarde", "OPEN", "null")).andExpect(status().isOk())
                .andExpect(jsonPath("$.reportDetails").value("Una hora y media tarde"));
        send(put(path), ownerToken, update.formatted("x", "RESOLVED", "\"Listo\"")).andExpect(status().isForbidden());
        send(put(path), adminToken, update.formatted("Una hora y media tarde", "IN_REVIEW", "null")).andExpect(status().isOk());
        send(put(path), ownerToken, update.formatted("Edito", "OPEN", "null")).andExpect(status().isConflict());
        send(put(path), adminToken, update.formatted("Una hora y media tarde", "RESOLVED", "null")).andExpect(status().isBadRequest());
        send(put(path), adminToken, update.formatted("Una hora y media tarde", "RESOLVED", "\"Se habló con ambas partes\""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resolvedById").value(admin.getId()))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());
        send(delete(path), ownerToken, null).andExpect(status().isConflict());
        send(get("/api/v1/notifications"), ownerToken, null)
                .andExpect(jsonPath("$[*].notificationType", hasItem("INCIDENT_STATUS")));
        send(delete(path), adminToken, null).andExpect(status().isNoContent());
        send(get(path), adminToken, null).andExpect(status().isNotFound());
    }

    @Test
    void incidentWithoutBookingRequiresApplicantAndOpenReportCanBeWithdrawn() throws Exception {
        long request = createRequest();
        String body = """
                {"reporterId":%d,"reportedUserId":%d,"requestId":%d,"reportReason":"Mensajes ofensivos",
                 "reportDetails":"Recibí mensajes ofensivos"}
                """;
        send(post("/api/v1/incident-reports"), caregiverToken, body.formatted(caregiver.getId(), owner.getId(), request))
                .andExpect(status().isConflict());
        apply(request, "45000");
        long id = id(send(post("/api/v1/incident-reports"), caregiverToken, body.formatted(caregiver.getId(), owner.getId(), request))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.bookingId").isEmpty()));
        send(post("/api/v1/incident-reports"), caregiverToken,
                body.formatted(caregiver.getId(), owner.getId(), request).replace("}", ",\"evidenceUrl\":\"no-url\"}"))
                .andExpect(status().isBadRequest());
        send(delete("/api/v1/incident-reports/" + id), caregiverToken, null).andExpect(status().isNoContent());
    }

    @Test
    void participantsReviewEachOtherOnceAfterCompletion() throws Exception {
        long pending = startedBooking();
        String body = "{\"bookingId\":%d,\"reviewerId\":%d,\"rating\":%d,\"reviewComment\":\"%s\"}";
        send(post("/api/v1/reviews"), ownerToken, body.formatted(pending, owner.getId(), 5, "Bien"))
                .andExpect(status().isConflict());
        long booking = completedBooking();
        long id = id(send(post("/api/v1/reviews"), ownerToken, body.formatted(booking, owner.getId(), 5, "Excelente"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.reviewedUserId").value(caregiver.getId())));
        send(post("/api/v1/reviews"), ownerToken, body.formatted(booking, owner.getId(), 4, "Otra")).andExpect(status().isConflict());
        send(post("/api/v1/reviews"), caregiverToken, body.formatted(booking, caregiver.getId(), 4, "Mascota tranquila"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.reviewedUserId").value(owner.getId()));
        send(post("/api/v1/reviews"), otherToken, body.formatted(booking, other.getId(), 1, "No participé"))
                .andExpect(status().isConflict());
        send(post("/api/v1/reviews"), otherToken, body.formatted(booking, owner.getId(), 1, "Suplantación"))
                .andExpect(status().isForbidden());
        for (int rating : new int[]{0, 6}) {
            send(post("/api/v1/reviews"), ownerToken, body.formatted(booking, owner.getId(), rating, "x"))
                    .andExpect(status().isBadRequest());
        }
        String path = "/api/v1/reviews/" + id;
        send(get(path), otherToken, null).andExpect(status().isOk()).andExpect(jsonPath("$.rating").value(5));
        send(get("/api/v1/reviews"), otherToken, null).andExpect(jsonPath("$[*].id", hasItem((int) id)));
        send(put(path), ownerToken, "{\"rating\":4,\"reviewComment\":\"Muy bien\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4)).andExpect(jsonPath("$.bookingId").value(booking));
        send(put(path), caregiverToken, "{\"rating\":1}").andExpect(status().isForbidden());
        send(get("/api/v1/notifications"), caregiverToken, null)
                .andExpect(jsonPath("$[*].notificationType", hasItem("REVIEW_RECEIVED")));
        send(delete(path), caregiverToken, null).andExpect(status().isForbidden());
        send(delete(path), ownerToken, null).andExpect(status().isNoContent());
        send(get(path), adminToken, null).andExpect(status().isNotFound());
    }

    @Test
    void swaggerDocumentsFollowUpOperations() throws Exception {
        var docs = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var paths = JsonMapper.builder().build().readTree(docs.getResponse().getContentAsString()).get("paths");
        for (String collection : List.of("notifications", "incident-reports", "reviews")) {
            String path = "/api/v1/" + collection;
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            for (String method : List.of("get", "post")) assertTrue(paths.get(path).has(method));
            for (String method : List.of("get", "put", "delete")) assertTrue(paths.get(path + "/{id}").has(method));
        }
        send(get("/api/v1/reviews/0"), adminToken, null).andExpect(status().isBadRequest());
    }
}

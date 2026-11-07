package com.icesi.cuidapetproyect.api.care;

import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CareBookingTrackingIntegrationTest extends CareFixture {
    @Autowired AppNotificationRepository notifications;

    @Test
    void ownerBooksPendingApplicationAndOtherApplicationsAreRejected() throws Exception {
        long requestId = createRequest();
        long accepted = apply(requestId, "45000");
        var offering = new CaregiverService();
        offering.setId(new CaregiverServiceId(secondCaregiver.getId(), type.getId()));
        offering.setCaregiver(profiles.findById(secondCaregiver.getId()).orElseThrow());
        offering.setServiceType(type);
        offering.setBaseRate(new BigDecimal("20000"));
        offerings.saveAndFlush(offering);
        long rejected = id(send(post("/api/v1/care-applications"), secondCaregiverToken,
                "{\"requestId\":%d,\"caregiverId\":%d,\"proposedRate\":40000}".formatted(requestId, secondCaregiver.getId()))
                .andExpect(status().isCreated()));
        send(post("/api/v1/care-bookings"), caregiverToken, "{\"applicationId\":%d,\"exactAddress\":\"X\"}".formatted(accepted))
                .andExpect(status().isForbidden());
        long booking = book(accepted);
        String path = "/api/v1/care-bookings/" + booking;
        send(get(path), caregiverToken, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.agreedRate").value(45000))
                .andExpect(jsonPath("$.caregiverId").value(caregiver.getId()))
                .andExpect(jsonPath("$.ownerConfirmedFlag").value("N"));
        send(get(path), secondCaregiverToken, null).andExpect(status().isForbidden());
        send(get("/api/v1/care-bookings"), otherToken, null).andExpect(jsonPath("$[*].id", not(hasItem((int) booking))));
        send(get("/api/v1/care-applications/" + accepted), ownerToken, null)
                .andExpect(jsonPath("$.applicationStatus").value("ACCEPTED"));
        send(get("/api/v1/care-applications/" + rejected), secondCaregiverToken, null)
                .andExpect(jsonPath("$.applicationStatus").value("REJECTED"));
        send(get("/api/v1/care-requests/" + requestId), ownerToken, null)
                .andExpect(jsonPath("$.requestStatus").value("ASSIGNED"));
        send(post("/api/v1/care-bookings"), ownerToken, "{\"applicationId\":%d,\"exactAddress\":\"X\"}".formatted(accepted))
                .andExpect(status().isConflict());
        send(post("/api/v1/care-bookings"), ownerToken, "{\"applicationId\":%d,\"exactAddress\":\" \"}".formatted(accepted))
                .andExpect(status().isBadRequest());
        send(delete("/api/v1/care-applications/" + accepted), caregiverToken, null).andExpect(status().isConflict());
        assertTrue(notifications.findByUser_Id(caregiver.getId()).stream()
                .anyMatch(n -> "BOOKING_CREATED".equals(n.getNotificationType()) && booking == n.getReferenceId()));
    }

    @Test
    void bookingFollowsStatusMachineWithParticipantRoles() throws Exception {
        long booking = book(apply(createRequest(), "45000"));
        changeBooking(booking, ownerToken, "IN_PROGRESS", null).andExpect(status().isForbidden());
        changeBooking(booking, otherToken, "IN_PROGRESS", null).andExpect(status().isForbidden());
        changeBooking(booking, caregiverToken, "COMPLETED", null).andExpect(status().isConflict());
        changeBooking(booking, caregiverToken, "IN_PROGRESS", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.startedAt").isNotEmpty());
        send(put("/api/v1/care-bookings/" + booking), ownerToken,
                "{\"bookingStatusCode\":\"IN_PROGRESS\",\"exactAddress\":\"Otra dirección\"}").andExpect(status().isConflict());
        changeBooking(booking, caregiverToken, "PENDING_CONFIRMATION", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.caregiverDoneFlag").value("Y"));
        changeBooking(booking, caregiverToken, "COMPLETED", null).andExpect(status().isForbidden());
        changeBooking(booking, ownerToken, "COMPLETED", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerConfirmedFlag").value("Y")).andExpect(jsonPath("$.completedAt").isNotEmpty());
        var request = JsonMapper.builder().build().readTree(send(get("/api/v1/care-bookings/" + booking), ownerToken, null)
                .andReturn().getResponse().getContentAsString()).get("requestId").asLong();
        send(get("/api/v1/care-requests/" + request), ownerToken, null).andExpect(jsonPath("$.requestStatus").value("CLOSED"));
        changeBooking(booking, ownerToken, "CANCELLED", "Tarde").andExpect(status().isConflict());
        assertTrue(notifications.findByUser_Id(owner.getId()).stream().anyMatch(n -> "BOOKING_STATUS".equals(n.getNotificationType())));
    }

    @Test
    void ownerChangesAddressBeforeStartAndCancellationNeedsReason() throws Exception {
        long requestId = createRequest();
        long booking = book(apply(requestId, "45000"));
        send(put("/api/v1/care-bookings/" + booking), caregiverToken,
                "{\"bookingStatusCode\":\"SCHEDULED\",\"exactAddress\":\"Otra\"}").andExpect(status().isForbidden());
        send(put("/api/v1/care-bookings/" + booking), ownerToken,
                "{\"bookingStatusCode\":\"SCHEDULED\",\"exactAddress\":\"Carrera 80 # 1-50\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.exactAddress").value("Carrera 80 # 1-50"));
        String cancel = "{\"bookingStatusCode\":\"CANCELLED\",\"exactAddress\":\"Carrera 80 # 1-50\",\"cancellationReason\":%s}";
        send(put("/api/v1/care-bookings/" + booking), caregiverToken, cancel.formatted("null")).andExpect(status().isBadRequest());
        send(put("/api/v1/care-bookings/" + booking), caregiverToken, cancel.formatted("\"Me enfermé\"")).andExpect(status().isOk())
                .andExpect(jsonPath("$.cancellationReason").value("Me enfermé"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());
        send(get("/api/v1/care-requests/" + requestId), ownerToken, null)
                .andExpect(jsonPath("$.requestStatus").value("CANCELLED"));
        changeBooking(booking, ownerToken, "PAUSED", null).andExpect(status().isBadRequest());
    }

    @Test
    void caregiverPostsUpdatesWithPhotosOnlyDuringCare() throws Exception {
        long booking = book(apply(createRequest(), "45000"));
        String body = "{\"bookingId\":%d,\"updateMessage\":\"Llegamos al parque\"}".formatted(booking);
        send(post("/api/v1/care-updates"), caregiverToken, body).andExpect(status().isConflict());
        changeBooking(booking, caregiverToken, "IN_PROGRESS", null).andExpect(status().isOk());
        send(post("/api/v1/care-updates"), ownerToken, body).andExpect(status().isForbidden());
        long update = id(send(post("/api/v1/care-updates"), caregiverToken, body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.caregiverId").value(caregiver.getId())));
        String path = "/api/v1/care-updates/" + update;
        send(get(path), ownerToken, null).andExpect(status().isOk());
        send(get(path), otherToken, null).andExpect(status().isForbidden());
        send(get("/api/v1/care-updates"), otherToken, null).andExpect(jsonPath("$[*].id", not(hasItem((int) update))));
        send(put(path), caregiverToken, "{\"updateMessage\":\"Comió bien\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.updateMessage").value("Comió bien"));
        send(put(path), ownerToken, "{\"updateMessage\":\"No\"}").andExpect(status().isForbidden());
        assertTrue(notifications.findByUser_Id(owner.getId()).stream().anyMatch(n -> "CARE_UPDATE".equals(n.getNotificationType())));

        String photo = "{\"updateId\":%d,\"photoUrl\":\"https://fotos.example.com/%s/1.jpg\"}".formatted(update, suffix);
        long photoId = id(send(post("/api/v1/care-update-photos"), caregiverToken, photo).andExpect(status().isCreated())
                .andExpect(jsonPath("$.updateId").value(update)));
        send(post("/api/v1/care-update-photos"), caregiverToken, photo).andExpect(status().isConflict());
        send(post("/api/v1/care-update-photos"), ownerToken, photo.replace("1.jpg", "2.jpg")).andExpect(status().isForbidden());
        send(post("/api/v1/care-update-photos"), caregiverToken, photo.replace("https://", "ftp://")).andExpect(status().isBadRequest());
        send(get("/api/v1/care-update-photos/" + photoId), ownerToken, null).andExpect(status().isOk());
        send(get("/api/v1/care-update-photos"), otherToken, null).andExpect(jsonPath("$[*].id", not(hasItem((int) photoId))));
        send(put("/api/v1/care-update-photos/" + photoId), caregiverToken,
                "{\"photoUrl\":\"https://fotos.example.com/%s/editada.jpg\"}".formatted(suffix))
                .andExpect(status().isOk()).andExpect(jsonPath("$.photoUrl", endsWith("editada.jpg")));
        for (int i = 2; i <= 10; i++) {
            send(post("/api/v1/care-update-photos"), caregiverToken, photo.replace("1.jpg", i + ".jpg"))
                    .andExpect(status().isCreated());
        }
        send(post("/api/v1/care-update-photos"), caregiverToken, photo.replace("1.jpg", "11.jpg")).andExpect(status().isConflict());
        send(delete("/api/v1/care-update-photos/" + photoId), ownerToken, null).andExpect(status().isForbidden());
        send(delete("/api/v1/care-update-photos/" + photoId), caregiverToken, null).andExpect(status().isNoContent());
        send(get("/api/v1/care-update-photos/" + photoId), adminToken, null).andExpect(status().isNotFound());
        send(delete(path), caregiverToken, null).andExpect(status().isNoContent());
        send(get(path), adminToken, null).andExpect(status().isNotFound());
        send(get("/api/v1/care-update-photos"), adminToken, null).andExpect(jsonPath("$[*].updateId", not(hasItem((int) update))));
    }

    @Test
    void finishedCareRejectsNewUpdatesAndAdminDeletionRestoresOpenRequest() throws Exception {
        long finished = completedBooking();
        send(post("/api/v1/care-updates"), caregiverToken,
                "{\"bookingId\":%d,\"updateMessage\":\"Tarde\"}".formatted(finished)).andExpect(status().isConflict());
        long requestId = createRequest();
        long application = apply(requestId, "45000");
        long booking = book(application);
        send(delete("/api/v1/care-bookings/" + booking), ownerToken, null).andExpect(status().isForbidden());
        send(delete("/api/v1/care-bookings/" + booking), adminToken, null).andExpect(status().isNoContent());
        send(get("/api/v1/care-bookings/" + booking), adminToken, null).andExpect(status().isNotFound());
        send(get("/api/v1/care-requests/" + requestId), ownerToken, null).andExpect(jsonPath("$.requestStatus").value("OPEN"));
        send(get("/api/v1/care-applications/" + application), ownerToken, null)
                .andExpect(jsonPath("$.applicationStatus").value("PENDING"));
        send(delete("/api/v1/care-bookings/" + finished), adminToken, null).andExpect(status().isNoContent());
    }

    @Test
    void swaggerDocumentsBookingTrackingOperations() throws Exception {
        var docs = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var paths = JsonMapper.builder().build().readTree(docs.getResponse().getContentAsString()).get("paths");
        for (String collection : List.of("care-bookings", "care-updates", "care-update-photos")) {
            String path = "/api/v1/" + collection;
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            for (String method : List.of("get", "post")) assertTrue(paths.get(path).has(method));
            for (String method : List.of("get", "put", "delete")) assertTrue(paths.get(path + "/{id}").has(method));
        }
    }
}

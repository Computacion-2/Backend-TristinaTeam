package com.icesi.cuidapetproyect.api;

import com.icesi.cuidapetproyect.api.security.JwtService;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import com.icesi.cuidapetproyect.service.AppUserDetailsService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.UUID;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.jwt.secret=dGVzdC1vbmx5LWtleS0zMi1ieXRlcy1taW5pbXVtLWxvbmc=")
@AutoConfigureMockMvc
@Transactional
class CaregiverScheduleIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService tokens;
    @Autowired AppUserRepository users;
    @Autowired AppRoleRepository roles;
    @Autowired UserRoleRepository userRoles;
    @Autowired CaregiverProfileRepository profiles;
    @Autowired ServiceTypeRepository types;
    @Autowired FavoriteCaregiverRepository favorites;
    @MockitoBean AppUserDetailsService details;
    private AppUser owner, caregiver, secondCaregiver;
    private CaregiverProfile profile;
    private ServiceType type;
    private AppRole caregiverRole;
    private String ownerToken, caregiverToken, otherToken, adminToken, suffix;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().replace("-", "");
        owner = user("owner");
        caregiver = user("caregiver");
        secondCaregiver = user("second");
        ownerToken = token(owner, "OWNER");
        caregiverToken = token(caregiver, "CAREGIVER");
        otherToken = token(user("other"), "OWNER");
        adminToken = token(user("admin"), "ADMIN");
        caregiverRole = roles.findByRoleName("CAREGIVER").orElseGet(() -> {
            var role = new AppRole();
            role.setRoleName("CAREGIVER");
            return roles.saveAndFlush(role);
        });
        caregiverRole.setActiveFlag("Y");
        roles.saveAndFlush(caregiverRole);
        profile = profile(caregiver);
        profile(secondCaregiver);
        type = new ServiceType();
        type.setServiceName("Servicio " + suffix);
        type = types.saveAndFlush(type);
    }

    private AppUser user(String prefix) {
        var entity = new AppUser();
        entity.setEmail(prefix + suffix + "@example.com");
        entity.setPasswordHash("hash-solo-fixture");
        return users.saveAndFlush(entity);
    }

    private String token(AppUser user, String role) {
        var principal = User.withUsername(user.getEmail()).password("unused").roles(role).build();
        when(details.loadUserByUsername(user.getEmail())).thenReturn(principal);
        return "Bearer " + tokens.createToken(principal).accessToken();
    }

    private CaregiverProfile profile(AppUser user) {
        var assignment = new UserRole();
        assignment.setId(new UserRoleId(user.getId(), caregiverRole.getId()));
        assignment.setUser(user);
        assignment.setRole(caregiverRole);
        userRoles.saveAndFlush(assignment);
        var entity = new CaregiverProfile();
        entity.setUser(user);
        entity.setBiography("Cuido mascotas");
        entity.setServiceCity("Cali");
        return profiles.saveAndFlush(entity);
    }

    private String availabilityBody() {
        return """
                {"caregiverId":%d,"dayOfWeek":1,"startTime":"08:00","endTime":"12:00",
                 "validFrom":"2027-01-01","validUntil":"2027-12-31"}
                """.formatted(caregiver.getId());
    }

    private long createAvailability() throws Exception {
        var result = mvc.perform(post("/api/v1/caregiver-availabilities")
                        .header("Authorization", caregiverToken).contentType("application/json").content(availabilityBody()))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.activeFlag").value("Y")).andReturn();
        return JsonMapper.builder().build().readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String serviceBody() {
        return """
                {"caregiverId":%d,"serviceTypeId":%d,"baseRate":25000.50}
                """.formatted(caregiver.getId(), type.getId());
    }

    private String servicePath() {
        return "/api/v1/caregiver-services/" + caregiver.getId() + "/" + type.getId();
    }

    private void createService() throws Exception {
        mvc.perform(post("/api/v1/caregiver-services").header("Authorization", caregiverToken)
                        .contentType("application/json").content(serviceBody()))
                .andExpect(status().isCreated()).andExpect(header().string("Location", endsWith(servicePath())))
                .andExpect(jsonPath("$.baseRate").value(25000.50));
    }

    private String favoritePath(Long caregiverId) {
        return "/api/v1/favorite-caregivers/" + owner.getId() + "/" + caregiverId;
    }

    private void createFavorite(Long caregiverId) throws Exception {
        mvc.perform(post("/api/v1/favorite-caregivers").header("Authorization", ownerToken)
                        .contentType("application/json").content("""
                        {"ownerId":%d,"caregiverId":%d}
                        """.formatted(owner.getId(), caregiverId)))
                .andExpect(status().isCreated()).andExpect(header().string("Location", endsWith(favoritePath(caregiverId))))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void availabilityCrudPreservesCaregiverAndHidesInactiveSlots() throws Exception {
        long id = createAvailability();
        String path = "/api/v1/caregiver-availabilities/" + id;
        mvc.perform(get(path).header("Authorization", ownerToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/caregiver-availabilities").header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem((int) id)));
        mvc.perform(put(path).header("Authorization", caregiverToken).contentType("application/json").content("""
                        {"caregiverId":%d,"dayOfWeek":2,"startTime":"09:00","endTime":"13:00","activeFlag":"N"}
                        """.formatted(secondCaregiver.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.caregiverId").value(caregiver.getId()));
        mvc.perform(get(path).header("Authorization", ownerToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/caregiver-availabilities").header("Authorization", ownerToken))
                .andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
        mvc.perform(get(path).header("Authorization", caregiverToken)).andExpect(status().isOk());
        mvc.perform(get(path).header("Authorization", adminToken)).andExpect(status().isOk());
        mvc.perform(delete(path).header("Authorization", caregiverToken)).andExpect(status().isNoContent());
        mvc.perform(get(path).header("Authorization", adminToken)).andExpect(status().isNotFound());
    }

    @Test
    void availabilityValidatesHoursDaysAndDates() throws Exception {
        for (String body : List.of(
                availabilityBody().replace("08:00", "24:00"),
                availabilityBody().replace("08:00", "8:00"),
                availabilityBody().replace("08:00", "12:00"),
                availabilityBody().replace("08:00", "13:00"),
                availabilityBody().replace("\"dayOfWeek\":1", "\"dayOfWeek\":8"),
                availabilityBody().replace("2027-12-31", "2026-12-31"))) {
            mvc.perform(post("/api/v1/caregiver-availabilities").header("Authorization", caregiverToken)
                    .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void foreignUsersCannotWriteAvailability() throws Exception {
        long id = createAvailability();
        String path = "/api/v1/caregiver-availabilities/" + id;
        mvc.perform(post("/api/v1/caregiver-availabilities").header("Authorization", otherToken)
                        .contentType("application/json").content(availabilityBody())).andExpect(status().isForbidden());
        mvc.perform(put(path).header("Authorization", otherToken).contentType("application/json").content("""
                        {"dayOfWeek":1,"startTime":"08:00","endTime":"12:00","activeFlag":"Y"}
                        """)).andExpect(status().isForbidden());
        mvc.perform(delete(path).header("Authorization", otherToken)).andExpect(status().isForbidden());
    }

    @Test
    void serviceCrudKeepsCompositeKeyAndHidesInactiveOffering() throws Exception {
        createService();
        mvc.perform(get(servicePath()).header("Authorization", ownerToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/caregiver-services").header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].caregiverId", hasItem(caregiver.getId().intValue())));
        mvc.perform(put(servicePath()).header("Authorization", caregiverToken).contentType("application/json")
                        .content("{\"baseRate\":30000,\"activeFlag\":\"N\",\"caregiverId\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.caregiverId").value(caregiver.getId()))
                .andExpect(jsonPath("$.baseRate").value(30000));
        mvc.perform(get(servicePath()).header("Authorization", ownerToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/caregiver-services").header("Authorization", ownerToken))
                .andExpect(jsonPath("$[*].caregiverId", not(hasItem(caregiver.getId().intValue()))));
        mvc.perform(get(servicePath()).header("Authorization", caregiverToken)).andExpect(status().isOk());
        mvc.perform(delete(servicePath()).header("Authorization", caregiverToken)).andExpect(status().isNoContent());
        mvc.perform(get(servicePath()).header("Authorization", adminToken)).andExpect(status().isNotFound());
    }

    @Test
    void serviceRejectsDuplicateInvalidRateAndForeignWrites() throws Exception {
        createService();
        mvc.perform(post("/api/v1/caregiver-services").header("Authorization", caregiverToken)
                        .contentType("application/json").content(serviceBody())).andExpect(status().isConflict());
        for (String rate : List.of("0", "-1", "1.234", "10000000000")) {
            mvc.perform(put(servicePath()).header("Authorization", caregiverToken).contentType("application/json")
                            .content("{\"baseRate\":" + rate + ",\"activeFlag\":\"Y\"}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/v1/caregiver-services").header("Authorization", otherToken)
                        .contentType("application/json").content(serviceBody())).andExpect(status().isForbidden());
        mvc.perform(put(servicePath()).header("Authorization", otherToken).contentType("application/json")
                        .content("{\"baseRate\":100,\"activeFlag\":\"Y\"}")).andExpect(status().isForbidden());
        mvc.perform(delete(servicePath()).header("Authorization", otherToken)).andExpect(status().isForbidden());
    }

    @Test
    void inactiveCatalogCannotBeOfferedOrReactivated() throws Exception {
        createService();
        type.setActiveFlag("N");
        types.saveAndFlush(type);
        mvc.perform(get(servicePath()).header("Authorization", ownerToken)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/caregiver-services").header("Authorization", caregiverToken)
                        .contentType("application/json").content(serviceBody())).andExpect(status().isConflict());
        mvc.perform(put(servicePath()).header("Authorization", caregiverToken).contentType("application/json")
                        .content("{\"baseRate\":100,\"activeFlag\":\"Y\"}")).andExpect(status().isConflict());
        mvc.perform(put(servicePath()).header("Authorization", caregiverToken).contentType("application/json")
                        .content("{\"baseRate\":100,\"activeFlag\":\"N\"}")).andExpect(status().isOk());
    }

    @Test
    void favoriteCrudReplacesAssociationAndPreservesTimestamp() throws Exception {
        createFavorite(caregiver.getId());
        var before = favorites.findById(new FavoriteCaregiverId(owner.getId(), caregiver.getId())).orElseThrow().getCreatedAt();
        mvc.perform(get(favoritePath(caregiver.getId())).header("Authorization", ownerToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/favorite-caregivers").header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(put(favoritePath(caregiver.getId())).header("Authorization", ownerToken)
                        .contentType("application/json").content("{\"caregiverId\":" + secondCaregiver.getId() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value(owner.getId()))
                .andExpect(jsonPath("$.caregiverId").value(secondCaregiver.getId()));
        assertFalse(favorites.existsById(new FavoriteCaregiverId(owner.getId(), caregiver.getId())));
        assertEquals(before, favorites.findById(new FavoriteCaregiverId(owner.getId(), secondCaregiver.getId())).orElseThrow().getCreatedAt());
        mvc.perform(get(favoritePath(caregiver.getId())).header("Authorization", ownerToken)).andExpect(status().isNotFound());
        mvc.perform(delete(favoritePath(secondCaregiver.getId())).header("Authorization", ownerToken)).andExpect(status().isNoContent());
        mvc.perform(get(favoritePath(secondCaregiver.getId())).header("Authorization", ownerToken)).andExpect(status().isNotFound());
    }

    @Test
    void favoriteDuplicateOrMissingReplacementDoesNotRemoveOriginal() throws Exception {
        createFavorite(caregiver.getId());
        createFavorite(secondCaregiver.getId());
        mvc.perform(post("/api/v1/favorite-caregivers").header("Authorization", ownerToken)
                        .contentType("application/json").content("""
                        {"ownerId":%d,"caregiverId":%d}
                        """.formatted(owner.getId(), caregiver.getId()))).andExpect(status().isConflict());
        mvc.perform(put(favoritePath(caregiver.getId())).header("Authorization", ownerToken)
                        .contentType("application/json").content("{\"caregiverId\":" + secondCaregiver.getId() + "}"))
                .andExpect(status().isConflict());
        mvc.perform(put(favoritePath(caregiver.getId())).header("Authorization", ownerToken)
                        .contentType("application/json").content("{\"caregiverId\":9223372036854775807}"))
                .andExpect(status().isNotFound());
        mvc.perform(get(favoritePath(caregiver.getId())).header("Authorization", ownerToken)).andExpect(status().isOk());
        mvc.perform(put(favoritePath(caregiver.getId())).header("Authorization", ownerToken)
                        .contentType("application/json").content("{\"caregiverId\":" + caregiver.getId() + "}"))
                .andExpect(status().isOk());
        assertEquals(2, favorites.findByOwner_Id(owner.getId()).size());
    }

    @Test
    void cannotFavoriteOwnProfileAndFailedReplacementPreservesPreviousFavorite() throws Exception {
        mvc.perform(post("/api/v1/favorite-caregivers").header("Authorization", caregiverToken)
                        .contentType("application/json").content("""
                        {"ownerId":%d,"caregiverId":%d}
                        """.formatted(caregiver.getId(), caregiver.getId())))
                .andExpect(status().isConflict());
        createFavorite(caregiver.getId());
        mvc.perform(put(favoritePath(caregiver.getId())).header("Authorization", ownerToken)
                        .contentType("application/json").content("{\"caregiverId\":" + owner.getId() + "}"))
                .andExpect(status().isConflict());
        mvc.perform(get(favoritePath(caregiver.getId())).header("Authorization", ownerToken))
                .andExpect(status().isOk());
    }

    @Test
    void favoritesArePrivateAndOtherUsersCannotChangeThem() throws Exception {
        createFavorite(caregiver.getId());
        mvc.perform(get("/api/v1/favorite-caregivers").header("Authorization", otherToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        String path = favoritePath(caregiver.getId());
        mvc.perform(get(path).header("Authorization", otherToken)).andExpect(status().isForbidden());
        mvc.perform(delete(path).header("Authorization", otherToken)).andExpect(status().isForbidden());
        mvc.perform(put(path).header("Authorization", otherToken).contentType("application/json")
                        .content("{\"caregiverId\":" + secondCaregiver.getId() + "}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/favorite-caregivers").header("Authorization", otherToken)
                        .contentType("application/json").content("""
                        {"ownerId":%d,"caregiverId":%d}
                        """.formatted(owner.getId(), caregiver.getId()))).andExpect(status().isForbidden());
        mvc.perform(get(path).header("Authorization", adminToken)).andExpect(status().isOk());
        mvc.perform(delete(path).header("Authorization", adminToken)).andExpect(status().isNoContent());
    }

    @Test
    void revokedRoleHidesOfferingsAndBlocksNewEntriesButAllowsRemoval() throws Exception {
        long id = createAvailability();
        createService();
        createFavorite(caregiver.getId());
        caregiverRole.setActiveFlag("N");
        roles.saveAndFlush(caregiverRole);
        mvc.perform(get("/api/v1/caregiver-availabilities/" + id).header("Authorization", ownerToken))
                .andExpect(status().isForbidden());
        mvc.perform(get(servicePath()).header("Authorization", ownerToken)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/caregiver-availabilities").header("Authorization", caregiverToken)
                        .contentType("application/json").content(availabilityBody())).andExpect(status().isConflict());
        mvc.perform(put(favoritePath(caregiver.getId())).header("Authorization", ownerToken)
                        .contentType("application/json").content("{\"caregiverId\":" + secondCaregiver.getId() + "}"))
                .andExpect(status().isConflict());
        mvc.perform(delete(favoritePath(caregiver.getId())).header("Authorization", ownerToken)).andExpect(status().isNoContent());
        mvc.perform(delete(servicePath()).header("Authorization", caregiverToken)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/caregiver-availabilities/" + id).header("Authorization", caregiverToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void inactiveProfilesBlockCreationAndAdminCanManageActiveResources() throws Exception {
        profile.setActiveFlag("N");
        profiles.saveAndFlush(profile);
        mvc.perform(post("/api/v1/caregiver-services").header("Authorization", adminToken)
                        .contentType("application/json").content(serviceBody())).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/favorite-caregivers").header("Authorization", ownerToken)
                        .contentType("application/json").content("""
                        {"ownerId":%d,"caregiverId":%d}
                        """.formatted(owner.getId(), caregiver.getId()))).andExpect(status().isConflict());
        profile.setActiveFlag("Y");
        profiles.saveAndFlush(profile);
        mvc.perform(post("/api/v1/caregiver-services").header("Authorization", adminToken)
                        .contentType("application/json").content(serviceBody())).andExpect(status().isCreated());
        mvc.perform(delete(servicePath()).header("Authorization", adminToken)).andExpect(status().isNoContent());
    }

    @Test
    void missingResourcesInvalidIdsAndJwtAreHandledAndSwaggerIsComplete() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var paths = JsonMapper.builder().build().readTree(result.getResponse().getContentAsString()).get("paths");
        String[] collections = {"caregiver-availabilities", "caregiver-services", "favorite-caregivers"};
        String[] parameters = {"/{id}", "/{caregiverId}/{serviceTypeId}", "/{ownerId}/{caregiverId}"};
        for (int i = 0; i < collections.length; i++) {
            String path = "/api/v1/" + collections[i];
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            for (String method : List.of("get", "post")) assertTrue(paths.get(path).has(method));
            for (String method : List.of("get", "put", "delete")) assertTrue(paths.get(path + parameters[i]).has(method));
        }
        mvc.perform(get("/api/v1/caregiver-availabilities/9223372036854775807").header("Authorization", adminToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/caregiver-availabilities/0").header("Authorization", adminToken))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/caregiver-services/0/1").header("Authorization", adminToken))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/caregiver-services/9223372036854775807/1").header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }
}

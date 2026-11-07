package com.icesi.cuidapetproyect.api.care;

import com.icesi.cuidapetproyect.api.security.JwtService;
import com.icesi.cuidapetproyect.model.*;
import com.icesi.cuidapetproyect.repository.*;
import com.icesi.cuidapetproyect.service.AppUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Datos comunes de los flujos de solicitudes, reservas y seguimiento. Cada prueba se revierte. */
@SpringBootTest(properties = "app.jwt.secret=dGVzdC1vbmx5LWtleS0zMi1ieXRlcy1taW5pbXVtLWxvbmc=")
@AutoConfigureMockMvc
@Transactional
abstract class CareFixture {
    @Autowired protected MockMvc mvc;
    @Autowired protected JwtService tokens;
    @Autowired protected AppUserRepository users;
    @Autowired protected AppRoleRepository roles;
    @Autowired protected UserRoleRepository userRoles;
    @Autowired protected CaregiverProfileRepository profiles;
    @Autowired protected CaregiverServiceRepository offerings;
    @Autowired protected ServiceTypeRepository types;
    @Autowired protected PetSpeciesRepository species;
    @Autowired protected PetRepository pets;
    @Autowired protected BookingStatusRepository statuses;
    @MockitoBean protected AppUserDetailsService details;

    protected AppUser owner, caregiver, secondCaregiver, other, admin;
    protected String ownerToken, caregiverToken, secondCaregiverToken, otherToken, adminToken, suffix;
    protected ServiceType type;
    protected Pet pet;
    protected LocalDateTime start;

    @BeforeEach
    void setUpCareData() {
        suffix = UUID.randomUUID().toString().replace("-", "");
        start = LocalDateTime.now().plusDays(10).truncatedTo(ChronoUnit.SECONDS).withHour(8).withMinute(0).withSecond(0);
        AppRole caregiverRole = role("CAREGIVER");
        owner = user("owner");
        caregiver = user("caregiver");
        secondCaregiver = user("second");
        other = user("other");
        admin = user("admin");
        ownerToken = token(owner, "OWNER");
        caregiverToken = token(caregiver, "CAREGIVER");
        secondCaregiverToken = token(secondCaregiver, "CAREGIVER");
        otherToken = token(other, "OWNER");
        adminToken = token(admin, "ADMIN");
        type = new ServiceType();
        type.setServiceName("Paseo " + suffix);
        type = types.saveAndFlush(type);
        CaregiverProfile profile = profile(caregiver, caregiverRole);
        profile(secondCaregiver, caregiverRole);
        var offering = new CaregiverService();
        offering.setId(new CaregiverServiceId(profile.getId(), type.getId()));
        offering.setCaregiver(profile);
        offering.setServiceType(type);
        offering.setBaseRate(new BigDecimal("30000"));
        offerings.saveAndFlush(offering);
        var petSpecies = new PetSpecies();
        petSpecies.setSpeciesName("Especie " + suffix);
        petSpecies = species.saveAndFlush(petSpecies);
        pet = new Pet();
        pet.setOwner(owner);
        pet.setSpecies(petSpecies);
        pet.setPetName("Luna");
        pet = pets.saveAndFlush(pet);
        for (String code : new String[]{"SCHEDULED", "IN_PROGRESS", "PENDING_CONFIRMATION", "COMPLETED", "CANCELLED"}) {
            if (!statuses.existsById(code)) {
                var status = new BookingStatus();
                status.setStatusCode(code);
                status.setStatusName("Estado " + code);
                statuses.saveAndFlush(status);
            }
        }
    }

    private AppRole role(String name) {
        AppRole role = roles.findByRoleName(name).orElseGet(() -> {
            var created = new AppRole();
            created.setRoleName(name);
            return roles.saveAndFlush(created);
        });
        role.setActiveFlag("Y");
        return roles.saveAndFlush(role);
    }

    private AppUser user(String prefix) {
        var entity = new AppUser();
        entity.setEmail(prefix + suffix + "@example.com");
        entity.setPasswordHash("hash-solo-fixture");
        return users.saveAndFlush(entity);
    }

    private CaregiverProfile profile(AppUser user, AppRole caregiverRole) {
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

    protected String token(AppUser user, String role) {
        var principal = User.withUsername(user.getEmail()).password("unused").roles(role).build();
        when(details.loadUserByUsername(user.getEmail())).thenReturn(principal);
        return "Bearer " + tokens.createToken(principal).accessToken();
    }

    protected ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                 String token, String body) throws Exception {
        request.header("Authorization", token);
        if (body != null) request.contentType("application/json").content(body);
        return mvc.perform(request);
    }

    protected long id(ResultActions result) throws Exception {
        String json = result.andReturn().getResponse().getContentAsString();
        return JsonMapper.builder().build().readTree(json).get("id").asLong();
    }

    protected String requestBody(LocalDateTime from, LocalDateTime to, String budget) {
        return """
                {"ownerId":%d,"petId":%d,"serviceTypeId":%d,"startAt":"%s","endAt":"%s",
                 "city":"Cali","zoneName":"Sur","addressReference":"Cerca al parque","maxBudget":%s}
                """.formatted(owner.getId(), pet.getId(), type.getId(), from, to, budget);
    }

    protected long createRequest() throws Exception {
        return id(send(post("/api/v1/care-requests"), ownerToken, requestBody(start, start.plusHours(4), "50000"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.requestStatus").value("OPEN")));
    }

    protected long apply(long requestId, String rate) throws Exception {
        return id(send(post("/api/v1/care-applications"), caregiverToken, """
                {"requestId":%d,"caregiverId":%d,"proposedRate":%s,"applicationMessage":"Disponible"}
                """.formatted(requestId, caregiver.getId(), rate))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.applicationStatus").value("PENDING")));
    }
}

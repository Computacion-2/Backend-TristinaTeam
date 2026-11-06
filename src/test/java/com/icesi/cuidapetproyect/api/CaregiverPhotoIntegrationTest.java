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

import java.math.BigDecimal;
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
class CaregiverPhotoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService tokens;
    @Autowired AppUserRepository users;
    @Autowired AppRoleRepository roles;
    @Autowired UserRoleRepository userRoles;
    @Autowired PetRepository pets;
    @Autowired PetPhotoRepository photos;
    @Autowired PetSpeciesRepository species;
    @Autowired CaregiverProfileRepository profiles;
    @Autowired ServiceTypeRepository serviceTypes;
    @Autowired CaregiverServiceRepository caregiverServices;
    @MockitoBean AppUserDetailsService details;
    private AppUser owner, caregiver, other;
    private Pet pet, foreignPet;
    private String ownerToken, caregiverToken, otherToken, adminToken, suffix;
    private AppRole caregiverRole;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().replace("-", "");
        owner = user("owner");
        caregiver = user("caregiver");
        other = user("other");
        ownerToken = token(owner, "OWNER");
        caregiverToken = token(caregiver, "CAREGIVER");
        otherToken = token(other, "OWNER");
        adminToken = token(user("admin"), "ADMIN");
        caregiverRole = roles.findByRoleName("CAREGIVER").orElseGet(() -> {
            AppRole role = new AppRole();
            role.setRoleName("CAREGIVER");
            return roles.saveAndFlush(role);
        });
        caregiverRole.setActiveFlag("Y");
        roles.saveAndFlush(caregiverRole);
        UserRole assignment = new UserRole();
        assignment.setId(new UserRoleId(caregiver.getId(), caregiverRole.getId()));
        assignment.setUser(caregiver);
        assignment.setRole(caregiverRole);
        userRoles.saveAndFlush(assignment);
        PetSpecies petSpecies = new PetSpecies();
        petSpecies.setSpeciesName("Test " + suffix);
        petSpecies = species.saveAndFlush(petSpecies);
        pet = pet(owner, petSpecies);
        foreignPet = pet(other, petSpecies);
    }

    private AppUser user(String prefix) {
        AppUser user = new AppUser();
        user.setEmail(prefix + suffix + "@example.com");
        user.setPasswordHash("hash-solo-fixture");
        return users.saveAndFlush(user);
    }

    private String token(AppUser user, String role) {
        var principal = User.withUsername(user.getEmail()).password("unused").roles(role).build();
        when(details.loadUserByUsername(user.getEmail())).thenReturn(principal);
        return "Bearer " + tokens.createToken(principal).accessToken();
    }

    private Pet pet(AppUser owner, PetSpecies species) {
        Pet entity = new Pet();
        entity.setOwner(owner);
        entity.setSpecies(species);
        entity.setPetName("Luna");
        return pets.saveAndFlush(entity);
    }

    private CaregiverProfile profile() {
        CaregiverProfile profile = new CaregiverProfile();
        profile.setUser(caregiver);
        profile.setBiography("Experiencia en cuidados");
        profile.setServiceCity("Cali");
        return profiles.saveAndFlush(profile);
    }

    private long createPhoto(String url, String flag) throws Exception {
        var result = mvc.perform(post("/api/v1/pet-photos").header("Authorization", ownerToken)
                        .contentType("application/json").content("""
                        {"petId":%d,"photoUrl":"%s","mainPhotoFlag":"%s"}
                        """.formatted(pet.getId(), url, flag)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.uploadedAt").isNotEmpty()).andReturn();
        return JsonMapper.builder().build().readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void photoCrudAndMainPhotoSelection() throws Exception {
        long first = createPhoto("https://example.com/first.jpg", "Y");
        long second = createPhoto("https://example.com/second.jpg", "Y");
        mvc.perform(get("/api/v1/pet-photos/" + first).header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mainPhotoFlag").value("N"));
        mvc.perform(get("/api/v1/pet-photos").header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(put("/api/v1/pet-photos/" + first).header("Authorization", ownerToken)
                        .contentType("application/json").content("""
                        {"petId":%d,"photoUrl":"https://example.com/edited.jpg","mainPhotoFlag":"Y"}
                        """.formatted(foreignPet.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.petId").value(pet.getId()));
        mvc.perform(get("/api/v1/pet-photos/" + second).header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mainPhotoFlag").value("N"));
        mvc.perform(delete("/api/v1/pet-photos/" + first).header("Authorization", ownerToken))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/pet-photos/" + first).header("Authorization", ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void foreignPhotosArePrivateAndCannotBeModified() throws Exception {
        PetPhoto foreign = new PetPhoto();
        foreign.setPet(foreignPet);
        foreign.setPhotoUrl("https://example.com/private.jpg");
        foreign = photos.saveAndFlush(foreign);
        String route = "/api/v1/pet-photos/" + foreign.getId();
        mvc.perform(get("/api/v1/pet-photos").header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(route).header("Authorization", ownerToken)).andExpect(status().isForbidden());
        mvc.perform(delete(route).header("Authorization", ownerToken)).andExpect(status().isForbidden());
        mvc.perform(put(route).header("Authorization", ownerToken).contentType("application/json")
                        .content("{\"photoUrl\":\"https://example.com/no.jpg\",\"mainPhotoFlag\":\"N\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pet-photos").header("Authorization", ownerToken)
                        .contentType("application/json").content("""
                        {"petId":%d,"photoUrl":"https://example.com/no.jpg","mainPhotoFlag":"N"}
                        """.formatted(foreignPet.getId()))).andExpect(status().isForbidden());
        mvc.perform(get(route).header("Authorization", adminToken)).andExpect(status().isOk());
    }

    @Test
    void duplicatePhotoReturns409AndInvalidUrl400() throws Exception {
        createPhoto("https://example.com/same.jpg", "N");
        mvc.perform(post("/api/v1/pet-photos").header("Authorization", ownerToken)
                        .contentType("application/json").content("""
                        {"petId":%d,"photoUrl":"https://example.com/same.jpg","mainPhotoFlag":"Y"}
                        """.formatted(pet.getId()))).andExpect(status().isConflict());
        for (String url : List.of("javascript:alert(1)", "no-url", "https://example.com/a b")) {
            mvc.perform(post("/api/v1/pet-photos").header("Authorization", ownerToken)
                            .contentType("application/json").content("""
                            {"petId":%d,"photoUrl":"%s","mainPhotoFlag":"N"}
                            """.formatted(pet.getId(), url))).andExpect(status().isBadRequest());
        }
    }

    @Test
    void serviceTypeCrudAndAdminPermission() throws Exception {
        var result = mvc.perform(post("/api/v1/service-types").header("Authorization", adminToken)
                        .contentType("application/json").content("{\"serviceName\":\"Paseo " + suffix + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.createdAt").isNotEmpty()).andReturn();
        long id = JsonMapper.builder().build().readTree(result.getResponse().getContentAsString()).get("id").asLong();
        String route = "/api/v1/service-types/" + id;
        mvc.perform(get("/api/v1/service-types").header("Authorization", ownerToken)).andExpect(status().isOk());
        mvc.perform(get(route).header("Authorization", ownerToken)).andExpect(status().isOk());
        String update = "{\"serviceName\":\"Editado " + suffix + "\",\"activeFlag\":\"N\"}";
        mvc.perform(put(route).header("Authorization", ownerToken).contentType("application/json").content(update))
                .andExpect(status().isForbidden());
        mvc.perform(delete(route).header("Authorization", ownerToken)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/service-types").header("Authorization", ownerToken)
                        .contentType("application/json").content("{\"serviceName\":\"No permitido\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(route).header("Authorization", adminToken).contentType("application/json").content(update))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activeFlag").value("N"));
        mvc.perform(delete(route).header("Authorization", adminToken)).andExpect(status().isNoContent());
        mvc.perform(get(route).header("Authorization", ownerToken)).andExpect(status().isNotFound());
    }

    @Test
    void duplicateServiceNameIsCaseInsensitive() throws Exception {
        ServiceType service = new ServiceType();
        service.setServiceName("Paseo " + suffix);
        serviceTypes.saveAndFlush(service);
        mvc.perform(post("/api/v1/service-types").header("Authorization", adminToken)
                        .contentType("application/json").content("{\"serviceName\":\"  PASEO " + suffix + "  \"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cannotDeleteServiceTypeUsedByCaregiver() throws Exception {
        ServiceType service = new ServiceType();
        service.setServiceName("Servicio " + suffix);
        service = serviceTypes.saveAndFlush(service);
        CaregiverService relation = new CaregiverService();
        relation.setId(new CaregiverServiceId(caregiver.getId(), service.getId()));
        relation.setCaregiver(profile());
        relation.setServiceType(service);
        relation.setBaseRate(BigDecimal.TEN);
        caregiverServices.saveAndFlush(relation);
        mvc.perform(delete("/api/v1/service-types/" + service.getId()).header("Authorization", adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void caregiverCrudUsesSharedUserIdAndCanHideProfile() throws Exception {
        String route = "/api/v1/caregiver-profiles/" + caregiver.getId();
        mvc.perform(post("/api/v1/caregiver-profiles").header("Authorization", caregiverToken)
                        .contentType("application/json").content("""
                        {"caregiverId":%d,"biography":"Cuido mascotas","experienceYears":2,"serviceCity":"Cali"}
                        """.formatted(caregiver.getId())))
                .andExpect(status().isCreated()).andExpect(header().string("Location", endsWith(route)))
                .andExpect(jsonPath("$.caregiverId").value(caregiver.getId()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty()).andExpect(jsonPath("$.user").doesNotExist());
        mvc.perform(get(route).header("Authorization", ownerToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/caregiver-profiles").header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].caregiverId", hasItem(caregiver.getId().intValue())));
        mvc.perform(put(route).header("Authorization", caregiverToken).contentType("application/json").content("""
                        {"biography":"Actualizado","experienceYears":3,"serviceCity":"Bogotá","activeFlag":"N"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activeFlag").value("N"));
        mvc.perform(get(route).header("Authorization", ownerToken)).andExpect(status().isForbidden());
        mvc.perform(get(route).header("Authorization", caregiverToken)).andExpect(status().isOk());
        mvc.perform(get(route).header("Authorization", adminToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/caregiver-profiles").header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].caregiverId", not(hasItem(caregiver.getId().intValue()))));
        mvc.perform(delete(route).header("Authorization", caregiverToken)).andExpect(status().isNoContent());
        mvc.perform(get(route).header("Authorization", adminToken)).andExpect(status().isNotFound());
        assertTrue(users.existsById(caregiver.getId()));
    }

    @Test
    void missingRoleAndForeignWritesAreRejected() throws Exception {
        String body = """
                {"caregiverId":%d,"biography":"Perfil","experienceYears":0,"serviceCity":"Cali"}
                """;
        mvc.perform(post("/api/v1/caregiver-profiles").header("Authorization", ownerToken)
                        .contentType("application/json").content(body.formatted(owner.getId())))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/caregiver-profiles").header("Authorization", otherToken)
                        .contentType("application/json").content(body.formatted(caregiver.getId())))
                .andExpect(status().isForbidden());
        profile();
        String route = "/api/v1/caregiver-profiles/" + caregiver.getId();
        mvc.perform(put(route).header("Authorization", otherToken).contentType("application/json")
                        .content("{\"biography\":\"No\",\"experienceYears\":0,\"serviceCity\":\"Cali\",\"activeFlag\":\"Y\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(route).header("Authorization", otherToken)).andExpect(status().isForbidden());
    }

    @Test
    void duplicateAndInvalidCaregiverProfilesAreRejected() throws Exception {
        profile();
        mvc.perform(post("/api/v1/caregiver-profiles").header("Authorization", caregiverToken)
                        .contentType("application/json").content("""
                        {"caregiverId":%d,"biography":"Perfil","experienceYears":0,"serviceCity":"Cali"}
                        """.formatted(caregiver.getId()))).andExpect(status().isConflict());
        for (int years : List.of(-1, 1000)) {
            mvc.perform(put("/api/v1/caregiver-profiles/" + caregiver.getId()).header("Authorization", caregiverToken)
                            .contentType("application/json").content("""
                            {"biography":"Perfil","experienceYears":%d,"serviceCity":"Cali","activeFlag":"Y"}
                            """.formatted(years))).andExpect(status().isBadRequest());
        }
    }

    @Test
    void revokedCaregiverRoleRemovesProfileFromDirectory() throws Exception {
        profile();
        caregiverRole.setActiveFlag("N");
        roles.saveAndFlush(caregiverRole);
        mvc.perform(get("/api/v1/caregiver-profiles/" + caregiver.getId()).header("Authorization", ownerToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/caregiver-profiles").header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].caregiverId", not(hasItem(caregiver.getId().intValue()))));
    }

    @Test
    void endpointsRequireJwtAndSwaggerDocumentsAllOperations() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var paths = JsonMapper.builder().build().readTree(result.getResponse().getContentAsString()).get("paths");
        for (String collection : List.of("pet-photos", "service-types", "caregiver-profiles")) {
            String path = "/api/v1/" + collection;
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            for (String method : List.of("get", "post")) assertTrue(paths.get(path).has(method));
            for (String method : List.of("get", "put", "delete")) assertTrue(paths.get(path + "/{id}").has(method));
        }
    }
}

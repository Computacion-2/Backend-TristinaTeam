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

import java.time.LocalDate;
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
class ProfilePetIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService tokens;
    @Autowired AppUserRepository users;
    @Autowired UserProfileRepository profiles;
    @Autowired PetSpeciesRepository species;
    @Autowired PetRepository pets;
    @MockitoBean AppUserDetailsService details;
    private AppUser owner, other;
    private PetSpecies petSpecies;
    private String ownerToken, adminToken, suffix;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().replace("-", "");
        owner = user("owner");
        other = user("other");
        ownerToken = token(owner, "OWNER");
        adminToken = token(user("admin"), "ADMIN");
        petSpecies = new PetSpecies();
        petSpecies.setSpeciesName("Prueba " + suffix);
        petSpecies = species.saveAndFlush(petSpecies);
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

    private String petJson(Long ownerId) {
        return """
                {"ownerId":%d,"speciesId":%d,"petName":"Luna","sex":"FEMALE","weightKg":4.25}
                """.formatted(ownerId, petSpecies.getId());
    }

    private Pet pet(AppUser user) {
        Pet pet = new Pet();
        pet.setOwner(user);
        pet.setSpecies(petSpecies);
        pet.setPetName("Fixture");
        return pets.saveAndFlush(pet);
    }

    private UserProfile profile(AppUser user) {
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setFullName("Perfil privado");
        return profiles.saveAndFlush(profile);
    }

    @Test
    void profileCrudUsesUserIdAndKeepsAuditOnServer() throws Exception {
        String route = "/api/v1/user-profiles/" + owner.getId();
        mvc.perform(post("/api/v1/user-profiles").header("Authorization", ownerToken)
                        .contentType("application/json").content("""
                        {"userId":%d,"fullName":"Persona de prueba","city":"Cali","phoneNumber":"3001234567"}
                        """.formatted(owner.getId())))
                .andExpect(status().isCreated()).andExpect(header().string("Location", endsWith(route)))
                .andExpect(jsonPath("$.userId").value(owner.getId()))
                .andExpect(jsonPath("$.profileUpdatedAt").isNotEmpty())
                .andExpect(jsonPath("$.user").doesNotExist());
        profile(other);
        mvc.perform(get("/api/v1/user-profiles").header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(route).header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.city").value("Cali"));
        mvc.perform(put(route).header("Authorization", ownerToken).contentType("application/json")
                        .content("{\"fullName\":\"Nombre actualizado\",\"city\":\"Bogotá\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Nombre actualizado"));
        mvc.perform(delete(route).header("Authorization", ownerToken)).andExpect(status().isNoContent());
        mvc.perform(get(route).header("Authorization", ownerToken)).andExpect(status().isNotFound());
        assertTrue(users.existsById(owner.getId()));
    }

    @Test
    void speciesCrudRequiresAdminForWrites() throws Exception {
        var created = mvc.perform(post("/api/v1/pet-species").header("Authorization", adminToken)
                        .contentType("application/json").content("{\"speciesName\":\"Nueva " + suffix + "\"}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        long id = JsonMapper.builder().build().readTree(created.getResponse().getContentAsString()).get("id").asLong();
        String route = "/api/v1/pet-species/" + id;
        mvc.perform(get("/api/v1/pet-species").header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(get(route).header("Authorization", ownerToken)).andExpect(status().isOk());
        mvc.perform(put(route).header("Authorization", adminToken).contentType("application/json")
                        .content("{\"speciesName\":\"Editada " + suffix + "\",\"activeFlag\":\"N\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activeFlag").value("N"));
        mvc.perform(delete(route).header("Authorization", adminToken)).andExpect(status().isNoContent());
        mvc.perform(get(route).header("Authorization", ownerToken)).andExpect(status().isNotFound());
    }

    @Test
    void ownerCannotChangeSpeciesCatalog() throws Exception {
        mvc.perform(post("/api/v1/pet-species").header("Authorization", ownerToken)
                        .contentType("application/json").content("{\"speciesName\":\"No permitido\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/pet-species/" + petSpecies.getId()).header("Authorization", ownerToken)
                        .contentType("application/json").content("{\"speciesName\":\"No permitido\",\"activeFlag\":\"Y\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/pet-species/" + petSpecies.getId()).header("Authorization", ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void petCrudPreservesOwnerAndBlocksDeletingUsedSpecies() throws Exception {
        var result = mvc.perform(post("/api/v1/pets").header("Authorization", ownerToken)
                        .contentType("application/json").content(petJson(owner.getId())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.petStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.ownerId").value(owner.getId())).andExpect(jsonPath("$.owner").doesNotExist())
                .andExpect(jsonPath("$.createdAt").isNotEmpty()).andReturn();
        long id = JsonMapper.builder().build().readTree(result.getResponse().getContentAsString()).get("id").asLong();
        String route = "/api/v1/pets/" + id;
        pet(other);
        mvc.perform(get("/api/v1/pets").header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(route).header("Authorization", ownerToken)).andExpect(status().isOk());
        mvc.perform(put(route).header("Authorization", ownerToken).contentType("application/json").content("""
                        {"ownerId":%d,"speciesId":%d,"petName":"Luna editada","petStatus":"ARCHIVED","weightKg":5.50}
                        """.formatted(other.getId(), petSpecies.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value(owner.getId()))
                .andExpect(jsonPath("$.petStatus").value("ARCHIVED"));
        mvc.perform(delete("/api/v1/pet-species/" + petSpecies.getId()).header("Authorization", adminToken))
                .andExpect(status().isConflict());
        mvc.perform(delete(route).header("Authorization", ownerToken)).andExpect(status().isNoContent());
        mvc.perform(get(route).header("Authorization", ownerToken)).andExpect(status().isNotFound());
    }

    @Test
    void ownersCannotReadUpdateDeleteOrCreateResourcesForOtherPeople() throws Exception {
        profile(other);
        Pet foreignPet = pet(other);
        String profileRoute = "/api/v1/user-profiles/" + other.getId();
        String petRoute = "/api/v1/pets/" + foreignPet.getId();
        for (String route : List.of(profileRoute, petRoute)) {
            mvc.perform(get(route).header("Authorization", ownerToken)).andExpect(status().isForbidden());
            mvc.perform(delete(route).header("Authorization", ownerToken)).andExpect(status().isForbidden());
        }
        mvc.perform(put(profileRoute).header("Authorization", ownerToken).contentType("application/json")
                        .content("{\"fullName\":\"No permitido\"}")).andExpect(status().isForbidden());
        mvc.perform(put(petRoute).header("Authorization", ownerToken).contentType("application/json").content("""
                        {"speciesId":%d,"petName":"No permitido","petStatus":"ACTIVE"}
                        """.formatted(petSpecies.getId()))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pets").header("Authorization", ownerToken)
                        .contentType("application/json").content(petJson(other.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/user-profiles").header("Authorization", ownerToken)
                        .contentType("application/json").content("""
                        {"userId":%d,"fullName":"No permitido"}
                        """.formatted(other.getId()))).andExpect(status().isForbidden());
        for (String collection : List.of("pets", "user-profiles")) {
            mvc.perform(get("/api/v1/" + collection).header("Authorization", ownerToken))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Test
    void adminCanManageAnotherUsersResources() throws Exception {
        mvc.perform(post("/api/v1/user-profiles").header("Authorization", adminToken)
                        .contentType("application/json").content("""
                        {"userId":%d,"fullName":"Creado por administrador"}
                        """.formatted(other.getId()))).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/user-profiles/" + other.getId()).header("Authorization", adminToken))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/pets").header("Authorization", adminToken)
                        .contentType("application/json").content(petJson(other.getId())))
                .andExpect(status().isCreated());
        Pet owned = pet(other);
        mvc.perform(delete("/api/v1/pets/" + owned.getId()).header("Authorization", adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidPetFieldsAndProfileLengthsReturn400() throws Exception {
        for (String field : List.of("\"weightKg\":0", "\"weightKg\":10000", "\"weightKg\":1.234",
                "\"sex\":\"INVALID\"", "\"birthDate\":\"" + LocalDate.now().plusDays(1) + "\"")) {
            String json = "{\"ownerId\":" + owner.getId() + ",\"speciesId\":" + petSpecies.getId()
                    + ",\"petName\":\"Luna\"," + field + "}";
            mvc.perform(post("/api/v1/pets").header("Authorization", ownerToken)
                            .contentType("application/json").content(json)).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/v1/user-profiles").header("Authorization", ownerToken)
                        .contentType("application/json").content("""
                        {"userId":%d,"fullName":"%s"}
                        """.formatted(owner.getId(), "X".repeat(141)))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/pets/-1").header("Authorization", ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inactiveSpeciesCannotBeAssignedButExistingPetCanBeEdited() throws Exception {
        Pet owned = pet(owner);
        petSpecies.setActiveFlag("N");
        species.saveAndFlush(petSpecies);
        mvc.perform(post("/api/v1/pets").header("Authorization", ownerToken)
                        .contentType("application/json").content(petJson(owner.getId())))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/pets/" + owned.getId()).header("Authorization", ownerToken)
                        .contentType("application/json").content("""
                        {"speciesId":%d,"petName":"Editada","petStatus":"ACTIVE"}
                        """.formatted(petSpecies.getId()))).andExpect(status().isOk());
    }

    @Test
    void duplicateProfileAndSpeciesReturn409AndMissingRelations404() throws Exception {
        profile(owner);
        mvc.perform(post("/api/v1/user-profiles").header("Authorization", ownerToken)
                        .contentType("application/json").content("""
                        {"userId":%d,"fullName":"Duplicado"}
                        """.formatted(owner.getId()))).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/pet-species").header("Authorization", adminToken)
                        .contentType("application/json").content("{\"speciesName\":\"  "
                                + petSpecies.getSpeciesName().toUpperCase() + "  \"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/pets").header("Authorization", ownerToken)
                        .contentType("application/json").content("""
                        {"ownerId":%d,"speciesId":999999999,"petName":"Luna"}
                        """.formatted(owner.getId()))).andExpect(status().isNotFound());
    }

    @Test
    void newResourcesRequireTokenAndAreDocumentedInSwagger() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var paths = JsonMapper.builder().build().readTree(result.getResponse().getContentAsString()).get("paths");
        for (String collection : List.of("user-profiles", "pet-species", "pets")) {
            String route = "/api/v1/" + collection;
            mvc.perform(get(route)).andExpect(status().isUnauthorized());
            for (String method : List.of("get", "post")) assertTrue(paths.get(route).has(method));
            for (String method : List.of("get", "put", "delete")) assertTrue(paths.get(route + "/{id}").has(method));
        }
    }
}

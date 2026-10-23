package com.icesi.cuidapetproyect.controller;

import com.icesi.cuidapetproyect.model.AppUser;
import com.icesi.cuidapetproyect.model.AppRole;
import java.util.List;
import com.icesi.cuidapetproyect.service.AppRoleService;
import com.icesi.cuidapetproyect.service.AppUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UserMvcController {
    private final AppUserService userService;
    private final AppRoleService roleService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String createForm(Model model) {
        if (!model.containsAttribute("userForm")) {
            model.addAttribute("userForm", new UserForm());
        }
        model.addAttribute("roles", roleService.findAll());
        model.addAttribute("editing", false);
        return "usuarios/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            AppUser user = userService.findById(id);
            if (!model.containsAttribute("userForm")) {
                UserForm form = new UserForm();
                form.setEmail(user.getEmail());
                form.setAccountStatus(user.getAccountStatus());
                // Nunca enviar el hash almacenado a la plantilla ni al formulario.
                model.addAttribute("userForm", form);
            }
            model.addAttribute("editing", true);
            model.addAttribute("userId", id);
            return "usuarios/formulario";
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", errorMessage(ex));
            return "redirect:/usuarios";
        }
    }

    @PostMapping("/crear")
    public String create(@Valid @ModelAttribute UserForm userForm, BindingResult result,
                         RedirectAttributes redirect) {
        validatePassword(userForm, result, true);
        if (userForm.getRoleIds() == null || userForm.getRoleIds().isEmpty()) {
            result.rejectValue("roleIds", "required", "Selecciona al menos un rol");
        }
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getAllErrors().getFirst().getDefaultMessage());
            return formError(userForm, redirect, "/usuarios/nuevo");
        }
        try {
            userService.create(toUser(userForm), userForm.getRoleIds());
            redirect.addFlashAttribute("message", "Usuario creado correctamente");
            return "redirect:/usuarios";
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", errorMessage(ex));
            return formError(userForm, redirect, "/usuarios/nuevo");
        }
    }

    @PostMapping("/{id}/editar")
    public String update(@PathVariable Long id, @Valid @ModelAttribute UserForm userForm,
                         BindingResult result, RedirectAttributes redirect) {
        validatePassword(userForm, result, false);
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getAllErrors().getFirst().getDefaultMessage());
            return formError(userForm, redirect, "/usuarios/" + id + "/editar");
        }
        try {
            userService.update(id, toUser(userForm));
            redirect.addFlashAttribute("message", "Usuario actualizado correctamente");
            return "redirect:/usuarios";
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", errorMessage(ex));
            return formError(userForm, redirect, "/usuarios/" + id + "/editar");
        }
    }

    @PostMapping("/{id}/eliminar")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            userService.delete(id);
            redirect.addFlashAttribute("message", "Usuario marcado como eliminado");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", errorMessage(ex));
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/{id}/roles")
    public String roles(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            AppUser user = userService.findById(id);
            List<AppRole> assignedRoles = userService.findRoles(id);
            List<Long> assignedIds = assignedRoles.stream().map(AppRole::getId).toList();
            model.addAttribute("user", user);
            model.addAttribute("assignedRoles", assignedRoles);
            model.addAttribute("availableRoles", roleService.findAll().stream()
                    .filter(role -> !assignedIds.contains(role.getId()))
                    .toList());
            return "usuarios/roles";
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", errorMessage(ex));
            return "redirect:/usuarios";
        }
    }

    @PostMapping("/{id}/roles/asignar")
    public String assignRole(@PathVariable Long id, @RequestParam(required = false) Long roleId,
                             RedirectAttributes redirect) {
        if (roleId == null) {
            redirect.addFlashAttribute("error", "Selecciona un rol");
            return "redirect:/usuarios/" + id + "/roles";
        }
        try {
            userService.assignRole(id, roleId);
            redirect.addFlashAttribute("message", "Rol asignado correctamente");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", errorMessage(ex));
        }
        return "redirect:/usuarios/" + id + "/roles";
    }

    @PostMapping("/{id}/roles/{roleId}/quitar")
    public String removeRole(@PathVariable Long id, @PathVariable Long roleId,
                             RedirectAttributes redirect) {
        try {
            userService.removeRole(id, roleId);
            redirect.addFlashAttribute("message", "Rol retirado correctamente");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", errorMessage(ex));
        }
        return "redirect:/usuarios/" + id + "/roles";
    }

    private void validatePassword(UserForm form, BindingResult result, boolean creating) {
        String password = form.getPassword();
        if (password == null || password.isBlank()) {
            if (creating) {
                result.rejectValue("password", "required", "La contraseña es obligatoria");
            }
        } else if (password.length() < 8) {
            result.rejectValue("password", "short", "La contraseña debe tener al menos 8 caracteres");
        }
    }

    private AppUser toUser(UserForm form) {
        AppUser user = new AppUser();
        user.setEmail(form.getEmail().trim());
        user.setPasswordHash(form.getPassword());
        user.setAccountStatus(form.getAccountStatus());
        return user;
    }

    private String formError(UserForm form, RedirectAttributes redirect, String path) {
        form.setPassword(null);
        redirect.addFlashAttribute("userForm", form);
        return "redirect:" + path;
    }

    private String errorMessage(RuntimeException ex) {
        // Los servicios previos lanzan RuntimeException para sus reglas de negocio.
        // Los errores de persistencia no deben exponer SQL ni datos de conexión.
        if (ex instanceof DataAccessException || ex.getMessage() == null) {
            return "No fue posible completar la operación. Inténtalo de nuevo.";
        }
        return ex.getMessage();
    }
}

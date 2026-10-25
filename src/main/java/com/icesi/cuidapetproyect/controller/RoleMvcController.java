package com.icesi.cuidapetproyect.controller;

import com.icesi.cuidapetproyect.model.AppRole;
import com.icesi.cuidapetproyect.service.AppPermissionService;
import com.icesi.cuidapetproyect.service.AppRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleMvcController {
    private final AppRoleService roleService;
    private final AppPermissionService permissionService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("roles", roleService.findAll());
        model.addAttribute("systemRoles", AppRoleService.SYSTEM_ROLES);
        return "roles/lista";
    }

    @GetMapping("/nuevo")
    public String createForm(Model model) {
        if (!model.containsAttribute("roleForm")) {
            model.addAttribute("roleForm", new RoleForm());
        }
        model.addAttribute("permissions", permissionService.findAll());
        model.addAttribute("editing", false);
        model.addAttribute("systemRole", false);
        return "roles/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            AppRole role = roleService.findById(id);
            if (!model.containsAttribute("roleForm")) {
                RoleForm form = new RoleForm();
                form.setRoleName(role.getRoleName());
                form.setDescription(role.getDescription());
                form.setActiveFlag(role.getActiveFlag());
                model.addAttribute("roleForm", form);
            }
            model.addAttribute("roleId", id);
            model.addAttribute("editing", true);
            model.addAttribute("systemRole", roleService.isSystemRole(role));
            return "roles/formulario";
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", MvcMessages.errorMessage(ex));
            return "redirect:/roles";
        }
    }

    @PostMapping("/crear")
    public String create(@Valid @ModelAttribute RoleForm roleForm, BindingResult result,
                         RedirectAttributes redirect) {
        if (roleForm.getPermissionIds() == null || roleForm.getPermissionIds().isEmpty()) {
            result.rejectValue("permissionIds", "required", "Selecciona al menos un permiso");
        }
        if (result.hasErrors()) {
            return formError(roleForm, result.getAllErrors().getFirst().getDefaultMessage(),
                    redirect, "/roles/nuevo");
        }
        try {
            roleService.create(toRole(roleForm), roleForm.getPermissionIds());
            redirect.addFlashAttribute("message", "Rol creado correctamente");
            return "redirect:/roles";
        } catch (RuntimeException ex) {
            return formError(roleForm, MvcMessages.errorMessage(ex), redirect, "/roles/nuevo");
        }
    }

    @PostMapping("/{id}/editar")
    public String update(@PathVariable Long id, @Valid @ModelAttribute RoleForm roleForm,
                         BindingResult result, RedirectAttributes redirect) {
        String path = "/roles/" + id + "/editar";
        if (result.hasErrors()) {
            return formError(roleForm, result.getAllErrors().getFirst().getDefaultMessage(), redirect, path);
        }
        try {
            roleService.update(id, toRole(roleForm));
            redirect.addFlashAttribute("message", "Rol actualizado correctamente");
            return "redirect:/roles";
        } catch (RuntimeException ex) {
            return formError(roleForm, MvcMessages.errorMessage(ex), redirect, path);
        }
    }

    @PostMapping("/{id}/eliminar")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            roleService.delete(id);
            redirect.addFlashAttribute("message", "Rol eliminado correctamente");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", MvcMessages.errorMessage(ex));
        }
        return "redirect:/roles";
    }

    private AppRole toRole(RoleForm form) {
        AppRole role = new AppRole();
        role.setRoleName(form.getRoleName());
        role.setDescription(form.getDescription());
        role.setActiveFlag(form.getActiveFlag());
        return role;
    }

    private String formError(RoleForm form, String message, RedirectAttributes redirect, String path) {
        redirect.addFlashAttribute("error", message);
        redirect.addFlashAttribute("roleForm", form);
        return "redirect:" + path;
    }
}

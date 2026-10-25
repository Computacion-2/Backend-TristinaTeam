package com.icesi.cuidapetproyect.controller;

import com.icesi.cuidapetproyect.model.AppPermission;
import com.icesi.cuidapetproyect.service.AppPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/permisos")
@RequiredArgsConstructor
public class PermissionMvcController {
    private final AppPermissionService permissionService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("permissions", permissionService.findAll());
        model.addAttribute("systemPermissions", AppPermissionService.SYSTEM_PERMISSIONS);
        return "permisos/lista";
    }

    @GetMapping("/nuevo")
    public String createForm(Model model) {
        if (!model.containsAttribute("permissionForm")) {
            model.addAttribute("permissionForm", new PermissionForm());
        }
        model.addAttribute("editing", false);
        return "permisos/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            AppPermission permission = permissionService.findById(id);
            if (!model.containsAttribute("permissionForm")) {
                PermissionForm form = new PermissionForm();
                form.setPermissionName(permission.getPermissionName());
                form.setDescription(permission.getDescription());
                model.addAttribute("permissionForm", form);
            }
            model.addAttribute("permissionCode", permission.getPermissionCode());
            model.addAttribute("permissionId", id);
            model.addAttribute("editing", true);
            return "permisos/formulario";
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", MvcMessages.errorMessage(ex));
            return "redirect:/permisos";
        }
    }

    @PostMapping("/crear")
    public String create(@Valid @ModelAttribute PermissionForm permissionForm, BindingResult result,
                         RedirectAttributes redirect) {
        if (permissionForm.getPermissionCode() == null || permissionForm.getPermissionCode().isBlank()) {
            result.rejectValue("permissionCode", "required", "El código del permiso es obligatorio");
        }
        if (result.hasErrors()) {
            return formError(permissionForm, result, redirect, "/permisos/nuevo");
        }
        try {
            permissionService.create(toPermission(permissionForm, true));
            redirect.addFlashAttribute("message", "Permiso creado correctamente");
            return "redirect:/permisos";
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", MvcMessages.errorMessage(ex));
            redirect.addFlashAttribute("permissionForm", permissionForm);
            return "redirect:/permisos/nuevo";
        }
    }

    @PostMapping("/{id}/editar")
    public String update(@PathVariable Long id, @Valid @ModelAttribute PermissionForm permissionForm,
                         BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return formError(permissionForm, result, redirect, "/permisos/" + id + "/editar");
        }
        try {
            permissionService.update(id, toPermission(permissionForm, false));
            redirect.addFlashAttribute("message", "Permiso actualizado correctamente");
            return "redirect:/permisos";
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", MvcMessages.errorMessage(ex));
            redirect.addFlashAttribute("permissionForm", permissionForm);
            return "redirect:/permisos/" + id + "/editar";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            permissionService.delete(id);
            redirect.addFlashAttribute("message", "Permiso eliminado correctamente");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", MvcMessages.errorMessage(ex));
        }
        return "redirect:/permisos";
    }

    private AppPermission toPermission(PermissionForm form, boolean creating) {
        AppPermission permission = new AppPermission();
        if (creating) {
            permission.setPermissionCode(form.getPermissionCode());
        }
        permission.setPermissionName(form.getPermissionName());
        permission.setDescription(form.getDescription());
        return permission;
    }

    private String formError(PermissionForm form, BindingResult result, RedirectAttributes redirect,
                             String path) {
        redirect.addFlashAttribute("error", result.getAllErrors().getFirst().getDefaultMessage());
        redirect.addFlashAttribute("permissionForm", form);
        return "redirect:" + path;
    }
}

package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.brunasti.dbdadi.frontend.client.UserManagementClient;
import it.brunasti.dbdadi.frontend.dto.UserDto;
import it.brunasti.dbdadi.frontend.dto.UserRole;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("DBDaDi | Users")
@RolesAllowed("ADMIN")
@Slf4j
public class UserManagementView extends VerticalLayout {

    private final UserManagementClient client;
    private final Grid<UserDto> grid = new Grid<>(UserDto.class, false);

    public UserManagementView(UserManagementClient client) {
        this.client = client;
        setSizeFull();
        configureGrid();
        add(createToolbar(), grid);
        refresh();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(UserDto::getUsername).setHeader("Username").setSortable(true);
        grid.addColumn(u -> u.getRole() != null ? u.getRole().name() : "").setHeader("Role").setSortable(true);
        grid.addColumn(u -> u.isEnabled() ? "Yes" : "No").setHeader("Enabled").setWidth("90px").setFlexGrow(0);
        grid.addColumn(UserDto::getCreatedAt).setHeader("Created").setSortable(true);
        grid.addComponentColumn(item -> {
            Button edit = new Button("Edit", e -> openDialog(item));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
            Button delete = new Button("Delete", e -> confirmDelete(item));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, delete);
        }).setHeader("Actions").setWidth("160px").setFlexGrow(0);
    }

    private HorizontalLayout createToolbar() {
        Button addBtn = new Button("New User", e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button refreshBtn = new Button("Refresh", e -> refresh());
        return new HorizontalLayout(addBtn, refreshBtn);
    }

    private void openDialog(UserDto item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item == null ? "New User" : "Edit User");
        dialog.setWidth("420px");

        TextField username = new TextField("Username");
        PasswordField password = new PasswordField(item == null ? "Password" : "New Password (leave blank to keep)");
        ComboBox<UserRole> role = new ComboBox<>("Role", UserRole.values());
        Checkbox enabled = new Checkbox("Enabled");
        enabled.setValue(true);

        if (item != null) {
            username.setValue(item.getUsername() != null ? item.getUsername() : "");
            role.setValue(item.getRole());
            enabled.setValue(item.isEnabled());
        }

        FormLayout form = new FormLayout(username, password, role, enabled);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button save = new Button("Save", e -> {
            try {
                UserDto dto = UserDto.builder()
                        .username(username.getValue())
                        .password(password.getValue().isBlank() ? null : password.getValue())
                        .role(role.getValue())
                        .enabled(enabled.getValue())
                        .build();
                if (item == null) client.create(dto);
                else client.update(item.getId(), dto);
                dialog.close();
                refresh();
                notify("Saved successfully", false);
            } catch (Exception ex) {
                log.error("Save failed", ex);
                notify("Save failed: " + ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.add(form);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmDelete(UserDto item) {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete user \"" + item.getUsername() + "\"?",
                "This action cannot be undone.",
                "Delete", e -> {
                    try {
                        client.delete(item.getId());
                        refresh();
                        notify("Deleted successfully", false);
                    } catch (Exception ex) {
                        notify("Delete failed: " + ex.getMessage(), true);
                    }
                },
                "Cancel", e -> {});
        confirm.setConfirmButtonTheme("error primary");
        confirm.open();
    }

    private void refresh() {
        try {
            grid.setItems(client.findAll());
        } catch (Exception e) {
            log.error("Failed to load users", e);
            notify("Could not load users: " + e.getMessage(), true);
        }
    }

    private void notify(String message, boolean error) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}

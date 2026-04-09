package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import it.brunasti.dbdadi.frontend.client.EntityDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.EntityDefinitionDto;
import lombok.extern.slf4j.Slf4j;

@Route(value = "entities", layout = MainLayout.class)
@PageTitle("DBDaDi | Entities")
@AnonymousAllowed
@Slf4j
public class EntityDefinitionView extends VerticalLayout {

    private final EntityDefinitionClient client;
    private final Grid<EntityDefinitionDto> grid = new Grid<>(EntityDefinitionDto.class, false);

    public EntityDefinitionView(EntityDefinitionClient client) {
        this.client = client;
        setSizeFull();
        configureGrid();
        add(createToolbar(), grid);
        refresh();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(EntityDefinitionDto::getId).setHeader("ID").setWidth("70px").setFlexGrow(0).setSortable(true);
        grid.addComponentColumn(item -> {
            Button btn = new Button(item.getName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("entities/" + item.getId()));
            return btn;
        }).setHeader("Name").setComparator(EntityDefinitionDto::getName);
        grid.addColumn(EntityDefinitionDto::getDescription).setHeader("Description").setSortable(true);
        grid.addComponentColumn(item -> {
            Button edit = new Button("Edit", e -> openDialog(item));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
            Button delete = new Button("Delete", e -> confirmDelete(item));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, delete);
        }).setHeader("Actions").setWidth("160px").setFlexGrow(0);
    }

    private HorizontalLayout createToolbar() {
        Button addBtn = new Button("New Entity", e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button refreshBtn = new Button("Refresh", e -> refresh());
        return new HorizontalLayout(addBtn, refreshBtn);
    }

    private void openDialog(EntityDefinitionDto item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item == null ? "New Entity" : "Edit Entity");
        dialog.setWidth("500px");

        TextField name = new TextField("Name");
        TextArea description = new TextArea("Description");

        if (item != null) {
            name.setValue(item.getName() != null ? item.getName() : "");
            description.setValue(item.getDescription() != null ? item.getDescription() : "");
        }

        FormLayout form = new FormLayout(name, description);
        form.setColspan(description, 2);

        Button save = new Button("Save", e -> {
            try {
                EntityDefinitionDto dto = EntityDefinitionDto.builder()
                        .name(name.getValue())
                        .description(description.getValue())
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

    private void confirmDelete(EntityDefinitionDto item) {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete entity \"" + item.getName() + "\"?",
                "This action cannot be undone. Tables linked to this entity will be unlinked.",
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
            log.error("Failed to load entities", e);
            notify("Could not load data: " + e.getMessage(), true);
        }
    }

    private void notify(String message, boolean error) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}

package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
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
import jakarta.annotation.security.PermitAll;
import it.brunasti.dbdadi.frontend.security.SecurityUtils;
import it.brunasti.dbdadi.frontend.client.DomainDefinitionClient;
import it.brunasti.dbdadi.frontend.client.EntityDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.DomainDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.EntityDefinitionDto;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "domains", layout = MainLayout.class)
@PageTitle("DBDaDi | Domains")
@PermitAll
@Slf4j
public class DomainDefinitionView extends VerticalLayout {

    private final DomainDefinitionClient client;
    private final EntityDefinitionClient entityClient;
    private final Grid<DomainDefinitionDto> grid = new Grid<>(DomainDefinitionDto.class, false);

    public DomainDefinitionView(DomainDefinitionClient client, EntityDefinitionClient entityClient) {
        this.client = client;
        this.entityClient = entityClient;
        setSizeFull();
        configureGrid();
        add(createToolbar(), grid);
        refresh();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addComponentColumn(item -> {
            Button btn = new Button(item.getName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("domains/" + item.getId()));
            return btn;
        }).setHeader("Name").setComparator(DomainDefinitionDto::getName);
        grid.addColumn(DomainDefinitionDto::getDescription).setHeader("Description").setSortable(true);
        grid.addComponentColumn(item -> {
            Button edit = new Button("Edit", e -> openDialog(item));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
            Button delete = new Button("Delete", e -> confirmDelete(item));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            if (!SecurityUtils.canEdit()) return new com.vaadin.flow.component.html.Span();
            return new HorizontalLayout(edit, delete);
        }).setHeader("Actions").setWidth("160px").setFlexGrow(0);
    }

    private HorizontalLayout createToolbar() {
        Button addBtn = new Button("New Domain", e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.setVisible(SecurityUtils.canEdit());
        Button refreshBtn = new Button("Refresh", e -> refresh());
        return new HorizontalLayout(addBtn, refreshBtn);
    }

    private void openDialog(DomainDefinitionDto item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item == null ? "New Domain" : "Edit Domain");
        dialog.setWidth("560px");

        TextField name = new TextField("Name");
        TextArea description = new TextArea("Description");

        MultiSelectComboBox<EntityDefinitionDto> entitiesBox = new MultiSelectComboBox<>("Entities");
        entitiesBox.setWidthFull();
        entitiesBox.setItemLabelGenerator(EntityDefinitionDto::getName);
        try {
            entitiesBox.setItems(entityClient.findAll());
        } catch (Exception e) {
            log.warn("Could not load entities for selector");
        }

        if (item != null) {
            name.setValue(item.getName() != null ? item.getName() : "");
            description.setValue(item.getDescription() != null ? item.getDescription() : "");
            try {
                List<EntityDefinitionDto> current = client.findEntities(item.getId());
                entitiesBox.setValue(Set.copyOf(current));
            } catch (Exception e) {
                log.warn("Could not load current entities for domain {}", item.getId());
            }
        }

        FormLayout form = new FormLayout(name, description, entitiesBox);
        form.setColspan(description, 2);
        form.setColspan(entitiesBox, 2);

        Button save = new Button("Save", e -> {
            try {
                DomainDefinitionDto dto = DomainDefinitionDto.builder()
                        .name(name.getValue())
                        .description(description.getValue())
                        .build();
                DomainDefinitionDto saved;
                if (item == null) {
                    saved = client.create(dto);
                } else {
                    saved = client.update(item.getId(), dto);
                }
                List<Long> entityIds = entitiesBox.getSelectedItems().stream()
                        .map(EntityDefinitionDto::getId)
                        .collect(Collectors.toList());
                client.setEntities(saved.getId(), entityIds);
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

    private void confirmDelete(DomainDefinitionDto item) {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete domain \"" + item.getName() + "\"?",
                "This action cannot be undone. Entity links will be removed.",
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
            log.error("Failed to load domains", e);
            notify("Could not load data: " + e.getMessage(), true);
        }
    }

    private void notify(String message, boolean error) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}

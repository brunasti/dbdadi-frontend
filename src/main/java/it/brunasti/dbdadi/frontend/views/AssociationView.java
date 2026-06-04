package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
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
import it.brunasti.dbdadi.frontend.client.AssociationClient;
import it.brunasti.dbdadi.frontend.client.EntityDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.AssociationDto;
import it.brunasti.dbdadi.frontend.dto.EntityDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.RelationshipType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Route(value = "associations", layout = MainLayout.class)
@PageTitle("DBDaDi | Associations")
@PermitAll
@Slf4j
public class AssociationView extends VerticalLayout {

    private final AssociationClient client;
    private final EntityDefinitionClient entityClient;
    private final Grid<AssociationDto> grid = new Grid<>(AssociationDto.class, false);

    public AssociationView(AssociationClient client, EntityDefinitionClient entityClient) {
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
            btn.addClickListener(e -> UI.getCurrent().navigate("associations/" + item.getId()));
            return btn;
        }).setHeader("Name").setComparator(AssociationDto::getName);
        grid.addComponentColumn(item -> {
            if (item.getFromEntityId() != null) {
                Button btn = new Button(item.getFromEntityName() != null ? item.getFromEntityName() : "");
                btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                btn.getStyle().set("padding", "0");
                btn.addClickListener(e -> UI.getCurrent().navigate("entities/" + item.getFromEntityId()));
                return btn;
            }
            return new Span();
        }).setHeader("From Entity").setComparator(item -> item.getFromEntityName() != null ? item.getFromEntityName() : "");
        grid.addComponentColumn(item -> {
            Span badge = new Span(item.getType() != null ? item.getType().name().replace('_', ':') : "");
            badge.getStyle()
                    .set("font-size", "0.8em")
                    .set("font-family", "monospace")
                    .set("color", "var(--lumo-secondary-text-color)");
            return badge;
        }).setHeader("Type").setWidth("130px").setFlexGrow(0);
        grid.addComponentColumn(item -> {
            if (item.getToEntityId() != null) {
                Button btn = new Button(item.getToEntityName() != null ? item.getToEntityName() : "");
                btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                btn.getStyle().set("padding", "0");
                btn.addClickListener(e -> UI.getCurrent().navigate("entities/" + item.getToEntityId()));
                return btn;
            }
            return new Span();
        }).setHeader("To Entity").setComparator(item -> item.getToEntityName() != null ? item.getToEntityName() : "");
        grid.addColumn(AssociationDto::getDescription).setHeader("Description").setSortable(true);
        grid.addComponentColumn(item -> {
            if (!SecurityUtils.canEdit()) return new Span();
            Button edit = new Button("Edit", e -> openDialog(item));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
            Button delete = new Button("Delete", e -> confirmDelete(item));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, delete);
        }).setHeader("Actions").setWidth("160px").setFlexGrow(0);
    }

    private HorizontalLayout createToolbar() {
        Button addBtn = new Button("New Association", e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.setVisible(SecurityUtils.canEdit());
        Button refreshBtn = new Button("Refresh", e -> refresh());
        return new HorizontalLayout(addBtn, refreshBtn);
    }

    private void openDialog(AssociationDto item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item == null ? "New Association" : "Edit Association");
        dialog.setWidth("580px");

        TextField name = new TextField("Name");
        TextArea description = new TextArea("Description");

        ComboBox<EntityDefinitionDto> fromEntityBox = new ComboBox<>("From Entity");
        fromEntityBox.setItemLabelGenerator(EntityDefinitionDto::getName);
        fromEntityBox.setPlaceholder("Select entity…");
        fromEntityBox.setWidthFull();

        ComboBox<EntityDefinitionDto> toEntityBox = new ComboBox<>("To Entity");
        toEntityBox.setItemLabelGenerator(EntityDefinitionDto::getName);
        toEntityBox.setPlaceholder("Select entity…");
        toEntityBox.setWidthFull();

        ComboBox<RelationshipType> typeBox = new ComboBox<>("Type");
        typeBox.setItems(RelationshipType.values());
        typeBox.setItemLabelGenerator(t -> t.name().replace('_', ':'));
        typeBox.setWidthFull();

        try {
            List<EntityDefinitionDto> entities = entityClient.findAll();
            fromEntityBox.setItems(entities);
            toEntityBox.setItems(entities);
        } catch (Exception e) {
            log.warn("Could not load entities for association dialog");
        }

        if (item != null) {
            name.setValue(item.getName() != null ? item.getName() : "");
            description.setValue(item.getDescription() != null ? item.getDescription() : "");
            typeBox.setValue(item.getType());
            if (item.getFromEntityId() != null) {
                fromEntityBox.setValue(EntityDefinitionDto.builder()
                        .id(item.getFromEntityId()).name(item.getFromEntityName()).build());
            }
            if (item.getToEntityId() != null) {
                toEntityBox.setValue(EntityDefinitionDto.builder()
                        .id(item.getToEntityId()).name(item.getToEntityName()).build());
            }
        }

        FormLayout form = new FormLayout(name, typeBox, fromEntityBox, toEntityBox, description);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(description, 2);

        Button save = new Button("Save", e -> {
            if (fromEntityBox.getValue() == null || toEntityBox.getValue() == null || typeBox.getValue() == null) {
                notify("From entity, to entity and type are required", true);
                return;
            }
            try {
                AssociationDto dto = AssociationDto.builder()
                        .name(name.getValue())
                        .description(description.getValue())
                        .type(typeBox.getValue())
                        .fromEntityId(fromEntityBox.getValue().getId())
                        .toEntityId(toEntityBox.getValue().getId())
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

    private void confirmDelete(AssociationDto item) {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete association \"" + item.getName() + "\"?",
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
            log.error("Failed to load associations", e);
            notify("Could not load data: " + e.getMessage(), true);
        }
    }

    private void notify(String message, boolean error) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}

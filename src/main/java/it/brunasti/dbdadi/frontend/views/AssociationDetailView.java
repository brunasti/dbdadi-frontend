package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
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

@Route(value = "associations/:associationId", layout = MainLayout.class)
@PageTitle("DBDaDi | Association")
@PermitAll
@Slf4j
public class AssociationDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final AssociationClient client;
    private final EntityDefinitionClient entityClient;
    private AssociationDto association;

    private final TextField nameField = new TextField("Name");
    private final TextArea descriptionField = new TextArea("Description");
    private final TextField typeField = new TextField("Type");

    public AssociationDetailView(AssociationClient client, EntityDefinitionClient entityClient) {
        this.client = client;
        this.entityClient = entityClient;
        setWidthFull();
        setPadding(true);
        configureFields();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("associationId").map(Long::valueOf).ifPresent(id -> {
            try {
                association = client.findById(id);
                populateFields();
            } catch (Exception e) {
                log.error("Could not load association {}", id, e);
                notify("Could not load association", true);
            }
        });
    }

    private void configureFields() {
        nameField.setReadOnly(true);
        descriptionField.setReadOnly(true);
        descriptionField.setWidthFull();
        typeField.setReadOnly(true);
    }

    private void populateFields() {
        removeAll();

        HorizontalLayout breadcrumb = new HorizontalLayout();
        Button backToList = new Button("← Associations");
        backToList.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToList.addClickListener(e -> UI.getCurrent().navigate(AssociationView.class));
        breadcrumb.add(backToList, new Span(" / "), new Span("Association: " + association.getName()));
        breadcrumb.setAlignItems(Alignment.CENTER);
        add(breadcrumb);

        nameField.setValue(association.getName() != null ? association.getName() : "");
        descriptionField.setValue(association.getDescription() != null ? association.getDescription() : "");
        typeField.setValue(association.getType() != null ? association.getType().name().replace('_', ':') : "");

        FormLayout form = new FormLayout(nameField, typeField, descriptionField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(descriptionField, 2);
        add(form);

        add(new Hr(), new H3("Entities"));

        HorizontalLayout entityLinks = new HorizontalLayout();
        entityLinks.setAlignItems(Alignment.CENTER);
        entityLinks.setSpacing(true);

        if (association.getFromEntityId() != null) {
            Button fromBtn = new Button("From: " + association.getFromEntityName());
            fromBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            fromBtn.getStyle().set("font-weight", "bold");
            fromBtn.addClickListener(e -> UI.getCurrent().navigate("entities/" + association.getFromEntityId()));
            entityLinks.add(fromBtn);
        }

        Span arrow = new Span(" → ");
        arrow.getStyle().set("font-size", "1.2em").set("color", "var(--lumo-secondary-text-color)");
        entityLinks.add(arrow);

        Span typeBadge = new Span(association.getType() != null ? association.getType().name().replace('_', ':') : "");
        typeBadge.getStyle()
                .set("font-family", "monospace")
                .set("font-size", "0.85em")
                .set("color", "var(--lumo-primary-color)")
                .set("border", "1px solid var(--lumo-primary-color)")
                .set("border-radius", "4px")
                .set("padding", "2px 6px");
        entityLinks.add(typeBadge);

        Span arrow2 = new Span(" → ");
        arrow2.getStyle().set("font-size", "1.2em").set("color", "var(--lumo-secondary-text-color)");
        entityLinks.add(arrow2);

        if (association.getToEntityId() != null) {
            Button toBtn = new Button("To: " + association.getToEntityName());
            toBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            toBtn.getStyle().set("font-weight", "bold");
            toBtn.addClickListener(e -> UI.getCurrent().navigate("entities/" + association.getToEntityId()));
            entityLinks.add(toBtn);
        }

        add(entityLinks);

        Button editBtn = new Button("Edit", e -> openEditDialog());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editBtn.setVisible(SecurityUtils.canEdit());
        Button deleteBtn = new Button("Delete", e -> confirmDelete());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteBtn.setVisible(SecurityUtils.canEdit());

        add(new Hr(), new HorizontalLayout(editBtn, deleteBtn));
    }

    private void openEditDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Association");
        dialog.setWidth("580px");

        TextField name = new TextField("Name");
        TextArea description = new TextArea("Description");
        name.setValue(association.getName() != null ? association.getName() : "");
        description.setValue(association.getDescription() != null ? association.getDescription() : "");

        ComboBox<EntityDefinitionDto> fromEntityBox = new ComboBox<>("From Entity");
        fromEntityBox.setItemLabelGenerator(EntityDefinitionDto::getName);
        fromEntityBox.setWidthFull();

        ComboBox<EntityDefinitionDto> toEntityBox = new ComboBox<>("To Entity");
        toEntityBox.setItemLabelGenerator(EntityDefinitionDto::getName);
        toEntityBox.setWidthFull();

        ComboBox<RelationshipType> typeBox = new ComboBox<>("Type");
        typeBox.setItems(RelationshipType.values());
        typeBox.setItemLabelGenerator(t -> t.name().replace('_', ':'));
        typeBox.setWidthFull();
        typeBox.setValue(association.getType());

        try {
            List<EntityDefinitionDto> entities = entityClient.findAll();
            fromEntityBox.setItems(entities);
            toEntityBox.setItems(entities);
            if (association.getFromEntityId() != null) {
                fromEntityBox.setValue(EntityDefinitionDto.builder()
                        .id(association.getFromEntityId()).name(association.getFromEntityName()).build());
            }
            if (association.getToEntityId() != null) {
                toEntityBox.setValue(EntityDefinitionDto.builder()
                        .id(association.getToEntityId()).name(association.getToEntityName()).build());
            }
        } catch (Exception e) {
            log.warn("Could not load entities for association editor");
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
                association = client.update(association.getId(), dto);
                dialog.close();
                populateFields();
                notify("Saved successfully", false);
            } catch (Exception ex) {
                notify("Save failed: " + ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.add(form);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmDelete() {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete association \"" + association.getName() + "\"?",
                "This action cannot be undone.",
                "Delete", e -> {
                    try {
                        client.delete(association.getId());
                        UI.getCurrent().navigate(AssociationView.class);
                    } catch (Exception ex) {
                        notify("Delete failed: " + ex.getMessage(), true);
                    }
                },
                "Cancel", e -> {});
        confirm.setConfirmButtonTheme("error primary");
        confirm.open();
    }

    private void notify(String message, boolean error) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}

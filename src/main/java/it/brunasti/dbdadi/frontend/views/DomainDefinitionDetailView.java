package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
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
import it.brunasti.dbdadi.frontend.client.DomainDefinitionClient;
import it.brunasti.dbdadi.frontend.client.EntityDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.DomainDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.EntityDefinitionDto;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "domains/:domainId", layout = MainLayout.class)
@PageTitle("DBDaDi | Domain")
@PermitAll
@Slf4j
public class DomainDefinitionDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final DomainDefinitionClient client;
    private final EntityDefinitionClient entityClient;
    private DomainDefinitionDto domain;

    private final TextField nameField = new TextField("Name");
    private final TextArea descriptionField = new TextArea("Description");
    private final Grid<EntityDefinitionDto> entitiesGrid = new Grid<>(EntityDefinitionDto.class, false);

    public DomainDefinitionDetailView(DomainDefinitionClient client, EntityDefinitionClient entityClient) {
        this.client = client;
        this.entityClient = entityClient;
        setWidthFull();
        setPadding(true);
        configureFields();
        configureEntitiesGrid();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("domainId").map(Long::valueOf).ifPresent(id -> {
            try {
                domain = client.findById(id);
                populateView();
                entitiesGrid.setItems(client.findEntities(id));
            } catch (Exception e) {
                log.error("Could not load domain {}", id, e);
                notify("Could not load domain", true);
            }
        });
    }

    private void configureFields() {
        nameField.setReadOnly(true);
        descriptionField.setReadOnly(true);
        descriptionField.setWidthFull();
    }

    private void configureEntitiesGrid() {
        entitiesGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("entities/" + item.getId()));
            return btn;
        }).setHeader("Entity Name").setComparator(Comparator.comparing(EntityDefinitionDto::getName));
        entitiesGrid.addColumn(EntityDefinitionDto::getDescription).setHeader("Description").setSortable(true);
        entitiesGrid.setAllRowsVisible(true);
    }

    private void populateView() {
        removeAll();

        HorizontalLayout breadcrumb = new HorizontalLayout();
        Button backToList = new Button("← Domains");
        backToList.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToList.addClickListener(e -> UI.getCurrent().navigate(DomainDefinitionView.class));
        breadcrumb.add(backToList, new Span(" / "), new Span("Domain: " + domain.getName()));
        breadcrumb.setAlignItems(Alignment.CENTER);
        add(breadcrumb);

        nameField.setValue(domain.getName() != null ? domain.getName() : "");
        descriptionField.setValue(domain.getDescription() != null ? domain.getDescription() : "");

        FormLayout form = new FormLayout(nameField, descriptionField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(descriptionField, 2);

        Button editBtn = new Button("Edit", e -> openEditDialog());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editBtn.setVisible(SecurityUtils.canEdit());
        Button deleteBtn = new Button("Delete", e -> confirmDelete());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteBtn.setVisible(SecurityUtils.canEdit());

        add(form, new HorizontalLayout(editBtn, deleteBtn),
            new Hr(), new H3("Linked Entities"), entitiesGrid);
    }

    private void openEditDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Domain");
        dialog.setWidth("560px");

        TextField name = new TextField("Name");
        TextArea description = new TextArea("Description");
        name.setValue(domain.getName() != null ? domain.getName() : "");
        description.setValue(domain.getDescription() != null ? domain.getDescription() : "");

        MultiSelectComboBox<EntityDefinitionDto> entitiesBox = new MultiSelectComboBox<>("Entities");
        entitiesBox.setWidthFull();
        entitiesBox.setItemLabelGenerator(EntityDefinitionDto::getName);
        try {
            entitiesBox.setItems(entityClient.findAll());
            List<EntityDefinitionDto> current = client.findEntities(domain.getId());
            entitiesBox.setValue(Set.copyOf(current));
        } catch (Exception e) {
            log.warn("Could not load entities for domain editor");
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
                domain = client.update(domain.getId(), dto);
                List<Long> entityIds = entitiesBox.getSelectedItems().stream()
                        .map(EntityDefinitionDto::getId)
                        .collect(Collectors.toList());
                client.setEntities(domain.getId(), entityIds);
                dialog.close();
                populateView();
                entitiesGrid.setItems(client.findEntities(domain.getId()));
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
                "Delete domain \"" + domain.getName() + "\"?",
                "This action cannot be undone. Entity links will be removed.",
                "Delete", e -> {
                    try {
                        client.delete(domain.getId());
                        UI.getCurrent().navigate(DomainDefinitionView.class);
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

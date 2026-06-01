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
import it.brunasti.dbdadi.frontend.client.DatabaseModelClient;
import it.brunasti.dbdadi.frontend.client.DomainDefinitionClient;
import it.brunasti.dbdadi.frontend.client.EntityDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.BulkEntityRequest;
import it.brunasti.dbdadi.frontend.dto.BulkEntityResult;
import it.brunasti.dbdadi.frontend.dto.DatabaseModelDto;
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
    private final DatabaseModelClient dbModelClient;
    private DomainDefinitionDto domain;

    private final TextField nameField = new TextField("Name");
    private final TextArea descriptionField = new TextArea("Description");
    private final Grid<EntityDefinitionDto> entitiesGrid = new Grid<>(EntityDefinitionDto.class, false);
    private final Grid<DatabaseModelDto> dbModelsGrid = new Grid<>(DatabaseModelDto.class, false);

    public DomainDefinitionDetailView(DomainDefinitionClient client, EntityDefinitionClient entityClient,
                                      DatabaseModelClient dbModelClient) {
        this.client = client;
        this.entityClient = entityClient;
        this.dbModelClient = dbModelClient;
        setWidthFull();
        setPadding(true);
        configureFields();
        configureEntitiesGrid();
        configureDbModelsGrid();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("domainId").map(Long::valueOf).ifPresent(id -> {
            try {
                domain = client.findById(id);
                populateView();
                entitiesGrid.setItems(client.findEntities(id));
                dbModelsGrid.setItems(client.findDatabaseModels(id));
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

    private void configureDbModelsGrid() {
        dbModelsGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("database-models/" + item.getId()));
            return btn;
        }).setHeader("Model Name").setComparator(Comparator.comparing(DatabaseModelDto::getName));
        dbModelsGrid.addColumn(item -> item.getDbType() != null ? item.getDbType().name() : "")
                .setHeader("Type").setWidth("120px").setFlexGrow(0).setSortable(true);
        dbModelsGrid.addColumn(DatabaseModelDto::getDescription).setHeader("Description").setSortable(true);
        dbModelsGrid.setAllRowsVisible(true);
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
        Button bulkCreateBtn = new Button("Create Entities from Tables", e -> openBulkCreateDialog());
        bulkCreateBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        bulkCreateBtn.setVisible(SecurityUtils.canEdit());

        add(form, new HorizontalLayout(editBtn, deleteBtn, bulkCreateBtn),
            new Hr(), new H3("Linked Database Models"), dbModelsGrid,
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

        MultiSelectComboBox<DatabaseModelDto> dbModelsBox = new MultiSelectComboBox<>("Database Models");
        dbModelsBox.setWidthFull();
        dbModelsBox.setItemLabelGenerator(DatabaseModelDto::getName);
        try {
            dbModelsBox.setItems(dbModelClient.findAll());
            List<DatabaseModelDto> currentModels = client.findDatabaseModels(domain.getId());
            dbModelsBox.setValue(Set.copyOf(currentModels));
        } catch (Exception e) {
            log.warn("Could not load database models for domain editor");
        }

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

        FormLayout form = new FormLayout(name, description, dbModelsBox, entitiesBox);
        form.setColspan(description, 2);
        form.setColspan(dbModelsBox, 2);
        form.setColspan(entitiesBox, 2);

        Button save = new Button("Save", e -> {
            try {
                DomainDefinitionDto dto = DomainDefinitionDto.builder()
                        .name(name.getValue())
                        .description(description.getValue())
                        .build();
                domain = client.update(domain.getId(), dto);
                List<Long> dbModelIds = dbModelsBox.getSelectedItems().stream()
                        .map(DatabaseModelDto::getId)
                        .collect(Collectors.toList());
                client.setDatabaseModels(domain.getId(), dbModelIds);
                List<Long> entityIds = entitiesBox.getSelectedItems().stream()
                        .map(EntityDefinitionDto::getId)
                        .collect(Collectors.toList());
                client.setEntities(domain.getId(), entityIds);
                dialog.close();
                populateView();
                dbModelsGrid.setItems(client.findDatabaseModels(domain.getId()));
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

    private void openBulkCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create Entities from Unmatched Tables");
        dialog.setWidth("580px");

        Span domainInfo = new Span("Domain: " + domain.getName());
        domainInfo.getStyle().set("font-weight", "bold");

        MultiSelectComboBox<DatabaseModelDto> modelsBox = new MultiSelectComboBox<>("Database Models");
        modelsBox.setWidthFull();
        modelsBox.setItemLabelGenerator(DatabaseModelDto::getName);
        try {
            List<DatabaseModelDto> allModels = dbModelClient.findAll();
            modelsBox.setItems(allModels);
            // Pre-select models already linked to this domain
            List<DatabaseModelDto> linked = client.findDatabaseModels(domain.getId());
            if (!linked.isEmpty()) modelsBox.setValue(Set.copyOf(linked));
        } catch (Exception e) {
            log.warn("Could not load database models");
        }

        Span helpText = new Span(
                "One entity will be created (or reused) per unmatched table. "
                + "Table names are converted to PascalCase. "
                + "Entities are added to this domain and tables are linked.");
        helpText.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "0.9em");

        VerticalLayout content = new VerticalLayout(domainInfo, modelsBox, helpText);
        content.setPadding(false);
        content.setSpacing(true);

        Button runBtn = new Button("Create Entities", e -> {
            Set<DatabaseModelDto> selected = modelsBox.getSelectedItems();
            if (selected.isEmpty()) {
                notify("Please select at least one database model", true);
                return;
            }
            List<Long> modelIds = selected.stream()
                    .map(DatabaseModelDto::getId)
                    .collect(Collectors.toList());
            try {
                BulkEntityResult result = entityClient.bulkCreate(
                        BulkEntityRequest.builder()
                                .databaseModelIds(modelIds)
                                .domainId(domain.getId())
                                .build());
                dialog.close();
                showBulkCreateResult(result);
                entitiesGrid.setItems(client.findEntities(domain.getId()));
            } catch (Exception ex) {
                notify("Failed: " + ex.getMessage(), true);
            }
        });
        runBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(content);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), runBtn);
        dialog.open();
    }

    private void showBulkCreateResult(BulkEntityResult result) {
        Dialog resultDialog = new Dialog();
        resultDialog.setHeaderTitle("Bulk Create — Result");
        resultDialog.setWidth("520px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);

        content.add(summary("Entities created:", result.getEntitiesCreated(), "#2e7d32"));
        content.add(summary("Entities reused (name match):", result.getEntitiesReused(), "#1565c0"));
        content.add(summary("Tables linked:", result.getTablesLinked(), "#2e7d32"));
        content.add(summary("Tables already matched (skipped):", result.getTablesSkipped(), "#757575"));

        if (result.getCreatedNames() != null && !result.getCreatedNames().isEmpty()) {
            TextArea names = new TextArea("New entities");
            names.setValue(String.join("\n", result.getCreatedNames()));
            names.setReadOnly(true);
            names.setWidthFull();
            names.setHeight("180px");
            content.add(names);
        }

        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            result.getWarnings().forEach(w -> {
                Span warn = new Span("⚠ " + w);
                warn.getStyle().set("color", "#e65100").set("font-size", "0.9em");
                content.add(warn);
            });
        }

        resultDialog.add(content);
        resultDialog.getFooter().add(new Button("Close", e -> resultDialog.close()));
        resultDialog.open();
    }

    private Span summary(String label, int count, String color) {
        Span s = new Span(label + " " + count);
        s.getStyle().set("color", count > 0 ? color : "var(--lumo-secondary-text-color)")
                .set("font-size", "0.95em").set("padding", "2px 0").set("display", "block");
        return s;
    }

    private void notify(String message, boolean error) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}

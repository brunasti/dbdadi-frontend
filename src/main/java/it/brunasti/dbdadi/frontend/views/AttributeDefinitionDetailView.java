package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import it.brunasti.dbdadi.frontend.client.AttributeDefinitionClient;
import it.brunasti.dbdadi.frontend.client.EntityDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.AttributeDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.AttributeEntitySuggestion;
import it.brunasti.dbdadi.frontend.dto.ColumnDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.EntityDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.MergeAttributeRequest;
import it.brunasti.dbdadi.frontend.dto.MergeAttributeResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;

@Route(value = "attributes/:attributeId", layout = MainLayout.class)
@PageTitle("DBDaDi | Attribute")
@PermitAll
@Slf4j
public class AttributeDefinitionDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final AttributeDefinitionClient client;
    private final EntityDefinitionClient entityClient;
    private AttributeDefinitionDto attribute;

    private final TextField nameField = new TextField("Name");
    private final TextArea descriptionField = new TextArea("Description");
    private final Grid<ColumnDefinitionDto> columnsGrid = new Grid<>(ColumnDefinitionDto.class, false);

    public AttributeDefinitionDetailView(AttributeDefinitionClient client, EntityDefinitionClient entityClient) {
        this.client = client;
        this.entityClient = entityClient;
        setWidthFull();
        setPadding(true);
        configureFields();
        configureGrid();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("attributeId").map(Long::valueOf).ifPresent(id -> {
            try {
                attribute = client.findById(id);
                populateFields();
                columnsGrid.setItems(client.findColumns(id));
            } catch (Exception e) {
                log.error("Could not load attribute {}", id, e);
                notify("Could not load attribute", true);
            }
        });
    }

    private void configureFields() {
        nameField.setReadOnly(true);
        descriptionField.setReadOnly(true);
        descriptionField.setWidthFull();
    }

    private void configureGrid() {
        columnsGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getDatabaseModelName() != null ? item.getDatabaseModelName() : "");
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0");
            btn.addClickListener(e -> UI.getCurrent().navigate("database-models/" + item.getDatabaseModelId()));
            return btn;
        }).setHeader("Database Model").setComparator(Comparator.comparing(item -> item.getDatabaseModelName() != null ? item.getDatabaseModelName() : ""));
        columnsGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getSchemaName() != null ? item.getSchemaName() : "");
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0");
            btn.addClickListener(e -> UI.getCurrent().navigate("schemas/" + item.getSchemaId()));
            return btn;
        }).setHeader("Schema").setComparator(Comparator.comparing(item -> item.getSchemaName() != null ? item.getSchemaName() : ""));
        columnsGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getTableName() != null ? item.getTableName() : "");
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0");
            btn.addClickListener(e -> UI.getCurrent().navigate("tables/" + item.getTableId()));
            return btn;
        }).setHeader("Table").setComparator(Comparator.comparing(item -> item.getTableName() != null ? item.getTableName() : ""));
        columnsGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("columns/" + item.getId()));
            return btn;
        }).setHeader("Column Name").setComparator(Comparator.comparing(ColumnDefinitionDto::getName));
        columnsGrid.addColumn(ColumnDefinitionDto::getDataType).setHeader("Data Type").setSortable(true);
        columnsGrid.addColumn(ColumnDefinitionDto::getDescription).setHeader("Description").setSortable(true);
        columnsGrid.setAllRowsVisible(true);
    }

    private void populateFields() {
        removeAll();

        HorizontalLayout breadcrumb = new HorizontalLayout();
        Button backToList = new Button("← Attributes");
        backToList.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToList.addClickListener(e -> UI.getCurrent().navigate(AttributeDefinitionView.class));
        breadcrumb.add(backToList, new Span(" / "), new Span("Attribute: " + attribute.getName()));
        breadcrumb.setAlignItems(Alignment.CENTER);
        add(breadcrumb);

        nameField.setValue(attribute.getName() != null ? attribute.getName() : "");
        descriptionField.setValue(attribute.getDescription() != null ? attribute.getDescription() : "");

        VerticalLayout entitySection = new VerticalLayout();
        entitySection.setPadding(false);
        entitySection.setSpacing(false);
        if (attribute.getEntityId() != null) {
            Button entityLink = new Button("Entity: " + attribute.getEntityName());
            entityLink.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            entityLink.getStyle().set("font-weight", "bold");
            entityLink.addClickListener(e -> UI.getCurrent().navigate("entities/" + attribute.getEntityId()));
            entitySection.add(entityLink);
        }

        FormLayout form = new FormLayout(nameField, descriptionField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(descriptionField, 2);

        Button editBtn = new Button("Edit", e -> openEditDialog());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editBtn.setVisible(SecurityUtils.canEdit());
        Button deleteBtn = new Button("Delete", e -> confirmDelete());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteBtn.setVisible(SecurityUtils.canEdit());
        Button suggestBtn = new Button("Suggest Entity", e -> openSuggestEntityDialog());
        suggestBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        suggestBtn.setVisible(SecurityUtils.canEdit());
        Button mergeBtn = new Button("Merge into…", e -> openMergeDialog());
        mergeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        mergeBtn.setVisible(SecurityUtils.canEdit());

        add(form, entitySection, new HorizontalLayout(editBtn, deleteBtn, suggestBtn, mergeBtn), new Hr(), new H3("Linked Columns"));
        add(columnsGrid);
    }

    private void openEditDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Attribute");
        dialog.setWidth("500px");

        TextField name = new TextField("Name");
        TextArea description = new TextArea("Description");
        ComboBox<EntityDefinitionDto> entityCombo = new ComboBox<>("Entity");
        entityCombo.setItemLabelGenerator(EntityDefinitionDto::getName);
        entityCombo.setClearButtonVisible(true);
        entityCombo.setPlaceholder("(none)");
        try { entityCombo.setItems(entityClient.findAll()); }
        catch (Exception e) { log.warn("Could not load entities"); }
        if (attribute.getEntityId() != null) {
            entityCombo.setValue(EntityDefinitionDto.builder()
                    .id(attribute.getEntityId()).name(attribute.getEntityName()).build());
        }

        name.setValue(attribute.getName() != null ? attribute.getName() : "");
        description.setValue(attribute.getDescription() != null ? attribute.getDescription() : "");

        FormLayout form = new FormLayout(name, entityCombo, description);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(description, 2);

        Button save = new Button("Save", e -> {
            try {
                EntityDefinitionDto selectedEntity = entityCombo.getValue();
                AttributeDefinitionDto dto = AttributeDefinitionDto.builder()
                        .name(name.getValue())
                        .description(description.getValue())
                        .entityId(selectedEntity != null ? selectedEntity.getId() : null)
                        .build();
                attribute = client.update(attribute.getId(), dto);
                dialog.close();
                populateFields();
                columnsGrid.setItems(client.findColumns(attribute.getId()));
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

    private void openMergeDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Merge attribute into another");
        dialog.setWidth("520px");

        Span sourceLabel = new Span("Source (will be deleted): " + attribute.getName());
        sourceLabel.getStyle()
                .set("color", "var(--lumo-error-color)")
                .set("font-weight", "bold");

        ComboBox<AttributeDefinitionDto> targetCombo = new ComboBox<>("Merge into");
        targetCombo.setItemLabelGenerator(a -> a.getName()
                + (a.getEntityName() != null ? "  [" + a.getEntityName() + "]" : ""));
        targetCombo.setPlaceholder("Select target attribute…");
        targetCombo.setWidthFull();

        // Pre-populate with attributes from the same entity if available, otherwise all
        List<AttributeDefinitionDto> candidates;
        try {
            if (attribute.getEntityId() != null) {
                candidates = client.findByEntity(attribute.getEntityId()).stream()
                        .filter(a -> !a.getId().equals(attribute.getId()))
                        .sorted(Comparator.comparing(AttributeDefinitionDto::getName))
                        .toList();
            } else {
                candidates = client.findAll().stream()
                        .filter(a -> !a.getId().equals(attribute.getId()))
                        .sorted(Comparator.comparing(AttributeDefinitionDto::getName))
                        .toList();
            }
            targetCombo.setItems(candidates);
        } catch (Exception e) {
            log.warn("Could not load candidate attributes", e);
            targetCombo.setItems(List.of());
        }

        Span hint = new Span(attribute.getEntityId() != null
                ? "Showing attributes from the same entity. All column links will move to the target; the source attribute will be deleted."
                : "All column links will move to the target; the source attribute will be deleted.");
        hint.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");

        Button mergeBtn = new Button("Merge");
        mergeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        mergeBtn.setEnabled(false);
        targetCombo.addValueChangeListener(e -> mergeBtn.setEnabled(e.getValue() != null));

        mergeBtn.addClickListener(e -> {
            AttributeDefinitionDto target = targetCombo.getValue();
            if (target == null) return;
            ConfirmDialog confirm = new ConfirmDialog(
                    "Merge \"" + attribute.getName() + "\" into \"" + target.getName() + "\"?",
                    "All column links from \"" + attribute.getName()
                            + "\" will be moved to \"" + target.getName()
                            + "\". The source attribute will be permanently deleted.",
                    "Merge", ce -> {
                        try {
                            MergeAttributeResult result = client.merge(MergeAttributeRequest.builder()
                                    .sourceAttributeId(attribute.getId())
                                    .targetAttributeId(target.getId())
                                    .build());
                            dialog.close();
                            showMergeResult(result);
                        } catch (Exception ex) {
                            notify("Merge failed: " + ex.getMessage(), true);
                        }
                    },
                    "Cancel", ce -> {});
            confirm.setConfirmButtonTheme("error primary");
            confirm.open();
        });

        VerticalLayout content = new VerticalLayout(sourceLabel, hint, targetCombo);
        content.setPadding(false);
        dialog.add(content);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), mergeBtn);
        dialog.open();
    }

    private void showMergeResult(MergeAttributeResult result) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Merge complete");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.add(new Span("Surviving attribute: " + result.getSurvivingAttributeName()));
        content.add(new Span("Columns migrated: " + result.getColumnsMigrated()));
        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            result.getWarnings().forEach(w -> {
                Span warn = new Span("⚠ " + w);
                warn.getStyle().set("color", "var(--lumo-warning-color)");
                content.add(warn);
            });
        }

        Button goBtn = new Button("Go to \"" + result.getSurvivingAttributeName() + "\"", e -> {
            dialog.close();
            UI.getCurrent().navigate("attributes/" + result.getSurvivingAttributeId());
        });
        goBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(content);
        dialog.getFooter().add(new Button("Close", e -> dialog.close()), goBtn);
        dialog.open();
    }

    private void openSuggestEntityDialog() {
        List<AttributeEntitySuggestion> suggestions;
        try {
            suggestions = client.suggestEntities(attribute.getId());
        } catch (Exception e) {
            notify("Could not load suggestions: " + e.getMessage(), true);
            return;
        }

        if (suggestions.isEmpty()) {
            notify("No entity suggestions found (attribute has no linked columns in tables with entities)", false);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Suggested Entities for \"" + attribute.getName() + "\"");
        dialog.setWidth("750px");

        Span hint = new Span("Select an entity to link this attribute to, based on the tables its columns appear in.");
        hint.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");

        Grid<AttributeEntitySuggestion> grid = new Grid<>(AttributeEntitySuggestion.class, false);
        grid.addComponentColumn(s -> {
            Button btn = new Button(s.getEntityName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("entities/" + s.getEntityId()));
            return btn;
        }).setHeader("Entity").setSortable(false);
        grid.addColumn(s -> s.getViaTableNames() != null
                ? String.join(", ", s.getViaTableNames()) : "")
                .setHeader("Via Tables").setSortable(false);
        grid.addColumn(AttributeEntitySuggestion::getLinkedColumnsCount)
                .setHeader("# Columns").setSortable(false).setWidth("100px").setFlexGrow(0);

        grid.setItems(suggestions);
        grid.setAllRowsVisible(true);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);

        Button linkBtn = new Button("Link to Selected Entity");
        linkBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        linkBtn.setEnabled(false);
        grid.addSelectionListener(sel -> linkBtn.setEnabled(sel.getFirstSelectedItem().isPresent()));

        linkBtn.addClickListener(e -> {
            grid.getSelectedItems().stream().findFirst().ifPresent(selected -> {
                ConfirmDialog confirm = new ConfirmDialog(
                        "Link attribute to \"" + selected.getEntityName() + "\"?",
                        "This will set the entity for attribute \"" + attribute.getName()
                                + "\" to \"" + selected.getEntityName() + "\".",
                        "Link", ce -> {
                            try {
                                AttributeDefinitionDto dto = AttributeDefinitionDto.builder()
                                        .name(attribute.getName())
                                        .description(attribute.getDescription())
                                        .entityId(selected.getEntityId())
                                        .build();
                                attribute = client.update(attribute.getId(), dto);
                                dialog.close();
                                populateFields();
                                columnsGrid.setItems(client.findColumns(attribute.getId()));
                                notify("Linked to entity \"" + selected.getEntityName() + "\"", false);
                            } catch (Exception ex) {
                                notify("Link failed: " + ex.getMessage(), true);
                            }
                        },
                        "Cancel", ce -> {});
                confirm.open();
            });
        });

        VerticalLayout content = new VerticalLayout(hint, grid);
        content.setPadding(false);
        dialog.add(content);
        dialog.getFooter().add(new Button("Close", e -> dialog.close()), linkBtn);
        dialog.open();
    }

    private void confirmDelete() {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete attribute \"" + attribute.getName() + "\"?",
                "This action cannot be undone. Columns linked to this attribute will be unlinked.",
                "Delete", e -> {
                    try {
                        client.delete(attribute.getId());
                        UI.getCurrent().navigate(AttributeDefinitionView.class);
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

package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import it.brunasti.dbdadi.frontend.security.SecurityUtils;
import it.brunasti.dbdadi.frontend.client.AttributeDefinitionClient;
import it.brunasti.dbdadi.frontend.client.ColumnDefinitionClient;
import it.brunasti.dbdadi.frontend.client.RelationshipDefinitionClient;
import it.brunasti.dbdadi.frontend.client.TableDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.AttributeDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.ColumnDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.RelationshipDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.RelationshipType;
import it.brunasti.dbdadi.frontend.dto.TableDefinitionDto;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;

@Route(value = "columns/:columnId", layout = MainLayout.class)
@PageTitle("DBDaDi | Column")
@PermitAll
@Slf4j
public class ColumnDefinitionDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final ColumnDefinitionClient client;
    private final RelationshipDefinitionClient relClient;
    private final AttributeDefinitionClient attributeClient;
    private final TableDefinitionClient tableClient;
    private ColumnDefinitionDto column;

    private final TextField nameField = new TextField("Name");
    private final TextField dataTypeField = new TextField("Data Type");
    private final IntegerField lengthField = new IntegerField("Length");
    private final IntegerField precisionField = new IntegerField("Precision");
    private final IntegerField scaleField = new IntegerField("Scale");
    private final IntegerField ordinalField = new IntegerField("Ordinal Position");
    private final TextField defaultValueField = new TextField("Default Value");
    private final Checkbox nullableField = new Checkbox("Nullable");
    private final Checkbox primaryKeyField = new Checkbox("Primary Key");
    private final Checkbox uniqueField = new Checkbox("Unique");
    private final TextArea descriptionField = new TextArea("Description");

    public ColumnDefinitionDetailView(ColumnDefinitionClient client, RelationshipDefinitionClient relClient,
                                       AttributeDefinitionClient attributeClient, TableDefinitionClient tableClient) {
        this.client = client;
        this.relClient = relClient;
        this.attributeClient = attributeClient;
        this.tableClient = tableClient;
        setWidthFull();
        setPadding(true);
        configureFields();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("columnId").map(Long::valueOf).ifPresent(id -> {
            try {
                column = client.findById(id);
                populateFields();
            } catch (Exception e) {
                log.error("Could not load column {}", id, e);
                notify("Could not load column", true);
            }
        });
    }

    private void configureFields() {
        nameField.setReadOnly(true);
        dataTypeField.setReadOnly(true);
        lengthField.setReadOnly(true);
        precisionField.setReadOnly(true);
        scaleField.setReadOnly(true);
        ordinalField.setReadOnly(true);
        defaultValueField.setReadOnly(true);
        nullableField.setReadOnly(true);
        primaryKeyField.setReadOnly(true);
        uniqueField.setReadOnly(true);
        descriptionField.setReadOnly(true);
        descriptionField.setWidthFull();
    }

    private void populateFields() {
        removeAll();

        HorizontalLayout breadcrumb = new HorizontalLayout();
        Button backToModels = new Button("← Database Models");
        backToModels.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToModels.addClickListener(e -> UI.getCurrent().navigate(DatabaseModelView.class));
        Button toModel = new Button(column.getDatabaseModelName());
        toModel.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        toModel.getStyle().set("padding", "0");
        toModel.addClickListener(e -> UI.getCurrent().navigate("database-models/" + column.getDatabaseModelId()));
        Button toSchema = new Button(column.getSchemaName());
        toSchema.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        toSchema.getStyle().set("padding", "0");
        toSchema.addClickListener(e -> UI.getCurrent().navigate("schemas/" + column.getSchemaId()));
        Button toTable = new Button(column.getTableName());
        toTable.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        toTable.getStyle().set("padding", "0");
        toTable.addClickListener(e -> UI.getCurrent().navigate("tables/" + column.getTableId()));
        breadcrumb.add(backToModels, new Span(" / "), toModel,
                new Span(" / "), toSchema,
                new Span(" / "), toTable,
                new Span(" / "), new Span("Column: " + column.getName()));
        breadcrumb.setAlignItems(Alignment.CENTER);
        add(breadcrumb);

        nameField.setValue(column.getName() != null ? column.getName() : "");
        dataTypeField.setValue(column.getDataType() != null ? column.getDataType() : "");
        lengthField.setValue(column.getLength());
        precisionField.setValue(column.getPrecision());
        scaleField.setValue(column.getScale());
        ordinalField.setValue(column.getOrdinalPosition());
        defaultValueField.setValue(column.getDefaultValue() != null ? column.getDefaultValue() : "");
        nullableField.setValue(column.isNullable());
        primaryKeyField.setValue(column.isPrimaryKey());
        uniqueField.setValue(column.isUnique());
        descriptionField.setValue(column.getDescription() != null ? column.getDescription() : "");

        FormLayout form = new FormLayout(nameField, dataTypeField, lengthField, precisionField,
                scaleField, ordinalField, defaultValueField, nullableField, primaryKeyField,
                uniqueField, descriptionField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
        form.setColspan(descriptionField, 3);

        Button editBtn = new Button("Edit", e -> openEditDialog());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editBtn.setVisible(SecurityUtils.canEdit());
        Button deleteBtn = new Button("Delete", e -> confirmDelete());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteBtn.setVisible(SecurityUtils.canEdit());

        VerticalLayout attributeSection = new VerticalLayout();
        attributeSection.setPadding(false);
        attributeSection.setSpacing(false);
        if (column.getAttributeId() != null) {
            Button attributeLink = new Button("Attribute: " + column.getAttributeName());
            attributeLink.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            attributeLink.getStyle().set("font-weight", "bold");
            attributeLink.addClickListener(e -> UI.getCurrent().navigate("attributes/" + column.getAttributeId()));
            attributeSection.add(attributeLink);
        }

        add(form, new HorizontalLayout(editBtn, deleteBtn), attributeSection);

        add(buildRelationshipSection());
    }

    private VerticalLayout buildRelationshipSection() {
        List<RelationshipDefinitionDto> fromRels = List.of();
        List<RelationshipDefinitionDto> toRels = List.of();
        try {
            fromRels = relClient.findByFromTable(column.getTableId()).stream()
                    .filter(r -> column.getName().equals(r.getFromColumnName()))
                    .toList();
            toRels = relClient.findByToTable(column.getTableId()).stream()
                    .filter(r -> column.getName().equals(r.getToColumnName()))
                    .toList();
        } catch (Exception e) {
            log.warn("Could not load relationships for column {}", column.getId());
        }

        Grid<RelationshipDefinitionDto> fromGrid = new Grid<>(RelationshipDefinitionDto.class, false);
        fromGrid.addComponentColumn(r -> {
            Button btn = new Button(r.getName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("relationships/" + r.getId()));
            return btn;
        }).setHeader("Name").setComparator(Comparator.comparing(RelationshipDefinitionDto::getName));
        fromGrid.addColumn(r -> r.getType() != null ? r.getType().name() : "").setHeader("Type").setSortable(true);
        fromGrid.addColumn(r -> r.getToTableName() + "." + r.getToColumnName()).setHeader("To").setSortable(true);
        fromGrid.addColumn(RelationshipDefinitionDto::getDescription).setHeader("Description");
        fromGrid.setItems(fromRels);
        fromGrid.setAllRowsVisible(true);

        Grid<RelationshipDefinitionDto> toGrid = new Grid<>(RelationshipDefinitionDto.class, false);
        toGrid.addComponentColumn(r -> {
            Button btn = new Button(r.getName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("relationships/" + r.getId()));
            return btn;
        }).setHeader("Name").setComparator(Comparator.comparing(RelationshipDefinitionDto::getName));
        toGrid.addColumn(r -> r.getType() != null ? r.getType().name() : "").setHeader("Type").setSortable(true);
        toGrid.addColumn(r -> r.getFromTableName() + "." + r.getFromColumnName()).setHeader("From").setSortable(true);
        toGrid.addColumn(RelationshipDefinitionDto::getDescription).setHeader("Description");
        toGrid.setItems(toRels);
        toGrid.setAllRowsVisible(true);

        Button newAsSource = new Button("New Relationship (as source)", e -> openNewRelationshipDialog(true));
        newAsSource.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        newAsSource.setVisible(SecurityUtils.canEdit());

        Button newAsTarget = new Button("New Relationship (as target)", e -> openNewRelationshipDialog(false));
        newAsTarget.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        newAsTarget.setVisible(SecurityUtils.canEdit());

        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(false);
        section.add(new H4("Relationships (as source)"), newAsSource, fromGrid,
                    new H4("Relationships (as target)"), newAsTarget, toGrid);
        return section;
    }

    private void openNewRelationshipDialog(boolean asSource) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(asSource ? "New Relationship (as source)" : "New Relationship (as target)");
        dialog.setWidth("600px");

        java.util.List<TableDefinitionDto> tables;
        try {
            tables = tableClient.findAll();
        } catch (Exception e) {
            tables = java.util.List.of();
            log.warn("Could not load tables");
        }

        TextField name = new TextField("Relationship Name");
        ComboBox<RelationshipType> type = new ComboBox<>("Type", RelationshipType.values());
        ComboBox<TableDefinitionDto> fromTable = new ComboBox<>("From Table");
        fromTable.setItemLabelGenerator(t -> t.getDatabaseModelName() + " / " + t.getName());
        fromTable.setItems(tables);
        TextField fromColumn = new TextField("From Column");
        ComboBox<TableDefinitionDto> toTable = new ComboBox<>("To Table");
        toTable.setItemLabelGenerator(t -> t.getDatabaseModelName() + " / " + t.getName());
        toTable.setItems(tables);
        TextField toColumn = new TextField("To Column");
        TextArea description = new TextArea("Description");

        if (asSource) {
            tables.stream().filter(t -> t.getId().equals(column.getTableId())).findFirst().ifPresent(fromTable::setValue);
            fromTable.setReadOnly(true);
            fromColumn.setValue(column.getName());
            fromColumn.setReadOnly(true);
        } else {
            tables.stream().filter(t -> t.getId().equals(column.getTableId())).findFirst().ifPresent(toTable::setValue);
            toTable.setReadOnly(true);
            toColumn.setValue(column.getName());
            toColumn.setReadOnly(true);
        }

        FormLayout form = new FormLayout(name, type, fromTable, fromColumn, toTable, toColumn, description);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(name, 2);
        form.setColspan(description, 2);

        Button save = new Button("Save", e -> {
            try {
                RelationshipDefinitionDto dto = RelationshipDefinitionDto.builder()
                        .name(name.getValue())
                        .description(description.getValue())
                        .type(type.getValue())
                        .fromTableId(fromTable.getValue() != null ? fromTable.getValue().getId() : null)
                        .fromColumnName(fromColumn.getValue())
                        .toTableId(toTable.getValue() != null ? toTable.getValue().getId() : null)
                        .toColumnName(toColumn.getValue())
                        .build();
                relClient.create(dto);
                dialog.close();
                populateFields();
                notify("Relationship created", false);
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

    private void openEditDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Column");
        dialog.setWidth("600px");

        TextField name = new TextField("Column Name");
        TextField dataType = new TextField("Data Type");
        IntegerField length = new IntegerField("Length");
        IntegerField precision = new IntegerField("Precision");
        IntegerField scale = new IntegerField("Scale");
        IntegerField ordinal = new IntegerField("Ordinal Position");
        TextField defaultValue = new TextField("Default Value");
        Checkbox nullable = new Checkbox("Nullable");
        Checkbox primaryKey = new Checkbox("Primary Key");
        Checkbox unique = new Checkbox("Unique");
        TextArea description = new TextArea("Description");
        ComboBox<AttributeDefinitionDto> attributeCombo = new ComboBox<>("Attribute");
        attributeCombo.setItemLabelGenerator(AttributeDefinitionDto::getName);
        attributeCombo.setClearButtonVisible(true);
        try { attributeCombo.setItems(attributeClient.findAll()); }
        catch (Exception e) { log.warn("Could not load attributes"); }

        name.setValue(column.getName() != null ? column.getName() : "");
        dataType.setValue(column.getDataType() != null ? column.getDataType() : "");
        length.setValue(column.getLength());
        precision.setValue(column.getPrecision());
        scale.setValue(column.getScale());
        ordinal.setValue(column.getOrdinalPosition());
        defaultValue.setValue(column.getDefaultValue() != null ? column.getDefaultValue() : "");
        nullable.setValue(column.isNullable());
        primaryKey.setValue(column.isPrimaryKey());
        unique.setValue(column.isUnique());
        description.setValue(column.getDescription() != null ? column.getDescription() : "");
        if (column.getAttributeId() != null) {
            try {
                attributeCombo.setItems(attributeClient.findAll());
                attributeClient.findAll().stream()
                        .filter(a -> a.getId().equals(column.getAttributeId()))
                        .findFirst().ifPresent(attributeCombo::setValue);
            } catch (Exception e) { log.warn("Could not pre-select attribute"); }
        }

        FormLayout form = new FormLayout(name, dataType, length, precision, scale, ordinal,
                defaultValue, nullable, primaryKey, unique, description, attributeCombo);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
        form.setColspan(description, 3);
        form.setColspan(attributeCombo, 3);

        Button save = new Button("Save", e -> {
            try {
                ColumnDefinitionDto dto = ColumnDefinitionDto.builder()
                        .name(name.getValue()).description(description.getValue())
                        .dataType(dataType.getValue()).length(length.getValue())
                        .precision(precision.getValue()).scale(scale.getValue())
                        .ordinalPosition(ordinal.getValue()).defaultValue(defaultValue.getValue())
                        .nullable(nullable.getValue()).primaryKey(primaryKey.getValue())
                        .unique(unique.getValue()).tableId(column.getTableId())
                        .attributeId(attributeCombo.getValue() != null ? attributeCombo.getValue().getId() : null)
                        .build();
                column = client.update(column.getId(), dto);
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
                "Delete column \"" + column.getName() + "\"?",
                "This action cannot be undone.",
                "Delete", e -> {
                    try {
                        client.delete(column.getId());
                        UI.getCurrent().navigate("tables/" + column.getTableId());
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

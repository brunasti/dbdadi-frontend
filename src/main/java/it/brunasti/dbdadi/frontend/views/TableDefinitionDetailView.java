package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import it.brunasti.dbdadi.frontend.security.SecurityUtils;
import it.brunasti.dbdadi.frontend.client.ColumnDefinitionClient;
import it.brunasti.dbdadi.frontend.client.EntityDefinitionClient;
import it.brunasti.dbdadi.frontend.client.RelationshipDefinitionClient;
import it.brunasti.dbdadi.frontend.client.TableDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.ColumnDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.EntityDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.RelationshipDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.TableDefinitionDto;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;

@Route(value = "tables/:tableId", layout = MainLayout.class)
@PageTitle("DBDaDi | Table")
@PermitAll
@Slf4j
public class TableDefinitionDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final TableDefinitionClient client;
    private final ColumnDefinitionClient columnClient;
    private final RelationshipDefinitionClient relationshipClient;
    private final EntityDefinitionClient entityClient;
    private TableDefinitionDto table;

    private final TextField nameField = new TextField("Name");
    private final TextField schemaField = new TextField("Schema");
    private final TextField dbModelField = new TextField("Database Model");
    private final TextArea descriptionField = new TextArea("Description");
    private final Grid<ColumnDefinitionDto> columnsGrid = new Grid<>(ColumnDefinitionDto.class, false);
    private final Grid<RelationshipDefinitionDto> outgoingGrid = new Grid<>(RelationshipDefinitionDto.class, false);
    private final Grid<RelationshipDefinitionDto> incomingGrid = new Grid<>(RelationshipDefinitionDto.class, false);

    public TableDefinitionDetailView(TableDefinitionClient client, ColumnDefinitionClient columnClient,
                                     RelationshipDefinitionClient relationshipClient,
                                     EntityDefinitionClient entityClient) {
        this.client = client;
        this.columnClient = columnClient;
        this.relationshipClient = relationshipClient;
        this.entityClient = entityClient;
        setWidthFull();
        setPadding(true);
        configureFields();
        configureGrid();
        configureRelationshipGrids();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("tableId").map(Long::valueOf).ifPresent(id -> {
            try {
                table = client.findById(id);
                populateFields();
                columnsGrid.setItems(columnClient.findByTable(id));
                outgoingGrid.setItems(relationshipClient.findByFromTable(id));
                incomingGrid.setItems(relationshipClient.findByToTable(id));
            } catch (Exception e) {
                log.error("Could not load table {}", id, e);
                notify("Could not load table", true);
            }
        });
    }

    private void configureFields() {
        nameField.setReadOnly(true);
        schemaField.setReadOnly(true);
        dbModelField.setReadOnly(true);
        descriptionField.setReadOnly(true);
        descriptionField.setWidthFull();
    }

    private VerticalLayout entityLinkField() {
        NativeLabel caption = new NativeLabel("Entity");
        caption.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "500");
        Button link;
        if (table.getEntityId() != null) {
            link = new Button(table.getEntityName());
            link.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            link.getStyle().set("padding", "0").set("font-size", "var(--lumo-font-size-m)");
            link.addClickListener(e -> UI.getCurrent().navigate("entities/" + table.getEntityId()));
        } else {
            link = new Button("(none)");
            link.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            link.getStyle().set("padding", "0").set("font-size", "var(--lumo-font-size-m)")
                    .set("color", "var(--lumo-secondary-text-color)");
            link.setEnabled(false);
        }
        VerticalLayout wrapper = new VerticalLayout(caption, link);
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-20pct)")
                          .set("padding-bottom", "var(--lumo-space-xs)");
        return wrapper;
    }

    private void populateFields() {
        removeAll();

        HorizontalLayout breadcrumb = new HorizontalLayout();
        Button backToModels = new Button("← Database Models");
        backToModels.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToModels.addClickListener(e -> UI.getCurrent().navigate(DatabaseModelView.class));
        Button toModel = new Button(table.getDatabaseModelName());
        toModel.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        toModel.getStyle().set("padding", "0");
        toModel.addClickListener(e -> UI.getCurrent().navigate("database-models/" + table.getDatabaseModelId()));
        Button toSchema = new Button(table.getSchemaName());
        toSchema.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        toSchema.getStyle().set("padding", "0");
        toSchema.addClickListener(e -> UI.getCurrent().navigate("schemas/" + table.getSchemaId()));
        breadcrumb.add(backToModels, new Span(" / "), toModel,
                new Span(" / "), toSchema,
                new Span(" / "), new Span("Table: " + table.getName()));
        breadcrumb.setAlignItems(Alignment.CENTER);
        add(breadcrumb);

        nameField.setValue(table.getName() != null ? table.getName() : "");
        schemaField.setValue(table.getSchemaName() != null ? table.getSchemaName() : "");
        dbModelField.setValue(table.getDatabaseModelName() != null ? table.getDatabaseModelName() : "");
        descriptionField.setValue(table.getDescription() != null ? table.getDescription() : "");

        FormLayout form = new FormLayout(nameField, schemaField, dbModelField, entityLinkField(), descriptionField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
        form.setColspan(descriptionField, 3);

        Button editBtn = new Button("Edit", e -> openEditDialog());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editBtn.setVisible(SecurityUtils.canEdit());
        Button deleteBtn = new Button("Delete", e -> confirmDelete());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteBtn.setVisible(SecurityUtils.canEdit());

        add(form, new HorizontalLayout(editBtn, deleteBtn), new Hr(), new H3("Columns"));
        columnsGrid.setAllRowsVisible(true);
        add(createAddColumnButton(), columnsGrid);

        add(new Hr(), new H3("Outgoing Relationships (this table is the origin)"));
        outgoingGrid.setAllRowsVisible(true);
        add(outgoingGrid);

        add(new Hr(), new H3("Incoming Relationships (this table is the target)"));
        incomingGrid.setAllRowsVisible(true);
        add(incomingGrid);
    }

    private void configureGrid() {
        columnsGrid.addColumn(ColumnDefinitionDto::getOrdinalPosition).setHeader("#").setWidth("60px").setFlexGrow(0).setSortable(true);
        columnsGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("columns/" + item.getId()));
            return btn;
        }).setHeader("Name").setComparator(Comparator.comparing(ColumnDefinitionDto::getName));
        columnsGrid.addColumn(ColumnDefinitionDto::getDataType).setHeader("Data Type").setSortable(true);
        columnsGrid.addColumn(ColumnDefinitionDto::getLength).setHeader("Length").setWidth("80px").setFlexGrow(0).setSortable(true);
        columnsGrid.addColumn(ColumnDefinitionDto::getPrecision).setHeader("Precision").setWidth("90px").setFlexGrow(0).setSortable(true);
        columnsGrid.addColumn(ColumnDefinitionDto::getScale).setHeader("Scale").setWidth("80px").setFlexGrow(0).setSortable(true);
        columnsGrid.addColumn(ColumnDefinitionDto::getDefaultValue).setHeader("Default").setSortable(true);
        columnsGrid.addColumn(ColumnDefinitionDto::getDescription).setHeader("Description").setSortable(true);
        columnsGrid.addComponentColumn(col -> {
            HorizontalLayout flags = new HorizontalLayout();
            if (col.isPrimaryKey()) flags.add(VaadinIcon.KEY.create());
            if (!col.isNullable()) flags.add(VaadinIcon.EXCLAMATION.create());
            if (col.isUnique()) flags.add(VaadinIcon.STAR.create());
            return flags;
        }).setHeader("Flags").setWidth("100px").setFlexGrow(0);
        if (SecurityUtils.canEdit()) {
            columnsGrid.addComponentColumn(item -> {
                Button edit = new Button("Edit", e -> openEditColumnDialog(item));
                edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
                Button delete = new Button("Delete", e -> confirmDeleteColumn(item));
                delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                return new HorizontalLayout(edit, delete);
            }).setHeader("Actions").setWidth("160px").setFlexGrow(0);
        }
    }

    private void configureRelationshipGrids() {
        for (Grid<RelationshipDefinitionDto> grid : new Grid[]{outgoingGrid, incomingGrid}) {
            grid.addComponentColumn(r -> {
                Button btn = new Button(r.getName());
                btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                btn.getStyle().set("padding", "0").set("font-weight", "bold");
                btn.addClickListener(e -> UI.getCurrent().navigate("relationships/" + r.getId()));
                return btn;
            }).setHeader("Name").setComparator(Comparator.comparing(RelationshipDefinitionDto::getName));
            grid.addColumn(r -> r.getType() != null ? r.getType().name() : "").setHeader("Type").setWidth("100px").setFlexGrow(0)
                    .setComparator(Comparator.comparing(r -> r.getType() != null ? r.getType().name() : ""));
        }
        outgoingGrid.addColumn(RelationshipDefinitionDto::getFromColumnName).setHeader("From Column").setSortable(true);
        outgoingGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getToTableName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0");
            btn.addClickListener(e -> UI.getCurrent().navigate("tables/" + item.getToTableId()));
            return btn;
        }).setHeader("To Table").setComparator(Comparator.comparing(RelationshipDefinitionDto::getToTableName));
        outgoingGrid.addColumn(RelationshipDefinitionDto::getToColumnName).setHeader("To Column").setSortable(true);
        outgoingGrid.addColumn(RelationshipDefinitionDto::getDescription).setHeader("Description").setSortable(true);

        incomingGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getFromTableName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0");
            btn.addClickListener(e -> UI.getCurrent().navigate("tables/" + item.getFromTableId()));
            return btn;
        }).setHeader("From Table").setComparator(Comparator.comparing(RelationshipDefinitionDto::getFromTableName));
        incomingGrid.addColumn(RelationshipDefinitionDto::getFromColumnName).setHeader("From Column").setSortable(true);
        incomingGrid.addColumn(RelationshipDefinitionDto::getToColumnName).setHeader("To Column").setSortable(true);
        incomingGrid.addColumn(RelationshipDefinitionDto::getDescription).setHeader("Description").setSortable(true);
    }

    private Button createAddColumnButton() {
        Button btn = new Button("New Column", e -> openEditColumnDialog(null));
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        btn.setVisible(SecurityUtils.canEdit());
        return btn;
    }

    private void openEditDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Table");
        dialog.setWidth("500px");

        TextField name = new TextField("Table Name");
        TextArea description = new TextArea("Description");
        name.setValue(table.getName() != null ? table.getName() : "");
        description.setValue(table.getDescription() != null ? table.getDescription() : "");

        ComboBox<EntityDefinitionDto> entityCombo = new ComboBox<>("Entity");
        entityCombo.setItemLabelGenerator(EntityDefinitionDto::getName);
        entityCombo.setClearButtonVisible(true);
        entityCombo.setPlaceholder("(none)");
        try {
            entityCombo.setItems(entityClient.findAll());
        } catch (Exception ex) {
            log.warn("Could not load entities");
        }
        if (table.getEntityId() != null) {
            entityCombo.getDataProvider().refreshAll();
            entityCombo.setValue(EntityDefinitionDto.builder()
                    .id(table.getEntityId()).name(table.getEntityName()).build());
        }

        FormLayout form = new FormLayout(name, entityCombo, description);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(description, 2);

        Button save = new Button("Save", e -> {
            try {
                EntityDefinitionDto selected = entityCombo.getValue();
                TableDefinitionDto dto = TableDefinitionDto.builder()
                        .name(name.getValue()).description(description.getValue())
                        .schemaId(table.getSchemaId())
                        .entityId(selected != null ? selected.getId() : null)
                        .build();
                table = client.update(table.getId(), dto);
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

    private void openEditColumnDialog(ColumnDefinitionDto item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item == null ? "New Column" : "Edit Column");
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

        if (item != null) {
            name.setValue(item.getName() != null ? item.getName() : "");
            dataType.setValue(item.getDataType() != null ? item.getDataType() : "");
            length.setValue(item.getLength());
            precision.setValue(item.getPrecision());
            scale.setValue(item.getScale());
            ordinal.setValue(item.getOrdinalPosition());
            defaultValue.setValue(item.getDefaultValue() != null ? item.getDefaultValue() : "");
            nullable.setValue(item.isNullable());
            primaryKey.setValue(item.isPrimaryKey());
            unique.setValue(item.isUnique());
            description.setValue(item.getDescription() != null ? item.getDescription() : "");
        } else {
            nullable.setValue(true);
        }

        FormLayout form = new FormLayout(name, dataType, length, precision, scale, ordinal,
                defaultValue, nullable, primaryKey, unique, description);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
        form.setColspan(description, 3);

        Button save = new Button("Save", e -> {
            try {
                ColumnDefinitionDto dto = ColumnDefinitionDto.builder()
                        .name(name.getValue()).description(description.getValue())
                        .dataType(dataType.getValue()).length(length.getValue())
                        .precision(precision.getValue()).scale(scale.getValue())
                        .ordinalPosition(ordinal.getValue()).defaultValue(defaultValue.getValue())
                        .nullable(nullable.getValue()).primaryKey(primaryKey.getValue())
                        .unique(unique.getValue()).tableId(table.getId()).build();
                if (item == null) columnClient.create(dto);
                else columnClient.update(item.getId(), dto);
                dialog.close();
                columnsGrid.setItems(columnClient.findByTable(table.getId()));
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
                "Delete table \"" + table.getName() + "\"?",
                "This will also delete all columns.",
                "Delete", e -> {
                    try {
                        client.delete(table.getId());
                        UI.getCurrent().navigate("schemas/" + table.getSchemaId());
                    } catch (Exception ex) {
                        notify("Delete failed: " + ex.getMessage(), true);
                    }
                },
                "Cancel", e -> {});
        confirm.setConfirmButtonTheme("error primary");
        confirm.open();
    }

    private void confirmDeleteColumn(ColumnDefinitionDto item) {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete column \"" + item.getName() + "\"?",
                "This action cannot be undone.",
                "Delete", e -> {
                    try {
                        columnClient.delete(item.getId());
                        columnsGrid.setItems(columnClient.findByTable(table.getId()));
                        notify("Deleted", false);
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

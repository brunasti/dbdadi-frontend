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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import it.brunasti.dbdadi.frontend.client.ColumnDefinitionClient;
import it.brunasti.dbdadi.frontend.client.DatabaseModelClient;
import it.brunasti.dbdadi.frontend.client.SchemaDefinitionClient;
import it.brunasti.dbdadi.frontend.client.TableDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.ColumnDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.DatabaseModelDto;
import it.brunasti.dbdadi.frontend.dto.SchemaDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.TableDefinitionDto;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Route(value = "columns", layout = MainLayout.class)
@PageTitle("DBDaDi | Columns")
@AnonymousAllowed
@Slf4j
public class ColumnDefinitionView extends VerticalLayout implements BeforeEnterObserver {

    private final ColumnDefinitionClient client;
    private final TableDefinitionClient tableClient;
    private final SchemaDefinitionClient schemaClient;
    private final DatabaseModelClient dbModelClient;

    private final Grid<ColumnDefinitionDto> grid = new Grid<>(ColumnDefinitionDto.class, false);
    private final ComboBox<DatabaseModelDto> dbModelFilter = new ComboBox<>("Filter by Database Model");
    private final ComboBox<SchemaDefinitionDto> schemaFilter = new ComboBox<>("Filter by Schema");
    private final ComboBox<TableDefinitionDto> tableFilter = new ComboBox<>("Filter by Table");
    private final HorizontalLayout breadcrumb = new HorizontalLayout();

    public ColumnDefinitionView(ColumnDefinitionClient client, TableDefinitionClient tableClient,
                                 SchemaDefinitionClient schemaClient, DatabaseModelClient dbModelClient) {
        this.client = client;
        this.tableClient = tableClient;
        this.schemaClient = schemaClient;
        this.dbModelClient = dbModelClient;
        setSizeFull();
        configureGrid();
        configureFilters();
        breadcrumb.setVisible(false);
        add(breadcrumb, createToolbar(), grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getLocation().getQueryParameters().getSingleParameter("tableId")
                .map(Long::valueOf)
                .ifPresent(id -> {
                    try {
                        TableDefinitionDto table = tableClient.findById(id);
                        // pre-select cascading filters without triggering intermediate refreshes
                        schemaFilter.setItems(schemaClient.findByDatabaseModel(table.getDatabaseModelId()));
                        tableFilter.setItems(tableClient.findBySchema(table.getSchemaId()));
                        dbModelFilter.setValue(dbModelClient.findById(table.getDatabaseModelId()));
                        schemaFilter.setValue(schemaClient.findById(table.getSchemaId()));
                        tableFilter.setValue(table);
                        showBreadcrumb(table);
                    } catch (Exception e) {
                        log.warn("Could not load table {}", id);
                    }
                });
        refresh();
    }

    private void configureFilters() {
        dbModelFilter.setItemLabelGenerator(DatabaseModelDto::getName);
        dbModelFilter.setClearButtonVisible(true);
        try { dbModelFilter.setItems(dbModelClient.findAll()); }
        catch (Exception e) { log.warn("Could not load database models for filter"); }

        schemaFilter.setItemLabelGenerator(SchemaDefinitionDto::getName);
        schemaFilter.setClearButtonVisible(true);
        schemaFilter.setEnabled(false);

        tableFilter.setItemLabelGenerator(TableDefinitionDto::getName);
        tableFilter.setClearButtonVisible(true);
        tableFilter.setEnabled(false);

        dbModelFilter.addValueChangeListener(e -> {
            schemaFilter.clear();
            tableFilter.clear();
            DatabaseModelDto model = e.getValue();
            if (model != null) {
                try { schemaFilter.setItems(schemaClient.findByDatabaseModel(model.getId())); }
                catch (Exception ex) { log.warn("Could not load schemas for model {}", model.getId()); }
                schemaFilter.setEnabled(true);
            } else {
                schemaFilter.setItems();
                schemaFilter.setEnabled(false);
                tableFilter.setItems();
                tableFilter.setEnabled(false);
            }
            updateBreadcrumb();
            refresh();
        });

        schemaFilter.addValueChangeListener(e -> {
            tableFilter.clear();
            SchemaDefinitionDto schema = e.getValue();
            if (schema != null) {
                try { tableFilter.setItems(tableClient.findBySchema(schema.getId())); }
                catch (Exception ex) { log.warn("Could not load tables for schema {}", schema.getId()); }
                tableFilter.setEnabled(true);
            } else {
                tableFilter.setItems();
                tableFilter.setEnabled(false);
            }
            updateBreadcrumb();
            refresh();
        });

        tableFilter.addValueChangeListener(e -> {
            updateBreadcrumb();
            refresh();
        });
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(ColumnDefinitionDto::getId).setHeader("ID").setWidth("70px").setFlexGrow(0).setSortable(true);
        grid.addComponentColumn(item -> {
            Button btn = new Button(item.getDatabaseModelName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0");
            btn.addClickListener(e -> UI.getCurrent().navigate("database-models/" + item.getDatabaseModelId()));
            return btn;
        }).setHeader("Database Model").setComparator(Comparator.comparing(ColumnDefinitionDto::getDatabaseModelName));
        grid.addComponentColumn(item -> {
            Button btn = new Button(item.getSchemaName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0");
            btn.addClickListener(e -> UI.getCurrent().navigate("schemas/" + item.getSchemaId()));
            return btn;
        }).setHeader("Schema").setComparator(Comparator.comparing(ColumnDefinitionDto::getSchemaName));
        grid.addComponentColumn(item -> {
            Button btn = new Button(item.getTableName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0");
            btn.addClickListener(e -> UI.getCurrent().navigate(
                    TableDefinitionView.class,
                    new QueryParameters(Map.of("schemaId",
                            List.of(String.valueOf(item.getSchemaId()))))));
            return btn;
        }).setHeader("Table").setComparator(Comparator.comparing(ColumnDefinitionDto::getTableName));
        grid.addColumn(ColumnDefinitionDto::getOrdinalPosition).setHeader("Position").setWidth("60px").setFlexGrow(0).setSortable(true);
        grid.addComponentColumn(item -> {
            Button nameBtn = new Button(item.getName());
            nameBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            nameBtn.getStyle().set("padding", "0").set("font-weight", "bold");
            nameBtn.addClickListener(e -> UI.getCurrent().navigate("columns/" + item.getId()));
            return nameBtn;
        }).setHeader("Column Name").setComparator(Comparator.comparing(ColumnDefinitionDto::getName));
        grid.addColumn(ColumnDefinitionDto::getDataType).setHeader("Data Type").setSortable(true);
        grid.addColumn(ColumnDefinitionDto::getLength).setHeader("Length").setWidth("80px").setFlexGrow(0).setSortable(true);
        grid.addColumn(ColumnDefinitionDto::getPrecision).setHeader("Precision").setWidth("90px").setFlexGrow(0).setSortable(true);
        grid.addColumn(ColumnDefinitionDto::getScale).setHeader("Scale").setWidth("80px").setFlexGrow(0).setSortable(true);
        grid.addColumn(ColumnDefinitionDto::getDefaultValue).setHeader("Default").setSortable(true);
        grid.addColumn(ColumnDefinitionDto::getDescription).setHeader("Description").setSortable(true);
        grid.addComponentColumn(col -> {
            HorizontalLayout flags = new HorizontalLayout();
            if (col.isPrimaryKey()) flags.add(VaadinIcon.KEY.create());
            if (!col.isNullable()) flags.add(VaadinIcon.EXCLAMATION.create());
            if (col.isUnique()) flags.add(VaadinIcon.STAR.create());
            return flags;
        }).setHeader("Flags").setWidth("100px").setFlexGrow(0);
        grid.addComponentColumn(item -> {
            Button edit = new Button("Edit", e -> openDialog(item));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
            Button delete = new Button("Delete", e -> confirmDelete(item));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, delete);
        }).setHeader("Actions").setWidth("160px").setFlexGrow(0);
    }

    private HorizontalLayout createToolbar() {
        Button addBtn = new Button("New Column", e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button refreshBtn = new Button("Refresh", e -> refresh());
        return new HorizontalLayout(addBtn, refreshBtn, dbModelFilter, schemaFilter, tableFilter);
    }

    private void showBreadcrumb(TableDefinitionDto table) {
        breadcrumb.removeAll();
        Button backToModels = new Button("← Database Models");
        backToModels.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToModels.addClickListener(e -> UI.getCurrent().navigate(DatabaseModelView.class));
        Button backToSchemas = new Button("Schemas of: " + table.getDatabaseModelName());
        backToSchemas.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToSchemas.addClickListener(e -> UI.getCurrent().navigate(
                SchemaDefinitionView.class,
                new QueryParameters(Map.of("databaseModelId",
                        List.of(String.valueOf(table.getDatabaseModelId()))))));
        Button backToTables = new Button("Tables of: " + table.getSchemaName());
        backToTables.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToTables.addClickListener(e -> UI.getCurrent().navigate(
                TableDefinitionView.class,
                new QueryParameters(Map.of("schemaId",
                        List.of(String.valueOf(table.getSchemaId()))))));
        breadcrumb.add(backToModels, new Span(" / "), backToSchemas,
                new Span(" / "), backToTables,
                new Span(" / "), new Span("Columns of: " + table.getName()));
        breadcrumb.setAlignItems(Alignment.CENTER);
        breadcrumb.setVisible(true);
    }

    private void updateBreadcrumb() {
        TableDefinitionDto table = tableFilter.getValue();
        if (table != null) showBreadcrumb(table);
        else breadcrumb.setVisible(false);
    }

    private void refresh() {
        try {
            List<ColumnDefinitionDto> items;
            if (tableFilter.getValue() != null) {
                items = client.findByTable(tableFilter.getValue().getId());
            } else if (schemaFilter.getValue() != null) {
                items = client.findBySchema(schemaFilter.getValue().getId());
            } else if (dbModelFilter.getValue() != null) {
                items = client.findByDatabaseModel(dbModelFilter.getValue().getId());
            } else {
                items = client.findAll();
            }
            grid.setItems(items);
        } catch (Exception e) {
            log.error("Failed to load columns", e);
            notify("Could not load data: " + e.getMessage(), true);
        }
    }

    private void openDialog(ColumnDefinitionDto item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item == null ? "New Column" : "Edit Column");
        dialog.setWidth("600px");

        ComboBox<TableDefinitionDto> table = new ComboBox<>("Table");
        table.setItemLabelGenerator(t -> t.getDatabaseModelName() + " / " + t.getSchemaName() + " / " + t.getName());
        try { table.setItems(tableClient.findAll()); }
        catch (Exception e) { log.warn("Could not load tables"); }

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
            if (item.getTableId() != null) {
                tableClient.findAll().stream()
                        .filter(t -> t.getId().equals(item.getTableId()))
                        .findFirst().ifPresent(table::setValue);
            }
        } else {
            nullable.setValue(true);
            table.setValue(tableFilter.getValue());
        }

        FormLayout form = new FormLayout(table, name, dataType, length, precision, scale, ordinal,
                defaultValue, nullable, primaryKey, unique, description);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
        form.setColspan(table, 3);
        form.setColspan(description, 3);

        Button save = new Button("Save", e -> {
            try {
                ColumnDefinitionDto dto = ColumnDefinitionDto.builder()
                        .name(name.getValue()).description(description.getValue())
                        .dataType(dataType.getValue()).length(length.getValue())
                        .precision(precision.getValue()).scale(scale.getValue())
                        .ordinalPosition(ordinal.getValue()).defaultValue(defaultValue.getValue())
                        .nullable(nullable.getValue()).primaryKey(primaryKey.getValue())
                        .unique(unique.getValue())
                        .tableId(table.getValue() != null ? table.getValue().getId() : null)
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
        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.add(form);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(ColumnDefinitionDto item) {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete column \"" + item.getName() + "\"?",
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

    private void notify(String message, boolean error) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}

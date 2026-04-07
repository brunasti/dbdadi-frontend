package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import it.brunasti.dbdadi.frontend.client.ColumnDefinitionClient;
import it.brunasti.dbdadi.frontend.client.TableDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.ColumnDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.TableDefinitionDto;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Route(value = "columns", layout = MainLayout.class)
@PageTitle("Columns | dbdadi")
@AnonymousAllowed
@Slf4j
public class ColumnDefinitionView extends VerticalLayout {

    private final ColumnDefinitionClient client;
    private final TableDefinitionClient tableClient;
    private final Grid<ColumnDefinitionDto> grid = new Grid<>(ColumnDefinitionDto.class, false);
    private final ComboBox<TableDefinitionDto> tableFilter = new ComboBox<>("Filter by Table");

    public ColumnDefinitionView(ColumnDefinitionClient client, TableDefinitionClient tableClient) {
        this.client = client;
        this.tableClient = tableClient;
        setSizeFull();
        configureGrid();
        configureFilter();
        add(createToolbar(), grid);
        refresh();
    }

    private void configureFilter() {
        tableFilter.setItemLabelGenerator(t -> t.getDatabaseModelName() + " / " + t.getName());
        tableFilter.setClearButtonVisible(true);
        try {
            tableFilter.setItems(tableClient.findAll());
        } catch (Exception e) {
            log.warn("Could not load tables for filter");
        }
        tableFilter.addValueChangeListener(e -> refresh());
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(ColumnDefinitionDto::getId).setHeader("ID").setWidth("70px").setFlexGrow(0);
        grid.addColumn(ColumnDefinitionDto::getOrdinalPosition).setHeader("#").setWidth("60px").setFlexGrow(0);
        grid.addColumn(ColumnDefinitionDto::getName).setHeader("Column Name").setSortable(true);
        grid.addColumn(ColumnDefinitionDto::getDataType).setHeader("Data Type");
        grid.addColumn(ColumnDefinitionDto::getLength).setHeader("Length").setWidth("80px").setFlexGrow(0);
        grid.addColumn(ColumnDefinitionDto::getTableName).setHeader("Table").setSortable(true);
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
        return new HorizontalLayout(addBtn, refreshBtn, tableFilter);
    }

    private void openDialog(ColumnDefinitionDto item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item == null ? "New Column" : "Edit Column");
        dialog.setWidth("600px");

        ComboBox<TableDefinitionDto> table = new ComboBox<>("Table");
        table.setItemLabelGenerator(t -> t.getDatabaseModelName() + " / " + t.getName());
        try {
            table.setItems(tableClient.findAll());
        } catch (Exception e) {
            log.warn("Could not load tables");
        }
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
                        .findFirst()
                        .ifPresent(table::setValue);
            }
        } else {
            nullable.setValue(true);
        }

        FormLayout form = new FormLayout(table, name, dataType, length, precision, scale, ordinal,
                defaultValue, nullable, primaryKey, unique, description);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
        form.setColspan(table, 3);
        form.setColspan(description, 3);

        Button save = new Button("Save", e -> {
            try {
                ColumnDefinitionDto dto = ColumnDefinitionDto.builder()
                        .name(name.getValue())
                        .description(description.getValue())
                        .dataType(dataType.getValue())
                        .length(length.getValue())
                        .precision(precision.getValue())
                        .scale(scale.getValue())
                        .ordinalPosition(ordinal.getValue())
                        .defaultValue(defaultValue.getValue())
                        .nullable(nullable.getValue())
                        .primaryKey(primaryKey.getValue())
                        .unique(unique.getValue())
                        .tableId(table.getValue() != null ? table.getValue().getId() : null)
                        .build();
                if (item == null) {
                    client.create(dto);
                } else {
                    client.update(item.getId(), dto);
                }
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
                "Cancel", e -> {}
        );
        confirm.setConfirmButtonTheme("error primary");
        confirm.open();
    }

    private void refresh() {
        try {
            List<ColumnDefinitionDto> items;
            TableDefinitionDto selected = tableFilter.getValue();
            if (selected != null) {
                items = client.findByTable(selected.getId());
            } else {
                items = client.findAll();
            }
            grid.setItems(items);
        } catch (Exception e) {
            log.error("Failed to load columns", e);
            notify("Could not load data: " + e.getMessage(), true);
        }
    }

    private void notify(String message, boolean error) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}

package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.server.auth.AnonymousAllowed;
import it.brunasti.dbdadi.frontend.client.DatabaseModelClient;
import it.brunasti.dbdadi.frontend.client.SchemaDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.DatabaseModelDto;
import it.brunasti.dbdadi.frontend.dto.SchemaDefinitionDto;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Route(value = "schemas", layout = MainLayout.class)
@PageTitle("Schemas | dbdadi")
@AnonymousAllowed
@Slf4j
public class SchemaDefinitionView extends VerticalLayout {

    private final SchemaDefinitionClient client;
    private final DatabaseModelClient dbModelClient;
    private final Grid<SchemaDefinitionDto> grid = new Grid<>(SchemaDefinitionDto.class, false);
    private final ComboBox<DatabaseModelDto> modelFilter = new ComboBox<>("Filter by Database Model");

    public SchemaDefinitionView(SchemaDefinitionClient client, DatabaseModelClient dbModelClient) {
        this.client = client;
        this.dbModelClient = dbModelClient;
        setSizeFull();
        configureGrid();
        configureFilter();
        add(createToolbar(), grid);
        refresh();
    }

    private void configureFilter() {
        modelFilter.setItemLabelGenerator(DatabaseModelDto::getName);
        modelFilter.setClearButtonVisible(true);
        try {
            modelFilter.setItems(dbModelClient.findAll());
        } catch (Exception e) {
            log.warn("Could not load database models for filter");
        }
        modelFilter.addValueChangeListener(e -> refresh());
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(SchemaDefinitionDto::getId).setHeader("ID").setWidth("80px").setFlexGrow(0);
        grid.addColumn(SchemaDefinitionDto::getName).setHeader("Schema Name").setSortable(true);
        grid.addColumn(SchemaDefinitionDto::getDatabaseModelName).setHeader("Database Model").setSortable(true);
        grid.addColumn(SchemaDefinitionDto::getDescription).setHeader("Description");
        grid.addComponentColumn(item -> {
            Button edit = new Button("Edit", e -> openDialog(item));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
            Button delete = new Button("Delete", e -> confirmDelete(item));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, delete);
        }).setHeader("Actions").setWidth("160px").setFlexGrow(0);
    }

    private HorizontalLayout createToolbar() {
        Button addBtn = new Button("New Schema", e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button refreshBtn = new Button("Refresh", e -> refresh());
        return new HorizontalLayout(addBtn, refreshBtn, modelFilter);
    }

    private void openDialog(SchemaDefinitionDto item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item == null ? "New Schema" : "Edit Schema");
        dialog.setWidth("500px");

        ComboBox<DatabaseModelDto> dbModel = new ComboBox<>("Database Model");
        dbModel.setItemLabelGenerator(DatabaseModelDto::getName);
        try {
            dbModel.setItems(dbModelClient.findAll());
        } catch (Exception e) {
            log.warn("Could not load database models");
        }
        TextField name = new TextField("Schema Name");
        TextArea description = new TextArea("Description");

        if (item != null) {
            name.setValue(item.getName() != null ? item.getName() : "");
            description.setValue(item.getDescription() != null ? item.getDescription() : "");
            if (item.getDatabaseModelId() != null) {
                dbModelClient.findAll().stream()
                        .filter(m -> m.getId().equals(item.getDatabaseModelId()))
                        .findFirst()
                        .ifPresent(dbModel::setValue);
            }
        }

        FormLayout form = new FormLayout(dbModel, name, description);
        form.setColspan(dbModel, 2);
        form.setColspan(description, 2);

        Button save = new Button("Save", e -> {
            try {
                SchemaDefinitionDto dto = SchemaDefinitionDto.builder()
                        .name(name.getValue())
                        .description(description.getValue())
                        .databaseModelId(dbModel.getValue() != null ? dbModel.getValue().getId() : null)
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

    private void confirmDelete(SchemaDefinitionDto item) {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete schema \"" + item.getName() + "\"?",
                "This will also delete all tables and columns in this schema.",
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
            List<SchemaDefinitionDto> items;
            DatabaseModelDto selected = modelFilter.getValue();
            if (selected != null) {
                items = client.findByDatabaseModel(selected.getId());
            } else {
                items = client.findAll();
            }
            grid.setItems(items);
        } catch (Exception e) {
            log.error("Failed to load schemas", e);
            notify("Could not load data: " + e.getMessage(), true);
        }
    }

    private void notify(String message, boolean error) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}

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
import it.brunasti.dbdadi.frontend.client.RelationshipDefinitionClient;
import it.brunasti.dbdadi.frontend.client.TableDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.RelationshipDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.RelationshipType;
import it.brunasti.dbdadi.frontend.dto.TableDefinitionDto;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Route(value = "relationships", layout = MainLayout.class)
@PageTitle("Relationships | dbdadi")
@AnonymousAllowed
@Slf4j
public class RelationshipDefinitionView extends VerticalLayout {

    private final RelationshipDefinitionClient client;
    private final TableDefinitionClient tableClient;
    private final Grid<RelationshipDefinitionDto> grid = new Grid<>(RelationshipDefinitionDto.class, false);

    public RelationshipDefinitionView(RelationshipDefinitionClient client, TableDefinitionClient tableClient) {
        this.client = client;
        this.tableClient = tableClient;
        setSizeFull();
        configureGrid();
        add(createToolbar(), grid);
        refresh();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(RelationshipDefinitionDto::getId).setHeader("ID").setWidth("70px").setFlexGrow(0);
        grid.addColumn(RelationshipDefinitionDto::getName).setHeader("Name").setSortable(true);
        grid.addColumn(RelationshipDefinitionDto::getType).setHeader("Type");
        grid.addColumn(r -> r.getFromTableName() + "." + r.getFromColumnName()).setHeader("From");
        grid.addColumn(r -> r.getToTableName() + "." + r.getToColumnName()).setHeader("To");
        grid.addColumn(RelationshipDefinitionDto::getDescription).setHeader("Description");
        grid.addComponentColumn(item -> {
            Button edit = new Button("Edit", e -> openDialog(item));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
            Button delete = new Button("Delete", e -> confirmDelete(item));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, delete);
        }).setHeader("Actions").setWidth("160px").setFlexGrow(0);
    }

    private HorizontalLayout createToolbar() {
        Button addBtn = new Button("New Relationship", e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button refreshBtn = new Button("Refresh", e -> refresh());
        return new HorizontalLayout(addBtn, refreshBtn);
    }

    private void openDialog(RelationshipDefinitionDto item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item == null ? "New Relationship" : "Edit Relationship");
        dialog.setWidth("600px");

        List<TableDefinitionDto> tables;
        try {
            tables = tableClient.findAll();
        } catch (Exception e) {
            tables = List.of();
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

        if (item != null) {
            name.setValue(item.getName() != null ? item.getName() : "");
            type.setValue(item.getType());
            fromColumn.setValue(item.getFromColumnName() != null ? item.getFromColumnName() : "");
            toColumn.setValue(item.getToColumnName() != null ? item.getToColumnName() : "");
            description.setValue(item.getDescription() != null ? item.getDescription() : "");
            tables.stream().filter(t -> t.getId().equals(item.getFromTableId())).findFirst().ifPresent(fromTable::setValue);
            tables.stream().filter(t -> t.getId().equals(item.getToTableId())).findFirst().ifPresent(toTable::setValue);
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

    private void confirmDelete(RelationshipDefinitionDto item) {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete relationship \"" + item.getName() + "\"?",
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
            grid.setItems(client.findAll());
        } catch (Exception e) {
            log.error("Failed to load relationships", e);
            notify("Could not load data: " + e.getMessage(), true);
        }
    }

    private void notify(String message, boolean error) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}

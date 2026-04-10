package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.server.auth.AnonymousAllowed;
import it.brunasti.dbdadi.frontend.client.EntityDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.EntityDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.TableDefinitionDto;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Route(value = "entities/:entityId", layout = MainLayout.class)
@PageTitle("DBDaDi | Entity")
@AnonymousAllowed
@Slf4j
public class EntityDefinitionDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final EntityDefinitionClient client;
    private EntityDefinitionDto entity;

    private final TextField nameField = new TextField("Name");
    private final TextArea descriptionField = new TextArea("Description");
    private final Grid<TableDefinitionDto> tablesGrid = new Grid<>(TableDefinitionDto.class, false);

    public EntityDefinitionDetailView(EntityDefinitionClient client) {
        this.client = client;
        setWidthFull();
        setPadding(true);
        configureFields();
        configureGrid();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("entityId").map(Long::valueOf).ifPresent(id -> {
            try {
                entity = client.findById(id);
                populateFields();
                tablesGrid.setItems(client.findTables(id));
            } catch (Exception e) {
                log.error("Could not load entity {}", id, e);
                notify("Could not load entity", true);
            }
        });
    }

    private void configureFields() {
        nameField.setReadOnly(true);
        descriptionField.setReadOnly(true);
        descriptionField.setWidthFull();
    }

    private void configureGrid() {
        tablesGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("tables/" + item.getId()));
            return btn;
        }).setHeader("Table Name").setComparator(Comparator.comparing(TableDefinitionDto::getName));
        tablesGrid.addColumn(TableDefinitionDto::getSchemaName).setHeader("Schema").setSortable(true);
        tablesGrid.addColumn(TableDefinitionDto::getDatabaseModelName).setHeader("Database Model").setSortable(true);
        tablesGrid.addColumn(TableDefinitionDto::getDescription).setHeader("Description").setSortable(true);
        tablesGrid.setAllRowsVisible(true);
    }

    private void populateFields() {
        removeAll();

        HorizontalLayout breadcrumb = new HorizontalLayout();
        Button backToList = new Button("← Entities");
        backToList.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToList.addClickListener(e -> UI.getCurrent().navigate(EntityDefinitionView.class));
        breadcrumb.add(backToList, new Span(" / "), new Span("Entity: " + entity.getName()));
        breadcrumb.setAlignItems(Alignment.CENTER);
        add(breadcrumb);

        nameField.setValue(entity.getName() != null ? entity.getName() : "");
        descriptionField.setValue(entity.getDescription() != null ? entity.getDescription() : "");

        FormLayout form = new FormLayout(nameField, descriptionField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(descriptionField, 2);

        Button editBtn = new Button("Edit", e -> openEditDialog());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button deleteBtn = new Button("Delete", e -> confirmDelete());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        add(form, new HorizontalLayout(editBtn, deleteBtn), new Hr(), new H3("Linked Tables"));
        add(tablesGrid);
    }

    private void openEditDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Entity");
        dialog.setWidth("500px");

        TextField name = new TextField("Name");
        TextArea description = new TextArea("Description");
        name.setValue(entity.getName() != null ? entity.getName() : "");
        description.setValue(entity.getDescription() != null ? entity.getDescription() : "");

        FormLayout form = new FormLayout(name, description);
        form.setColspan(description, 2);

        Button save = new Button("Save", e -> {
            try {
                EntityDefinitionDto dto = EntityDefinitionDto.builder()
                        .name(name.getValue())
                        .description(description.getValue())
                        .build();
                entity = client.update(entity.getId(), dto);
                dialog.close();
                populateFields();
                tablesGrid.setItems(client.findTables(entity.getId()));
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
                "Delete entity \"" + entity.getName() + "\"?",
                "This action cannot be undone. Tables linked to this entity will be unlinked.",
                "Delete", e -> {
                    try {
                        client.delete(entity.getId());
                        UI.getCurrent().navigate(EntityDefinitionView.class);
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

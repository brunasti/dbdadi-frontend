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
import it.brunasti.dbdadi.frontend.client.SchemaDefinitionClient;
import it.brunasti.dbdadi.frontend.client.TableDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.SchemaDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.TableDefinitionDto;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;

@Route(value = "schemas/:schemaId", layout = MainLayout.class)
@PageTitle("DBDaDi | Schema")
@AnonymousAllowed
@Slf4j
public class SchemaDefinitionDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final SchemaDefinitionClient client;
    private final TableDefinitionClient tableClient;
    private SchemaDefinitionDto schema;

    private final TextField nameField = new TextField("Name");
    private final TextField dbModelField = new TextField("Database Model");
    private final TextArea descriptionField = new TextArea("Description");
    private final Grid<TableDefinitionDto> tablesGrid = new Grid<>(TableDefinitionDto.class, false);

    public SchemaDefinitionDetailView(SchemaDefinitionClient client, TableDefinitionClient tableClient) {
        this.client = client;
        this.tableClient = tableClient;
        setWidthFull();
        setPadding(true);
        configureFields();
        configureGrid();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("schemaId").map(Long::valueOf).ifPresent(id -> {
            try {
                schema = client.findById(id);
                populateFields();
                tablesGrid.setItems(tableClient.findBySchema(id));
            } catch (Exception e) {
                log.error("Could not load schema {}", id, e);
                notify("Could not load schema", true);
            }
        });
    }

    private void configureFields() {
        nameField.setReadOnly(true);
        dbModelField.setReadOnly(true);
        descriptionField.setReadOnly(true);
        descriptionField.setWidthFull();
    }

    private void populateFields() {
        removeAll();

        HorizontalLayout breadcrumb = new HorizontalLayout();
        Button backToModels = new Button("← Database Models");
        backToModels.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToModels.addClickListener(e -> UI.getCurrent().navigate(DatabaseModelView.class));
        Button backToModel = new Button(schema.getDatabaseModelName());
        backToModel.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToModel.getStyle().set("padding", "0");
        backToModel.addClickListener(e -> UI.getCurrent().navigate("database-models/" + schema.getDatabaseModelId()));
        breadcrumb.add(backToModels, new Span(" / "), backToModel,
                new Span(" / "), new Span("Schema: " + schema.getName()));
        breadcrumb.setAlignItems(Alignment.CENTER);
        add(breadcrumb);

        nameField.setValue(schema.getName() != null ? schema.getName() : "");
        dbModelField.setValue(schema.getDatabaseModelName() != null ? schema.getDatabaseModelName() : "");
        descriptionField.setValue(schema.getDescription() != null ? schema.getDescription() : "");

        FormLayout form = new FormLayout(nameField, dbModelField, descriptionField);
        form.setColspan(descriptionField, 2);

        Button editBtn = new Button("Edit", e -> openEditDialog());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button deleteBtn = new Button("Delete", e -> confirmDelete());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        add(form, new HorizontalLayout(editBtn, deleteBtn), new Hr(), new H3("Tables"));
        tablesGrid.setAllRowsVisible(true);
        add(createAddTableButton(), tablesGrid);
    }

    private void configureGrid() {
        tablesGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("tables/" + item.getId()));
            return btn;
        }).setHeader("Name").setComparator(Comparator.comparing(TableDefinitionDto::getName));
        tablesGrid.addColumn(TableDefinitionDto::getDescription).setHeader("Description").setSortable(true);
        tablesGrid.addComponentColumn(item -> {
            Button edit = new Button("Edit", e -> openEditTableDialog(item));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
            Button delete = new Button("Delete", e -> confirmDeleteTable(item));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, delete);
        }).setHeader("Actions").setWidth("160px").setFlexGrow(0);
    }

    private Button createAddTableButton() {
        Button btn = new Button("New Table", e -> openEditTableDialog(null));
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        return btn;
    }

    private void openEditDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Schema");
        dialog.setWidth("500px");

        TextField name = new TextField("Schema Name");
        TextArea description = new TextArea("Description");
        name.setValue(schema.getName() != null ? schema.getName() : "");
        description.setValue(schema.getDescription() != null ? schema.getDescription() : "");

        FormLayout form = new FormLayout(name, description);
        form.setColspan(description, 2);

        Button save = new Button("Save", e -> {
            try {
                SchemaDefinitionDto dto = SchemaDefinitionDto.builder()
                        .name(name.getValue()).description(description.getValue())
                        .databaseModelId(schema.getDatabaseModelId()).build();
                schema = client.update(schema.getId(), dto);
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

    private void openEditTableDialog(TableDefinitionDto item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item == null ? "New Table" : "Edit Table");
        dialog.setWidth("500px");

        TextField name = new TextField("Table Name");
        TextArea description = new TextArea("Description");
        if (item != null) {
            name.setValue(item.getName() != null ? item.getName() : "");
            description.setValue(item.getDescription() != null ? item.getDescription() : "");
        }

        FormLayout form = new FormLayout(name, description);
        form.setColspan(description, 2);

        Button save = new Button("Save", e -> {
            try {
                TableDefinitionDto dto = TableDefinitionDto.builder()
                        .name(name.getValue()).description(description.getValue())
                        .schemaId(schema.getId()).build();
                if (item == null) tableClient.create(dto);
                else tableClient.update(item.getId(), dto);
                dialog.close();
                tablesGrid.setItems(tableClient.findBySchema(schema.getId()));
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
                "Delete schema \"" + schema.getName() + "\"?",
                "This will also delete all tables and columns.",
                "Delete", e -> {
                    try {
                        client.delete(schema.getId());
                        UI.getCurrent().navigate("database-models/" + schema.getDatabaseModelId());
                    } catch (Exception ex) {
                        notify("Delete failed: " + ex.getMessage(), true);
                    }
                },
                "Cancel", e -> {});
        confirm.setConfirmButtonTheme("error primary");
        confirm.open();
    }

    private void confirmDeleteTable(TableDefinitionDto item) {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete table \"" + item.getName() + "\"?",
                "This will also delete all columns.",
                "Delete", e -> {
                    try {
                        tableClient.delete(item.getId());
                        tablesGrid.setItems(tableClient.findBySchema(schema.getId()));
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

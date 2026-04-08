package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
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
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import it.brunasti.dbdadi.frontend.client.DatabaseModelClient;
import it.brunasti.dbdadi.frontend.client.SchemaDefinitionClient;
import it.brunasti.dbdadi.frontend.client.TableDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.DatabaseModelDto;
import it.brunasti.dbdadi.frontend.dto.SchemaDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.TableDefinitionDto;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Route(value = "tables", layout = MainLayout.class)
@PageTitle("DBDaDi | Tables")
@AnonymousAllowed
@Slf4j
public class TableDefinitionView extends VerticalLayout implements BeforeEnterObserver {

    private final TableDefinitionClient client;
    private final SchemaDefinitionClient schemaClient;
    private final DatabaseModelClient dbModelClient;
    private final Grid<TableDefinitionDto> grid = new Grid<>(TableDefinitionDto.class, false);
    private final ComboBox<DatabaseModelDto> dbModelFilter = new ComboBox<>("Filter by Database Model");
    private final ComboBox<SchemaDefinitionDto> schemaFilter = new ComboBox<>("Filter by Schema");
    private final HorizontalLayout breadcrumb = new HorizontalLayout();

    public TableDefinitionView(TableDefinitionClient client, SchemaDefinitionClient schemaClient,
                                DatabaseModelClient dbModelClient) {
        this.client = client;
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
        event.getLocation().getQueryParameters().getSingleParameter("schemaId")
                .map(Long::valueOf)
                .ifPresent(id -> {
                    try {
                        SchemaDefinitionDto schema = schemaClient.findById(id);
                        // pre-select cascading filters without triggering intermediate refreshes
                        schemaFilter.setItems(schemaClient.findByDatabaseModel(schema.getDatabaseModelId()));
                        dbModelFilter.setValue(dbModelClient.findById(schema.getDatabaseModelId()));
                        schemaFilter.setValue(schema);
                        showBreadcrumb(schema);
                    } catch (Exception e) {
                        log.warn("Could not load schema {}", id);
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

        dbModelFilter.addValueChangeListener(e -> {
            schemaFilter.clear();
            DatabaseModelDto model = e.getValue();
            if (model != null) {
                try { schemaFilter.setItems(schemaClient.findByDatabaseModel(model.getId())); }
                catch (Exception ex) { log.warn("Could not load schemas for model {}", model.getId()); }
                schemaFilter.setEnabled(true);
            } else {
                schemaFilter.setItems();
                schemaFilter.setEnabled(false);
            }
            updateBreadcrumb();
            refresh();
        });

        schemaFilter.addValueChangeListener(e -> {
            updateBreadcrumb();
            refresh();
        });
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(TableDefinitionDto::getId).setHeader("ID").setWidth("80px").setFlexGrow(0).setSortable(true);
        grid.addComponentColumn(item -> {
            Button nameBtn = new Button(item.getName());
            nameBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            nameBtn.getStyle().set("padding", "0").set("font-weight", "bold");
            nameBtn.addClickListener(e -> UI.getCurrent().navigate("tables/" + item.getId()));
            return nameBtn;
        }).setHeader("Name").setComparator(Comparator.comparing(TableDefinitionDto::getName));
        grid.addComponentColumn(item -> {
            Button btn = new Button(item.getSchemaName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0");
            btn.addClickListener(e -> UI.getCurrent().navigate(
                    SchemaDefinitionView.class,
                    new QueryParameters(Map.of("databaseModelId",
                            List.of(String.valueOf(item.getDatabaseModelId()))))));
            return btn;
        }).setHeader("Schema").setComparator(Comparator.comparing(TableDefinitionDto::getSchemaName));
        grid.addColumn(TableDefinitionDto::getDatabaseModelName).setHeader("Database Model").setSortable(true);
        grid.addColumn(TableDefinitionDto::getDescription).setHeader("Description").setSortable(true);
        grid.addComponentColumn(item -> {
            Button edit = new Button("Edit", e -> openDialog(item));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
            Button delete = new Button("Delete", e -> confirmDelete(item));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, delete);
        }).setHeader("Actions").setWidth("160px").setFlexGrow(0);
    }

    private HorizontalLayout createToolbar() {
        Button addBtn = new Button("New Table", e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button refreshBtn = new Button("Refresh", e -> refresh());
        return new HorizontalLayout(addBtn, refreshBtn, dbModelFilter, schemaFilter);
    }

    private void showBreadcrumb(SchemaDefinitionDto schema) {
        breadcrumb.removeAll();
        Button backToModel = new Button("← Database Models");
        backToModel.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToModel.addClickListener(e -> UI.getCurrent().navigate(DatabaseModelView.class));
        Button backToSchemas = new Button("Schemas of: " + schema.getDatabaseModelName());
        backToSchemas.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToSchemas.addClickListener(e -> UI.getCurrent().navigate(
                SchemaDefinitionView.class,
                new QueryParameters(Map.of("databaseModelId",
                        List.of(String.valueOf(schema.getDatabaseModelId()))))));
        breadcrumb.add(backToModel, new Span(" / "), backToSchemas,
                new Span(" / "), new Span("Tables of: " + schema.getName()));
        breadcrumb.setAlignItems(Alignment.CENTER);
        breadcrumb.setVisible(true);
    }

    private void updateBreadcrumb() {
        SchemaDefinitionDto selected = schemaFilter.getValue();
        if (selected != null) showBreadcrumb(selected);
        else breadcrumb.setVisible(false);
    }

    private void openDialog(TableDefinitionDto item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item == null ? "New Table" : "Edit Table");
        dialog.setWidth("500px");

        ComboBox<SchemaDefinitionDto> schema = new ComboBox<>("Schema");
        schema.setItemLabelGenerator(s -> s.getDatabaseModelName() + " / " + s.getName());
        try { schema.setItems(schemaClient.findAll()); }
        catch (Exception e) { log.warn("Could not load schemas"); }
        TextField name = new TextField("Table Name");
        TextArea description = new TextArea("Description");

        if (item != null) {
            name.setValue(item.getName() != null ? item.getName() : "");
            description.setValue(item.getDescription() != null ? item.getDescription() : "");
            if (item.getSchemaId() != null) {
                schemaClient.findAll().stream()
                        .filter(s -> s.getId().equals(item.getSchemaId()))
                        .findFirst().ifPresent(schema::setValue);
            }
        } else {
            schema.setValue(schemaFilter.getValue());
        }

        FormLayout form = new FormLayout(schema, name, description);
        form.setColspan(schema, 2);
        form.setColspan(description, 2);

        Button save = new Button("Save", e -> {
            try {
                TableDefinitionDto dto = TableDefinitionDto.builder()
                        .name(name.getValue()).description(description.getValue())
                        .schemaId(schema.getValue() != null ? schema.getValue().getId() : null)
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

    private void confirmDelete(TableDefinitionDto item) {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete table \"" + item.getName() + "\"?",
                "This will also delete all columns in this table.",
                "Delete", e -> {
                    try { client.delete(item.getId()); refresh(); notify("Deleted", false); }
                    catch (Exception ex) { notify("Delete failed: " + ex.getMessage(), true); }
                },
                "Cancel", e -> {});
        confirm.setConfirmButtonTheme("error primary");
        confirm.open();
    }

    private void refresh() {
        try {
            SchemaDefinitionDto selectedSchema = schemaFilter.getValue();
            DatabaseModelDto selectedModel = dbModelFilter.getValue();
            grid.setItems(selectedSchema != null
                    ? client.findBySchema(selectedSchema.getId())
                    : selectedModel != null
                        ? client.findByDatabaseModel(selectedModel.getId())
                        : client.findAll());
        } catch (Exception e) {
            notify("Could not load data: " + e.getMessage(), true);
        }
    }

    private void notify(String message, boolean error) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}

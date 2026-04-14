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
import jakarta.annotation.security.PermitAll;
import it.brunasti.dbdadi.frontend.security.SecurityUtils;
import it.brunasti.dbdadi.frontend.client.DatabaseModelClient;
import it.brunasti.dbdadi.frontend.client.SchemaDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.DatabaseModelDto;
import it.brunasti.dbdadi.frontend.dto.SchemaDefinitionDto;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Route(value = "schemas", layout = MainLayout.class)
@PageTitle("DBDaDi | Schemas")
@PermitAll
@Slf4j
public class SchemaDefinitionView extends VerticalLayout implements BeforeEnterObserver {

    private final SchemaDefinitionClient client;
    private final DatabaseModelClient dbModelClient;
    private final Grid<SchemaDefinitionDto> grid = new Grid<>(SchemaDefinitionDto.class, false);
    private final ComboBox<DatabaseModelDto> modelFilter = new ComboBox<>("Filter by Database Model");
    private final HorizontalLayout breadcrumb = new HorizontalLayout();

    public SchemaDefinitionView(SchemaDefinitionClient client, DatabaseModelClient dbModelClient) {
        this.client = client;
        this.dbModelClient = dbModelClient;
        setSizeFull();
        configureGrid();
        configureFilter();
        breadcrumb.setVisible(false);
        add(breadcrumb, createToolbar(), grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getLocation().getQueryParameters().getSingleParameter("databaseModelId")
                .map(Long::valueOf)
                .ifPresent(id -> {
                    try {
                        DatabaseModelDto model = dbModelClient.findById(id);
                        modelFilter.setValue(model);
                        showBreadcrumb("Schemas of: " + model.getName());
                    } catch (Exception e) {
                        log.warn("Could not load database model {}", id);
                    }
                });
        refresh();
    }

    private void configureFilter() {
        modelFilter.setItemLabelGenerator(DatabaseModelDto::getName);
        modelFilter.setClearButtonVisible(true);
        try { modelFilter.setItems(dbModelClient.findAll()); }
        catch (Exception e) { log.warn("Could not load database models for filter"); }
        modelFilter.addValueChangeListener(e -> {
            updateBreadcrumb();
            refresh();
        });
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addComponentColumn(item -> {
            Button btn = new Button(item.getDatabaseModelName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0");
            btn.addClickListener(e -> UI.getCurrent().navigate(DatabaseModelView.class));
            return btn;
        }).setHeader("Database Model").setComparator(Comparator.comparing(SchemaDefinitionDto::getDatabaseModelName));
        grid.addComponentColumn(item -> {
            Button nameBtn = new Button(item.getName());
            nameBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            nameBtn.getStyle().set("padding", "0").set("font-weight", "bold");
            nameBtn.addClickListener(e -> UI.getCurrent().navigate("schemas/" + item.getId()));
            return nameBtn;
        }).setHeader("Name").setComparator(Comparator.comparing(SchemaDefinitionDto::getName));
        grid.addColumn(SchemaDefinitionDto::getDescription).setHeader("Description").setSortable(true);
        grid.addComponentColumn(item -> {
            Button edit = new Button("Edit", e -> openDialog(item));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
            Button delete = new Button("Delete", e -> confirmDelete(item));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            if (!SecurityUtils.canEdit()) return new com.vaadin.flow.component.html.Span();
            return new HorizontalLayout(edit, delete);
        }).setHeader("Actions").setWidth("160px").setFlexGrow(0);
    }

    private HorizontalLayout createToolbar() {
        Button addBtn = new Button("New Schema", e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.setVisible(SecurityUtils.canEdit());
        Button refreshBtn = new Button("Refresh", e -> refresh());
        return new HorizontalLayout(addBtn, refreshBtn, modelFilter);
    }

    private void showBreadcrumb(String currentLabel) {
        breadcrumb.removeAll();
        Button backBtn = new Button("← Database Models");
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backBtn.addClickListener(e -> UI.getCurrent().navigate(DatabaseModelView.class));
        breadcrumb.add(backBtn, new Span(" / "), new Span(currentLabel));
        breadcrumb.setAlignItems(Alignment.CENTER);
        breadcrumb.setVisible(true);
    }

    private void updateBreadcrumb() {
        DatabaseModelDto selected = modelFilter.getValue();
        if (selected != null) showBreadcrumb("Schemas of: " + selected.getName());
        else breadcrumb.setVisible(false);
    }

    private void openDialog(SchemaDefinitionDto item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item == null ? "New Schema" : "Edit Schema");
        dialog.setWidth("500px");

        ComboBox<DatabaseModelDto> dbModel = new ComboBox<>("Database Model");
        dbModel.setItemLabelGenerator(DatabaseModelDto::getName);
        try { dbModel.setItems(dbModelClient.findAll()); }
        catch (Exception e) { log.warn("Could not load database models"); }
        TextField name = new TextField("Schema Name");
        TextArea description = new TextArea("Description");

        if (item != null) {
            name.setValue(item.getName() != null ? item.getName() : "");
            description.setValue(item.getDescription() != null ? item.getDescription() : "");
            if (item.getDatabaseModelId() != null) {
                dbModelClient.findAll().stream()
                        .filter(m -> m.getId().equals(item.getDatabaseModelId()))
                        .findFirst().ifPresent(dbModel::setValue);
            }
        } else {
            dbModel.setValue(modelFilter.getValue());
        }

        FormLayout form = new FormLayout(dbModel, name, description);
        form.setColspan(dbModel, 2);
        form.setColspan(description, 2);

        Button save = new Button("Save", e -> {
            try {
                SchemaDefinitionDto dto = SchemaDefinitionDto.builder()
                        .name(name.getValue()).description(description.getValue())
                        .databaseModelId(dbModel.getValue() != null ? dbModel.getValue().getId() : null)
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

    private void confirmDelete(SchemaDefinitionDto item) {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete schema \"" + item.getName() + "\"?",
                "This will also delete all tables and columns in this schema.",
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
            DatabaseModelDto selected = modelFilter.getValue();
            grid.setItems(selected != null
                    ? client.findByDatabaseModel(selected.getId())
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

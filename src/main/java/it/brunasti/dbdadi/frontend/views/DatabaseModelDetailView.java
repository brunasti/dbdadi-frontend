package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import it.brunasti.dbdadi.frontend.security.SecurityUtils;
import it.brunasti.dbdadi.frontend.client.AlignmentClient;
import it.brunasti.dbdadi.frontend.client.DatabaseModelClient;
import it.brunasti.dbdadi.frontend.client.DomainDefinitionClient;
import it.brunasti.dbdadi.frontend.client.SchemaDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.AlignmentItem;
import it.brunasti.dbdadi.frontend.dto.AlignmentResult;
import it.brunasti.dbdadi.frontend.dto.DatabaseModelDto;
import it.brunasti.dbdadi.frontend.dto.DomainDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.SchemaDefinitionDto;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;

@Route(value = "database-models/:modelId", layout = MainLayout.class)
@PageTitle("DBDaDi | Database Model")
@PermitAll
@Slf4j
public class DatabaseModelDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final DatabaseModelClient client;
    private final SchemaDefinitionClient schemaClient;
    private final AlignmentClient alignmentClient;
    private final DomainDefinitionClient domainClient;
    private DatabaseModelDto model;

    private final Grid<DomainDefinitionDto> domainsGrid = new Grid<>(DomainDefinitionDto.class, false);

    private final TextField nameField = new TextField("Name");
    private final TextField dbTypeField = new TextField("DB Type");
    private final TextField versionField = new TextField("Version");
    private final TextArea descriptionField = new TextArea("Description");
    private final TextField jdbcUrlField = new TextField("JDBC URL");
    private final TextField usernameField = new TextField("Username");
    private final TextField schemaPatternField = new TextField("Schema Pattern");
    private final TextField tablePatternField = new TextField("Table Pattern");
    private final TextField importFlagsField = new TextField("Import Flags");
    private final Grid<SchemaDefinitionDto> schemasGrid = new Grid<>(SchemaDefinitionDto.class, false);

    public DatabaseModelDetailView(DatabaseModelClient client, SchemaDefinitionClient schemaClient,
                                    AlignmentClient alignmentClient, DomainDefinitionClient domainClient) {
        this.client = client;
        this.schemaClient = schemaClient;
        this.alignmentClient = alignmentClient;
        this.domainClient = domainClient;
        setWidthFull();
        setPadding(true);
        configureFields();
        configureGrid();
        configureDomainsGrid();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("modelId").map(Long::valueOf).ifPresent(id -> {
            try {
                model = client.findById(id);
                populateFields();
                schemasGrid.setItems(schemaClient.findByDatabaseModel(id));
                domainsGrid.setItems(domainClient.findByDatabaseModel(id));
            } catch (Exception e) {
                log.error("Could not load database model {}", id, e);
                notify("Could not load database model", true);
            }
        });
    }

    private void configureFields() {
        nameField.setReadOnly(true);
        dbTypeField.setReadOnly(true);
        versionField.setReadOnly(true);
        descriptionField.setReadOnly(true);
        descriptionField.setWidthFull();
        jdbcUrlField.setReadOnly(true);
        jdbcUrlField.setWidthFull();
        usernameField.setReadOnly(true);
        schemaPatternField.setReadOnly(true);
        tablePatternField.setReadOnly(true);
        importFlagsField.setReadOnly(true);
        importFlagsField.setWidthFull();
    }

    private void populateFields() {
        removeAll();

        HorizontalLayout breadcrumb = new HorizontalLayout();
        Button backBtn = new Button("← Database Models");
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backBtn.addClickListener(e -> UI.getCurrent().navigate(DatabaseModelView.class));
        breadcrumb.add(backBtn, new Span(" / "), new Span("Model: " + model.getName()));
        breadcrumb.setAlignItems(Alignment.CENTER);
        add(breadcrumb);

        nameField.setValue(model.getName() != null ? model.getName() : "");
        dbTypeField.setValue(model.getDbType() != null ? model.getDbType().name() : "");
        versionField.setValue(model.getVersion() != null ? model.getVersion() : "");
        descriptionField.setValue(model.getDescription() != null ? model.getDescription() : "");

        FormLayout form = new FormLayout(nameField, dbTypeField, versionField, descriptionField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
        form.setColspan(descriptionField, 3);

        jdbcUrlField.setValue(model.getJdbcUrl() != null ? model.getJdbcUrl() : "");
        usernameField.setValue(model.getUsername() != null ? model.getUsername() : "");
        schemaPatternField.setValue(model.getSchemaPattern() != null ? model.getSchemaPattern() : "");
        tablePatternField.setValue(model.getTablePattern() != null ? model.getTablePattern() : "");
        importFlagsField.setValue(model.getImportFlags() != null ? model.getImportFlags() : "");

        FormLayout importForm = new FormLayout(jdbcUrlField, usernameField, schemaPatternField, tablePatternField, importFlagsField);
        importForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        importForm.setColspan(jdbcUrlField, 2);
        importForm.setColspan(importFlagsField, 2);

        Button editBtn = new Button("Edit", e -> openEditDialog());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editBtn.setVisible(SecurityUtils.canEdit());
        Button deleteBtn = new Button("Delete", e -> confirmDelete());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteBtn.setVisible(SecurityUtils.canEdit());

        Button alignBtn = new Button("Check Alignment", e -> openAlignmentDialog());
        alignBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        alignBtn.setVisible(model.getJdbcUrl() != null && !model.getJdbcUrl().isBlank());

        add(form, new HorizontalLayout(editBtn, deleteBtn, alignBtn), new Hr(), new H3("Schemas"));
        schemasGrid.setAllRowsVisible(true);
        add(createAddSchemaButton(), schemasGrid);

        add(new Hr(), new H3("Linked Domains"));
        domainsGrid.setAllRowsVisible(true);
        add(domainsGrid);

        add(new Hr(), new H4("Import Configuration"), importForm);
    }

    private void configureGrid() {
        schemasGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("schemas/" + item.getId()));
            return btn;
        }).setHeader("Name").setComparator(Comparator.comparing(SchemaDefinitionDto::getName));
        schemasGrid.addColumn(SchemaDefinitionDto::getDescription).setHeader("Description").setSortable(true);
        if (SecurityUtils.canEdit()) {
            schemasGrid.addComponentColumn(item -> {
                Button edit = new Button("Edit", e -> openEditSchemaDialog(item));
                edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
                Button delete = new Button("Delete", e -> confirmDeleteSchema(item));
                delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                return new HorizontalLayout(edit, delete);
            }).setHeader("Actions").setWidth("160px").setFlexGrow(0);
        }
    }

    private void configureDomainsGrid() {
        domainsGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("domains/" + item.getId()));
            return btn;
        }).setHeader("Domain Name").setComparator(Comparator.comparing(DomainDefinitionDto::getName));
        domainsGrid.addColumn(DomainDefinitionDto::getDescription).setHeader("Description").setSortable(true);
    }

    private Button createAddSchemaButton() {
        Button btn = new Button("New Schema", e -> openEditSchemaDialog(null));
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        btn.setVisible(SecurityUtils.canEdit());
        return btn;
    }

    private void openEditDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Database Model");
        dialog.setWidth("600px");

        TextField name = new TextField("Name");
        TextArea description = new TextArea("Description");
        TextField version = new TextField("Version");
        TextField jdbcUrl = new TextField("JDBC URL");
        TextField username = new TextField("Username");
        TextField schemaPattern = new TextField("Schema Pattern");
        TextField tablePattern = new TextField("Table Pattern");
        TextField importFlags = new TextField("Import Flags");

        name.setValue(model.getName() != null ? model.getName() : "");
        description.setValue(model.getDescription() != null ? model.getDescription() : "");
        version.setValue(model.getVersion() != null ? model.getVersion() : "");
        jdbcUrl.setValue(model.getJdbcUrl() != null ? model.getJdbcUrl() : "");
        username.setValue(model.getUsername() != null ? model.getUsername() : "");
        schemaPattern.setValue(model.getSchemaPattern() != null ? model.getSchemaPattern() : "");
        tablePattern.setValue(model.getTablePattern() != null ? model.getTablePattern() : "");
        importFlags.setValue(model.getImportFlags() != null ? model.getImportFlags() : "");

        FormLayout form = new FormLayout(name, version, description,
                jdbcUrl, username, schemaPattern, tablePattern, importFlags);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(description, 2);
        form.setColspan(jdbcUrl, 2);
        form.setColspan(importFlags, 2);

        Button save = new Button("Save", e -> {
            try {
                DatabaseModelDto dto = DatabaseModelDto.builder()
                        .name(name.getValue()).description(description.getValue())
                        .dbType(model.getDbType()).version(version.getValue())
                        .jdbcUrl(jdbcUrl.getValue()).username(username.getValue())
                        .schemaPattern(schemaPattern.getValue()).tablePattern(tablePattern.getValue())
                        .importFlags(importFlags.getValue()).build();
                model = client.update(model.getId(), dto);
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

    private void openEditSchemaDialog(SchemaDefinitionDto item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item == null ? "New Schema" : "Edit Schema");
        dialog.setWidth("500px");

        TextField name = new TextField("Schema Name");
        TextArea description = new TextArea("Description");
        if (item != null) {
            name.setValue(item.getName() != null ? item.getName() : "");
            description.setValue(item.getDescription() != null ? item.getDescription() : "");
        }

        FormLayout form = new FormLayout(name, description);
        form.setColspan(description, 2);

        Button save = new Button("Save", e -> {
            try {
                SchemaDefinitionDto dto = SchemaDefinitionDto.builder()
                        .name(name.getValue()).description(description.getValue())
                        .databaseModelId(model.getId()).build();
                if (item == null) schemaClient.create(dto);
                else schemaClient.update(item.getId(), dto);
                dialog.close();
                schemasGrid.setItems(schemaClient.findByDatabaseModel(model.getId()));
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
                "Delete \"" + model.getName() + "\"?",
                "This will also delete all schemas, tables and columns.",
                "Delete", e -> {
                    try {
                        client.delete(model.getId());
                        UI.getCurrent().navigate(DatabaseModelView.class);
                    } catch (Exception ex) {
                        notify("Delete failed: " + ex.getMessage(), true);
                    }
                },
                "Cancel", e -> {});
        confirm.setConfirmButtonTheme("error primary");
        confirm.open();
    }

    private void confirmDeleteSchema(SchemaDefinitionDto item) {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete schema \"" + item.getName() + "\"?",
                "This will also delete all tables and columns.",
                "Delete", e -> {
                    try {
                        schemaClient.delete(item.getId());
                        schemasGrid.setItems(schemaClient.findByDatabaseModel(model.getId()));
                        notify("Deleted", false);
                    } catch (Exception ex) {
                        notify("Delete failed: " + ex.getMessage(), true);
                    }
                },
                "Cancel", e -> {});
        confirm.setConfirmButtonTheme("error primary");
        confirm.open();
    }

    private void openAlignmentDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Check Alignment — " + model.getName());
        dialog.setWidth("520px");

        TextField jdbcUrlInfo = new TextField("JDBC URL");
        jdbcUrlInfo.setValue(model.getJdbcUrl() != null ? model.getJdbcUrl() : "");
        jdbcUrlInfo.setReadOnly(true);
        jdbcUrlInfo.setWidthFull();

        TextField usernameInfo = new TextField("Username");
        usernameInfo.setValue(model.getUsername() != null ? model.getUsername() : "");
        usernameInfo.setReadOnly(true);

        PasswordField passwordField = new PasswordField("Password");
        passwordField.setWidthFull();

        FormLayout form = new FormLayout(jdbcUrlInfo, usernameInfo, passwordField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(jdbcUrlInfo, 2);
        form.setColspan(passwordField, 2);

        Button runBtn = new Button("Run Check", e -> {
            dialog.close();
            runAlignmentCheck(passwordField.getValue());
        });
        runBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(form);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), runBtn);
        dialog.open();
    }

    private void runAlignmentCheck(String password) {
        AlignmentResult result;
        try {
            result = alignmentClient.check(model.getId(), password);
        } catch (Exception e) {
            notify("Alignment check failed: " + e.getMessage(), true);
            return;
        }

        Dialog resultDialog = new Dialog();
        resultDialog.setHeaderTitle("Alignment Result — " + model.getName());
        resultDialog.setWidth("950px");

        String statusText = result.isAligned()
                ? "ALIGNED"
                : result.getDifferences().size() + " difference(s) found";
        String summaryText = String.format("Schemas: %d  |  Tables: %d  |  Columns: %d  |  %s",
                result.getSchemasChecked(), result.getTablesChecked(),
                result.getColumnsChecked(), statusText);
        Span summarySpan = new Span(summaryText);
        summarySpan.getStyle()
                .set("font-weight", "bold")
                .set("color", result.isAligned() ? "#2e7d32" : "#c62828");

        VerticalLayout content = new VerticalLayout(summarySpan);
        content.setPadding(false);
        content.setSpacing(true);

        if (!result.isAligned()) {
            Grid<AlignmentItem> grid = new Grid<>();
            grid.setItems(result.getDifferences());

            grid.addColumn(item -> {
                if (item.getColumnName() != null) return "COLUMN";
                if (item.getTableName() != null) return "TABLE";
                return "SCHEMA";
            }).setHeader("Level").setWidth("90px").setFlexGrow(0);

            grid.addColumn(AlignmentItem::getSchemaName).setHeader("Schema").setWidth("160px").setFlexGrow(0);

            grid.addColumn(item -> item.getTableName() != null ? item.getTableName() : "")
                    .setHeader("Table").setWidth("160px").setFlexGrow(0);

            grid.addColumn(item -> item.getColumnName() != null ? item.getColumnName() : "")
                    .setHeader("Column").setWidth("160px").setFlexGrow(0);

            grid.addComponentColumn(item -> {
                Span badge = new Span(item.getStatus());
                String color = switch (item.getStatus()) {
                    case "ADDED"   -> "#2e7d32";
                    case "REMOVED" -> "#c62828";
                    case "CHANGED" -> "#e65100";
                    default        -> "#1565c0";
                };
                badge.getStyle()
                        .set("background", color).set("color", "white")
                        .set("padding", "2px 8px").set("border-radius", "4px")
                        .set("font-size", "0.82em").set("font-weight", "bold");
                return badge;
            }).setHeader("Status").setWidth("100px").setFlexGrow(0);

            grid.addColumn(AlignmentItem::getDetails).setHeader("Details").setAutoWidth(true);
            grid.setAllRowsVisible(false);
            grid.setHeight("450px");
            content.add(grid);
        }

        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            result.getWarnings().forEach(w -> {
                Span warn = new Span("⚠ " + w);
                warn.getStyle().set("color", "#e65100").set("font-size", "0.9em");
                content.add(warn);
            });
        }

        resultDialog.add(content);
        resultDialog.getFooter().add(new Button("Close", e -> resultDialog.close()));
        resultDialog.open();
    }

    private void notify(String message, boolean error) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}

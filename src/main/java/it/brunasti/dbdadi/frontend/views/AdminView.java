package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;
import it.brunasti.dbdadi.frontend.client.ExcelExportClient;
import it.brunasti.dbdadi.frontend.client.ExcelImportClient;
import it.brunasti.dbdadi.frontend.client.JdbcImportClient;
import it.brunasti.dbdadi.frontend.client.ResetClient;
import it.brunasti.dbdadi.frontend.dto.ExcelImportResult;
import it.brunasti.dbdadi.frontend.dto.JdbcImportRequest;
import it.brunasti.dbdadi.frontend.dto.JdbcImportResult;
import it.brunasti.dbdadi.frontend.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;

@Route(value = "admin", layout = MainLayout.class)
@PageTitle("DBDaDi | Admin")
@RolesAllowed({"ADMIN", "POWER_USER"})
@Slf4j
public class AdminView extends VerticalLayout {

    private final JdbcImportClient importClient;
    private final ExcelExportClient exportClient;
    private final ExcelImportClient excelImportClient;
    private final ResetClient resetClient;

    // Result panel (hidden until JDBC import runs)
    private final VerticalLayout resultPanel = new VerticalLayout();

    public AdminView(JdbcImportClient importClient, ExcelExportClient exportClient,
                     ExcelImportClient excelImportClient, ResetClient resetClient) {
        this.importClient = importClient;
        this.exportClient = exportClient;
        this.excelImportClient = excelImportClient;
        this.resetClient = resetClient;
        setSizeFull();
        setPadding(true);

        resultPanel.setVisible(false);

        TabSheet tabSheet = new TabSheet();
        tabSheet.setWidthFull();
        tabSheet.add("Import from JDBC", buildImportTab());
        tabSheet.add("Import from Excel", buildExcelImportTab());
        tabSheet.add("Export to Excel", buildExportTab());
        if (SecurityUtils.isAdmin()) {
            tabSheet.add("Reset", buildResetTab());
        }

        add(tabSheet);
    }

    private VerticalLayout buildResetTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setMaxWidth("700px");

        layout.add(new Paragraph(
                "These operations permanently delete data from the system. " +
                "Use with extreme caution — deletions cannot be undone."));

        // --- Reset Database information ---
        layout.add(new H3("Database Information"));
        layout.add(new Paragraph(
                "Deletes all Database Models, Schemas, Tables, Columns and Relationships."));

        Button resetDbBtn = new Button("Reset all Database information");
        resetDbBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        resetDbBtn.addClickListener(e -> {
            ConfirmDialog confirm = new ConfirmDialog(
                    "Reset all Database information?",
                    "This will permanently delete ALL Database Models, Schemas, Tables, Columns and Relationships. " +
                    "This cannot be undone.",
                    "Delete", ev -> {
                        try {
                            resetClient.resetDatabase();
                            notify("All database information deleted.", false);
                        } catch (Exception ex) {
                            log.error("Reset database failed", ex);
                            notify("Reset failed: " + ex.getMessage(), true);
                        }
                    },
                    "Cancel", ev -> {});
            confirm.setConfirmButtonTheme("error primary");
            confirm.open();
        });

        layout.add(resetDbBtn, new Hr());

        // --- Reset Modeling information ---
        layout.add(new H3("Modeling Information"));
        layout.add(new Paragraph(
                "Deletes all Entities and Attributes."));

        Button resetModelBtn = new Button("Reset all Modeling information");
        resetModelBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        resetModelBtn.addClickListener(e -> {
            ConfirmDialog confirm = new ConfirmDialog(
                    "Reset all Modeling information?",
                    "This will permanently delete ALL Entities and Attributes. " +
                    "This cannot be undone.",
                    "Delete", ev -> {
                        try {
                            resetClient.resetModeling();
                            notify("All modeling information deleted.", false);
                        } catch (Exception ex) {
                            log.error("Reset modeling failed", ex);
                            notify("Reset failed: " + ex.getMessage(), true);
                        }
                    },
                    "Cancel", ev -> {});
            confirm.setConfirmButtonTheme("error primary");
            confirm.open();
        });

        layout.add(resetModelBtn);
        return layout;
    }

    private VerticalLayout buildExportTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setMaxWidth("600px");

        layout.add(new Paragraph(
                "Export the full content of the data dictionary to a multi-sheet Excel file (.xlsx). " +
                "The file contains one sheet per entity type: Database Models, Entities, Schemas, " +
                "Tables, Columns and Relationships."));

        String filename = "dbdadi-export-" + LocalDate.now() + ".xlsx";
        StreamResource resource = new StreamResource(filename, () -> {
            try {
                byte[] bytes = exportClient.exportExcel();
                return new ByteArrayInputStream(bytes);
            } catch (Exception ex) {
                log.error("Export failed", ex);
                return new ByteArrayInputStream(new byte[0]);
            }
        });
        resource.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        com.vaadin.flow.component.html.Anchor downloadLink =
                new com.vaadin.flow.component.html.Anchor(resource, "Export to Excel (.xlsx)");
        downloadLink.getElement().setAttribute("download", true);
        downloadLink.getStyle()
                .set("display", "inline-block")
                .set("padding", "var(--lumo-space-m) var(--lumo-space-l)")
                .set("background-color", "var(--lumo-primary-color)")
                .set("color", "var(--lumo-primary-contrast-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("text-decoration", "none")
                .set("font-weight", "600");

        layout.add(downloadLink);
        return layout;
    }

    private VerticalLayout buildExcelImportTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setMaxWidth("700px");

        layout.add(new Paragraph(
                "Upload a previously exported DbDaDi Excel file (.xlsx) to recreate all entities, " +
                "attributes, database models, schemas, tables, columns and relationships. " +
                "Existing records with the same name are skipped (no overwrite)."));

        VerticalLayout importResultPanel = new VerticalLayout();
        importResultPanel.setPadding(false);
        importResultPanel.setVisible(false);

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");
        upload.setMaxFiles(1);
        upload.setDropLabel(new Span("Drop .xlsx file here or click to browse"));

        Checkbox clearCheckbox = new Checkbox("Delete ALL existing data before import");
        clearCheckbox.getStyle()
                .set("color", "var(--lumo-error-color)")
                .set("font-weight", "bold");

        Paragraph clearWarning = new Paragraph(
                "Warning: this will permanently delete all entities, attributes, database models, " +
                "schemas, tables, columns, relationships and users before importing. This cannot be undone.");
        clearWarning.getStyle()
                .set("color", "var(--lumo-error-color)")
                .set("font-size", "var(--lumo-font-size-s)");
        clearWarning.setVisible(false);
        clearCheckbox.addValueChangeListener(e -> clearWarning.setVisible(e.getValue()));

        Button runBtn = new Button("Import");
        runBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        runBtn.setEnabled(false);

        // Track uploaded bytes so we can pass them when the button is clicked
        final byte[][] uploadedBytes = {null};
        final String[] uploadedName = {null};

        upload.addSucceededListener(event -> {
            try {
                InputStream is = buffer.getInputStream();
                uploadedBytes[0] = is.readAllBytes();
                uploadedName[0] = event.getFileName();
                runBtn.setEnabled(true);
            } catch (Exception e) {
                log.error("Failed to read uploaded file", e);
                notify("Failed to read file: " + e.getMessage(), true);
            }
        });

        upload.addFileRemovedListener(event -> {
            uploadedBytes[0] = null;
            uploadedName[0] = null;
            runBtn.setEnabled(false);
            importResultPanel.setVisible(false);
        });

        runBtn.addClickListener(e -> {
            if (uploadedBytes[0] == null) return;
            if (clearCheckbox.getValue()) {
                ConfirmDialog confirm = new ConfirmDialog(
                        "Delete all data?",
                        "This will permanently delete ALL data in the system before importing. This cannot be undone.",
                        "Delete and Import", ev -> runImportExcel(uploadedBytes[0], uploadedName[0], true, importResultPanel),
                        "Cancel", ev -> {});
                confirm.setConfirmButtonTheme("error primary");
                confirm.open();
            } else {
                runImportExcel(uploadedBytes[0], uploadedName[0], false, importResultPanel);
            }
        });

        layout.add(upload, clearCheckbox, clearWarning, runBtn, importResultPanel);
        return layout;
    }

    private void runImportExcel(byte[] bytes, String filename, boolean clear, VerticalLayout resultPanel) {
        try {
            ExcelImportResult result = excelImportClient.importExcel(bytes, filename, clear);
            showExcelImportResult(result, resultPanel);
            notify("Import completed!", false);
        } catch (Exception ex) {
            log.error("Excel import failed", ex);
            notify("Import failed: " + ex.getMessage(), true);
        }
    }

    private void showExcelImportResult(ExcelImportResult result, VerticalLayout panel) {
        panel.removeAll();
        panel.setVisible(true);
        panel.add(new H3("Import Result"));

        VerticalLayout stats = new VerticalLayout();
        stats.setSpacing(false);
        stats.setPadding(false);
        stats.add(stat("Entities imported",        String.valueOf(result.getEntitiesImported())));
        stats.add(stat("Attributes imported",      String.valueOf(result.getAttributesImported())));
        stats.add(stat("Database Models imported", String.valueOf(result.getDatabaseModelsImported())));
        stats.add(stat("Schemas imported",         String.valueOf(result.getSchemasImported())));
        stats.add(stat("Tables imported",          String.valueOf(result.getTablesImported())));
        stats.add(stat("Columns imported",         String.valueOf(result.getColumnsImported())));
        stats.add(stat("Relationships imported",   String.valueOf(result.getRelationshipsImported())));
        stats.add(stat("Users imported",           String.valueOf(result.getUsersImported())));
        stats.add(stat("Skipped (duplicates)",     String.valueOf(result.getSkipped())));
        panel.add(stats);

        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            panel.add(new H3("Warnings (" + result.getWarnings().size() + ")"));
            for (String warning : result.getWarnings()) {
                Paragraph p = new Paragraph("⚠ " + warning);
                p.getStyle().set("color", "var(--lumo-warning-text-color)");
                panel.add(p);
            }
        }
    }

    private VerticalLayout buildImportTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setMaxWidth("800px");
        layout.add(new Paragraph(
                "Connect to any database via JDBC and import its schemas, tables, columns " +
                "and foreign-key relationships into the data dictionary."));
        layout.add(buildForm());
        layout.add(new Hr());
        layout.add(resultPanel);
        return layout;
    }

    private FormLayout buildForm() {
        // Connection fields
        TextField modelName    = new TextField("Model Name");
        modelName.setPlaceholder("e.g. My Production DB");
        modelName.setWidthFull();

        TextField jdbcUrl      = new TextField("JDBC URL");
        jdbcUrl.setPlaceholder("e.g. jdbc:postgresql://localhost:5432/mydb");
        jdbcUrl.setWidthFull();

        TextField username     = new TextField("Username");
        PasswordField password = new PasswordField("Password");

        // Filter fields
        TextField schemaPattern = new TextField("Schema Pattern");
        schemaPattern.setPlaceholder("Leave empty for all schemas (supports % wildcard)");

        TextField tablePattern  = new TextField("Table Pattern");
        tablePattern.setPlaceholder("Leave empty for all tables (supports % wildcard)");

        // Options
        Checkbox includeViews = new Checkbox("Include views");
        Checkbox overwrite    = new Checkbox("Overwrite existing model with same name");

        // Quick-fill buttons for common DBs
        Button fillH2       = quickFillButton("H2 (local)",
                "jdbc:h2:mem:dbdadi", "sa", "");
        Button fillPostgres = quickFillButton("PostgreSQL (local)",
                "jdbc:postgresql://localhost:5432/mydb", "postgres", "");
        Button fillMysql    = quickFillButton("MySQL (local)",
                "jdbc:mysql://localhost:3306/mydb", "root", "");

        HorizontalLayout quickFill = new HorizontalLayout(
                new Span("Quick fill: "), fillH2, fillPostgres, fillMysql);
        quickFill.setAlignItems(Alignment.CENTER);

        fillH2.addClickListener(e -> {
            jdbcUrl.setValue("jdbc:h2:mem:dbdadi");
            username.setValue("sa");
            password.setValue("");
            modelName.setValue("H2 Local");
        });
        fillPostgres.addClickListener(e -> {
            jdbcUrl.setValue("jdbc:postgresql://localhost:5432/mydb");
            username.setValue("postgres");
            password.setValue("");
            modelName.setValue("PostgreSQL Local");
        });
        fillMysql.addClickListener(e -> {
            jdbcUrl.setValue("jdbc:mysql://localhost:3306/mydb");
            username.setValue("root");
            password.setValue("");
            modelName.setValue("MySQL Local");
        });

        // Run button
        Button runBtn = new Button("Run Import");
        runBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        runBtn.addClickListener(e -> runImport(
                modelName.getValue(),
                jdbcUrl.getValue(),
                username.getValue(),
                password.getValue(),
                schemaPattern.getValue(),
                tablePattern.getValue(),
                includeViews.getValue(),
                overwrite.getValue()
        ));

        FormLayout form = new FormLayout(
                modelName, jdbcUrl,
                username, password,
                schemaPattern, tablePattern,
                includeViews, overwrite
        );
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(modelName, 2);
        form.setColspan(jdbcUrl, 2);
        form.setColspan(schemaPattern, 2);
        form.setColspan(tablePattern, 2);

        VerticalLayout wrapper = new VerticalLayout(quickFill, form, runBtn);
        wrapper.setPadding(false);
        return new FormLayout(wrapper);
    }

    private void runImport(String modelName, String jdbcUrl, String username, String password,
                           String schemaPattern, String tablePattern,
                           boolean includeViews, boolean overwrite) {

        if (modelName.isBlank() || jdbcUrl.isBlank()) {
            notify("Model Name and JDBC URL are required.", true);
            return;
        }

        JdbcImportRequest request = JdbcImportRequest.builder()
                .modelName(modelName)
                .jdbcUrl(jdbcUrl)
                .username(username.isBlank() ? null : username)
                .password(password.isBlank() ? null : password)
                .schemaPattern(schemaPattern.isBlank() ? null : schemaPattern)
                .tablePattern(tablePattern.isBlank() ? null : tablePattern)
                .includeViews(includeViews)
                .overwrite(overwrite)
                .build();

        try {
            JdbcImportResult result = importClient.importFromJdbc(request);
            showResult(result);
            notify("Import completed successfully!", false);
        } catch (Exception ex) {
            log.error("Import failed", ex);
            notify("Import failed: " + ex.getMessage(), true);
        }
    }

    private void showResult(JdbcImportResult result) {
        resultPanel.removeAll();
        resultPanel.setVisible(true);

        resultPanel.add(new H3("Import Result"));

        VerticalLayout stats = new VerticalLayout();
        stats.setSpacing(false);
        stats.setPadding(false);
        stats.add(stat("Database Model", result.getDatabaseModelName()
                + " (id: " + result.getDatabaseModelId() + ")"));
        stats.add(stat("Schemas imported",    String.valueOf(result.getSchemasImported())));
        stats.add(stat("Tables imported",     String.valueOf(result.getTablesImported())));
        stats.add(stat("Columns imported",    String.valueOf(result.getColumnsImported())));
        stats.add(stat("Relationships imported", String.valueOf(result.getRelationshipsImported())));
        resultPanel.add(stats);

        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            resultPanel.add(new H3("Warnings (" + result.getWarnings().size() + ")"));
            for (String warning : result.getWarnings()) {
                Paragraph p = new Paragraph("⚠ " + warning);
                p.getStyle().set("color", "var(--lumo-warning-text-color)");
                resultPanel.add(p);
            }
        }
    }

    private HorizontalLayout stat(String label, String value) {
        Span labelSpan = new Span(label + ": ");
        labelSpan.getStyle().set("font-weight", "bold").set("min-width", "220px");
        Span valueSpan = new Span(value);
        HorizontalLayout row = new HorizontalLayout(labelSpan, valueSpan);
        row.setSpacing(false);
        return row;
    }

    private Button quickFillButton(String label, String url, String user, String pass) {
        Button btn = new Button(label);
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        return btn;
    }

    private void notify(String message, boolean error) {
        Notification n = Notification.show(message, 4000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}

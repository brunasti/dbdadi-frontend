package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import it.brunasti.dbdadi.frontend.client.JdbcImportClient;
import it.brunasti.dbdadi.frontend.dto.JdbcImportRequest;
import it.brunasti.dbdadi.frontend.dto.JdbcImportResult;
import lombok.extern.slf4j.Slf4j;

@Route(value = "admin", layout = MainLayout.class)
@PageTitle("DBDaDi | Admin")
@AnonymousAllowed
@Slf4j
public class AdminView extends VerticalLayout {

    private final JdbcImportClient importClient;

    // Result panel (hidden until import runs)
    private final VerticalLayout resultPanel = new VerticalLayout();

    public AdminView(JdbcImportClient importClient) {
        this.importClient = importClient;
        setSpacing(true);
        setPadding(true);
        setMaxWidth("800px");

        add(new H3("Import from JDBC"));
        add(new Paragraph(
                "Connect to any database via JDBC and import its schemas, tables, columns " +
                "and foreign-key relationships into the data dictionary."));

        add(buildForm());
        add(new Hr());
        resultPanel.setVisible(false);
        add(resultPanel);
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

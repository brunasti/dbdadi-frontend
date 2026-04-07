package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import it.brunasti.dbdadi.frontend.client.DatabaseModelClient;
import it.brunasti.dbdadi.frontend.client.ColumnDefinitionClient;
import it.brunasti.dbdadi.frontend.client.RelationshipDefinitionClient;
import it.brunasti.dbdadi.frontend.client.TableDefinitionClient;
import lombok.extern.slf4j.Slf4j;

@Route(value = "dashboard", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | dbdadi")
@AnonymousAllowed
@Slf4j
public class DashboardView extends VerticalLayout {

    public DashboardView(DatabaseModelClient dbModelClient,
                         TableDefinitionClient tableClient,
                         ColumnDefinitionClient columnClient,
                         RelationshipDefinitionClient relationshipClient) {
        setSpacing(true);
        setPadding(true);

        add(new H2("Welcome to dbdadi"));
        add(new Paragraph("DB Data Dictionary — manage database models, tables, columns and relationships."));

        try {
            int models = dbModelClient.findAll().size();
            int tables = tableClient.findAll().size();
            int columns = columnClient.findAll().size();
            int relationships = relationshipClient.findAll().size();

            add(new Paragraph("Database models: " + models));
            add(new Paragraph("Tables: " + tables));
            add(new Paragraph("Columns: " + columns));
            add(new Paragraph("Relationships: " + relationships));
        } catch (Exception e) {
            log.warn("Could not load stats from backend: {}", e.getMessage());
            add(new Paragraph("Backend not reachable. Start the dbdadi API on port 8080."));
        }
    }
}

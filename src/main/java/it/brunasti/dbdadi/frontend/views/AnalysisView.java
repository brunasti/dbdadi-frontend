package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import it.brunasti.dbdadi.frontend.client.AnalysisClient;
import it.brunasti.dbdadi.frontend.client.DomainDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Route(value = "analysis", layout = MainLayout.class)
@PageTitle("DBDaDi | Analysis")
@PermitAll
@Slf4j
public class AnalysisView extends VerticalLayout {

    private final AnalysisClient client;
    private final DomainDefinitionClient domainClient;

    private final Grid<AnalysisEntitySuggestion> entityGrid =
            new Grid<>(AnalysisEntitySuggestion.class, false);
    private final Grid<AnalysisAttributeSuggestion> attributeGrid =
            new Grid<>(AnalysisAttributeSuggestion.class, false);
    private final ComboBox<DomainDefinitionDto> domainBox = new ComboBox<>("Assign to Domain (optional)");
    private final Span statsLabel = new Span();
    private final Button applyBtn = new Button("Apply Selected");

    private AnalysisResult lastResult;

    public AnalysisView(AnalysisClient client, DomainDefinitionClient domainClient) {
        this.client = client;
        this.domainClient = domainClient;
        setSizeFull();
        setPadding(true);

        configureEntityGrid();
        configureAttributeGrid();
        configureDomainBox();

        Button runBtn = new Button("Run Analysis");
        runBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        runBtn.addClickListener(e -> runAnalysis());

        applyBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        applyBtn.setEnabled(false);
        applyBtn.addClickListener(e -> applySelected());

        HorizontalLayout toolbar = new HorizontalLayout(runBtn, domainBox, applyBtn);
        toolbar.setAlignItems(Alignment.END);

        add(toolbar, statsLabel,
            new Hr(), new H3("Entity Suggestions"), entityGrid,
            new Hr(), new H3("Attribute Suggestions"), attributeGrid);
    }

    private void configureDomainBox() {
        domainBox.setItemLabelGenerator(DomainDefinitionDto::getName);
        domainBox.setClearButtonVisible(true);
        domainBox.setWidth("260px");
        try {
            domainBox.setItems(domainClient.findAll());
        } catch (Exception e) {
            log.warn("Could not load domains");
        }
    }

    private void configureEntityGrid() {
        entityGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        entityGrid.setAllRowsVisible(true);

        entityGrid.addColumn(AnalysisEntitySuggestion::getSuggestedName)
                .setHeader("Suggested Entity Name").setWidth("220px").setFlexGrow(0).setSortable(true);

        entityGrid.addComponentColumn(item -> {
            Span badge = new Span(item.getExistingEntityId() != null ? "Existing" : "New");
            badge.getElement().getThemeList().add(
                    item.getExistingEntityId() != null ? "badge contrast" : "badge success");
            return badge;
        }).setHeader("Status").setWidth("110px").setFlexGrow(0);

        entityGrid.addColumn(item -> String.join("  |  ", item.getTableLabels()))
                .setHeader("Matching Tables").setFlexGrow(1);
    }

    private void configureAttributeGrid() {
        attributeGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        attributeGrid.setAllRowsVisible(true);

        attributeGrid.addColumn(AnalysisAttributeSuggestion::getEntityName)
                .setHeader("Entity").setWidth("200px").setFlexGrow(0).setSortable(true);

        attributeGrid.addColumn(AnalysisAttributeSuggestion::getSuggestedName)
                .setHeader("Suggested Attribute Name").setWidth("220px").setFlexGrow(0).setSortable(true);

        attributeGrid.addComponentColumn(item -> {
            Span badge = new Span(item.getExistingAttributeId() != null ? "Existing" : "New");
            badge.getElement().getThemeList().add(
                    item.getExistingAttributeId() != null ? "badge contrast" : "badge success");
            return badge;
        }).setHeader("Status").setWidth("110px").setFlexGrow(0);

        attributeGrid.addColumn(item -> String.join("  |  ", item.getColumnLabels()))
                .setHeader("Matching Columns").setFlexGrow(1);
    }

    private void runAnalysis() {
        try {
            lastResult = client.run();
            List<AnalysisEntitySuggestion> entities =
                    lastResult.getEntitySuggestions() != null ? lastResult.getEntitySuggestions() : List.of();
            List<AnalysisAttributeSuggestion> attributes =
                    lastResult.getAttributeSuggestions() != null ? lastResult.getAttributeSuggestions() : List.of();

            entityGrid.setItems(entities);
            attributeGrid.setItems(attributes);

            // Pre-select all rows
            entityGrid.asMultiSelect().setValue(new java.util.HashSet<>(entities));
            attributeGrid.asMultiSelect().setValue(new java.util.HashSet<>(attributes));

            statsLabel.setText("Analysed " + lastResult.getTablesAnalyzed() + " tables and "
                    + lastResult.getColumnsAnalyzed() + " columns — found "
                    + entities.size() + " entity suggestion(s) and "
                    + attributes.size() + " attribute suggestion(s).");

            applyBtn.setEnabled(!entities.isEmpty() || !attributes.isEmpty());

            if (entities.isEmpty() && attributes.isEmpty()) {
                notify("No new matches found. All tables may already be linked.", false);
            }
        } catch (Exception e) {
            log.error("Analysis failed", e);
            notify("Analysis failed: " + e.getMessage(), true);
        }
    }

    private void applySelected() {
        List<AnalysisEntitySuggestion> selectedEntities =
                new ArrayList<>(entityGrid.asMultiSelect().getSelectedItems());
        List<AnalysisAttributeSuggestion> selectedAttributes =
                new ArrayList<>(attributeGrid.asMultiSelect().getSelectedItems());

        if (selectedEntities.isEmpty() && selectedAttributes.isEmpty()) {
            notify("Nothing selected — tick the checkboxes in the grids first.", false);
            return;
        }

        try {
            DomainDefinitionDto domain = domainBox.getValue();
            AnalysisApplyRequest req = AnalysisApplyRequest.builder()
                    .entities(selectedEntities)
                    .attributes(selectedAttributes)
                    .domainId(domain != null ? domain.getId() : null)
                    .build();

            AnalysisApplyResult result = client.apply(req);

            String msg = "Done — entities: " + result.getEntitiesCreated() + " created, "
                    + result.getEntitiesReused() + " reused, "
                    + result.getTablesLinked() + " tables linked; "
                    + "attributes: " + result.getAttributesCreated() + " created, "
                    + result.getAttributesReused() + " reused, "
                    + result.getColumnsLinked() + " columns linked.";
            notify(msg, false);

            // Re-run to refresh remaining unlinked items
            runAnalysis();
        } catch (Exception e) {
            log.error("Apply failed", e);
            notify("Apply failed: " + e.getMessage(), true);
        }
    }

    private void notify(String message, boolean error) {
        Notification n = Notification.show(message, 5000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}

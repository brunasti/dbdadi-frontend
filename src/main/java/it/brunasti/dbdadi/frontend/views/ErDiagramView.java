package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import it.brunasti.dbdadi.frontend.client.DomainDefinitionClient;
import it.brunasti.dbdadi.frontend.client.ErDiagramClient;
import it.brunasti.dbdadi.frontend.dto.DomainDefinitionDto;
import lombok.extern.slf4j.Slf4j;

@Route(value = "er-diagram", layout = MainLayout.class)
@PageTitle("DBDaDi | ER Diagram")
@PermitAll
@Slf4j
public class ErDiagramView extends VerticalLayout {

    private final ErDiagramClient client;
    private final ComboBox<DomainDefinitionDto> domainBox = new ComboBox<>("Domain (leave blank for all)");
    private final TextArea outputArea = new TextArea("PlantUML");

    public ErDiagramView(ErDiagramClient client, DomainDefinitionClient domainClient) {
        this.client = client;
        setSizeFull();
        setPadding(true);

        domainBox.setItemLabelGenerator(DomainDefinitionDto::getName);
        domainBox.setClearButtonVisible(true);
        domainBox.setWidth("300px");
        try {
            domainBox.setItems(domainClient.findAll());
        } catch (Exception e) {
            log.warn("Could not load domains");
        }

        Button generateBtn = new Button("Generate", e -> generate());
        generateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button copyBtn = new Button("Copy to Clipboard", e -> {
            String text = outputArea.getValue();
            if (!text.isBlank()) {
                UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0)", text);
                Notification.show("Copied!", 2000, Notification.Position.BOTTOM_END);
            }
        });
        copyBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        Button renderBtn = new Button("View Rendering", e -> {
            if (outputArea.getValue().isBlank()) {
                Notification n = Notification.show("Generate a diagram first", 2000, Notification.Position.BOTTOM_END);
                n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                return;
            }
            try {
                DomainDefinitionDto selected = domainBox.getValue();
                String svg = client.generateSvg(selected != null ? selected.getId() : null);
                openSvgDialog("ER Diagram", svg);
            } catch (Exception ex) {
                log.error("SVG rendering failed", ex);
                Notification n = Notification.show("Rendering failed: " + ex.getMessage(), 4000, Notification.Position.BOTTOM_END);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        renderBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        HorizontalLayout toolbar = new HorizontalLayout(domainBox, generateBtn, copyBtn, renderBtn);
        toolbar.setAlignItems(Alignment.END);

        outputArea.setSizeFull();
        outputArea.setReadOnly(true);
        outputArea.getStyle().set("font-family", "monospace").set("font-size", "var(--lumo-font-size-s)");

        add(toolbar, outputArea);
        setFlexGrow(1, outputArea);
    }

    private void openSvgDialog(String title, String svgContent) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(title);
        dialog.setWidth("900px");
        dialog.setMaxHeight("85vh");

        Div svgWrapper = new Div();
        svgWrapper.getElement().setProperty("innerHTML", svgContent);
        svgWrapper.setWidthFull();
        svgWrapper.getStyle().set("overflow", "auto").set("display", "block");

        VerticalLayout content = new VerticalLayout(svgWrapper);
        content.setSizeFull();
        content.setPadding(false);
        dialog.add(content);
        dialog.getFooter().add(new Button("Close", e -> dialog.close()));
        dialog.open();
    }

    private void generate() {
        try {
            DomainDefinitionDto selected = domainBox.getValue();
            String result = client.generate(selected != null ? selected.getId() : null);
            outputArea.setValue(result != null ? result : "");
        } catch (Exception e) {
            log.error("ER diagram generation failed", e);
            Notification n = Notification.show("Generation failed: " + e.getMessage(), 4000, Notification.Position.BOTTOM_END);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}

package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
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
import jakarta.annotation.security.PermitAll;
import it.brunasti.dbdadi.frontend.security.SecurityUtils;
import it.brunasti.dbdadi.frontend.client.RelationshipDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.RelationshipDefinitionDto;
import lombok.extern.slf4j.Slf4j;

@Route(value = "relationships/:relationshipId", layout = MainLayout.class)
@PageTitle("DBDaDi | Relationship")
@PermitAll
@Slf4j
public class RelationshipDefinitionDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final RelationshipDefinitionClient client;
    private RelationshipDefinitionDto relationship;

    private final TextField nameField = new TextField("Name");
    private final TextField typeField = new TextField("Type");
    private final TextArea descriptionField = new TextArea("Description");

    public RelationshipDefinitionDetailView(RelationshipDefinitionClient client) {
        this.client = client;
        setWidthFull();
        setPadding(true);
        configureFields();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("relationshipId").map(Long::valueOf).ifPresent(id -> {
            try {
                relationship = client.findById(id);
                populateFields();
            } catch (Exception e) {
                log.error("Could not load relationship {}", id, e);
                notify("Could not load relationship", true);
            }
        });
    }

    private void configureFields() {
        nameField.setReadOnly(true);
        typeField.setReadOnly(true);
        descriptionField.setReadOnly(true);
        descriptionField.setWidthFull();
    }

    private VerticalLayout linkField(String label, String text, String route) {
        NativeLabel caption = new NativeLabel(label);
        caption.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "500");
        Button link = new Button(text);
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        link.getStyle().set("padding", "0").set("font-size", "var(--lumo-font-size-m)");
        if (route != null) {
            link.addClickListener(e -> UI.getCurrent().navigate(route));
        }
        VerticalLayout wrapper = new VerticalLayout(caption, link);
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-20pct)")
                          .set("padding-bottom", "var(--lumo-space-xs)");
        return wrapper;
    }

    private void populateFields() {
        removeAll();

        HorizontalLayout breadcrumb = new HorizontalLayout();
        Button backToList = new Button("← Relationships");
        backToList.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToList.addClickListener(e -> UI.getCurrent().navigate(RelationshipDefinitionView.class));

        Button fromTableBtn = new Button(relationship.getFromTableName());
        fromTableBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        fromTableBtn.getStyle().set("padding", "0");
        fromTableBtn.addClickListener(e -> UI.getCurrent().navigate("tables/" + relationship.getFromTableId()));

        Button fromColumnBtn = new Button(relationship.getFromColumnName());
        fromColumnBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        fromColumnBtn.getStyle().set("padding", "0");
        if (relationship.getFromColumnId() != null) {
            fromColumnBtn.addClickListener(e -> UI.getCurrent().navigate("columns/" + relationship.getFromColumnId()));
        }

        Button toTableBtn = new Button(relationship.getToTableName());
        toTableBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        toTableBtn.getStyle().set("padding", "0");
        toTableBtn.addClickListener(e -> UI.getCurrent().navigate("tables/" + relationship.getToTableId()));

        Button toColumnBtn = new Button(relationship.getToColumnName());
        toColumnBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        toColumnBtn.getStyle().set("padding", "0");
        if (relationship.getToColumnId() != null) {
            toColumnBtn.addClickListener(e -> UI.getCurrent().navigate("columns/" + relationship.getToColumnId()));
        }

        breadcrumb.add(backToList, new Span(" / "),
                new Span("Relationship: " + relationship.getName()),
                new Span("  ("),
                fromTableBtn, new Span("."), fromColumnBtn,
                new Span(" → "),
                toTableBtn, new Span("."), toColumnBtn,
                new Span(")"));
        breadcrumb.setAlignItems(Alignment.CENTER);
        add(breadcrumb);

        nameField.setValue(relationship.getName() != null ? relationship.getName() : "");
        typeField.setValue(relationship.getType() != null ? relationship.getType().name() : "");
        descriptionField.setValue(relationship.getDescription() != null ? relationship.getDescription() : "");

        VerticalLayout fromTableLink = linkField("From Table",
                relationship.getFromTableName() != null ? relationship.getFromTableName() : "",
                "tables/" + relationship.getFromTableId());
        VerticalLayout fromColumnLink = linkField("From Column",
                relationship.getFromColumnName() != null ? relationship.getFromColumnName() : "",
                relationship.getFromColumnId() != null ? "columns/" + relationship.getFromColumnId() : null);
        VerticalLayout toTableLink = linkField("To Table",
                relationship.getToTableName() != null ? relationship.getToTableName() : "",
                "tables/" + relationship.getToTableId());
        VerticalLayout toColumnLink = linkField("To Column",
                relationship.getToColumnName() != null ? relationship.getToColumnName() : "",
                relationship.getToColumnId() != null ? "columns/" + relationship.getToColumnId() : null);

        FormLayout form = new FormLayout(nameField, typeField, fromTableLink, fromColumnLink,
                toTableLink, toColumnLink, descriptionField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(descriptionField, 2);

        Button deleteBtn = new Button("Delete", e -> confirmDelete());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteBtn.setVisible(SecurityUtils.canEdit());

        add(form, new HorizontalLayout(deleteBtn));
    }

    private void confirmDelete() {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete relationship \"" + relationship.getName() + "\"?",
                "This action cannot be undone.",
                "Delete", e -> {
                    try {
                        client.delete(relationship.getId());
                        UI.getCurrent().navigate(RelationshipDefinitionView.class);
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

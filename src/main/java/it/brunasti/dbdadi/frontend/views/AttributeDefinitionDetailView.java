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
import it.brunasti.dbdadi.frontend.client.AttributeDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.AttributeDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.ColumnDefinitionDto;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Route(value = "attributes/:attributeId", layout = MainLayout.class)
@PageTitle("DBDaDi | Attribute")
@AnonymousAllowed
@Slf4j
public class AttributeDefinitionDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final AttributeDefinitionClient client;
    private AttributeDefinitionDto attribute;

    private final TextField nameField = new TextField("Name");
    private final TextArea descriptionField = new TextArea("Description");
    private final Grid<ColumnDefinitionDto> columnsGrid = new Grid<>(ColumnDefinitionDto.class, false);

    public AttributeDefinitionDetailView(AttributeDefinitionClient client) {
        this.client = client;
        setWidthFull();
        setPadding(true);
        configureFields();
        configureGrid();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("attributeId").map(Long::valueOf).ifPresent(id -> {
            try {
                attribute = client.findById(id);
                populateFields();
                columnsGrid.setItems(client.findColumns(id));
            } catch (Exception e) {
                log.error("Could not load attribute {}", id, e);
                notify("Could not load attribute", true);
            }
        });
    }

    private void configureFields() {
        nameField.setReadOnly(true);
        descriptionField.setReadOnly(true);
        descriptionField.setWidthFull();
    }

    private void configureGrid() {
        columnsGrid.addColumn(ColumnDefinitionDto::getId).setHeader("ID").setWidth("70px").setFlexGrow(0).setSortable(true);
        columnsGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("columns/" + item.getId()));
            return btn;
        }).setHeader("Column Name").setComparator(Comparator.comparing(ColumnDefinitionDto::getName));
        columnsGrid.addColumn(ColumnDefinitionDto::getDataType).setHeader("Data Type").setSortable(true);
        columnsGrid.addColumn(ColumnDefinitionDto::getTableName).setHeader("Table").setSortable(true);
        columnsGrid.addColumn(ColumnDefinitionDto::getSchemaName).setHeader("Schema").setSortable(true);
        columnsGrid.addColumn(ColumnDefinitionDto::getDatabaseModelName).setHeader("Database Model").setSortable(true);
        columnsGrid.addColumn(ColumnDefinitionDto::getDescription).setHeader("Description").setSortable(true);
        columnsGrid.setAllRowsVisible(true);
    }

    private void populateFields() {
        removeAll();

        HorizontalLayout breadcrumb = new HorizontalLayout();
        Button backToList = new Button("← Attributes");
        backToList.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToList.addClickListener(e -> UI.getCurrent().navigate(AttributeDefinitionView.class));
        breadcrumb.add(backToList, new Span(" / "), new Span("Attribute: " + attribute.getName()));
        breadcrumb.setAlignItems(Alignment.CENTER);
        add(breadcrumb);

        nameField.setValue(attribute.getName() != null ? attribute.getName() : "");
        descriptionField.setValue(attribute.getDescription() != null ? attribute.getDescription() : "");

        FormLayout form = new FormLayout(nameField, descriptionField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(descriptionField, 2);

        Button editBtn = new Button("Edit", e -> openEditDialog());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button deleteBtn = new Button("Delete", e -> confirmDelete());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        add(form, new HorizontalLayout(editBtn, deleteBtn), new Hr(), new H3("Linked Columns"));
        add(columnsGrid);
    }

    private void openEditDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Attribute");
        dialog.setWidth("500px");

        TextField name = new TextField("Name");
        TextArea description = new TextArea("Description");
        name.setValue(attribute.getName() != null ? attribute.getName() : "");
        description.setValue(attribute.getDescription() != null ? attribute.getDescription() : "");

        FormLayout form = new FormLayout(name, description);
        form.setColspan(description, 2);

        Button save = new Button("Save", e -> {
            try {
                AttributeDefinitionDto dto = AttributeDefinitionDto.builder()
                        .name(name.getValue())
                        .description(description.getValue())
                        .build();
                attribute = client.update(attribute.getId(), dto);
                dialog.close();
                populateFields();
                columnsGrid.setItems(client.findColumns(attribute.getId()));
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
                "Delete attribute \"" + attribute.getName() + "\"?",
                "This action cannot be undone. Columns linked to this attribute will be unlinked.",
                "Delete", e -> {
                    try {
                        client.delete(attribute.getId());
                        UI.getCurrent().navigate(AttributeDefinitionView.class);
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

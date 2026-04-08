package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import it.brunasti.dbdadi.frontend.client.ColumnDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.ColumnDefinitionDto;
import lombok.extern.slf4j.Slf4j;

@Route(value = "columns/:columnId", layout = MainLayout.class)
@PageTitle("Column | dbdadi")
@AnonymousAllowed
@Slf4j
public class ColumnDefinitionDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final ColumnDefinitionClient client;
    private ColumnDefinitionDto column;

    private final TextField nameField = new TextField("Name");
    private final TextField dataTypeField = new TextField("Data Type");
    private final IntegerField lengthField = new IntegerField("Length");
    private final IntegerField precisionField = new IntegerField("Precision");
    private final IntegerField scaleField = new IntegerField("Scale");
    private final IntegerField ordinalField = new IntegerField("Ordinal Position");
    private final TextField defaultValueField = new TextField("Default Value");
    private final Checkbox nullableField = new Checkbox("Nullable");
    private final Checkbox primaryKeyField = new Checkbox("Primary Key");
    private final Checkbox uniqueField = new Checkbox("Unique");
    private final TextArea descriptionField = new TextArea("Description");

    public ColumnDefinitionDetailView(ColumnDefinitionClient client) {
        this.client = client;
        setWidthFull();
        setPadding(true);
        configureFields();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("columnId").map(Long::valueOf).ifPresent(id -> {
            try {
                column = client.findById(id);
                populateFields();
            } catch (Exception e) {
                log.error("Could not load column {}", id, e);
                notify("Could not load column", true);
            }
        });
    }

    private void configureFields() {
        nameField.setReadOnly(true);
        dataTypeField.setReadOnly(true);
        lengthField.setReadOnly(true);
        precisionField.setReadOnly(true);
        scaleField.setReadOnly(true);
        ordinalField.setReadOnly(true);
        defaultValueField.setReadOnly(true);
        nullableField.setReadOnly(true);
        primaryKeyField.setReadOnly(true);
        uniqueField.setReadOnly(true);
        descriptionField.setReadOnly(true);
        descriptionField.setWidthFull();
    }

    private void populateFields() {
        removeAll();

        HorizontalLayout breadcrumb = new HorizontalLayout();
        Button backToModels = new Button("← Database Models");
        backToModels.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToModels.addClickListener(e -> UI.getCurrent().navigate(DatabaseModelView.class));
        Button toModel = new Button(column.getDatabaseModelName());
        toModel.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        toModel.getStyle().set("padding", "0");
        toModel.addClickListener(e -> UI.getCurrent().navigate("database-models/" + column.getDatabaseModelId()));
        Button toSchema = new Button(column.getSchemaName());
        toSchema.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        toSchema.getStyle().set("padding", "0");
        toSchema.addClickListener(e -> UI.getCurrent().navigate("schemas/" + column.getSchemaId()));
        Button toTable = new Button(column.getTableName());
        toTable.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        toTable.getStyle().set("padding", "0");
        toTable.addClickListener(e -> UI.getCurrent().navigate("tables/" + column.getTableId()));
        breadcrumb.add(backToModels, new Span(" / "), toModel,
                new Span(" / "), toSchema,
                new Span(" / "), toTable,
                new Span(" / "), new Span("Column: " + column.getName()));
        breadcrumb.setAlignItems(Alignment.CENTER);
        add(breadcrumb);

        nameField.setValue(column.getName() != null ? column.getName() : "");
        dataTypeField.setValue(column.getDataType() != null ? column.getDataType() : "");
        lengthField.setValue(column.getLength());
        precisionField.setValue(column.getPrecision());
        scaleField.setValue(column.getScale());
        ordinalField.setValue(column.getOrdinalPosition());
        defaultValueField.setValue(column.getDefaultValue() != null ? column.getDefaultValue() : "");
        nullableField.setValue(column.isNullable());
        primaryKeyField.setValue(column.isPrimaryKey());
        uniqueField.setValue(column.isUnique());
        descriptionField.setValue(column.getDescription() != null ? column.getDescription() : "");

        FormLayout form = new FormLayout(nameField, dataTypeField, lengthField, precisionField,
                scaleField, ordinalField, defaultValueField, nullableField, primaryKeyField,
                uniqueField, descriptionField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
        form.setColspan(descriptionField, 3);

        Button editBtn = new Button("Edit", e -> openEditDialog());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button deleteBtn = new Button("Delete", e -> confirmDelete());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        add(form, new HorizontalLayout(editBtn, deleteBtn));
    }

    private void openEditDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Column");
        dialog.setWidth("600px");

        TextField name = new TextField("Column Name");
        TextField dataType = new TextField("Data Type");
        IntegerField length = new IntegerField("Length");
        IntegerField precision = new IntegerField("Precision");
        IntegerField scale = new IntegerField("Scale");
        IntegerField ordinal = new IntegerField("Ordinal Position");
        TextField defaultValue = new TextField("Default Value");
        Checkbox nullable = new Checkbox("Nullable");
        Checkbox primaryKey = new Checkbox("Primary Key");
        Checkbox unique = new Checkbox("Unique");
        TextArea description = new TextArea("Description");

        name.setValue(column.getName() != null ? column.getName() : "");
        dataType.setValue(column.getDataType() != null ? column.getDataType() : "");
        length.setValue(column.getLength());
        precision.setValue(column.getPrecision());
        scale.setValue(column.getScale());
        ordinal.setValue(column.getOrdinalPosition());
        defaultValue.setValue(column.getDefaultValue() != null ? column.getDefaultValue() : "");
        nullable.setValue(column.isNullable());
        primaryKey.setValue(column.isPrimaryKey());
        unique.setValue(column.isUnique());
        description.setValue(column.getDescription() != null ? column.getDescription() : "");

        FormLayout form = new FormLayout(name, dataType, length, precision, scale, ordinal,
                defaultValue, nullable, primaryKey, unique, description);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
        form.setColspan(description, 3);

        Button save = new Button("Save", e -> {
            try {
                ColumnDefinitionDto dto = ColumnDefinitionDto.builder()
                        .name(name.getValue()).description(description.getValue())
                        .dataType(dataType.getValue()).length(length.getValue())
                        .precision(precision.getValue()).scale(scale.getValue())
                        .ordinalPosition(ordinal.getValue()).defaultValue(defaultValue.getValue())
                        .nullable(nullable.getValue()).primaryKey(primaryKey.getValue())
                        .unique(unique.getValue()).tableId(column.getTableId()).build();
                column = client.update(column.getId(), dto);
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

    private void confirmDelete() {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete column \"" + column.getName() + "\"?",
                "This action cannot be undone.",
                "Delete", e -> {
                    try {
                        client.delete(column.getId());
                        UI.getCurrent().navigate("tables/" + column.getTableId());
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

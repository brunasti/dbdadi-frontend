package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
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
import jakarta.annotation.security.PermitAll;
import it.brunasti.dbdadi.frontend.security.SecurityUtils;
import it.brunasti.dbdadi.frontend.client.AttributeDefinitionClient;
import it.brunasti.dbdadi.frontend.client.DomainDefinitionClient;
import it.brunasti.dbdadi.frontend.client.EntityDefinitionClient;
import it.brunasti.dbdadi.frontend.dto.AttributeDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.DomainDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.EntityDefinitionDto;
import it.brunasti.dbdadi.frontend.dto.GenerateAttributesResult;
import it.brunasti.dbdadi.frontend.dto.MergeEntityRequest;
import it.brunasti.dbdadi.frontend.dto.MergeEntityResult;
import it.brunasti.dbdadi.frontend.dto.TableDefinitionDto;
import com.vaadin.flow.component.combobox.ComboBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "entities/:entityId", layout = MainLayout.class)
@PageTitle("DBDaDi | Entity")
@PermitAll
@Slf4j
public class EntityDefinitionDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final EntityDefinitionClient client;
    private final AttributeDefinitionClient attributeClient;
    private final DomainDefinitionClient domainClient;
    private EntityDefinitionDto entity;

    private final TextField nameField = new TextField("Name");
    private final TextArea descriptionField = new TextArea("Description");
    private final Grid<TableDefinitionDto> tablesGrid = new Grid<>(TableDefinitionDto.class, false);
    private final Grid<AttributeDefinitionDto> attributesGrid = new Grid<>(AttributeDefinitionDto.class, false);
    private final Grid<DomainDefinitionDto> domainsGrid = new Grid<>(DomainDefinitionDto.class, false);

    public EntityDefinitionDetailView(EntityDefinitionClient client, AttributeDefinitionClient attributeClient,
                                      DomainDefinitionClient domainClient) {
        this.client = client;
        this.attributeClient = attributeClient;
        this.domainClient = domainClient;
        setWidthFull();
        setPadding(true);
        configureFields();
        configureGrid();
        configureAttributesGrid();
        configureDomainsGrid();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("entityId").map(Long::valueOf).ifPresent(id -> {
            try {
                entity = client.findById(id);
                populateFields();
                tablesGrid.setItems(client.findTables(id));
                attributesGrid.setItems(attributeClient.findByEntity(id));
                domainsGrid.setItems(client.findDomains(id));
            } catch (Exception e) {
                log.error("Could not load entity {}", id, e);
                notify("Could not load entity", true);
            }
        });
    }

    private void configureFields() {
        nameField.setReadOnly(true);
        descriptionField.setReadOnly(true);
        descriptionField.setWidthFull();
    }

    private void configureGrid() {
        tablesGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("tables/" + item.getId()));
            return btn;
        }).setHeader("Table Name").setComparator(Comparator.comparing(TableDefinitionDto::getName));
        tablesGrid.addColumn(TableDefinitionDto::getSchemaName).setHeader("Schema").setSortable(true);
        tablesGrid.addColumn(TableDefinitionDto::getDatabaseModelName).setHeader("Database Model").setSortable(true);
        tablesGrid.addColumn(TableDefinitionDto::getDescription).setHeader("Description").setSortable(true);
        tablesGrid.setAllRowsVisible(true);
    }

    private void populateFields() {
        removeAll();

        HorizontalLayout breadcrumb = new HorizontalLayout();
        Button backToList = new Button("← Entities");
        backToList.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToList.addClickListener(e -> UI.getCurrent().navigate(EntityDefinitionView.class));
        breadcrumb.add(backToList, new Span(" / "), new Span("Entity: " + entity.getName()));
        breadcrumb.setAlignItems(Alignment.CENTER);
        add(breadcrumb);

        nameField.setValue(entity.getName() != null ? entity.getName() : "");
        descriptionField.setValue(entity.getDescription() != null ? entity.getDescription() : "");

        FormLayout form = new FormLayout(nameField, descriptionField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(descriptionField, 2);

        Button editBtn = new Button("Edit", e -> openEditDialog());
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editBtn.setVisible(SecurityUtils.canEdit());
        Button deleteBtn = new Button("Delete", e -> confirmDelete());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteBtn.setVisible(SecurityUtils.canEdit());
        Button mergeBtn = new Button("Merge into…", e -> openMergeDialog());
        mergeBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        mergeBtn.setVisible(SecurityUtils.canEdit());

        Button newAttributeBtn = new Button("New Attribute", e -> openEditAttributeDialog(null));
        newAttributeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        newAttributeBtn.setVisible(SecurityUtils.canEdit());

        Button generateAttributesBtn = new Button("Generate from Columns", e -> confirmGenerateAttributes());
        generateAttributesBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
        generateAttributesBtn.setVisible(SecurityUtils.canEdit());

        HorizontalLayout attributeActions = new HorizontalLayout(newAttributeBtn, generateAttributesBtn);

        add(form, new HorizontalLayout(editBtn, deleteBtn, mergeBtn),
            new Hr(), new H3("Linked Domains"), domainsGrid,
            new Hr(), new H3("Linked Attributes"), attributeActions, attributesGrid,
            new Hr(), new H3("Linked Tables"));
        add(tablesGrid);
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
        domainsGrid.setAllRowsVisible(true);
    }

    private void openEditDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Entity");
        dialog.setWidth("560px");

        TextField name = new TextField("Name");
        TextArea description = new TextArea("Description");
        name.setValue(entity.getName() != null ? entity.getName() : "");
        description.setValue(entity.getDescription() != null ? entity.getDescription() : "");

        MultiSelectComboBox<DomainDefinitionDto> domainsBox = new MultiSelectComboBox<>("Domains");
        domainsBox.setWidthFull();
        domainsBox.setItemLabelGenerator(DomainDefinitionDto::getName);
        try {
            domainsBox.setItems(domainClient.findAll());
            List<DomainDefinitionDto> current = client.findDomains(entity.getId());
            domainsBox.setValue(Set.copyOf(current));
        } catch (Exception e) {
            log.warn("Could not load domains for entity editor");
        }

        FormLayout form = new FormLayout(name, description, domainsBox);
        form.setColspan(description, 2);
        form.setColspan(domainsBox, 2);

        Button save = new Button("Save", e -> {
            try {
                EntityDefinitionDto dto = EntityDefinitionDto.builder()
                        .name(name.getValue())
                        .description(description.getValue())
                        .build();
                entity = client.update(entity.getId(), dto);
                List<Long> domainIds = domainsBox.getSelectedItems().stream()
                        .map(DomainDefinitionDto::getId)
                        .collect(Collectors.toList());
                client.setDomains(entity.getId(), domainIds);
                dialog.close();
                populateFields();
                tablesGrid.setItems(client.findTables(entity.getId()));
                attributesGrid.setItems(attributeClient.findByEntity(entity.getId()));
                domainsGrid.setItems(client.findDomains(entity.getId()));
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

    private void configureAttributesGrid() {
        attributesGrid.addComponentColumn(item -> {
            Button btn = new Button(item.getName());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            btn.getStyle().set("padding", "0").set("font-weight", "bold");
            btn.addClickListener(e -> UI.getCurrent().navigate("attributes/" + item.getId()));
            return btn;
        }).setHeader("Attribute Name").setComparator(Comparator.comparing(AttributeDefinitionDto::getName));
        attributesGrid.addColumn(AttributeDefinitionDto::getDescription).setHeader("Description").setSortable(true);
        if (SecurityUtils.canEdit()) {
            attributesGrid.addComponentColumn(item -> {
                Button edit = new Button("Edit", e -> openEditAttributeDialog(item));
                edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
                Button delete = new Button("Unlink", e -> unlinkAttribute(item));
                delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                return new HorizontalLayout(edit, delete);
            }).setHeader("Actions").setWidth("160px").setFlexGrow(0);
        }
        attributesGrid.setAllRowsVisible(true);
    }

    private void openEditAttributeDialog(AttributeDefinitionDto item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item == null ? "New Attribute" : "Edit Attribute");
        dialog.setWidth("500px");

        TextField name = new TextField("Name");
        TextArea description = new TextArea("Description");
        if (item != null) {
            name.setValue(item.getName() != null ? item.getName() : "");
            description.setValue(item.getDescription() != null ? item.getDescription() : "");
        }

        FormLayout form = new FormLayout(name, description);
        form.setColspan(description, 2);

        Button save = new Button("Save", e -> {
            try {
                AttributeDefinitionDto dto = AttributeDefinitionDto.builder()
                        .name(name.getValue())
                        .description(description.getValue())
                        .entityId(entity.getId())
                        .build();
                if (item == null) attributeClient.create(dto);
                else attributeClient.update(item.getId(), dto);
                dialog.close();
                attributesGrid.setItems(attributeClient.findByEntity(entity.getId()));
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

    private void unlinkAttribute(AttributeDefinitionDto item) {
        try {
            AttributeDefinitionDto dto = AttributeDefinitionDto.builder()
                    .name(item.getName())
                    .description(item.getDescription())
                    .entityId(null)
                    .build();
            attributeClient.update(item.getId(), dto);
            attributesGrid.setItems(attributeClient.findByEntity(entity.getId()));
            notify("Attribute unlinked", false);
        } catch (Exception ex) {
            notify("Unlink failed: " + ex.getMessage(), true);
        }
    }

    private void confirmGenerateAttributes() {
        ConfirmDialog confirm = new ConfirmDialog(
                "Generate attributes from columns?",
                "For every column in tables linked to \"" + entity.getName()
                        + "\" that has no attribute yet, an attribute will be created (or an existing one reused) "
                        + "and linked to both the column and this entity.",
                "Generate", e -> {
                    try {
                        GenerateAttributesResult result = client.generateAttributes(entity.getId());
                        attributesGrid.setItems(attributeClient.findByEntity(entity.getId()));
                        showGenerateResult(result);
                    } catch (Exception ex) {
                        notify("Generation failed: " + ex.getMessage(), true);
                    }
                },
                "Cancel", e -> {});
        confirm.open();
    }

    private void showGenerateResult(GenerateAttributesResult result) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Generate Attributes — done");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.add(line("Attributes created: " + result.getAttributesCreated()));
        content.add(line("Columns linked: " + result.getColumnsLinked()));
        content.add(line("Columns already linked (skipped): " + result.getColumnsAlreadyLinked()));

        if (result.getCreatedNames() != null && !result.getCreatedNames().isEmpty()) {
            TextArea names = new TextArea("New attribute names");
            names.setValue(String.join("\n", result.getCreatedNames()));
            names.setReadOnly(true);
            names.setWidthFull();
            names.setMinHeight("80px");
            content.add(names);
        }

        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            result.getWarnings().forEach(w -> {
                Span warn = new Span("⚠ " + w);
                warn.getStyle().set("color", "#e65100").set("font-size", "0.88em");
                content.add(warn);
            });
        }

        dialog.add(content);
        dialog.getFooter().add(new Button("Close", e -> dialog.close()));
        dialog.open();
    }

    private void confirmDelete() {
        ConfirmDialog confirm = new ConfirmDialog(
                "Delete entity \"" + entity.getName() + "\"?",
                "This action cannot be undone. Tables linked to this entity will be unlinked.",
                "Delete", e -> {
                    try {
                        client.delete(entity.getId());
                        UI.getCurrent().navigate(EntityDefinitionView.class);
                    } catch (Exception ex) {
                        notify("Delete failed: " + ex.getMessage(), true);
                    }
                },
                "Cancel", e -> {});
        confirm.setConfirmButtonTheme("error primary");
        confirm.open();
    }

    private void openMergeDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Merge Entity — " + entity.getName());
        dialog.setWidth("520px");

        Span sourceInfo = new Span("Source (will be deleted): " + entity.getName());
        sourceInfo.getStyle().set("font-weight", "bold").set("color", "#c62828");

        ComboBox<EntityDefinitionDto> targetBox = new ComboBox<>("Merge into");
        targetBox.setWidthFull();
        targetBox.setItemLabelGenerator(EntityDefinitionDto::getName);
        targetBox.setPlaceholder("Select target entity…");
        try {
            List<EntityDefinitionDto> all = client.findAll();
            targetBox.setItems(all.stream()
                    .filter(e -> !e.getId().equals(entity.getId()))
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.warn("Could not load entities for merge dialog");
        }

        Span helpText = new Span(
                "All attributes and table links from \"" + entity.getName()
                + "\" will be moved to the selected entity. "
                + "Domain memberships will be merged. "
                + "This entity will then be deleted.");
        helpText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.9em");

        VerticalLayout content = new VerticalLayout(sourceInfo, targetBox, helpText);
        content.setPadding(false);
        content.setSpacing(true);

        Button runBtn = new Button("Merge", e -> {
            EntityDefinitionDto target = targetBox.getValue();
            if (target == null) {
                notify("Please select a target entity", true);
                return;
            }
            dialog.close();
            confirmMerge(target);
        });
        runBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        dialog.add(content);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), runBtn);
        dialog.open();
    }

    private void confirmMerge(EntityDefinitionDto target) {
        ConfirmDialog confirm = new ConfirmDialog(
                "Confirm merge",
                "Merge \"" + entity.getName() + "\" into \"" + target.getName() + "\"? "
                + "This will delete \"" + entity.getName() + "\" permanently.",
                "Merge", e -> {
                    try {
                        MergeEntityResult result = client.merge(MergeEntityRequest.builder()
                                .sourceEntityId(entity.getId())
                                .targetEntityId(target.getId())
                                .build());
                        showMergeResult(result);
                    } catch (Exception ex) {
                        notify("Merge failed: " + ex.getMessage(), true);
                    }
                },
                "Cancel", e -> {});
        confirm.setConfirmButtonTheme("error primary");
        confirm.open();
    }

    private void showMergeResult(MergeEntityResult result) {
        Dialog resultDialog = new Dialog();
        resultDialog.setHeaderTitle("Merge complete — " + result.getSurvivingEntityName());
        resultDialog.setWidth("480px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);

        content.add(line("Attributes migrated: " + result.getAttributesMigrated()));
        content.add(line("Tables re-linked: " + result.getTablesMigrated()));
        content.add(line("Domain memberships merged: " + result.getDomainsMigrated()));

        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            result.getWarnings().forEach(w -> {
                Span warn = new Span("⚠ " + w);
                warn.getStyle().set("color", "#e65100").set("font-size", "0.88em");
                content.add(warn);
            });
        }

        Button goToTarget = new Button("Go to " + result.getSurvivingEntityName(), e -> {
            resultDialog.close();
            UI.getCurrent().navigate("entities/" + result.getSurvivingEntityId());
        });
        goToTarget.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        resultDialog.add(content);
        resultDialog.getFooter().add(goToTarget);
        resultDialog.open();
    }

    private Span line(String text) {
        Span s = new Span(text);
        s.getStyle().set("font-size", "0.95em").set("display", "block").set("padding", "2px 0");
        return s;
    }

    private void notify(String message, boolean error) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}

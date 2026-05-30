package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.VaadinServletRequest;
import it.brunasti.dbdadi.frontend.security.SecurityUtils;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

@PermitAll
public class MainLayout extends AppLayout {

    public MainLayout() {
        addToNavbar(createHeader());
        addToDrawer(createNavigation());
    }

    private HorizontalLayout createHeader() {
        DrawerToggle toggle = new DrawerToggle();

        Image logo = new Image("icons/dbdadi-icon.png", "dbdadi logo");
        logo.setHeight("36px");
        logo.getStyle().set("margin-right", "8px");

        H1 title = new H1("DBDaDi");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");

        String username = SecurityUtils.getCurrentUsername();
        Span userSpan = new Span(username != null ? username : "");
        userSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-right", "var(--lumo-space-m)");

        Button logoutBtn = new Button("Logout", VaadinIcon.SIGN_OUT.create(), e -> {
            new SecurityContextLogoutHandler().logout(
                    VaadinServletRequest.getCurrent().getHttpServletRequest(), null,
                    SecurityContextHolder.getContext().getAuthentication());
            getUI().ifPresent(ui -> ui.getPage().setLocation("/login"));
        });
        logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        HorizontalLayout header = new HorizontalLayout(toggle, logo, title);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");

        HorizontalLayout right = new HorizontalLayout(userSpan, logoutBtn);
        right.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        right.getStyle().set("margin-left", "auto");
        header.add(right);

        return header;
    }

    private Component createNavigation() {
        SideNav dashboardNav = new SideNav();
        dashboardNav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));
        dashboardNav.addItem(new SideNavItem("Analysis", AnalysisView.class, VaadinIcon.MAGIC.create()));

        SideNav physicalNav = new SideNav();
        physicalNav.addItem(new SideNavItem("Database Models", DatabaseModelView.class, VaadinIcon.DATABASE.create()));
        physicalNav.addItem(new SideNavItem("Schemas", SchemaDefinitionView.class, VaadinIcon.BOOK.create()));
        physicalNav.addItem(new SideNavItem("Tables", TableDefinitionView.class, VaadinIcon.TABLE.create()));
        physicalNav.addItem(new SideNavItem("Columns", ColumnDefinitionView.class, VaadinIcon.RECORDS.create()));
        physicalNav.addItem(new SideNavItem("Relationships", RelationshipDefinitionView.class, VaadinIcon.CONNECT.create()));

        SideNav logicalNav = new SideNav();
        logicalNav.addItem(new SideNavItem("Domains", DomainDefinitionView.class, VaadinIcon.CLUSTER.create()));
        logicalNav.addItem(new SideNavItem("Entities", EntityDefinitionView.class, VaadinIcon.CUBES.create()));
        logicalNav.addItem(new SideNavItem("Attributes", AttributeDefinitionView.class, VaadinIcon.TAG.create()));

        VerticalLayout wrapper = new VerticalLayout(dashboardNav, new Hr(), physicalNav, new Hr(), logicalNav);
        wrapper.setPadding(false);
        wrapper.setSpacing(false);

        if (SecurityUtils.canImportExport() || SecurityUtils.isAdmin()) {
            SideNav adminNav = new SideNav();
            if (SecurityUtils.canImportExport()) {
                adminNav.addItem(new SideNavItem("Admin", AdminView.class, VaadinIcon.COGS.create()));
            }
            if (SecurityUtils.isAdmin()) {
                adminNav.addItem(new SideNavItem("Users", UserManagementView.class, VaadinIcon.USERS.create()));
            }
            wrapper.add(new Hr(), adminNav);
        }

        return wrapper;
    }
}

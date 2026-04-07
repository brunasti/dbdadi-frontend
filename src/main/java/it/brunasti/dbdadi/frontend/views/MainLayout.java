package it.brunasti.dbdadi.frontend.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {

    public MainLayout() {
        addToNavbar(createHeader());
        addToDrawer(createNavigation());
    }

    private HorizontalLayout createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        H1 title = new H1("dbdadi");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");

        HorizontalLayout header = new HorizontalLayout(toggle, title);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");
        return header;
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Database Models", DatabaseModelView.class, VaadinIcon.DATABASE.create()));
        nav.addItem(new SideNavItem("Tables", TableDefinitionView.class, VaadinIcon.TABLE.create()));
        nav.addItem(new SideNavItem("Columns", ColumnDefinitionView.class, VaadinIcon.RECORDS.create()));
        nav.addItem(new SideNavItem("Relationships", RelationshipDefinitionView.class, VaadinIcon.CONNECT.create()));

        return nav;
    }
}

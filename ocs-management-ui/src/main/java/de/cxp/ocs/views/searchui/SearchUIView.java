package de.cxp.ocs.views.searchui;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import de.cxp.ocs.model.index.Category;
import de.cxp.ocs.model.result.ResultHit;
import de.cxp.ocs.model.result.SearchResult;
import de.cxp.ocs.service.SearchUIService;

@PageTitle("Search-UI")
@Route(value = "search-ui")
@RouteAlias(value = "")
@PermitAll
public class SearchUIView extends Div implements AfterNavigationObserver {

	HorizontalLayout	header			= new HorizontalLayout();

	HorizontalLayout	searchBar		= new HorizontalLayout();
	TextField			searchBox		= new TextField();
	Button				searchButton	= new Button("Search");

	Grid<ResultHit> grid = new Grid<>();

	SearchUIService searchService = new SearchUIService();

	public SearchUIView() {
		addClassName("search-ui-view");
		setSizeFull();
		grid.setHeight("90%");
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
		grid.addComponentColumn(resultHit -> createResultHit(resultHit));

		header.setHeight("5%");

		searchBar.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		
		searchBox.setWidth("50%");
		searchBox.setPlaceholder("Search ...");
		searchBar.add(searchBox);

		searchButton.addClickListener( new ComponentEventListener<ClickEvent<Button>>() {	
			@Override
			public void onComponentEvent(ClickEvent<Button> event) {
				handleSearch(searchBox.getValue(), null);
			}
		});
		searchButton.addClickShortcut(Key.ENTER);
		searchBar.add(searchButton);
		
		searchBar.setHeight("5%");

		add(searchBar);
		add(header);
		add(grid);
	}


	private void handleSearch(String query, Map<String, String> filter) {
		SearchResult hits = searchService.search(query, filter);
		List<ResultHit> resultHits = hits.getSlices()
				.stream()
				.map(s -> s.getHits())
				.flatMap(l -> l.stream())
				.collect(Collectors.toList());
		grid.setItems(resultHits);
	}

	private HorizontalLayout createResultHit(ResultHit resultHit) {
		HorizontalLayout card = new HorizontalLayout();
		card.addClassName("card");
		card.setSpacing(false);
		card.getThemeList().add("spacing-s");

		VerticalLayout description = new VerticalLayout();
		description.addClassName("description");
		description.setSpacing(false);
		description.setPadding(false);

		Label id = new Label(resultHit.getDocument().getId());

		HorizontalLayout header = new HorizontalLayout();
		header.addClassName("header");
		header.setSpacing(false);
		header.getThemeList().add("spacing-s");

		String title = resultHit.getDocument().getData().get("title").toString();
		if (title != null) {
			Span name = new Span(title);
			name.addClassName("title");
			header.add(name);
		}

		List<Category[]> cats = resultHit.getDocument().getCategories();
		if (cats != null) {
			List<String> categories = resultHit.getDocument().getCategories().stream()
					.flatMap(c -> Arrays.stream(c))
					.map(c -> (String) c.getName())
					.collect(Collectors.toList());
			Span c = new Span(String.join(",", categories));
			c.addClassName("date");
			header.add(c);
		}

		description.add(header);

		resultHit.getDocument().getData().keySet().stream().filter(k -> !"title".equals(k)).forEach(k -> {
			Span currentField = new Span(resultHit.getDocument().getData().get(k).toString());
			description.add(currentField);
		});

		card.add(id, description);
		return card;
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event) {
		handleSearch("", null);
	}

}

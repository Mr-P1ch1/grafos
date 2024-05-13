package com.example.application.views.paginaprincial;

import com.example.application.views.MainLayout;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.dialog.Dialog;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@PageTitle("Página Principal")
@Route(value = "paginaprincipal", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class PaginaprincipalView extends Composite<VerticalLayout> {
    private final long INF = Long.MAX_VALUE / 2; // Use a practical infinite value for path calculations
    private List<Node> nodes = new ArrayList<>();
    private long[][] mAdy; // Adjacency matrix
    private Grid<Node> grid;
    private Select<String> startSelect, endSelect;
    private TextField nameField;
    private NumberField xField, yField;

    public PaginaprincipalView() {
        setupUI();
    }

    private void setupUI() {
        VerticalLayout content = getContent();
        content.setSizeFull();

        H4 header = new H4("Cálculo de ruta pasando por al menos dos puntos intermedios");
        content.add(header);

        nameField = new TextField("Nombre del Nodo");
        xField = new NumberField("Coordenada X");
        yField = new NumberField("Coordenada Y");
        Button addNodeButton = new Button("Agregar Nodo", e -> addNode());
        startSelect = new Select<>();
        startSelect.setLabel("Nodo de inicio");
        endSelect = new Select<>();
        endSelect.setLabel("Nodo final");
        Button calculateButton = new Button("Calcular Ruta", e -> calculateRoute());

        content.add(nameField, xField, yField, addNodeButton, startSelect, endSelect, calculateButton);

        grid = new Grid<>(Node.class);
        grid.addColumn(Node::getName).setHeader("Nombre");
        grid.addColumn(Node::getX).setHeader("Coordenada X");
        grid.addColumn(Node::getY).setHeader("Coordenada Y");
        content.add(grid);
    }

    private void addNode() {
        String name = nameField.getValue();
        Double x = xField.getValue();
        Double y = yField.getValue();
        if (name.isEmpty() || x == null || y == null) {
            Notification.show("Todos los campos son necesarios.", 3000, Notification.Position.MIDDLE);
            return;
        }
        Node newNode = new Node(name, x, y);
        nodes.add(newNode);
        updateAdjacencyMatrix();
        startSelect.setItems(nodes.stream().map(Node::getName).collect(Collectors.toList()));
        endSelect.setItems(nodes.stream().map(Node::getName).collect(Collectors.toList()));
        grid.setItems(nodes);
    }

    private void updateAdjacencyMatrix() {
        int size = nodes.size();
        mAdy = new long[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    mAdy[i][j] = 0;
                } else {
                    Node node1 = nodes.get(i);
                    Node node2 = nodes.get(j);
                    mAdy[i][j] = (long) Math.sqrt(Math.pow(node1.getX() - node2.getX(), 2) + Math.pow(node1.getY() - node2.getY(), 2));
                }
            }
        }
    }

    private void calculateRoute() {
        if (nodes.size() < 4) {
            Notification.show("Debe haber al menos cuatro nodos para garantizar dos puntos intermedios.", 5000, Notification.Position.MIDDLE);
            return;
        }
        String result = algoritmoFloyd(mAdy);
        showResultsInDialog(result);
    }
    private void showResultsInDialog(String htmlContent) {
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");
        dialog.setHeight("600px");

        Html content = new Html(htmlContent);
        VerticalLayout layout = new VerticalLayout(content);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        Button closeButton = new Button("Cerrar", event -> dialog.close());
        layout.add(closeButton);

        dialog.add(layout);
        dialog.open();
    }



    private String algoritmoFloyd(long [][] mAdy) {
        int vertices = mAdy.length;
        long[][] dist = new long[vertices][vertices];
        String[][] paths = new String[vertices][vertices];

        for (int i = 0; i < vertices; i++) {
            for (int j = 0; j < vertices; j++) {
                dist[i][j] = mAdy[i][j];
                if (mAdy[i][j] != INF) {
                    paths[i][j] = Integer.toString(j + 1);
                } else {
                    paths[i][j] = "";
                }
            }
        }

        for (int k = 0; k < vertices; k++) {
            for (int i = 0; i < vertices; i++) {
                for (int j = 0; j < vertices; j++) {
                    if (dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                        paths[i][j] = paths[i][k] + " -> " + paths[k][j];
                    }
                }
            }
        }

        return formatResult(dist, paths, vertices);
    }

    private String formatResult(long[][] dist, String[][] paths, int vertices) {
        StringBuilder result = new StringBuilder("<div><b>Resultados de rutas:</b><br>");
        for (int i = 0; i < vertices; i++) {
            for (int j = 0; j < vertices; j++) {
                if (i != j && dist[i][j] != INF) {
                    List<Integer> path = reconstructPath(i, j, paths);
                    // Asegurar que el camino tiene al menos dos nodos intermedios
                    if (path.size() > 3) { // path incluye el inicio y el final
                        result.append(String.format("Del nodo %d al nodo %d, pasando por [%s], con un costo total de %d.<br>",
                                i + 1, j + 1, pathToString(path.subList(1, path.size() - 1)), dist[i][j])); // Omitimos el nodo de inicio y final en la lista
                    }
                }
            }
        }
        result.append("</div>");
        return result.toString();
    }



    private List<Integer> reconstructPath(int start, int end, String[][] paths) {
        List<Integer> path = new ArrayList<>();
        String route = paths[start][end];
        if (route != null && !route.isEmpty()) {
            for (String step : route.split(" -> ")) {
                if (!step.isEmpty()) {
                    path.add(Integer.parseInt(step.trim()));
                }
            }
        }
        return path;
    }

    private String pathToString(List<Integer> path) {
        if (path.isEmpty() || path.size() < 3) {
            return "directamente"; // Para mantener coherencia, aunque no se mostrarán estas rutas
        }
        // Formatear la cadena omitiendo el primer y último elemento
        return path.subList(1, path.size() - 1).stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }

    public class Node {
        private String name;
        private double x, y;

        public Node(String name, double x, double y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }

        public String getName() { return name; }
        public double getX() { return x; }
        public double getY() { return y; }
    }
}

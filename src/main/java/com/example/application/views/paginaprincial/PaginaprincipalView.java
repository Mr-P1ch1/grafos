package com.example.application.views.paginaprincial;

import com.example.application.views.MainLayout;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

@PageTitle("Página Principal")
@Route(value = "paginaprincipal", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class PaginaprincipalView extends Composite<VerticalLayout> {
    private List<Nodo> nodos = new ArrayList<>();
    private long[][] mAdy; // Matriz de adyacencia
    private Grid<Nodo> grid;
    private Select<String> selectInicio, selectFinal;
    private TextField nombreField;
    private NumberField xField, yField;

    public PaginaprincipalView() {
        configurarUI();
    }

    private void configurarUI() {
        VerticalLayout contenido = getContent();
        contenido.setSizeFull();

        // Título
        H6 titulo = new H6("Integrantes: Alejandro Paqui, Naomi Lizano y Camila Torres");
        contenido.add(titulo);

        // Encabezado
        H4 header = new H4("Cálculo de ruta pasando por al menos dos puntos intermedios");
        contenido.add(header);

        // Campos de entrada
        nombreField = new TextField("Nombre del Nodo");
        xField = new NumberField("Coordenada X");
        yField = new NumberField("Coordenada Y");
        Button addNodeButton = new Button("Agregar Nodo", e -> agregarNodo());

        // Selectores de nodo de inicio y final
        selectInicio = new Select<>();
        selectInicio.setLabel("Nodo de inicio");
        selectFinal = new Select<>();
        selectFinal.setLabel("Nodo final");
        Button calculateButton = new Button("Calcular Ruta", e -> calcularRuta());

        // Añadir componentes al layout
        contenido.add(nombreField, xField, yField, addNodeButton, selectInicio, selectFinal, calculateButton);

        // Configuración del Grid para mostrar los nodos
        grid = new Grid<>(Nodo.class);
        grid.addColumn(Nodo::getNombre).setHeader("Nombre");
        grid.addColumn(Nodo::getX).setHeader("Coordenada X");
        grid.addColumn(Nodo::getY).setHeader("Coordenada Y");
        grid.setHeight("300px");

        // Crear un contenedor que permita el scroll para el Grid
        VerticalLayout gridContainer = new VerticalLayout(grid);
        gridContainer.setSizeFull();
        gridContainer.setPadding(false);
        gridContainer.setSpacing(false);
        gridContainer.getStyle().set("overflow", "auto");

        contenido.add(gridContainer);
        contenido.setFlexGrow(1, gridContainer);
    }

    private void agregarNodo() {
        String nombre = nombreField.getValue();
        Double x = xField.getValue();
        Double y = yField.getValue();
        if (nombre.isEmpty() || x == null || y == null) {
            Notification.show("Todos los campos son necesarios.", 3000, Notification.Position.MIDDLE);
            return;
        }
        Nodo nuevoNodo = new Nodo(nombre, x, y);
        nodos.add(nuevoNodo);
        actualizarMatrizAdyacencia();
        actualizarSelectores();
        grid.setItems(nodos); // Actualizar items del Grid
    }

    private void actualizarMatrizAdyacencia() {
        int size = nodos.size();
        mAdy = new long[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    mAdy[i][j] = 0;
                } else {
                    Nodo nodo1 = nodos.get(i);
                    Nodo nodo2 = nodos.get(j);
                    mAdy[i][j] = (long) Math.sqrt(Math.pow(nodo1.getX() - nodo2.getX(), 2) + Math.pow(nodo1.getY() - nodo2.getY(), 2));
                }
            }
        }
    }

    private void actualizarSelectores() {
        selectInicio.setItems(nodos.stream().map(Nodo::getNombre).collect(Collectors.toList()));
        selectFinal.setItems(nodos.stream().map(Nodo::getNombre).collect(Collectors.toList()));
    }

    private void calcularRuta() {
        if (nodos.size() < 4) {
            Notification.show("Debe haber al menos cuatro nodos para garantizar dos puntos intermedios.", 5000, Notification.Position.MIDDLE);
            return;
        }
        String nombreInicio = selectInicio.getValue();
        String nombreFinal = selectFinal.getValue();
        if (nombreInicio == null || nombreFinal == null) {
            Notification.show("Debe seleccionar tanto el nodo de inicio como el nodo final.", 3000, Notification.Position.MIDDLE);
            return;
        }
        int indiceInicio = encontrarIndicePorNombre(nombreInicio);
        int indiceFinal = encontrarIndicePorNombre(nombreFinal);
        if (indiceInicio == -1 || indiceFinal == -1) {
            Notification.show("Nodos seleccionados no válidos.", 3000, Notification.Position.MIDDLE);
            return;
        }
        String resultado = encontrarRutaConDijkstra(indiceInicio, indiceFinal);
        mostrarResultadosEnDialogo(resultado);
    }

    private int encontrarIndicePorNombre(String nombre) {
        for (int i = 0; i < nodos.size(); i++) {
            if (nodos.get(i).getNombre().equals(nombre)) {
                return i;
            }
        }
        return -1;
    }

    private String encontrarRutaConDijkstra(int inicio, int fin) {
        List<Integer> rutaMasCorta = null;
        long distanciaMasCorta = Long.MAX_VALUE;

        // Intentar cada par de nodos intermedios
        for (int i = 0; i < nodos.size(); i++) {
            if (i == inicio || i == fin) continue;
            for (int j = i + 1; j < nodos.size(); j++) {
                if (j == inicio || j == fin) continue;

                List<Integer> ruta1 = dijkstra(inicio, i);
                List<Integer> ruta2 = dijkstra(i, j);
                List<Integer> ruta3 = dijkstra(j, fin);

                if (ruta1 != null && ruta2 != null && ruta3 != null) {
                    List<Integer> rutaCombinada = new ArrayList<>();
                    rutaCombinada.addAll(ruta1);
                    rutaCombinada.remove(rutaCombinada.size() - 1); // Eliminar nodo duplicado
                    rutaCombinada.addAll(ruta2);
                    rutaCombinada.remove(rutaCombinada.size() - 1); // Eliminar nodo duplicado
                    rutaCombinada.addAll(ruta3);

                    long distancia = calcularDistanciaRuta(rutaCombinada);
                    if (distancia < distanciaMasCorta) {
                        distanciaMasCorta = distancia;
                        rutaMasCorta = rutaCombinada;
                    }
                }
            }
        }

        if (rutaMasCorta == null) {
            return "No hay ruta disponible que pase por al menos dos nodos intermedios.";
        }
        return formatearResultado(distanciaMasCorta, rutaMasCorta);
    }

    private List<Integer> dijkstra(int inicio, int fin) {
        int vertices = mAdy.length;
        long[] dist = new long[vertices];
        boolean[] visitado = new boolean[vertices];
        int[] prev = new int[vertices];

        for (int i = 0; i < vertices; i++) {
            dist[i] = Long.MAX_VALUE;
            prev[i] = -1;
        }
        dist[inicio] = 0;

        PriorityQueue<DistanciaNodo> queue = new PriorityQueue<>();
        queue.add(new DistanciaNodo(inicio, 0));

        while (!queue.isEmpty()) {
            DistanciaNodo current = queue.poll();
            int u = current.nodo;
            if (visitado[u]) continue;
            visitado[u] = true;

            for (int v = 0; v < vertices; v++) {
                if (mAdy[u][v] != 0 && !visitado[v]) {
                    long nuevaDist = dist[u] + mAdy[u][v];
                    if (nuevaDist < dist[v]) {
                        dist[v] = nuevaDist;
                        prev[v] = u;
                        queue.add(new DistanciaNodo(v, nuevaDist));
                    }
                }
            }
        }

        List<Integer> ruta = reconstruirRuta(prev, inicio, fin);
        return ruta.size() > 1 ? ruta : null;
    }

    private List<Integer> reconstruirRuta(int[] prev, int inicio, int fin) {
        List<Integer> ruta = new ArrayList<>();
        for (int at = fin; at != -1; at = prev[at]) {
            ruta.add(0, at);
        }
        if (ruta.get(0) != inicio) {
            ruta.clear(); // Limpiar la ruta si no comienza con el nodo inicial
        }
        return ruta;
    }

    private long calcularDistanciaRuta(List<Integer> ruta) {
        long distancia = 0;
        for (int i = 0; i < ruta.size() - 1; i++) {
            distancia += mAdy[ruta.get(i)][ruta.get(i + 1)];
        }
        return distancia;
    }

    private String formatearResultado(long distancia, List<Integer> ruta) {
        StringBuilder resultado = new StringBuilder("<div><b>Resultado de la ruta:</b><br>");
        resultado.append("Distancia total: ").append(distancia).append("<br>");
        resultado.append("Ruta: ");
        for (int i = 0; i < ruta.size(); i++) {
            resultado.append(nodos.get(ruta.get(i)).getNombre());
            if (i < ruta.size() - 1) {
                resultado.append(" -> ");
            }
        }
        resultado.append("</div>");
        return resultado.toString();
    }

    private void mostrarResultadosEnDialogo(String contenidoHtml) {
        Dialog dialogo = new Dialog();
        dialogo.setWidth("800px");
        dialogo.setHeight("600px");

        Label etiquetaResultado = new Label();
        etiquetaResultado.getElement().setProperty("innerHTML", contenidoHtml);
        etiquetaResultado.setSizeFull();

        Button botonCerrar = new Button("Cerrar", event -> dialogo.close());
        botonCerrar.getStyle().set("margin-top", "20px");

        VerticalLayout layout = new VerticalLayout(etiquetaResultado, botonCerrar);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        dialogo.add(layout);
        dialogo.open();
    }

    private static class DistanciaNodo implements Comparable<DistanciaNodo> {
        int nodo;
        long distancia;

        DistanciaNodo(int nodo, long distancia) {
            this.nodo = nodo;
            this.distancia = distancia;
        }

        @Override
        public int compareTo(DistanciaNodo otro) {
            return Long.compare(this.distancia, otro.distancia);
        }
    }

    public static class Nodo {
        private String nombre;
        private double x, y;

        public Nodo(String nombre, double x, double y) {
            this.nombre = nombre;
            this.x = x;
            this.y = y;
        }

        public String getNombre() { return nombre; }
        public double getX() { return x; }
        public double getY() { return y; }
    }
}

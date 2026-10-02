package es.luna.controller;

import es.luna.config.ApiConfig;
import es.luna.model.VigenereCifradoResponse;
import es.luna.model.VigenereDescifradoResponse;
import es.luna.service.AesService;
import es.luna.service.VigenereService;
import es.luna.util.AlertaUtil;
import es.luna.util.Mensajes;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador para la ventana principal de ScriptumFX.
 * Gestiona los eventos de la interfaz de usuario y las operaciones de cifrado/descifrado.
 *
 * @author Arantxa
 * @version 1.0
 * @since 2025-11-11
 */
public class VentanaController {

    private static final Logger logger = LoggerFactory.getLogger(VentanaController.class);

    /**
     * Constructor público por defecto.
     * JavaFX utiliza este constructor al cargar el controlador desde el archivo FXML.
     */
    public VentanaController() {
        // Constructor vacío requerido por JavaFX
    }

    // ========== Elementos del menú ==========
    @FXML
    private VBox root;
    @FXML
    private Menu menuArchivo;
    @FXML
    private Menu menuIdioma;
    @FXML
    private Menu menuAyuda;
    @FXML
    private Menu menuClose;
    @FXML
    private MenuItem menuIdiomaEspanol;
    @FXML
    private MenuItem menuIdiomaIngles;
    @FXML
    private MenuItem menuThemeToggle;
    @FXML
    private MenuItem menuAbout;
    @FXML
    private MenuItem menuExit;

    // ========== Icono del tema ==========
    @FXML
    private ImageView iconoTema;

    // ========== Estado de la API ==========
    @FXML
    private Label lblEstadoApiTitulo;
    @FXML
    private Label lblEstadoApi;
    @FXML
    private ProgressIndicator progressConexion;

    // ========== Campos de entrada/salida ==========
    @FXML
    private Label lblSeccionEntrada;
    @FXML
    private Label lblEntradaDescripcion;
    @FXML
    private TextArea txtEntrada;
    @FXML
    private Label lblSeccionSalida;
    @FXML
    private TextArea txtSalida;
    @FXML
    private Button btnSubirArchivo;
    @FXML
    private Button btnVaciarEntrada;
    @FXML
    private Button btnCopiar;
    @FXML
    private Button btnDescargar;
    @FXML
    private Button btnVaciarSalida;

    // ========== Configuración de cifrado ==========
    @FXML
    private TitledPane titledPaneAjustes;
    @FXML
    private Label lblAjustesDescripcion;
    @FXML
    private Label lblModo;
    @FXML
    private ComboBox<String> comboModo;
    @FXML
    private Label lblMetodo;
    @FXML
    private ComboBox<String> comboMetodo;
    @FXML
    private Label lblDescripcionMetodo;
    @FXML
    private Label lblAjustesAvanzados;
    @FXML
    private Label lblResultado;
    @FXML
    private ProgressIndicator progressOperacion;

    // ========== Campos específicos de Vigenère ==========
    @FXML
    private VBox vboxClaveVigenere;
    @FXML
    private Label lblClaveVigenere;
    @FXML
    private TextField txtClaveVigenere;
    @FXML
    private Label lblClaveVigenereDesc;

    // ========== Campos específicos de AES ==========
    @FXML
    private VBox vboxCamposAes;
    @FXML
    private Label lblPasswordAes;
    @FXML
    private TextField txtPasswordAes;
    @FXML
    private Label lblPasswordAesDesc;
    @FXML
    private Label lblTipoAes;
    @FXML
    private ComboBox<String> comboTipoAes;
    @FXML
    private Label lblTipoAesDesc;
    @FXML
    private VBox vboxSaltAes;
    @FXML
    private Label lblSaltAes;
    @FXML
    private TextField txtSaltAes;
    @FXML
    private Label lblSaltAesDesc;

    // ========== Botón de acción ==========
    @FXML
    private Button btnAccion;
    @FXML
    private Label lblMensajeEstado;

    // ========== Servicios ==========
    private VigenereService vigenereService;
    private AesService aesService;

    // ========== Estado ==========
    private boolean temaClaro = true;
    private File archivoSubido = null; // Mantener referencia al archivo subido
    private String nombreArchivoOriginal = null; // Nombre del archivo original
    private String archivoSalidaCifrado = null; // Contenido cifrado del archivo (texto)
    private File archivoSalidaGrande = null; // Resultado grande guardado en disco (no se carga en memoria)
    private byte[] archivoSalidaBinaria = null; // Contenido binario del archivo descifrado
    private boolean esSalidaArchivo = false; // Indica si la salida es un archivo cifrado
    private boolean esSalidaBinaria = false; // Indica si la salida contiene datos binarios
    private boolean ultimaOperacionFueCifrado = true; // true = cifrado, false = descifrado
    private String paqueteAesCifrado = null; // Paquete AES que contiene (archivo + metadatos)
    private Double alturaVentanaContraida = null; // Altura de la ventana cuando el TitledPane está contraído
    private Double alturaVentanaExpandida = null; // Altura de la ventana cuando el TitledPane está expandido

    /**
     * Inicialización del controlador.
     * Se ejecuta automáticamente al cargar el FXML.
     */
    @FXML
    public void initialize() {
        logger.info("=== Inicializando VentanaController ===");

        // Inicializar servicios
        inicializarServicios();

        // Configurar elementos del menú
        configurarMenu();

        // Configurar combos
        configurarCombos();

        // Configurar tooltips
        configurarTooltips();

        // Configurar tema inicial
        configurarTemaInicial();

        // Verificar conexión con la API
        verificarConexionApi();

        logger.info("Controlador inicializado correctamente");
    }

    /**
     * Inicializa los servicios de Vigenère y AES.
     */
    private void inicializarServicios() {
        try {
            String apiUrl = ApiConfig.API_BASE_URL;
            logger.info("Inicializando servicios con API URL: {}", apiUrl);

            vigenereService = new VigenereService(apiUrl);
            aesService = new AesService(apiUrl);

            logger.info("Servicios inicializados correctamente");
        } catch (Exception e) {
            logger.error("Error al inicializar servicios", e);
            mostrarAlerta(
                    Mensajes.obtener("error.inicializacion.titulo"),
                    Mensajes.obtener("error.inicializacion.mensaje"),
                    Alert.AlertType.ERROR
            );
        }
    }

    /**
     * Configura el menú de la aplicación.
     */
    private void configurarMenu() {
        if (menuIdiomaEspanol != null) {
            menuIdiomaEspanol.setOnAction(e -> cambiarIdioma("es"));
        }

        if (menuIdiomaIngles != null) {
            menuIdiomaIngles.setOnAction(e -> cambiarIdioma("en"));
        }

        if (menuThemeToggle != null) {
            menuThemeToggle.setOnAction(e -> cambiarTema());
        }

        if (menuAbout != null) {
            menuAbout.setOnAction(e -> mostrarAcercaDe());
        }

        if (menuExit != null) {
            menuExit.setOnAction(e -> salirAplicacion());
        }
    }

    /**
     * Configura los ComboBox con sus opciones.
     */
    private void configurarCombos() {
        // Combo de modo (Cifrar/Descifrar)
        comboModo.setItems(FXCollections.observableArrayList(
                Mensajes.obtener("modo.cifrar"),
                Mensajes.obtener("modo.descifrar")
        ));
        comboModo.setValue(Mensajes.obtener("modo.cifrar"));

        // Combo de metodo (Vigenère/AES)
        comboMetodo.setItems(FXCollections.observableArrayList(
                Mensajes.obtener("metodo.vigenere"),
                Mensajes.obtener("metodo.aes")
        ));
        comboMetodo.setValue(Mensajes.obtener("metodo.vigenere"));

        // Combo de tipo AES
        comboTipoAes.setItems(FXCollections.observableArrayList(
                Mensajes.obtener("aes.tipo.128"),
                Mensajes.obtener("aes.tipo.192"),
                Mensajes.obtener("aes.tipo.256")
        ));
        comboTipoAes.setValue(Mensajes.obtener("aes.tipo.256"));

        // Actualizar descripción inicial
        actualizarDescripcionMetodo();
        actualizarCamposSegunMetodo();
    }

    /**
     * Configura todos los tooltips de la interfaz.
     */
    private void configurarTooltips() {
        // Tooltips de los botones
        btnSubirArchivo.setTooltip(new Tooltip(Mensajes.obtener("tooltip.btn.subir")));
        btnVaciarEntrada.setTooltip(new Tooltip(Mensajes.obtener("tooltip.btn.vaciar.entrada")));
        btnCopiar.setTooltip(new Tooltip(Mensajes.obtener("tooltip.btn.copiar")));
        btnDescargar.setTooltip(new Tooltip(Mensajes.obtener("tooltip.btn.descargar")));
        btnVaciarSalida.setTooltip(new Tooltip(Mensajes.obtener("tooltip.btn.vaciar.salida")));

        // Tooltips de los ComboBox
        comboModo.setTooltip(new Tooltip(Mensajes.obtener("tooltip.combo.modo")));
        comboMetodo.setTooltip(new Tooltip(Mensajes.obtener("tooltip.combo.metodo")));
        comboTipoAes.setTooltip(new Tooltip(Mensajes.obtener("tooltip.combo.tipo.aes")));

        // Tooltips de los campos de texto
        txtEntrada.setTooltip(new Tooltip(Mensajes.obtener("tooltip.txt.entrada")));
        txtSalida.setTooltip(new Tooltip(Mensajes.obtener("tooltip.txt.salida")));
        txtClaveVigenere.setTooltip(new Tooltip(Mensajes.obtener("tooltip.txt.clave.vigenere")));
        txtPasswordAes.setTooltip(new Tooltip(Mensajes.obtener("tooltip.txt.password.aes")));
        txtSaltAes.setTooltip(new Tooltip(Mensajes.obtener("tooltip.txt.salt.aes")));

        // Tooltips para el botón de acción (se actualizará dinámicamente según el modo)
        actualizarTooltipBotonAccion();

        logger.debug("Tooltips configurados");
    }

    /**
     * Actualiza el tooltip del botón de acción según el modo seleccionado.
     */
    private void actualizarTooltipBotonAccion() {
        String modo = comboModo.getValue();
        if (Mensajes.obtener("modo.cifrar").equals(modo)) {
            btnAccion.setTooltip(new Tooltip(Mensajes.obtener("tooltip.btn.accion.cifrar")));
        } else {
            btnAccion.setTooltip(new Tooltip(Mensajes.obtener("tooltip.btn.accion.descifrar")));
        }
    }

    /**
     * Configura el tema inicial.
     */
    private void configurarTemaInicial() {
        root.sceneProperty().addListener((obs, oldVal, newScene) -> {
            if (newScene != null) {
                aplicarTemaInicial();
                configurarAjusteAutomaticoVentana();
            }
        });
    }

    /**
     * Configura el ajuste automático de la ventana cuando se expande/contrae el TitledPane.
     */
    private void configurarAjusteAutomaticoVentana() {
        // Guardar la altura inicial (expandida) después de que la ventana se muestre
        javafx.animation.PauseTransition pauseInicial = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
        pauseInicial.setOnFinished(e -> {
            var stage = (javafx.stage.Stage) root.getScene().getWindow();
            if (stage != null) {
                alturaVentanaExpandida = stage.getHeight();
                logger.debug("Altura inicial guardada (expandida): {}", alturaVentanaExpandida);
            }
        });
        pauseInicial.play();

        titledPaneAjustes.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            var stage = (javafx.stage.Stage) root.getScene().getWindow();
            if (stage == null || stage.isMaximized()) {
                return;
            }

            // Calcular la altura objetivo
            double targetHeight;
            if (isExpanded) {
                // Expandido: usar altura expandida guardada
                if (alturaVentanaExpandida != null) {
                    targetHeight = alturaVentanaExpandida;
                } else {
                    // Primera vez expandiendo, usar altura actual
                    alturaVentanaExpandida = stage.getHeight();
                    return;
                }
            } else {
                // Contraído: calcular y guardar altura contraída si no existe
                if (alturaVentanaContraida == null) {
                    double contentHeight = titledPaneAjustes.getContent().getBoundsInLocal().getHeight();
                    alturaVentanaContraida = stage.getHeight() - contentHeight;
                    logger.debug("Altura contraída calculada: {}", alturaVentanaContraida);
                }
                targetHeight = alturaVentanaContraida;
            }

            // Simplemente ajustar sin animación (más confiable)
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
            pause.setOnFinished(event -> {
                stage.setHeight(targetHeight);
                logger.debug("Ventana ajustada - TitledPane {} - altura: {}",
                    isExpanded ? "expandido" : "contraído", targetHeight);
            });
            pause.play();
        });
    }

    /**
     * Aplica el tema claro inicial.
     */
    private void aplicarTemaInicial() {
        try {
            Scene scene = root.getScene();
            if (scene != null) {
                var cssClaro = getClass().getResource("/es/luna/css/estilos_claro.css");
                if (cssClaro != null) {
                    scene.getStylesheets().add(cssClaro.toExternalForm());
                    logger.info("Tema claro inicial aplicado");
                }

                // Actualizar icono del tema
                actualizarIconoTema();
            }
        } catch (Exception e) {
            logger.error("Error al aplicar tema inicial", e);
        }
    }

    /**
     * Verifica la conexión con la API al iniciar.
     */
    private void verificarConexionApi() {
        lblEstadoApi.setText(Mensajes.obtener("api.estado.verificando"));
        lblEstadoApi.setStyle("-fx-text-fill: orange;");
        progressConexion.setVisible(true);

        vigenereService.verificarConexion()
                .thenAccept(conectado -> Platform.runLater(() -> {
                    progressConexion.setVisible(false);
                    if (conectado) {
                        lblEstadoApi.setText(Mensajes.obtener("api.estado.conectada"));
                        lblEstadoApi.setStyle("-fx-text-fill: #478778;");
                        logger.info("Conexión con API exitosa");
                    } else {
                        lblEstadoApi.setText(Mensajes.obtener("api.estado.desconectada"));
                        lblEstadoApi.setStyle("-fx-text-fill: #DC143C;");
                        logger.warn("No se pudo conectar con la API");
                        mostrarAlerta(
                                Mensajes.obtener("error.conexion.titulo"),
                                Mensajes.obtener("error.conexion.mensaje", ApiConfig.API_BASE_URL),
                                Alert.AlertType.WARNING
                        );
                    }
                }));
    }

    // ==================== EVENTOS DE LA UI ====================

    /**
     * Maneja el cambio de modo (Cifrar/Descifrar).
     */
    @FXML
    private void onModoChanged() {
        String modo = comboModo.getValue();
        String metodo = comboMetodo.getValue();

        // Actualizar texto e icono del botón
        if (Mensajes.obtener("modo.cifrar").equals(modo)) {
            btnAccion.setText(Mensajes.obtener("boton.cifrar"));
                                FontIcon lockIcon = new FontIcon("fa-filter");
            lockIcon.setIconSize(16);
            btnAccion.setGraphic(lockIcon);
            lblResultado.setText(Mensajes.obtener("salida.resultado.cifrado"));
        } else {
            btnAccion.setText(Mensajes.obtener("boton.descifrar"));
            FontIcon unlockIcon = new FontIcon("fas-unlock");
            unlockIcon.setIconSize(16);
            btnAccion.setGraphic(unlockIcon);
            lblResultado.setText(Mensajes.obtener("salida.resultado.descifrado"));
        }

        // Mostrar/ocultar campo de salt para AES
        if (Mensajes.obtener("metodo.aes").equals(metodo)) {
            boolean esDescifrado = Mensajes.obtener("modo.descifrar").equals(modo);
            vboxSaltAes.setManaged(esDescifrado);
            vboxSaltAes.setVisible(esDescifrado);
        }

        // Actualizar tooltip del botón de acción
        actualizarTooltipBotonAccion();

        logger.debug("Modo cambiado a: {}", modo);
    }

    /**
     * Maneja el cambio de metodo (Vigenère/AES).
     */
    @FXML
    private void onMetodoChanged() {
        actualizarDescripcionMetodo();
        actualizarCamposSegunMetodo();
    }

    /**
     * Actualiza la descripción del metodo seleccionado.
     */
    private void actualizarDescripcionMetodo() {
        String metodo = comboMetodo.getValue();

        if (Mensajes.obtener("metodo.vigenere").equals(metodo)) {
            lblDescripcionMetodo.setText(Mensajes.obtener("metodo.descripcion.vigenere"));
        } else if (Mensajes.obtener("metodo.aes").equals(metodo)) {
            lblDescripcionMetodo.setText(Mensajes.obtener("metodo.descripcion.aes"));
        }
    }

    /**
     * Mostrar y ocultar campos según el metodo seleccionado.
     */
    private void actualizarCamposSegunMetodo() {
        String metodo = comboMetodo.getValue();
        String modo = comboModo.getValue();

        if (Mensajes.obtener("metodo.vigenere").equals(metodo)) {
            // Mostrar campos de Vigenère
            vboxClaveVigenere.setManaged(true);
            vboxClaveVigenere.setVisible(true);

            // Ocultar campos de AES
            vboxCamposAes.setManaged(false);
            vboxCamposAes.setVisible(false);

        } else if (Mensajes.obtener("metodo.aes").equals(metodo)) {
            // Ocultar campos de Vigenère
            vboxClaveVigenere.setManaged(false);
            vboxClaveVigenere.setVisible(false);

            // Mostrar campos de AES
            vboxCamposAes.setManaged(true);
            vboxCamposAes.setVisible(true);

            // Mostrar/ocultar salt según modo
            boolean esDescifrado = Mensajes.obtener("modo.descifrar").equals(modo);
            vboxSaltAes.setManaged(esDescifrado);
            vboxSaltAes.setVisible(esDescifrado);
        }
    }

    /**
     * Acción principal: Cifrar o Descifrar según la configuración.
     */
    @FXML
    private void onAccionPrincipal() {
        String modo = comboModo.getValue();
        String metodo = comboMetodo.getValue();

        logger.info("Ejecutando acción: {} con {}", modo, metodo);

        // Validar que haya texto de entrada
        String texto = txtEntrada.getText();
        if (texto == null || texto.trim().isEmpty()) {
            mostrarAlerta(
                    Mensajes.obtener("validacion.campo.vacio.titulo"),
                    Mensajes.obtener("validacion.campo.vacio.mensaje"),
                    Alert.AlertType.WARNING
            );
            return;
        }

        // Validar tamaño del texto solo si NO es un archivo subido
        // (los archivos se validan en el backend según el límite de streaming)
        if (archivoSubido == null) {
            final int LIMITE_CARACTERES = 1_000_000; // 1 millón de caracteres
            if (texto.length() > LIMITE_CARACTERES) {
                double tamanoMB = texto.length() / (1024.0 * 1024.0);
                mostrarAlerta(
                        "Texto demasiado largo",
                        String.format(
                                """
                                        El texto ingresado es demasiado largo para procesar.
                                        
                                        • Tamaño actual: %,d caracteres (%.2f MB)
                                        • Límite máximo: %,d caracteres
                                        
                                        Para textos grandes, usa la opción 'Subir archivo' en su lugar.""",
                                texto.length(),
                                tamanoMB,
                                LIMITE_CARACTERES
                        ),
                        Alert.AlertType.WARNING
                );
                return;
            }
        }

        // Ejecutar la operación correspondiente
        if (Mensajes.obtener("metodo.vigenere").equals(metodo)) {
            if (Mensajes.obtener("modo.cifrar").equals(modo)) {
                cifrarVigenere(texto);
            } else {
                descifrarVigenere(texto);
            }
        } else if (Mensajes.obtener("metodo.aes").equals(metodo)) {
            if (Mensajes.obtener("modo.cifrar").equals(modo)) {
                cifrarAes(texto);
            } else {
                descifrarAes(texto);
            }
        }
    }

    /**
     * Cifra texto con Vigenère.
     */
    private void cifrarVigenere(String texto) {
        String clave = validarClave();
        if (clave == null) return;

        mostrarCargando(true);
        lblMensajeEstado.setText(Mensajes.obtener("estado.cifrando.vigenere"));

        // Si hay un archivo subido, usar el endpoint de archivo
        CompletableFuture<VigenereCifradoResponse> operacion;
        if (archivoSubido != null) {
            logger.info("Usando endpoint de archivo para cifrar con Vigenère");
            operacion = vigenereService.cifrarArchivo(archivoSubido, clave);
        } else {
            logger.info("Usando endpoint de texto para cifrar con Vigenère");
            operacion = vigenereService.cifrarTexto(texto, clave);
        }

        operacion.thenAccept(response -> Platform.runLater(() -> {
                    // Guardar contenido cifrado
                    archivoSalidaGrande = response.getArchivoSalida();
                    archivoSalidaCifrado = archivoSalidaGrande != null ? null : response.getTextoCifrado();
                    long longitudSalida = archivoSalidaGrande != null ? archivoSalidaGrande.length() : response.getTextoCifrado().length();

                    if (archivoSubido != null) {
                        // Es un archivo - mostrar mensaje informativo
                        esSalidaArchivo = true;
                        ultimaOperacionFueCifrado = true;
                        long tamanioCifrado = longitudSalida;
                        double tamanioMB = tamanioCifrado / (1024.0 * 1024.0);
                        String mensaje = String.format(
                                """
                                        Archivo cifrado con Vigenère
                                        Archivo original: %s
                                        Tamaño cifrado: %.2f MB (%,d caracteres)
                                        Clave usada: %s
                                        Usa el botón 'Descargar' para guardar el archivo cifrado""",
                                nombreArchivoOriginal,
                                tamanioMB,
                                tamanioCifrado,
                                response.getClaveUsada()
                        );
                        txtSalida.setText(mensaje);
                    } else {
                        // Es texto - mostrar contenido
                        esSalidaArchivo = false;
                        txtSalida.setText(response.getTextoCifrado());
                    }

                    mostrarExito(Mensajes.obtener("estado.cifrado.exitoso", response.getClaveUsada()));
                    logger.info("Cifrado Vigenère exitoso");
                }))
                .exceptionally(error -> manejarErrorOperacion(error, Mensajes.obtener("error.cifrar")));
    }

    /**
     * Descifra texto con Vigenère.
     */
    private void descifrarVigenere(String textoCifrado) {
        String clave = validarClave();
        if (clave == null) return;

        mostrarCargando(true);
        lblMensajeEstado.setText(Mensajes.obtener("estado.descifrando.vigenere"));

        // Si hay un archivo subido, usar el endpoint de archivo
        CompletableFuture<VigenereDescifradoResponse> operacion;
        if (archivoSubido != null) {
            logger.info("Usando endpoint de archivo para descifrar con Vigenère");
            operacion = vigenereService.descifrarArchivo(archivoSubido, clave);
        } else {
            logger.info("Usando endpoint de texto para descifrar con Vigenère");
            operacion = vigenereService.descifrarTexto(textoCifrado, clave);
        }

        operacion.thenAccept(response -> Platform.runLater(() -> {
                    // Guardar contenido descifrado
                    archivoSalidaGrande = response.getArchivoSalida();
                    archivoSalidaCifrado = archivoSalidaGrande != null ? null : response.getTextoDescifrado();
                    long longitudSalida = archivoSalidaGrande != null ? archivoSalidaGrande.length() : response.getTextoDescifrado().length();

                    if (archivoSubido != null) {
                        // Es un archivo - mostrar mensaje informativo
                        esSalidaArchivo = true;
                        ultimaOperacionFueCifrado = false;
                        long tamanioDescifrado = longitudSalida;
                        double tamanioMB = tamanioDescifrado / (1024.0 * 1024.0);
                        String mensaje = String.format(
                                """
                                        Archivo descifrado con Vigenère
                                        Archivo original: %s
                                        Tamaño descifrado: %.2f MB (%,d caracteres)
                                        Usa el botón 'Descargar' para guardar el archivo descifrado""",
                                nombreArchivoOriginal,
                                tamanioMB,
                                tamanioDescifrado
                        );
                        txtSalida.setText(mensaje);
                    } else {
                        // Es texto - mostrar contenido
                        esSalidaArchivo = false;
                        txtSalida.setText(response.getTextoDescifrado());
                    }

                    mostrarExito(Mensajes.obtener("estado.descifrado.exitoso"));
                    logger.info("Descifrado Vigenère exitoso");
                }))
                .exceptionally(error -> manejarErrorOperacion(error, Mensajes.obtener("error.descifrar")));
    }

    /**
     * Cifra texto con AES.
     */
    private void cifrarAes(String texto) {
        String password = validarPassword();
        if (password == null) return;

        String tipoAes = comboTipoAes.getValue();
        mostrarCargando(true);
        lblMensajeEstado.setText(Mensajes.obtener("estado.cifrando.aes", tipoAes));

        // Si hay un archivo subido, usar el endpoint de archivo
        if (archivoSubido != null) {
            logger.info("Usando endpoint de archivo para cifrar con AES");
            aesService.cifrarArchivo(archivoSubido, password, tipoAes)
                    .thenAccept(response -> Platform.runLater(() -> {
                        // Guardar paquete cifrado (contiene: archivo, salt, metadatos)
                        paqueteAesCifrado = response.getPaquete();
                        archivoSalidaCifrado = paqueteAesCifrado; // Para descarga
                        esSalidaArchivo = true;
                        ultimaOperacionFueCifrado = true;

                        // Obtener información del paquete
                        String nombreOriginal = response.getNombreOriginal();
                        Integer tamanioOriginal = response.getTamanioOriginalBytes();
                        long tamanioPaquete = response.getTamanioPaqueteBytes();
                        double tamanioMB = tamanioPaquete / (1024.0 * 1024.0);

                        String mensaje = String.format(
                                """
                                        Archivo cifrado con AES
                                        Archivo original: %s
                                        Tamaño original: %,d bytes
                                        Tamaño del paquete: %.2f MB (%,d bytes)
                                        El paquete contiene TODO lo necesario para descifrar
                                        Usa el botón 'Descargar' para guardar el paquete cifrado

                                        Nota: No necesitas guardar el salt por separado, está en el paquete""",
                                nombreOriginal != null ? nombreOriginal : "desconocido",
                                tamanioOriginal != null ? tamanioOriginal : 0,
                                tamanioMB,
                                tamanioPaquete
                        );
                        txtSalida.setText(mensaje);

                        mostrarExito(Mensajes.obtener("estado.cifrado.exitoso.aes", tipoAes));
                        logger.info("Cifrado AES de archivo exitoso con paquete");
                    }))
                    .exceptionally(error -> manejarErrorOperacion(error, Mensajes.obtener("error.cifrar")));
        } else {
            logger.info("Usando endpoint de texto para cifrar con AES");
            aesService.cifrarTexto(texto, password, tipoAes)
                    .thenAccept(response -> Platform.runLater(() -> {
                        archivoSalidaCifrado = response.getTextoCifrado();
                        esSalidaArchivo = false;
                        txtSalida.setText(archivoSalidaCifrado);
                        mostrarExito(Mensajes.obtener("estado.cifrado.exitoso.aes", response.getTipoAes()));
                        mostrarAlertaSaltConBotonCopiar(response.getSalt());
                        logger.info("Cifrado AES exitoso");
                    }))
                    .exceptionally(error -> manejarErrorOperacion(error, Mensajes.obtener("error.cifrar")));
        }
    }

    /**
     * Descifra texto con AES.
     */
    private void descifrarAes(String textoCifrado) {
        String password = validarPassword();
        if (password == null) return;

        String tipoAes = comboTipoAes.getValue();
        mostrarCargando(true);
        lblMensajeEstado.setText(Mensajes.obtener("estado.descifrando.aes", tipoAes));

        // Si hay un archivo subido, leer su contenido real (puede ser un paquete)
        String contenidoReal = textoCifrado;
        if (archivoSubido != null) {
            try {
                contenidoReal = Files.readString(archivoSubido.toPath());
                logger.info("Leyendo contenido real del archivo subido para descifrar: {} bytes", contenidoReal.length());
            } catch (IOException e) {
                logger.error("Error al leer archivo para descifrar", e);
                mostrarCargando(false);
                mostrarAlerta(
                        Mensajes.obtener("error.archivo.leer.titulo"),
                        Mensajes.obtener("error.archivo.leer.mensaje"),
                        Alert.AlertType.ERROR
                );
                return;
            }
        }

        // Detectar si es un paquete AES
        // Los paquetes empiezan con "SCRIPTUM" en el header, que en base64 es "U0NSSVBUVU0"
        // También son considerablemente más largos que texto normal cifrado
        boolean esPaquete = detectarPaqueteAes(contenidoReal);

        if (esPaquete) {
            // Es un paquete de archivo, no necesita salt por separado
            logger.info("Detectado paquete AES, descifrando con sistema de paquetes");
            aesService.descifrarArchivo(contenidoReal, password)
                    .thenAccept(archivoDescifradoBase64 -> Platform.runLater(() -> {
                        // El endpoint devuelve el archivo binario en base64
                        try {
                            byte[] bytesDescifrados = java.util.Base64.getDecoder().decode(archivoDescifradoBase64);

                            // Guardar bytes directamente sin convertir a String
                            archivoSalidaBinaria = bytesDescifrados;
                            archivoSalidaCifrado = null; // Limpiar contenido de texto
                            esSalidaArchivo = true;
                            esSalidaBinaria = true;
                            ultimaOperacionFueCifrado = false;

                            // Mostrar mensaje informativo en lugar del contenido
                            long tamanioDescifrado = bytesDescifrados.length;
                            double tamanioMB = tamanioDescifrado / (1024.0 * 1024.0);
                            String mensaje = String.format(
                                    """
                                            Archivo descifrado con AES
                                            Archivo original: %s
                                            Tamaño descifrado: %.2f MB (%,d bytes)
                                            Usa el botón 'Descargar' para guardar el archivo descifrado""",
                                    nombreArchivoOriginal != null ? nombreArchivoOriginal : "desconocido",
                                    tamanioMB,
                                    tamanioDescifrado
                            );
                            txtSalida.setText(mensaje);

                            mostrarExito(Mensajes.obtener("estado.descifrado.exitoso"));
                            logger.info("Descifrado AES de paquete exitoso: {} bytes", bytesDescifrados.length);
                        } catch (IllegalArgumentException e) {
                            logger.error("Error al decodificar base64 del archivo descifrado", e);
                            mostrarAlerta(
                                    Mensajes.obtener("error.descifrar"),
                                    "Error al decodificar el archivo descifrado",
                                    Alert.AlertType.ERROR
                            );
                        }
                    }))
                    .exceptionally(this::manejarErrorAesDescifrado);
        } else {
            // Es texto normal, requiere salt
            String salt = validarSalt();
            if (salt == null) {
                mostrarCargando(false);
                return;
            }

            // Validar que el texto cifrado tenga formato base64
            if (validarFormatoBase64(contenidoReal, "texto cifrado")) {
                mostrarCargando(false);
                return;
            }
            if (validarFormatoBase64(salt, "salt")) {
                mostrarCargando(false);
                return;
            }

            logger.info("Descifrando texto con AES (requiere salt)");
            aesService.descifrarTexto(contenidoReal, password, salt, tipoAes)
                    .thenAccept(response -> Platform.runLater(() -> {
                        txtSalida.setText(response.getTextoDescifrado());
                        mostrarExito(Mensajes.obtener("estado.descifrado.exitoso"));
                        logger.info("Descifrado AES de texto exitoso");
                    }))
                    .exceptionally(this::manejarErrorAesDescifrado);
        }
    }

    /**
     * Detecta si un texto cifrado es un paquete AES.
     * Los paquetes tienen un header específico que empieza con "SCRIPTUM".
     *
     * @param textoCifrado el texto a analizar
     * @return true si es un paquete AES, false si es texto normal cifrado
     */
    private boolean detectarPaqueteAes(String textoCifrado) {
        if (textoCifrado == null || textoCifrado.isEmpty()) {
            return false;
        }

        // Si tenemos el paquete guardado y coincide, es un paquete
        if (textoCifrado.equals(paqueteAesCifrado)) {
            logger.debug("Coincide con paquete AES guardado");
            return true;
        }

        // Verificar el magic header "SCRIPTUM" en base64
        // "SCRIPTUM" = U0NSSVBUVU0=
        if (textoCifrado.startsWith("U0NSSVBUVU0")) {
            logger.debug("Detectado header SCRIPTUM en el texto");
            return true;
        }

        // Verificar longitud: los paquetes son mucho más largos (incluyen metadatos)
        // Un texto cifrado normal de AES tiene ~200-500 caracteres en base64
        // Un paquete tiene fácilmente >1000 caracteres por los metadatos
        if (textoCifrado.length() > 800) {
            logger.debug("Texto muy largo ({}), probablemente un paquete", textoCifrado.length());
            return true;
        }

        return false;
    }

    /**
     * Sube un archivo de texto.
     */
    @FXML
    private void onSubirArchivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(Mensajes.obtener("entrada.titulo.dialogo.archivo"));

        // Determinar qué extensiones permitir según el metodo seleccionado
        String metodo = comboMetodo.getValue();
        if (Mensajes.obtener("metodo.aes").equals(metodo)) {
            // AES permite múltiples tipos de archivo
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Todos los archivos soportados",
                            "*.txt", "*.pdf", "*.doc", "*.docx", "*.xls", "*.xlsx",
                            "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp",
                            "*.zip", "*.rar", "*.7z",
                            "*.csv", "*.json", "*.xml"),
                    new FileChooser.ExtensionFilter("Archivos de texto (*.txt)", "*.txt"),
                    new FileChooser.ExtensionFilter("Documentos (*.pdf, *.doc, *.docx)", "*.pdf", "*.doc", "*.docx"),
                    new FileChooser.ExtensionFilter("Hojas de cálculo (*.xls, *.xlsx, *.csv)", "*.xls", "*.xlsx", "*.csv"),
                    new FileChooser.ExtensionFilter("Imágenes (*.jpg, *.png, *.gif, *.bmp)", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"),
                    new FileChooser.ExtensionFilter("Archivos comprimidos (*.zip, *.rar, *.7z)", "*.zip", "*.rar", "*.7z"),
                    new FileChooser.ExtensionFilter("Datos (*.json, *.xml)", "*.json", "*.xml")
            );
        } else {
            // Vigenère solo permite archivos de texto
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(Mensajes.obtener("entrada.filtro.archivo"), "*.txt")
            );
        }

        File archivo = fileChooser.showOpenDialog(root.getScene().getWindow());

        if (archivo != null) {
            try {
                // Validar que el archivo existe y es accesible
                if (!archivo.exists() || !archivo.canRead()) {
                    throw new IOException("El archivo no existe o no se puede leer");
                }

                // Guardar referencia al archivo y su nombre
                archivoSubido = archivo;
                nombreArchivoOriginal = archivo.getName();

                // Obtener información del archivo
                long tamanioBytes = archivo.length();
                double tamanioMB = tamanioBytes / (1024.0 * 1024.0);

                // Determinar el tipo de archivo
                String extension = "";
                String nombre = archivo.getName();
                int lastDot = nombre.lastIndexOf('.');
                if (lastDot > 0) {
                    extension = nombre.substring(lastDot);
                }

                String tipoArchivo = determinarTipoArchivo(extension);

                // Mostrar mensaje informativo en lugar del contenido
                String mensaje = String.format(
                        """
                                Archivo subido: %s
                                Tipo: %s
                                Tamaño: %.2f MB (%,d bytes)
                                Listo para cifrar""",
                        nombreArchivoOriginal,
                        tipoArchivo,
                        tamanioMB,
                        tamanioBytes
                );
                txtEntrada.setText(mensaje);

                logger.info("Archivo cargado: {} ({} bytes, tipo: {}) - se usará el endpoint de archivo",
                        archivo.getName(), tamanioBytes, tipoArchivo);
            } catch (Exception e) {
                logger.error("Error al cargar archivo", e);
                archivoSubido = null;
                nombreArchivoOriginal = null;
                mostrarAlerta(
                        Mensajes.obtener("error.archivo.leer.titulo"),
                        Mensajes.obtener("error.archivo.leer.mensaje"),
                        Alert.AlertType.ERROR
                );
            }
        }
    }

    /**
     * Vacía el campo de entrada.
     */
    @FXML
    private void onVaciarEntrada() {
        txtEntrada.clear();
        archivoSubido = null; // Limpiar referencia al archivo
        nombreArchivoOriginal = null; // Limpiar nombre del archivo
    }

    /**
     * Vacía el campo de salida.
     */
    @FXML
    private void onVaciarSalida() {
        txtSalida.clear();
        lblMensajeEstado.setText("");
        archivoSalidaCifrado = null; // Limpiar contenido cifrado
        archivoSalidaGrande = null;
        archivoSalidaBinaria = null; // Limpiar bytes binarios
        esSalidaArchivo = false; // Resetear flag
        esSalidaBinaria = false; // Resetear flag binario
        paqueteAesCifrado = null; // Limpiar paquete AES
    }

    /**
     * Copia el resultado al portapapeles.
     */
    @FXML
    private void onCopiar() {
        String texto = txtSalida.getText();
        if (texto != null && !texto.isEmpty()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(texto);
            clipboard.setContent(content);

            lblMensajeEstado.setText(Mensajes.obtener("estado.copiado"));
            lblMensajeEstado.setStyle("-fx-text-fill: green;");
            logger.info("Texto copiado al portapapeles");
        } else {
            mostrarAlerta(
                    Mensajes.obtener("validacion.no.texto.copiar.titulo"),
                    Mensajes.obtener("validacion.no.texto.copiar.mensaje"),
                    Alert.AlertType.WARNING
            );
        }
    }

    /**
     * Descarga el resultado como archivo.
     */
    @FXML
    private void onDescargar() {
        // Determinar el contenido a guardar y el nombre del archivo
        String contenidoTexto = null;
        byte[] contenidoBinario = null;
        String nombreArchivoSugerido;

        // Verificar si tenemos datos binarios o de texto
        if (esSalidaBinaria && archivoSalidaBinaria != null && archivoSalidaBinaria.length > 0) {
            // Tenemos datos binarios (archivo descifrado)
            contenidoBinario = archivoSalidaBinaria;

            // Generar nombre de archivo basado en el original
            if (nombreArchivoOriginal != null && !nombreArchivoOriginal.isEmpty()) {
                // Obtener el nombre sin extensión
                String nombreSinExtension = nombreArchivoOriginal;
                String extension = ".bin";

                int lastDot = nombreArchivoOriginal.lastIndexOf('.');
                if (lastDot > 0) {
                    nombreSinExtension = nombreArchivoOriginal.substring(0, lastDot);
                    extension = nombreArchivoOriginal.substring(lastDot);
                }

                // Si es descifrado, eliminar el sufijo _encrypted si existe
                if (!ultimaOperacionFueCifrado && nombreSinExtension.endsWith("_encrypted")) {
                    nombreSinExtension = nombreSinExtension.substring(0, nombreSinExtension.length() - "_encrypted".length());
                }

                // Agregar sufijo según la operación
                String sufijo = ultimaOperacionFueCifrado ? "_encrypted" : "_decrypted";
                nombreArchivoSugerido = nombreSinExtension + sufijo + extension;
            } else {
                nombreArchivoSugerido = "archivo_decrypted.bin";
            }

            logger.info("Descargando archivo binario descifrado: {} -> {}", nombreArchivoOriginal, nombreArchivoSugerido);

        } else if (esSalidaArchivo && (archivoSalidaGrande != null || (archivoSalidaCifrado != null && !archivoSalidaCifrado.isEmpty()))) {
            // Tenemos datos de texto (archivo cifrado o texto normal); si es grande, está en disco
            contenidoTexto = archivoSalidaGrande != null ? "" : archivoSalidaCifrado;

            // Generar nombre de archivo basado en el original y la operación
            if (nombreArchivoOriginal != null && !nombreArchivoOriginal.isEmpty()) {
                // Obtener el nombre sin extensión
                String nombreSinExtension = nombreArchivoOriginal;
                String extension = ".txt";

                int lastDot = nombreArchivoOriginal.lastIndexOf('.');
                if (lastDot > 0) {
                    nombreSinExtension = nombreArchivoOriginal.substring(0, lastDot);
                    extension = nombreArchivoOriginal.substring(lastDot);
                }

                // Si es descifrado, eliminar el sufijo _encrypted si existe
                if (!ultimaOperacionFueCifrado && nombreSinExtension.endsWith("_encrypted")) {
                    nombreSinExtension = nombreSinExtension.substring(0, nombreSinExtension.length() - "_encrypted".length());
                }

                // Agregar sufijo según la operación
                String sufijo = ultimaOperacionFueCifrado ? "_encrypted" : "_decrypted";
                nombreArchivoSugerido = nombreSinExtension + sufijo + extension;
            } else {
                // Nombre genérico según la operación
                String sufijo = ultimaOperacionFueCifrado ? "_encrypted" : "_decrypted";
                nombreArchivoSugerido = "archivo" + sufijo + ".txt";
            }

            logger.info("Descargando archivo {}: {} -> {}",
                    ultimaOperacionFueCifrado ? "cifrado" : "descifrado",
                    nombreArchivoOriginal, nombreArchivoSugerido);
        } else {
            // Comportamiento tradicional: guardar el texto del área de salida
            contenidoTexto = txtSalida.getText();
            nombreArchivoSugerido = Mensajes.obtener("salida.archivo.nombre");
        }

        // Validar que hay contenido
        if ((contenidoTexto == null || contenidoTexto.isEmpty()) && contenidoBinario == null && archivoSalidaGrande == null) {
            mostrarAlerta(
                    Mensajes.obtener("validacion.no.texto.descargar.titulo"),
                    Mensajes.obtener("validacion.no.texto.descargar.mensaje"),
                    Alert.AlertType.WARNING
            );
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(Mensajes.obtener("salida.titulo.dialogo.guardar"));
        fileChooser.setInitialFileName(nombreArchivoSugerido);

        // Determinar qué extensiones permitir según el metodo seleccionado y el tipo de archivo
        String metodo = comboMetodo.getValue();
        if (Mensajes.obtener("metodo.aes").equals(metodo) && esSalidaArchivo) {
            // AES con archivo: permitir múltiples tipos de archivo
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Todos los archivos soportados",
                            "*.txt", "*.pdf", "*.doc", "*.docx", "*.xls", "*.xlsx",
                            "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp",
                            "*.zip", "*.rar", "*.7z",
                            "*.csv", "*.json", "*.xml"),
                    new FileChooser.ExtensionFilter("Archivos de texto (*.txt)", "*.txt"),
                    new FileChooser.ExtensionFilter("Documentos (*.pdf, *.doc, *.docx)", "*.pdf", "*.doc", "*.docx"),
                    new FileChooser.ExtensionFilter("Hojas de cálculo (*.xls, *.xlsx, *.csv)", "*.xls", "*.xlsx", "*.csv"),
                    new FileChooser.ExtensionFilter("Imágenes (*.jpg, *.png, *.gif, *.bmp)", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"),
                    new FileChooser.ExtensionFilter("Archivos comprimidos (*.zip, *.rar, *.7z)", "*.zip", "*.rar", "*.7z"),
                    new FileChooser.ExtensionFilter("Datos (*.json, *.xml)", "*.json", "*.xml")
            );
        } else {
            // Vigenère o texto: solo archivos de texto
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(Mensajes.obtener("entrada.filtro.archivo"), "*.txt")
            );
        }

        File archivo = fileChooser.showSaveDialog(root.getScene().getWindow());

        if (archivo != null) {
            try {
                // Escribir según el tipo de contenido
                if (contenidoBinario != null) {
                    // Escribir bytes binarios directamente
                    Files.write(archivo.toPath(), contenidoBinario);
                    logger.info("Archivo binario guardado: {} ({} bytes)", archivo.getAbsolutePath(), contenidoBinario.length);
                } else if (archivoSalidaGrande != null && esSalidaArchivo) {
                    // Resultado grande: copiar desde disco sin cargarlo en memoria
                    Files.copy(archivoSalidaGrande.toPath(), archivo.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Archivo grande guardado: {}", archivo.getAbsolutePath());
                } else {
                    // Escribir como texto
                    Files.writeString(archivo.toPath(), contenidoTexto);
                    logger.info("Archivo de texto guardado: {}", archivo.getAbsolutePath());
                }

                lblMensajeEstado.setText(Mensajes.obtener("estado.archivo.guardado", archivo.getName()));
                lblMensajeEstado.setStyle("-fx-text-fill: green;");
            } catch (IOException e) {
                logger.error("Error al guardar archivo", e);
                mostrarAlerta(
                        Mensajes.obtener("error.archivo.guardar.titulo"),
                        Mensajes.obtener("error.archivo.guardar.mensaje"),
                        Alert.AlertType.ERROR
                );
            }
        }
    }

    // ==================== FUNCIONES DEL MENÚ ====================

    /**
     * Cambia el idioma de la aplicación y actualiza todos los textos.
     *
     * @param codigoIdioma El código del idioma ("es" para español, "en" para inglés)
     */
    private void cambiarIdioma(String codigoIdioma) {
        try {
            // Cambiar el idioma en la clase Mensajes
            if ("es".equals(codigoIdioma)) {
                Mensajes.usarEspanol();
            } else if ("en".equals(codigoIdioma)) {
                Mensajes.usarIngles();
            }

            // Actualizar todos los textos de la interfaz sin recargar
            actualizarTextosInterfaz();

            logger.info("Idioma cambiado a: {}", codigoIdioma);
        } catch (Exception e) {
            logger.error("Error al cambiar idioma", e);
            mostrarAlerta(
                    "Error",
                    "No se pudo cambiar el idioma",
                    Alert.AlertType.ERROR
            );
        }
    }

    /**
     * Actualiza todos los textos de la interfaz con el idioma actual.
     * Mantiene el estado de la aplicación (texto ingresado, selecciones, etc.)
     */
    private void actualizarTextosInterfaz() {
        // Actualizar textos de los menús principales
        menuArchivo.setText(Mensajes.obtener("menu.archivo"));
        menuIdioma.setText(Mensajes.obtener("menu.idioma"));
        menuAyuda.setText(Mensajes.obtener("menu.ayuda"));
        menuClose.setText(Mensajes.obtener("menu.cerrar"));

        // Actualizar textos de los items del menú
        menuThemeToggle.setText(temaClaro ?
                Mensajes.obtener("menu.tema.oscuro") :
                Mensajes.obtener("menu.tema.claro")
        );
        menuAbout.setText(Mensajes.obtener("menu.acerca"));
        menuExit.setText(Mensajes.obtener("menu.salir"));
        menuIdiomaEspanol.setText(Mensajes.obtener("menu.idioma.espanol"));
        menuIdiomaIngles.setText(Mensajes.obtener("menu.idioma.ingles"));

        // Actualizar estado de la API
        lblEstadoApi.setText(lblEstadoApi.getText().contains("Conectada") ||
                lblEstadoApi.getText().contains("Connected") ?
                Mensajes.obtener("api.estado.conectada") :
                lblEstadoApi.getText().contains("Verificando") ||
                        lblEstadoApi.getText().contains("Checking") ?
                        Mensajes.obtener("api.estado.verificando") :
                        Mensajes.obtener("api.estado.desconectada")
        );

        // Actualizar labels de secciones
        lblSeccionEntrada.setText(Mensajes.obtener("seccion.entrada"));
        lblSeccionSalida.setText(Mensajes.obtener("seccion.salida"));
        lblEntradaDescripcion.setText(Mensajes.obtener("entrada.descripcion"));

        // Actualizar TitledPane y labels de ajustes
        titledPaneAjustes.setText(Mensajes.obtener("ajustes.titulo"));
        lblAjustesDescripcion.setText(Mensajes.obtener("ajustes.descripcion"));
        lblModo.setText(Mensajes.obtener("modo.label"));
        lblMetodo.setText(Mensajes.obtener("metodo.label"));
        lblAjustesAvanzados.setText(Mensajes.obtener("ajustes.avanzados"));
        lblEstadoApiTitulo.setText(Mensajes.obtener("api.estado.label"));

        // Actualizar labels de Vigenère
        lblClaveVigenere.setText(Mensajes.obtener("vigenere.clave.label"));
        lblClaveVigenereDesc.setText(Mensajes.obtener("vigenere.clave.descripcion"));

        // Actualizar labels de AES
        lblPasswordAes.setText(Mensajes.obtener("aes.password.label"));
        lblPasswordAesDesc.setText(Mensajes.obtener("aes.password.descripcion"));
        lblTipoAes.setText(Mensajes.obtener("aes.tipo.label"));
        lblTipoAesDesc.setText(Mensajes.obtener("aes.tipo.descripcion"));
        lblSaltAes.setText(Mensajes.obtener("aes.salt.label"));
        lblSaltAesDesc.setText(Mensajes.obtener("aes.salt.descripcion"));

        // Actualizar botones de entrada
        btnSubirArchivo.setText(Mensajes.obtener("entrada.boton.subir"));
        btnVaciarEntrada.setText(Mensajes.obtener("entrada.boton.vaciar"));

        // Actualizar botones de salida
        btnCopiar.setText(Mensajes.obtener("salida.boton.copiar"));
        btnDescargar.setText(Mensajes.obtener("salida.boton.descargar"));
        btnVaciarSalida.setText(Mensajes.obtener("salida.boton.limpiar"));

        // Actualizar placeholders
        txtEntrada.setPromptText(Mensajes.obtener("entrada.placeholder"));
        txtSalida.setPromptText(Mensajes.obtener("salida.placeholder"));
        txtClaveVigenere.setPromptText(Mensajes.obtener("vigenere.clave.placeholder"));
        txtPasswordAes.setPromptText(Mensajes.obtener("aes.password.placeholder"));
        txtSaltAes.setPromptText(Mensajes.obtener("aes.salt.placeholder"));

        // Actualizar ComboBox manteniendo las selecciones
        actualizarComboModo();
        actualizarComboMetodo();
        actualizarComboTipoAes();

        // Actualizar labels de descripción
        actualizarDescripcionMetodo();

        // Actualizar botón de acción según el modo actual
        String modoActual = comboModo.getSelectionModel().getSelectedIndex() == 0 ? "cifrar" : "descifrar";
        if (modoActual.equals("cifrar")) {
            btnAccion.setText(Mensajes.obtener("boton.cifrar"));
            FontIcon lockIcon = new FontIcon("fas-lock");
            lockIcon.setIconSize(16);
            btnAccion.setGraphic(lockIcon);
        } else {
            btnAccion.setText(Mensajes.obtener("boton.descifrar"));
            FontIcon unlockIcon = new FontIcon("fas-unlock");
            unlockIcon.setIconSize(16);
            btnAccion.setGraphic(unlockIcon);
        }

        // Actualizar label de resultado según el modo actual
        lblResultado.setText(modoActual.equals("cifrar") ?
                Mensajes.obtener("salida.resultado.cifrado") :
                Mensajes.obtener("salida.resultado.descifrado")
        );

        // Limpiar mensaje de estado al cambiar idioma
        lblMensajeEstado.setText("");

        // Actualizar todos los tooltips
        configurarTooltips();

        logger.debug("Textos de la interfaz actualizados");
    }

    /**
     * Actualiza el ComboBox de modo manteniendo la selección actual.
     */
    private void actualizarComboModo() {
        int seleccionActual = comboModo.getSelectionModel().getSelectedIndex();
        comboModo.setItems(FXCollections.observableArrayList(
                Mensajes.obtener("modo.cifrar"),
                Mensajes.obtener("modo.descifrar")
        ));
        comboModo.getSelectionModel().select(Math.max(seleccionActual, 0));
    }

    /**
     * Actualiza el ComboBox de método manteniendo la selección actual.
     */
    private void actualizarComboMetodo() {
        int seleccionActual = comboMetodo.getSelectionModel().getSelectedIndex();
        comboMetodo.setItems(FXCollections.observableArrayList(
                Mensajes.obtener("metodo.vigenere"),
                Mensajes.obtener("metodo.aes")
        ));
        comboMetodo.getSelectionModel().select(Math.max(seleccionActual, 0));
    }

    /**
     * Actualiza el ComboBox de tipo AES manteniendo la selección actual.
     */
    private void actualizarComboTipoAes() {
        int seleccionActual = comboTipoAes.getSelectionModel().getSelectedIndex();
        comboTipoAes.setItems(FXCollections.observableArrayList(
                Mensajes.obtener("aes.tipo.128"),
                Mensajes.obtener("aes.tipo.192"),
                Mensajes.obtener("aes.tipo.256")
        ));
        comboTipoAes.getSelectionModel().select(seleccionActual >= 0 ? seleccionActual : 2);
    }

    /**
     * Cambia entre tema claro y oscuro.
     */
    private void cambiarTema() {
        try {
            Scene scene = root.getScene();
            if (scene == null) return;

            scene.getStylesheets().clear();

            if (temaClaro) {
                var cssOscuro = getClass().getResource("/es/luna/css/estilos_oscuro.css");
                if (cssOscuro != null) {
                    scene.getStylesheets().add(cssOscuro.toExternalForm());
                    menuThemeToggle.setText(Mensajes.obtener("menu.tema.claro"));
                }
            } else {
                var cssClaro = getClass().getResource("/es/luna/css/estilos_claro.css");
                if (cssClaro != null) {
                    scene.getStylesheets().add(cssClaro.toExternalForm());
                    menuThemeToggle.setText(Mensajes.obtener("menu.tema.oscuro"));
                }
            }

            temaClaro = !temaClaro;
            logger.info("Tema cambiado a: {}", temaClaro ? "claro" : "oscuro");

            // Actualizar icono del tema
            actualizarIconoTema();

            // Sincronizar tema con AlertaUtil
            AlertaUtil.setTemaClaro(temaClaro);

        } catch (Exception e) {
            logger.error("Error al cambiar tema", e);
        }
    }

    /**
     * Actualiza el icono del tema según el tema actual.
     */
    private void actualizarIconoTema() {
        try {
            if (iconoTema != null) {
                String rutaIcono = temaClaro ?
                    "/es/luna/img/Icono-Claro.png" :
                    "/es/luna/img/Icono-Oscuro.png";

                var imagenUrl = getClass().getResource(rutaIcono);
                if (imagenUrl != null) {
                    iconoTema.setImage(new Image(imagenUrl.toExternalForm()));
                    logger.debug("Icono del tema actualizado: {}", temaClaro ? "claro" : "oscuro");
                } else {
                    logger.warn("No se pudo encontrar el icono: {}", rutaIcono);
                }
            }
        } catch (Exception e) {
            logger.error("Error al actualizar icono del tema", e);
        }
    }

    /**
     * Muestra el diálogo "Acerca de".
     */
    private void mostrarAcercaDe() {
        Window owner = root.getScene() != null ? root.getScene().getWindow() : null;
        AlertaUtil.mostrarAlertaConHeader(
                owner,
                Alert.AlertType.INFORMATION,
                Mensajes.obtener("acerca.titulo"),
                Mensajes.obtener("acerca.header"),
                Mensajes.obtener("acerca.contenido")
        );
    }

    /**
     * Sale de la aplicación con confirmación.
     */
    private void salirAplicacion() {
        Window owner = root.getScene() != null ? root.getScene().getWindow() : null;
        boolean confirmado = AlertaUtil.mostrarConfirmacionConHeader(
                owner,
                Mensajes.obtener("salir.titulo"),
                Mensajes.obtener("salir.header"),
                Mensajes.obtener("salir.mensaje")
        );

        if (confirmado) {
            logger.info("Cerrando aplicación");
            System.exit(0);
        }
    }

    // ==================== UTILIDADES ====================

    /**
     * Determina el tipo de archivo basado en su extensión.
     *
     * @param extension La extensión del archivo (incluye el punto, ej: ".pdf")
     * @return Una descripción legible del tipo de archivo
     */
    private String determinarTipoArchivo(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "Desconocido";
        }

        return switch (extension.toLowerCase()) {
            case ".txt" -> "Texto";
            case ".pdf" -> "PDF";
            case ".doc", ".docx" -> "Word";
            case ".xls", ".xlsx" -> "Excel";
            case ".csv" -> "CSV";
            case ".json" -> "JSON";
            case ".xml" -> "XML";
            case ".jpg", ".jpeg" -> "JPEG";
            case ".png" -> "PNG";
            case ".gif" -> "GIF";
            case ".bmp" -> "BMP";
            case ".zip" -> "ZIP";
            case ".rar" -> "RAR";
            case ".7z" -> "7-Zip";
            default -> extension.substring(1).toUpperCase();
        };
    }

    /**
     * Valida que la clave de Vigenère no esté vacía.
     *
     * @return La clave si es válida, null si no lo es.
     */
    private String validarClave() {
        String clave = txtClaveVigenere.getText();
        if (clave == null || clave.trim().isEmpty()) {
            mostrarAlerta(
                    Mensajes.obtener("validacion.clave.vacia.titulo"),
                    Mensajes.obtener("validacion.clave.vacia.mensaje"),
                    Alert.AlertType.WARNING
            );
            return null;
        }
        return clave;
    }

    /**
     * Valida que el password de AES tenga al menos 8 caracteres.
     *
     * @return El password si es válido, null si no lo es.
     */
    private String validarPassword() {
        String password = txtPasswordAes.getText();
        if (password == null || password.length() < 8) {
            mostrarAlerta(
                    Mensajes.obtener("validacion.password.invalido.titulo"),
                    Mensajes.obtener("validacion.password.invalido.mensaje"),
                    Alert.AlertType.WARNING
            );
            return null;
        }
        return password;
    }

    /**
     * Valida que el salt no esté vacío.
     *
     * @return El salt si es válido, null si no lo es.
     */
    private String validarSalt() {
        String salt = txtSaltAes.getText();
        if (salt == null || salt.trim().isEmpty()) {
            mostrarAlerta(
                    Mensajes.obtener("validacion.salt.vacio.titulo"),
                    Mensajes.obtener("validacion.salt.vacio.mensaje"),
                    Alert.AlertType.WARNING
            );
            return null;
        }
        return salt;
    }

    /**
     * Valida que un texto tenga formato base64 válido.
     *
     * @param texto       el texto a validar
     * @param nombreCampo nombre del campo para el mensaje de error
     * @return true si hay error (inválido), false si es válido
     */
    private boolean validarFormatoBase64(String texto, String nombreCampo) {
        // Patrón básico de base64: caracteres A-Za-z0-9+/= y longitud múltiplo de 4
        if (!texto.matches("^[A-Za-z0-9+/]+=*$")) {
            mostrarAlerta(
                    Mensajes.obtener("validacion.formato.invalido.titulo"),
                    Mensajes.obtener("validacion.base64.invalido.mensaje", nombreCampo),
                    Alert.AlertType.WARNING
            );
            return true; // Error: formato inválido
        }

        // Validar longitud (base64 debe ser múltiplo de 4)
        if ((texto.length() % 4) != 0) {
            mostrarAlerta(
                    Mensajes.obtener("validacion.formato.invalido.titulo"),
                    Mensajes.obtener("validacion.base64.incompleto.mensaje", nombreCampo),
                    Alert.AlertType.WARNING
            );
            return true; // Error: longitud inválida
        }

        return false; // Válido
    }

    /**
     * Muestra un mensaje de éxito y oculta el indicador de carga.
     */
    private void mostrarExito(String mensaje) {
        lblMensajeEstado.setText(mensaje);
        lblMensajeEstado.setStyle("-fx-text-fill: green;");
        mostrarCargando(false);
    }

    /**
     * Maneja errores de operaciones asíncronas de cifrado/descifrado.
     *
     * @return null (requerido por CompletableFuture.exceptionally)
     */
    private Void manejarErrorOperacion(Throwable error, String tituloError) {
        Platform.runLater(() -> {
            String mensaje = obtenerMensajeError(error);
            String mensajeCompleto = error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
            mostrarCargando(false);

            // Detectar errores de conexión
            if (mensaje.contains("conexión") || mensaje.contains("internet") || mensaje.contains("conectar al servidor") || mensaje.contains("servidor esté disponible")) {
                lblMensajeEstado.setText("✗ Error de conexión");
                lblMensajeEstado.setStyle("-fx-text-fill: red;");
                mostrarAlerta(
                        "Error de conexión",
                        mensaje + "\n\nPor favor verifica:\n" +
                        "• Tu conexión a internet está activa\n" +
                        "• El servidor está disponible\n" +
                        "• No hay problemas de firewall",
                        Alert.AlertType.ERROR
                );
            // Detectar error 422 (texto demasiado largo)
            } else if (mensajeCompleto != null && (mensajeCompleto.contains("422") ||
                mensajeCompleto.contains("Unprocessable Entity") ||
                mensajeCompleto.contains("demasiado largo") ||
                mensajeCompleto.contains("too large"))) {
                lblMensajeEstado.setText("✗ Texto demasiado largo");
                lblMensajeEstado.setStyle("-fx-text-fill: red;");
                mostrarAlerta(
                        "Texto demasiado largo",
                        """
                                El texto es demasiado grande para procesarlo directamente.

                                Recomendaciones:
                                • Usa la opción 'Subir archivo' para textos muy largos
                                • Reduce el tamaño del texto (máximo 1,000,000 caracteres)
                                • Divide el texto en partes más pequeñas""",
                        Alert.AlertType.WARNING
                );
            } else {
                lblMensajeEstado.setText("✗ Error: " + mensaje);
                lblMensajeEstado.setStyle("-fx-text-fill: red;");
                mostrarAlerta(tituloError, mensaje, Alert.AlertType.ERROR);
            }
        });
        return null;
    }

    /**
     * Maneja errores específicos del descifrado AES.
     *
     * @return null (requerido por CompletableFuture.exceptionally)
     */
    private Void manejarErrorAesDescifrado(Throwable error) {
        Platform.runLater(() -> {
            String mensaje = obtenerMensajeError(error);
            mostrarCargando(false);

            // Mensajes específicos según el tipo de error
            if (mensaje.contains("Paquete inválido") || mensaje.contains("Paquete base64 inválido") ||
                mensaje.contains("corrupto") || mensaje.contains("ASCII characters")) {
                lblMensajeEstado.setText("✗ Archivo no válido para descifrar");
                lblMensajeEstado.setStyle("-fx-text-fill: red;");
                mostrarAlerta(
                        "Error al descifrar archivo",
                        """
                                El archivo seleccionado no es un paquete cifrado válido.
                                
                                Asegúrate de que:
                                • El archivo fue cifrado con esta aplicación
                                • El archivo no está corrupto o modificado
                                • Estás usando el archivo correcto""",
                        Alert.AlertType.ERROR
                );
            } else if (mensaje.contains("500") || mensaje.contains("Internal Server Error")) {
                lblMensajeEstado.setText(Mensajes.obtener("error.descifrar.aes.500.estado"));
                lblMensajeEstado.setStyle("-fx-text-fill: red;");
                mostrarAlerta(
                        Mensajes.obtener("error.descifrar.aes.titulo"),
                        Mensajes.obtener("error.descifrar.aes.500.mensaje"),
                        Alert.AlertType.ERROR
                );
            } else if (mensaje.contains("autenticación") || mensaje.contains("tag")) {
                lblMensajeEstado.setText(Mensajes.obtener("error.descifrar.aes.auth.estado"));
                lblMensajeEstado.setStyle("-fx-text-fill: red;");
                mostrarAlerta(
                        Mensajes.obtener("error.descifrar.aes.titulo"),
                        Mensajes.obtener("error.descifrar.aes.mensaje"),
                        Alert.AlertType.ERROR
                );
            } else {
                lblMensajeEstado.setText("✗ Error: " + mensaje);
                lblMensajeEstado.setStyle("-fx-text-fill: red;");
                mostrarAlerta(Mensajes.obtener("error.descifrar"), mensaje, Alert.AlertType.ERROR);
            }
        });
        return null;
    }

    /**
     * Mostrar y ocultar indicadores de carga.
     */
    private void mostrarCargando(boolean mostrar) {
        progressOperacion.setVisible(mostrar);
        btnAccion.setDisable(mostrar);
    }

    /**
     * Extrae el mensaje de error de una excepción.
     * Intenta extraer el mensaje real del JSON de error de la API.
     */
    private String obtenerMensajeError(Throwable error) {
        Throwable causa = error.getCause();
        String mensajeCompleto = Objects.requireNonNullElse(causa, error).getMessage();

        if (mensajeCompleto == null) {
            return "Error desconocido";
        }

        // Intentar extraer el mensaje del JSON de error
        // Formato esperado: {"detail":{"error":"mensaje real"}}
        try {
            // Buscar el patrón "error":"mensaje"
            int errorStart = mensajeCompleto.indexOf("\"error\":\"");
            if (errorStart != -1) {
                errorStart += "\"error\":\"".length();
                int errorEnd = mensajeCompleto.indexOf("\"", errorStart);
                if (errorEnd != -1) {
                    return mensajeCompleto.substring(errorStart, errorEnd);
                }
            }

            // Si no se encuentra el patrón de error, buscar "detail" directamente
            int detailStart = mensajeCompleto.indexOf("\"detail\":\"");
            if (detailStart != -1) {
                detailStart += "\"detail\":\"".length();
                int detailEnd = mensajeCompleto.indexOf("\"", detailStart);
                if (detailEnd != -1) {
                    return mensajeCompleto.substring(detailStart, detailEnd);
                }
            }
        } catch (Exception e) {
            logger.debug("No se pudo extraer mensaje de error del JSON, usando mensaje completo", e);
        }

        return mensajeCompleto;
    }

    /**
     * Muestra un diálogo de alerta.
     */
    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Window owner = root.getScene() != null ? root.getScene().getWindow() : null;

        switch (tipo) {
            case WARNING:
                AlertaUtil.mostrarAdvertencia(owner, titulo, mensaje);
                break;
            case ERROR:
                AlertaUtil.mostrarError(owner, titulo, mensaje);
                break;
            case CONFIRMATION:
                AlertaUtil.mostrarConfirmacion(owner, titulo, mensaje);
                break;
            default:
                AlertaUtil.mostrarInfo(owner, titulo, mensaje);
                break;
        }
    }

    /**
     * Muestra una alerta especial para el salt con opción de copiarlo al portapapeles.
     */
    private void mostrarAlertaSaltConBotonCopiar(String salt) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(Mensajes.obtener("dialogo.salt.titulo"));
        alert.setHeaderText(Mensajes.obtener("dialogo.salt.header"));
        alert.setContentText(Mensajes.obtener("dialogo.salt.mensaje", salt));

        // Crear botón personalizado para copiar
        ButtonType btnCopiarSalt = new ButtonType(Mensajes.obtener("dialogo.salt.boton.copiar"));
        ButtonType btnCerrar = new ButtonType(Mensajes.obtener("dialogo.salt.boton.cerrar"), ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnCopiarSalt, btnCerrar);

        // Manejar la acción del botón
        Optional<ButtonType> resultado = alert.showAndWait();

        if (resultado.isPresent() && resultado.get() == btnCopiarSalt) {
            // Copiar salt al portapapeles
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(salt);
            clipboard.setContent(content);

            // Mostrar confirmación
            lblMensajeEstado.setText(Mensajes.obtener("estado.salt.copiado"));
            lblMensajeEstado.setStyle("-fx-text-fill: green;");
            logger.info("SALT copiado al portapapeles");
        }
    }
}

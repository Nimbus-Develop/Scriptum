package es.luna.service;

import es.luna.client.ApiClient;
import es.luna.model.VigenereCifradoLargeResponse;
import es.luna.model.VigenereCifradoResponse;
import es.luna.model.VigenereCifrarRequest;
import es.luna.model.VigenereDescifradoLargeResponse;
import es.luna.model.VigenereDescifradoResponse;
import es.luna.model.VigenereDescifrarRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio para operaciones de cifrado y descifrado con el algoritmo Vigenère.
 * Gestiona la comunicación con el backend de la API para las operaciones Vigenère.
 *
 * @author Arantxa
 * @version 1.0
 * @since 2025-11-11
 */
@SuppressWarnings("ClassCanBeRecord")
public class VigenereService {

    private static final Logger logger = LoggerFactory.getLogger(VigenereService.class);

    /** Cliente HTTP para comunicarse con la API */
    private final ApiClient apiClient;

    /** Endpoint base para operaciones Vigenère */
    private static final String BASE_ENDPOINT = "/vigenere";

    /** Umbral de tamaño para usar endpoint /large (9.5 MB en bytes) */
    private static final long LARGE_FILE_THRESHOLD = (long) (9.5 * 1024 * 1024); // 9.5 MB

    /**
     * Constructor que inicializa el servicio con la URL de la API.
     *
     * @param apiUrl la URL base de la API
     */
    public VigenereService(String apiUrl) {
        this.apiClient = new ApiClient(apiUrl);
        logger.info("VigenereService inicializado con API: {}", apiUrl);
    }

    /**
     * Constructor que acepta un ApiClient personalizado (útil para testing).
     *
     * @param apiClient el cliente HTTP a usar
     */
    public VigenereService(ApiClient apiClient) {
        this.apiClient = apiClient;
        logger.info("VigenereService inicializado con ApiClient personalizado");
    }

    /**
     * Cifra un texto usando el algoritmo Vigenère de forma asíncrona.
     *
     * @param texto el texto a cifrar
     * @param clave la clave para el cifrado
     * @return CompletableFuture con la respuesta del cifrado
     */
    public CompletableFuture<VigenereCifradoResponse> cifrarTexto(String texto, String clave) {
        logger.debug("Cifrando texto con Vigenère - Texto length: {}", texto.length());

        // Validaciones básicas
        if (texto.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("El texto no puede estar vacío")
            );
        }

        if (clave.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("La clave no puede estar vacía")
            );
        }

        // Crear request
        VigenereCifrarRequest request = new VigenereCifrarRequest(texto, clave);

        // Realizar petición asíncrona
        return apiClient.postAsync(
                BASE_ENDPOINT + "/cifrar/texto",
                request,
                VigenereCifradoResponse.class
        ).whenComplete((response, error) -> {
            if (error != null) {
                logger.warn("Error al cifrar texto con Vigenère: {}", error.getMessage());
            } else {
                logger.info("Texto cifrado exitosamente con Vigenère");
            }
        });
    }

    /**
     * Descifra un texto cifrado con Vigenère de forma asíncrona.
     *
     * @param textoCifrado el texto cifrado a descifrar
     * @param clave la clave para el descifrado
     * @return CompletableFuture con la respuesta del descifrado
     */
    public CompletableFuture<VigenereDescifradoResponse> descifrarTexto(String textoCifrado, String clave) {
        logger.debug("Descifrando texto con Vigenère - Texto cifrado length: {}", textoCifrado.length());

        // Validaciones básicas
        if (textoCifrado.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("El texto cifrado no puede estar vacío")
            );
        }

        if (clave.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("La clave no puede estar vacía")
            );
        }

        // Crear request
        VigenereDescifrarRequest request = new VigenereDescifrarRequest(textoCifrado, clave);

        // Realizar petición asíncrona
        return apiClient.postAsync(
                BASE_ENDPOINT + "/descifrar/texto",
                request,
                VigenereDescifradoResponse.class
        ).whenComplete((response, error) -> {
            if (error != null) {
                logger.warn("Error al descifrar texto con Vigenère: {}", error.getMessage());
            } else {
                logger.info("Texto descifrado exitosamente con Vigenère");
            }
        });
    }

    /**
     * Cifra un archivo usando el algoritmo Vigenère de forma asíncrona.
     * Detecta automáticamente el tamaño y usa el endpoint /large si es necesario.
     *
     * @param archivo el archivo a cifrar
     * @param clave la clave para el cifrado
     * @return CompletableFuture con la respuesta del cifrado
     */
    public CompletableFuture<VigenereCifradoResponse> cifrarArchivo(File archivo, String clave) {
        return cifrarArchivo(archivo, clave, "MAGICV1\n", true);
    }

    /**
     * Cifra un archivo usando el algoritmo Vigenère de forma asíncrona con parámetros del magic header.
     * Detecta automáticamente el tamaño y usa el endpoint /large si es necesario.
     *
     * @param archivo el archivo a cifrar
     * @param clave la clave para el cifrado
     * @param magicHeader el header mágico a agregar al inicio del archivo (para canary check posterior)
     * @param addHeader si se debe agregar el magic header al inicio
     * @return CompletableFuture con la respuesta del cifrado
     */
    public CompletableFuture<VigenereCifradoResponse> cifrarArchivo(
            File archivo,
            String clave,
            String magicHeader,
            boolean addHeader
    ) {
        logger.debug("Cifrando archivo con Vigenère - Archivo: {}, Tamaño: {} bytes",
                archivo.getName(), archivo.length());

        // Validaciones básicas
        if (!archivo.exists()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("El archivo no existe")
            );
        }

        if (clave.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("La clave no puede estar vacía")
            );
        }

        // Detectar si es un archivo grande
        long fileSize = archivo.length();
        boolean isLargeFile = fileSize >= LARGE_FILE_THRESHOLD;

        if (isLargeFile) {
            logger.info("Archivo grande detectado ({} MB), usando endpoint /large con streaming a archivo",
                    fileSize / (1024.0 * 1024.0));

            // Crear archivos de salida automáticamente
            String nombreOriginal = archivo.getName();
            int punto = nombreOriginal.lastIndexOf('.');
            String nombreSinExtension = punto > 0 ? nombreOriginal.substring(0, punto) : nombreOriginal;
            File archivoJsonTemp = new File(archivo.getParentFile(), nombreSinExtension + "_response.json");
            File archivoTextoFinal = new File(archivo.getParentFile(), nombreSinExtension + "_cifrado.txt");

            // Crear form data con parámetros adicionales para el endpoint large
            Map<String, String> formData = new HashMap<>();
            formData.put("clave", clave);
            formData.put("magic_header", magicHeader);
            formData.put("add_header", String.valueOf(addHeader));

            // Usar endpoint /large guardando respuesta JSON temporal
            return apiClient.postMultipartStreamingToFileAsync(
                    BASE_ENDPOINT + "/cifrar/file/large",
                    archivo,
                    formData,
                    archivoJsonTemp
            ).thenApply(jsonFile -> {
                // Extraer texto cifrado a archivo separado sin cargar todo en memoria
                try {
                    String claveUsada = extraerCampoTextoAArchivo(jsonFile, "texto_cifrado", archivoTextoFinal);
                    logger.info("Archivo grande cifrado exitosamente - Guardado en: {}", archivoTextoFinal.getAbsolutePath());

                    // Crear respuesta indicando que se guardó en archivo
                    VigenereCifradoResponse response = new VigenereCifradoResponse();
                    response.setTextoCifrado("[ARCHIVO GRANDE - Guardado en: " + archivoTextoFinal.getAbsolutePath() + "]");
                    response.setArchivoSalida(archivoTextoFinal);
                    response.setClaveUsada(claveUsada);
                    return response;
                } catch (Exception e) {
                    logger.error("Error al procesar archivo de respuesta: {}", e.getMessage());
                    throw new RuntimeException("Error al procesar respuesta: " + e.getMessage(), e);
                }
            }).whenComplete((response, error) -> {
                if (error != null) {
                    logger.warn("Error al cifrar archivo grande con Vigenère: {}", error.getMessage());
                } else {
                    logger.info("Archivo grande cifrado exitosamente con Vigenère (magic header: {})",
                            addHeader ? "agregado" : "omitido");
                }
            });
        } else {
            logger.info("Archivo pequeño ({} MB), usando endpoint estándar", fileSize / (1024.0 * 1024.0));

            // Crear form data
            Map<String, String> formData = new HashMap<>();
            formData.put("clave", clave);

            // Realizar petición asíncrona multipart con endpoint estándar
            return apiClient.postMultipartAsync(
                    BASE_ENDPOINT + "/cifrar/file",
                    archivo,
                    formData,
                    VigenereCifradoResponse.class
            ).whenComplete((response, error) -> {
                if (error != null) {
                    logger.warn("Error al cifrar archivo con Vigenère: {}", error.getMessage());
                } else {
                    logger.info("Archivo cifrado exitosamente con Vigenère");
                }
            });
        }
    }

    /**
     * Crea una instancia de VigenereCifradoResponse a partir de los valores.
     * Helper method para convertir la respuesta del endpoint large.
     */
    private VigenereCifradoResponse createCifradoResponse(String textoCifrado, String claveUsada) {
        VigenereCifradoResponse response = new VigenereCifradoResponse();
        response.setTextoCifrado(textoCifrado);
        response.setClaveUsada(claveUsada);
        return response;
    }

    /**
     * Descifra un archivo cifrado con Vigenère de forma asíncrona.
     * Detecta automáticamente el tamaño y usa el endpoint /large si es necesario.
     *
     * @param archivoCifrado el archivo cifrado a descifrar
     * @param clave la clave para el descifrado
     * @return CompletableFuture con la respuesta del descifrado
     */
    public CompletableFuture<VigenereDescifradoResponse> descifrarArchivo(File archivoCifrado, String clave) {
        return descifrarArchivo(archivoCifrado, clave, "MAGICV1\n", false);
    }

    /**
     * Descifra un archivo cifrado con Vigenère de forma asíncrona con parámetros del canary check.
     * Detecta automáticamente el tamaño y usa el endpoint /large si es necesario.
     *
     * @param archivoCifrado el archivo cifrado a descifrar
     * @param clave la clave para el descifrado
     * @param magicHeader el header mágico esperado al inicio del archivo descifrado
     * @param skipCanary si se debe omitir el canary check
     * @return CompletableFuture con la respuesta del descifrado
     */
    public CompletableFuture<VigenereDescifradoResponse> descifrarArchivo(
            File archivoCifrado,
            String clave,
            String magicHeader,
            boolean skipCanary
    ) {
        logger.debug("Descifrando archivo con Vigenère - Archivo: {}, Tamaño: {} bytes",
                archivoCifrado.getName(), archivoCifrado.length());

        // Validaciones básicas
        if (!archivoCifrado.exists()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("El archivo no existe")
            );
        }

        if (clave.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("La clave no puede estar vacía")
            );
        }

        // Detectar si es un archivo grande
        long fileSize = archivoCifrado.length();
        boolean isLargeFile = fileSize >= LARGE_FILE_THRESHOLD;

        if (isLargeFile) {
            logger.info("Archivo grande detectado ({} MB), usando endpoint /large con canary check",
                    fileSize / (1024.0 * 1024.0));

            // Crear form data con parámetros adicionales para el endpoint large
            Map<String, String> formData = new HashMap<>();
            formData.put("clave", clave);
            formData.put("magic_header", magicHeader);
            formData.put("skip_canary", String.valueOf(skipCanary));

            // Guardar la respuesta JSON directamente a disco para no cargar cientos de MB en el heap
            String nombreOriginal = archivoCifrado.getName();
            int punto = nombreOriginal.lastIndexOf('.');
            String nombreSinExtension = punto > 0 ? nombreOriginal.substring(0, punto) : nombreOriginal;
            File archivoJsonTemp = new File(archivoCifrado.getParentFile(), nombreSinExtension + "_response.json");
            File archivoTextoFinal = new File(archivoCifrado.getParentFile(), nombreSinExtension + "_descifrado.txt");

            return apiClient.postMultipartStreamingToFileAsync(
                    BASE_ENDPOINT + "/descifrar/file/large",
                    archivoCifrado,
                    formData,
                    archivoJsonTemp
            ).thenApply(jsonFile -> {
                try {
                    String claveUsada = extraerCampoTextoAArchivo(jsonFile, "texto_descifrado", archivoTextoFinal);
                    VigenereDescifradoResponse response = createDescifradoResponse(
                            "[ARCHIVO GRANDE - Guardado en: " + archivoTextoFinal.getAbsolutePath() + "]", claveUsada);
                    response.setArchivoSalida(archivoTextoFinal);
                    return response;
                } catch (Exception e) {
                    logger.error("Error al procesar archivo de respuesta: {}", e.getMessage());
                    throw new RuntimeException("Error al procesar respuesta: " + e.getMessage(), e);
                }
            }).whenComplete((response, error) -> {
                if (error != null) {
                    logger.warn("Error al descifrar archivo grande con Vigenère: {}", error.getMessage());
                } else {
                    logger.info("Archivo grande descifrado exitosamente con Vigenère (canary: {})",
                            skipCanary ? "skipped" : "passed");
                }
            });
        } else {
            logger.info("Archivo pequeño ({} MB), usando endpoint estándar", fileSize / (1024.0 * 1024.0));

            // Crear form data
            Map<String, String> formData = new HashMap<>();
            formData.put("clave", clave);

            // Realizar petición asíncrona multipart con endpoint estándar
            return apiClient.postMultipartAsync(
                    BASE_ENDPOINT + "/descifrar/file",
                    archivoCifrado,
                    formData,
                    VigenereDescifradoResponse.class
            ).whenComplete((response, error) -> {
                if (error != null) {
                    logger.warn("Error al descifrar archivo con Vigenère: {}", error.getMessage());
                } else {
                    logger.info("Archivo descifrado exitosamente con Vigenère");
                }
            });
        }
    }

    /**
     * Crea una instancia de VigenereDescifradoResponse a partir de los valores.
     * Helper method para convertir la respuesta del endpoint large.
     */
    private VigenereDescifradoResponse createDescifradoResponse(String textoDescifrado, String claveUsada) {
        VigenereDescifradoResponse response = new VigenereDescifradoResponse();
        response.setTextoDescifrado(textoDescifrado);
        response.setClaveUsada(claveUsada);
        return response;
    }

    /**
     * Extrae un campo de texto del JSON de respuesta y lo escribe en un archivo, carácter a carácter,
     * sin cargar nunca el valor completo en memoria (el JSON puede venir en una sola línea de cientos de MB).
     *
     * El JSON tiene esta estructura: {"texto_cifrado": "TEXTO_GIGANTE...", "clave_usada": "CLAVE", ...}
     *
     * @param jsonFile archivo JSON que contiene la respuesta (será eliminado después)
     * @param campoTexto nombre del campo cuyo valor se vuelca al archivo ("texto_cifrado" o "texto_descifrado")
     * @param outputTextFile archivo donde guardar solo el texto
     * @return el valor de clave_usada ("DESCONOCIDA" si no aparece)
     */
    private String extraerCampoTextoAArchivo(File jsonFile, String campoTexto, File outputTextFile) throws Exception {
        String claveUsada = "DESCONOCIDA";

        try (java.io.Reader reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                     new java.io.FileInputStream(jsonFile), java.nio.charset.StandardCharsets.UTF_8), 65536);
             java.io.Writer writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(
                     new java.io.FileOutputStream(outputTextFile), java.nio.charset.StandardCharsets.UTF_8), 65536)) {

            int depth = 0;
            int c;
            while ((c = reader.read()) != -1) {
                if (c == '{' || c == '[') {
                    depth++;
                } else if (c == '}' || c == ']') {
                    depth--;
                } else if (c == '"' && depth == 1) {
                    // Clave (o valor string) del objeto raíz
                    String clave = leerStringJson(reader, null);
                    c = saltarEspacios(reader);
                    if (c != ':') {
                        continue;
                    }
                    c = saltarEspacios(reader);
                    if (c != '"') {
                        // Valor no string (número, objeto...): lo procesa el bucle principal
                        if (c == '{' || c == '[') {
                            depth++;
                        }
                        continue;
                    }
                    if (campoTexto.equals(clave)) {
                        leerStringJson(reader, writer);
                        logger.info("Texto extraído a: {}", outputTextFile.getAbsolutePath());
                    } else {
                        String valor = leerStringJson(reader, null);
                        if ("clave_usada".equals(clave)) {
                            claveUsada = valor;
                            logger.debug("Clave usada extraída: {}", claveUsada);
                        }
                    }
                }
            }
        }

        // Eliminar archivo JSON temporal (ya no se necesita)
        if (jsonFile.delete()) {
            logger.debug("Archivo JSON temporal eliminado: {}", jsonFile.getName());
        }

        return claveUsada;
    }

    /** Avanza hasta el primer carácter que no sea espacio en blanco y lo devuelve (-1 si fin). */
    private int saltarEspacios(java.io.Reader reader) throws java.io.IOException {
        int c;
        do {
            c = reader.read();
        } while (c != -1 && Character.isWhitespace(c));
        return c;
    }

    /**
     * Lee el contenido de un string JSON (la comilla de apertura ya está consumida) hasta la comilla
     * de cierre, decodificando los escapes. Si {@code destino} no es null, escribe ahí los caracteres
     * y devuelve null; si es null, acumula y devuelve el valor (solo para valores pequeños).
     */
    private String leerStringJson(java.io.Reader reader, java.io.Writer destino) throws java.io.IOException {
        StringBuilder acumulado = destino == null ? new StringBuilder() : null;
        int c;
        while ((c = reader.read()) != -1 && c != '"') {
            if (c == '\\') {
                int e = reader.read();
                switch (e) {
                    case 'n' -> c = '\n';
                    case 'r' -> c = '\r';
                    case 't' -> c = '\t';
                    case 'b' -> c = '\b';
                    case 'f' -> c = '\f';
                    case 'u' -> {
                        char[] hex = new char[4];
                        for (int i = 0; i < 4; i++) {
                            hex[i] = (char) reader.read();
                        }
                        c = Integer.parseInt(new String(hex), 16);
                    }
                    default -> c = e; // \" \\ \/
                }
            }
            if (destino != null) {
                destino.write(c);
            } else {
                acumulado.append((char) c);
            }
        }
        return acumulado == null ? null : acumulado.toString();
    }

    /**
     * Verifica la conectividad con el backend.
     *
     * @return CompletableFuture que retorna true si la conexión es exitosa, false en caso contrario
     */
    public CompletableFuture<Boolean> verificarConexion() {
        logger.debug("Verificando conexión con la API");

        return apiClient.getAsync("/health", Object.class)
                .thenApply(response -> {
                    logger.info("Conexión con API verificada exitosamente");
                    return true;
                })
                .exceptionally(error -> {
                    logger.error("Error al verificar conexión con la API", error);
                    return false;
                });
    }
}

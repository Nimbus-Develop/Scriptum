package es.luna.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Cliente HTTP centralizado para comunicarse con la API de Scriptum.
 * Utiliza el HttpClient nativo de Java 11+ y Gson para el manejo de JSON.
 *
 * @author Arantxa
 * @version 1.0
 * @since 2025-11-11
 */
public class ApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);

    /** URL base de la API */
    private final String baseUrl;

    /** Cliente HTTP reutilizable */
    private final HttpClient httpClient;

    /** Instancia de Gson para serialización/deserialización JSON */
    private final Gson gson;

    /** Timeout por defecto para las peticiones (30 segundos) */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /** Timeout extendido para archivos grandes (5 minutos) */
    private static final Duration LARGE_FILE_TIMEOUT = Duration.ofMinutes(5);

    /**
     * Constructor que crea un cliente HTTP con configuración por defecto.
     *
     * @param baseUrl la URL base de la API
     */
    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .version(HttpClient.Version.HTTP_1_1)  // Forzar HTTP/1.1, no HTTP/2
                .build();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        logger.info("ApiClient inicializado con baseUrl: {}", this.baseUrl);
    }

    /**
     * Realiza una petición POST de forma asíncrona.
     *
     * @param endpoint el endpoint a llamar (ej: "/vigenere/cifrar-texto")
     * @param requestBody el objeto request a enviar (será serializado a JSON)
     * @param responseClass la clase del objeto response esperado
     * @param <T> el tipo del response
     * @return CompletableFuture con el objeto response deserializado
     */
    public <T> CompletableFuture<T> postAsync(String endpoint, Object requestBody, Class<T> responseClass) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Serializar el request body a JSON
                String jsonBody = gson.toJson(requestBody);

                logger.debug("POST {} - Body size: {} bytes", endpoint, jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);

                // Construir la petición HTTP
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + endpoint))
                        .timeout(DEFAULT_TIMEOUT)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, java.nio.charset.StandardCharsets.UTF_8))
                        .build();

                logger.debug("POST {} - Sending request to: {}", endpoint, request.uri());

                // Enviar la petición
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                logger.debug("POST {} - Status: {}", endpoint, response.statusCode());

                // Manejar respuestas de error HTTP
                if (response.statusCode() >= 400) {
                    handleErrorResponse(response);
                }

                // Deserializar el response body
                T responseObject = gson.fromJson(response.body(), responseClass);
                logger.info("POST {} - Success", endpoint);
                return responseObject;

            } catch (ApiException e) {
                // ApiException ya fue registrada en handleErrorResponse, solo relanzar
                throw e;
            } catch (JsonSyntaxException e) {
                logger.error("Error al parsear JSON en POST {}: {}", endpoint, e.getMessage());
                throw new ApiException("Error al parsear la respuesta JSON: " + e.getMessage(), e);
            } catch (IOException e) {
                // Registrar solo el tipo de error, no el stack trace completo
                logger.error("Error de I/O en POST {}: {}", endpoint, e.getClass().getSimpleName());

                // Determinar el tipo de error de I/O
                String tipoError = e.getClass().getSimpleName();
                String mensajeError;

                if (tipoError.contains("UnknownHost") || tipoError.contains("NoRouteToHost")) {
                    mensajeError = "No se puede conectar al servidor. Verifica tu conexión a internet.";
                } else if (tipoError.contains("ConnectException") || tipoError.contains("SocketTimeout")) {
                    mensajeError = "Error de conexión con el servidor. Verifica tu conexión a internet o que el servidor esté disponible.";
                } else if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                    mensajeError = "Error de red: " + e.getMessage();
                } else {
                    mensajeError = "Error de conexión. Verifica tu conexión a internet.";
                }

                throw new ApiException(mensajeError, e);
            } catch (Exception e) {
                logger.error("Error inesperado en petición POST {}: {}", endpoint, e.getMessage());
                String mensaje = e.getMessage() != null ? e.getMessage() : "Error desconocido en la petición";
                throw new ApiException("Error en la petición HTTP: " + mensaje, e);
            }
        });
    }

    /**
     * Realiza una petición GET de forma asíncrona.
     *
     * @param endpoint el endpoint a llamar
     * @param responseClass la clase del objeto response esperado
     * @param <T> el tipo del response
     * @return CompletableFuture con el objeto response deserializado
     */
    public <T> CompletableFuture<T> getAsync(String endpoint, Class<T> responseClass) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("GET {}", endpoint);

                // Construir la petición HTTP
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + endpoint))
                        .timeout(DEFAULT_TIMEOUT)
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                // Enviar la petición
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                logger.debug("GET {} - Status: {}", endpoint, response.statusCode());

                // Manejar respuestas de error HTTP
                if (response.statusCode() >= 400) {
                    handleErrorResponse(response);
                }

                // Deserializar el response body
                T responseObject = gson.fromJson(response.body(), responseClass);
                logger.info("GET {} - Success", endpoint);
                return responseObject;

            } catch (ApiException e) {
                // ApiException ya fue registrada en handleErrorResponse, solo relanzar
                throw e;
            } catch (JsonSyntaxException e) {
                logger.error("Error al parsear JSON en GET {}: {}", endpoint, e.getMessage());
                throw new ApiException("Error al parsear la respuesta JSON: " + e.getMessage(), e);
            } catch (IOException e) {
                // Registrar solo el tipo de error, no el stack trace completo
                logger.error("Error de I/O en GET {}: {}", endpoint, e.getClass().getSimpleName());

                // Determinar el tipo de error de I/O
                String tipoError = e.getClass().getSimpleName();
                String mensajeError;

                if (tipoError.contains("UnknownHost") || tipoError.contains("NoRouteToHost")) {
                    mensajeError = "No se puede conectar al servidor. Verifica tu conexión a internet.";
                } else if (tipoError.contains("ConnectException") || tipoError.contains("SocketTimeout")) {
                    mensajeError = "Error de conexión con el servidor. Verifica tu conexión a internet o que el servidor esté disponible.";
                } else if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                    mensajeError = "Error de red: " + e.getMessage();
                } else {
                    mensajeError = "Error de conexión. Verifica tu conexión a internet.";
                }

                throw new ApiException(mensajeError, e);
            } catch (Exception e) {
                logger.error("Error inesperado en petición GET {}: {}", endpoint, e.getMessage());
                String mensaje = e.getMessage() != null ? e.getMessage() : "Error desconocido en la petición";
                throw new ApiException("Error en la petición HTTP: " + mensaje, e);
            }
        });
    }

    /**
     * Maneja las respuestas de error HTTP, intentando extraer el mensaje de error de la API.
     *
     * @param response la respuesta HTTP con error
     * @throws ApiException con el mensaje de error extraído
     */
    private void handleErrorResponse(HttpResponse<String> response) throws ApiException {
        try {
            int statusCode = response.statusCode();
            String responseBody = response.body();
            String errorMsg = null;

            // Intentar parsear como objeto con estructura FastAPI: {"detail": {"error": "..."}}
            try {
                com.google.gson.JsonObject jsonObject = gson.fromJson(responseBody, com.google.gson.JsonObject.class);

                // Caso 1: {"detail": {"error": "mensaje"}}
                if (jsonObject.has("detail")) {
                    com.google.gson.JsonElement detailElement = jsonObject.get("detail");
                    if (detailElement.isJsonObject()) {
                        com.google.gson.JsonObject detailObj = detailElement.getAsJsonObject();
                        if (detailObj.has("error")) {
                            errorMsg = detailObj.get("error").getAsString();
                        }
                    } else if (detailElement.isJsonPrimitive()) {
                        // Caso 2: {"detail": "mensaje"}
                        errorMsg = detailElement.getAsString();
                    }
                }

                // Caso 3: {"error": "mensaje", "detalle": "..."}
                if (errorMsg == null && jsonObject.has("error")) {
                    errorMsg = jsonObject.get("error").getAsString();
                    if (jsonObject.has("detalle")) {
                        String detalle = jsonObject.get("detalle").getAsString();
                        if (detalle != null && !detalle.isEmpty()) {
                            errorMsg += " - " + detalle;
                        }
                    }
                }

            } catch (Exception e) {
                logger.debug("No se pudo parsear como JSON estructurado, usando cuerpo completo");
            }

            // Si no se pudo extraer mensaje, usar el cuerpo completo
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "Error HTTP " + statusCode + ": " + responseBody;
            }

            // Diferenciar entre errores de cliente (4xx) y servidor (5xx)
            if (statusCode >= 400 && statusCode < 500) {
                // Errores 4xx son de validación/cliente - WARN sin stack trace
                logger.warn("Error de validación de API ({}): {}", statusCode, errorMsg);
            } else {
                // Errores 5xx son del servidor - ERROR
                logger.error("Error del servidor API ({}): {}", statusCode, errorMsg);
            }

            throw new ApiException(errorMsg);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            // Si falla lo demás, lanzar error genérico
            String errorMsg = "Error HTTP " + response.statusCode() + ": " + response.body();
            logger.error(errorMsg);
            throw new ApiException(errorMsg);
        }
    }

    /**
     * Realiza una petición POST multipart/form-data de forma asíncrona.
     * Útil para enviar archivos al servidor.
     *
     * @param endpoint el endpoint a llamar (ej: "/vigenere/cifrar/file")
     * @param file el archivo a enviar
     * @param formData campos adicionales del formulario (ej: clave, password, etc.)
     * @param responseClass la clase del objeto response esperado
     * @param <T> el tipo del response
     * @return CompletableFuture con el objeto response deserializado
     */
    public <T> CompletableFuture<T> postMultipartAsync(
            String endpoint,
            File file,
            Map<String, String> formData,
            Class<T> responseClass
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Generar boundary único para multipart
                String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().replace("-", "");

                // Leer contenido del archivo como bytes
                byte[] fileBytes = Files.readAllBytes(file.toPath());

                // Construir el body multipart manualmente con bytes
                java.io.ByteArrayOutputStream bodyStream = new java.io.ByteArrayOutputStream();

                // Agregar campos del formulario
                if (formData != null) {
                    for (Map.Entry<String, String> entry : formData.entrySet()) {
                        bodyStream.write(("--" + boundary + "\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        bodyStream.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        bodyStream.write((entry.getValue() + "\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                }

                // Agregar archivo (con Content-Type correcto para binarios)
                bodyStream.write(("--" + boundary + "\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                bodyStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                bodyStream.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));

                // Escribir los bytes del archivo directamente
                bodyStream.write(fileBytes);
                bodyStream.write("\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));

                // Cerrar boundary
                bodyStream.write(("--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));

                byte[] bodyBytes = bodyStream.toByteArray();

                logger.debug("POST multipart {} - Body size: {} bytes (file: {} bytes)", endpoint, bodyBytes.length, fileBytes.length);

                // Determinar timeout según el endpoint y tamaño del archivo
                Duration timeout = DEFAULT_TIMEOUT;
                if (endpoint.contains("/large") || fileBytes.length > 10 * 1024 * 1024) {
                    timeout = LARGE_FILE_TIMEOUT;
                    logger.info("Usando timeout extendido ({} minutos) para archivo grande", timeout.toMinutes());
                }

                // Construir la petición HTTP con bytes
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + endpoint))
                        .timeout(timeout)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                        .build();

                logger.debug("POST multipart {} - Sending request to: {}", endpoint, request.uri());

                // Enviar la petición
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                logger.debug("POST multipart {} - Status: {}", endpoint, response.statusCode());

                // Manejar respuestas de error HTTP
                if (response.statusCode() >= 400) {
                    handleErrorResponse(response);
                }

                // Deserializar el response body
                T responseObject = gson.fromJson(response.body(), responseClass);
                logger.info("POST multipart {} - Success", endpoint);
                return responseObject;

            } catch (ApiException e) {
                // ApiException ya fue registrada en handleErrorResponse, solo relanzar
                throw e;
            } catch (JsonSyntaxException e) {
                logger.error("Error al parsear JSON en POST multipart {}: {}", endpoint, e.getMessage());
                throw new ApiException("Error al parsear la respuesta JSON: " + e.getMessage(), e);
            } catch (IOException e) {
                // Registrar solo el tipo de error, no el stack trace completo
                logger.error("Error de I/O en POST multipart {}: {}", endpoint, e.getClass().getSimpleName());

                // Determinar el tipo de error de I/O
                String tipoError = e.getClass().getSimpleName();
                String mensajeError;

                if (tipoError.contains("UnknownHost") || tipoError.contains("NoRouteToHost")) {
                    mensajeError = "No se puede conectar al servidor. Verifica tu conexión a internet.";
                } else if (tipoError.contains("ConnectException") || tipoError.contains("SocketTimeout")) {
                    mensajeError = "Error de conexión con el servidor. Verifica tu conexión a internet o que el servidor esté disponible.";
                } else if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                    mensajeError = "Error de red: " + e.getMessage();
                } else {
                    mensajeError = "Error de conexión. Verifica tu conexión a internet.";
                }

                throw new ApiException(mensajeError, e);
            } catch (Exception e) {
                logger.error("Error inesperado en petición POST multipart {}: {}", endpoint, e.getMessage());
                String mensaje = e.getMessage() != null ? e.getMessage() : "Error desconocido en la petición";
                throw new ApiException("Error en la petición HTTP: " + mensaje, e);
            }
        });
    }

    /**
     * Realiza una petición POST multipart/form-data con streaming para archivos grandes.
     * Este método usa BodyPublishers.ofInputStream para evitar cargar el archivo completo en memoria.
     *
     * @param endpoint el endpoint a llamar
     * @param file el archivo a enviar
     * @param formData campos adicionales del formulario
     * @param responseClass la clase del objeto response esperado
     * @param <T> el tipo del response
     * @return CompletableFuture con el objeto response deserializado
     */
    public <T> CompletableFuture<T> postMultipartStreamingAsync(
            String endpoint,
            File file,
            Map<String, String> formData,
            Class<T> responseClass
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Generar boundary único para multipart
                String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().replace("-", "");

                // Crear archivo temporal con el multipart completo
                java.io.File tempFile = java.io.File.createTempFile("multipart_", ".tmp");
                tempFile.deleteOnExit();

                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
                     java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(fos)) {

                    // Agregar campos del formulario
                    if (formData != null) {
                        for (Map.Entry<String, String> entry : formData.entrySet()) {
                            bos.write(("--" + boundary + "\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            bos.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n")
                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            bos.write((entry.getValue() + "\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        }
                    }

                    // Agregar archivo (header)
                    bos.write(("--" + boundary + "\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    bos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n")
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    bos.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));

                    // Copiar archivo en bloques (streaming)
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
                         java.io.BufferedInputStream bis = new java.io.BufferedInputStream(fis)) {
                        byte[] buffer = new byte[8192]; // 8KB buffer
                        int bytesRead;
                        while ((bytesRead = bis.read(buffer)) != -1) {
                            bos.write(buffer, 0, bytesRead);
                        }
                    }

                    // Cerrar boundary
                    bos.write("\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    bos.write(("--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }

                long fileSize = tempFile.length();
                logger.info("POST multipart streaming {} - Archivo temporal creado: {} bytes", endpoint, fileSize);

                // Construir la petición HTTP con streaming
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + endpoint))
                        .timeout(LARGE_FILE_TIMEOUT)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofFile(tempFile.toPath()))
                        .build();

                logger.debug("POST multipart streaming {} - Enviando request a: {}", endpoint, request.uri());

                // Enviar la petición
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                logger.debug("POST multipart streaming {} - Status: {}", endpoint, response.statusCode());

                // Manejar respuestas de error HTTP
                if (response.statusCode() >= 400) {
                    handleErrorResponse(response);
                }

                // Deserializar el response body
                T responseObject = gson.fromJson(response.body(), responseClass);
                logger.info("POST multipart streaming {} - Success", endpoint);
                return responseObject;

            } catch (ApiException e) {
                throw e;
            } catch (JsonSyntaxException e) {
                logger.error("Error al parsear JSON en POST multipart streaming {}: {}", endpoint, e.getMessage());
                throw new ApiException("Error al parsear la respuesta JSON: " + e.getMessage(), e);
            } catch (IOException e) {
                logger.error("Error de I/O en POST multipart streaming {}: {}", endpoint, e.getClass().getSimpleName());

                String tipoError = e.getClass().getSimpleName();
                String mensajeError;

                if (tipoError.contains("UnknownHost") || tipoError.contains("NoRouteToHost")) {
                    mensajeError = "No se puede conectar al servidor. Verifica tu conexión a internet.";
                } else if (tipoError.contains("ConnectException") || tipoError.contains("SocketTimeout")) {
                    mensajeError = "Error de conexión con el servidor. Verifica tu conexión a internet o que el servidor esté disponible.";
                } else if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                    mensajeError = "Error de red: " + e.getMessage();
                } else {
                    mensajeError = "Error de conexión. Verifica tu conexión a internet.";
                }

                throw new ApiException(mensajeError, e);
            } catch (Exception e) {
                logger.error("Error inesperado en petición POST multipart streaming {}: {}", endpoint, e.getMessage());
                String mensaje = e.getMessage() != null ? e.getMessage() : "Error desconocido en la petición";
                throw new ApiException("Error en la petición HTTP: " + mensaje, e);
            }
        });
    }

    /**
     * Realiza una petición POST multipart con streaming y guarda la respuesta JSON directamente a archivo.
     * Evita cargar respuestas grandes en memoria.
     *
     * @param endpoint el endpoint a llamar
     * @param file el archivo a enviar
     * @param formData campos adicionales del formulario
     * @param outputFile archivo donde guardar la respuesta JSON
     * @return CompletableFuture que se completa cuando el archivo se ha guardado
     */
    public CompletableFuture<File> postMultipartStreamingToFileAsync(
            String endpoint,
            File file,
            Map<String, String> formData,
            File outputFile
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Generar boundary único para multipart
                String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().replace("-", "");

                // Crear archivo temporal con el multipart completo
                java.io.File tempFile = java.io.File.createTempFile("multipart_", ".tmp");
                tempFile.deleteOnExit();

                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
                     java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(fos)) {

                    // Agregar campos del formulario
                    if (formData != null) {
                        for (Map.Entry<String, String> entry : formData.entrySet()) {
                            bos.write(("--" + boundary + "\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            bos.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n")
                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            bos.write((entry.getValue() + "\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        }
                    }

                    // Agregar archivo (header)
                    bos.write(("--" + boundary + "\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    bos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n")
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    bos.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));

                    // Copiar archivo en bloques (streaming)
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
                         java.io.BufferedInputStream bis = new java.io.BufferedInputStream(fis)) {
                        byte[] buffer = new byte[8192]; // 8KB buffer
                        int bytesRead;
                        while ((bytesRead = bis.read(buffer)) != -1) {
                            bos.write(buffer, 0, bytesRead);
                        }
                    }

                    // Cerrar boundary
                    bos.write("\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    bos.write(("--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }

                long fileSize = tempFile.length();
                logger.info("POST multipart streaming to file {} - Request preparado: {} bytes", endpoint, fileSize);

                // Construir la petición HTTP con streaming
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + endpoint))
                        .timeout(LARGE_FILE_TIMEOUT)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofFile(tempFile.toPath()))
                        .build();

                logger.info("POST multipart streaming to file {} - Enviando y guardando respuesta en: {}",
                        endpoint, outputFile.getAbsolutePath());

                // Enviar la petición y guardar respuesta directamente a archivo
                HttpResponse<java.nio.file.Path> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofFile(outputFile.toPath())
                );

                logger.debug("POST multipart streaming to file {} - Status: {}", endpoint, response.statusCode());

                // Manejar respuestas de error HTTP
                if (response.statusCode() >= 400) {
                    // Leer el archivo de error para obtener el mensaje
                    String errorBody = Files.readString(outputFile.toPath());
                    outputFile.delete(); // Eliminar archivo de error

                    // Crear una respuesta simulada para handleErrorResponse
                    HttpResponse<String> errorResponse = new HttpResponse<String>() {
                        public int statusCode() { return response.statusCode(); }
                        public String body() { return errorBody; }
                        public HttpRequest request() { return response.request(); }
                        public java.net.http.HttpHeaders headers() { return response.headers(); }
                        public java.util.Optional<HttpResponse<String>> previousResponse() { return java.util.Optional.empty(); }
                        public java.util.Optional<javax.net.ssl.SSLSession> sslSession() { return response.sslSession(); }
                        public java.net.URI uri() { return response.uri(); }
                        public HttpClient.Version version() { return response.version(); }
                    };

                    handleErrorResponse(errorResponse);
                }

                logger.info("POST multipart streaming to file {} - Guardado exitosamente: {} bytes",
                        endpoint, outputFile.length());
                return outputFile;

            } catch (ApiException e) {
                throw e;
            } catch (IOException e) {
                logger.error("Error de I/O en POST multipart streaming to file {}: {}", endpoint, e.getClass().getSimpleName());

                String tipoError = e.getClass().getSimpleName();
                String mensajeError;

                if (tipoError.contains("UnknownHost") || tipoError.contains("NoRouteToHost")) {
                    mensajeError = "No se puede conectar al servidor. Verifica tu conexión a internet.";
                } else if (tipoError.contains("ConnectException") || tipoError.contains("SocketTimeout")) {
                    mensajeError = "Error de conexión con el servidor. Verifica tu conexión a internet o que el servidor esté disponible.";
                } else if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                    mensajeError = "Error de red: " + e.getMessage();
                } else {
                    mensajeError = "Error de conexión. Verifica tu conexión a internet.";
                }

                throw new ApiException(mensajeError, e);
            } catch (Exception e) {
                logger.error("Error inesperado en petición POST multipart streaming to file {}: {}", endpoint, e.getMessage());
                String mensaje = e.getMessage() != null ? e.getMessage() : "Error desconocido en la petición";
                throw new ApiException("Error en la petición HTTP: " + mensaje, e);
            }
        });
    }

    /**
     * Realiza una petición POST con form data y obtiene respuesta binaria en base64.
     * Usado para endpoints que devuelven archivos binarios directamente.
     *
     * @param endpoint el endpoint (ej: "/aes/descifrar/file")
     * @param formData mapa con los campos del formulario
     * @return CompletableFuture con el contenido binario en base64
     */
    public CompletableFuture<String> postFormAsync(String endpoint, Map<String, String> formData) {
        return CompletableFuture.supplyAsync(() -> {
            String url = baseUrl + endpoint;
            logger.debug("POST form request to: {}", url);

            try {
                // Construir el body con application/x-www-form-urlencoded
                StringBuilder bodyBuilder = new StringBuilder();
                for (Map.Entry<String, String> entry : formData.entrySet()) {
                    if (!bodyBuilder.isEmpty()) {
                        bodyBuilder.append("&");
                    }
                    bodyBuilder.append(java.net.URLEncoder.encode(entry.getKey(), java.nio.charset.StandardCharsets.UTF_8));
                    bodyBuilder.append("=");
                    bodyBuilder.append(java.net.URLEncoder.encode(entry.getValue(), java.nio.charset.StandardCharsets.UTF_8));
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(bodyBuilder.toString()))
                        .build();

                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

                logger.debug("Response status: {}", response.statusCode());

                // Verificar código de respuesta
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    // Convertir la respuesta binaria a base64
                    String base64Content = java.util.Base64.getEncoder().encodeToString(response.body());
                    logger.info("Binary response received successfully, size: {} bytes", response.body().length);
                    return base64Content;
                } else {
                    String errorBody = new String(response.body(), java.nio.charset.StandardCharsets.UTF_8);
                    logger.error("Error response ({}): {}", response.statusCode(), errorBody);
                    throw new ApiException("HTTP " + response.statusCode() + ": " + errorBody);
                }
            } catch (ApiException e) {
                // ApiException ya tiene un mensaje apropiado, solo relanzar
                throw e;
            } catch (IOException e) {
                // Registrar solo el tipo de error, no el stack trace completo
                logger.error("Error de I/O en POST form request: {}", e.getClass().getSimpleName());

                // Determinar el tipo de error de I/O
                String tipoError = e.getClass().getSimpleName();
                String mensajeError;

                if (tipoError.contains("UnknownHost") || tipoError.contains("NoRouteToHost")) {
                    mensajeError = "No se puede conectar al servidor. Verifica tu conexión a internet.";
                } else if (tipoError.contains("ConnectException") || tipoError.contains("SocketTimeout")) {
                    mensajeError = "Error de conexión con el servidor. Verifica tu conexión a internet o que el servidor esté disponible.";
                } else if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                    mensajeError = "Error de red: " + e.getMessage();
                } else {
                    mensajeError = "Error de conexión. Verifica tu conexión a internet.";
                }

                throw new ApiException(mensajeError, e);
            } catch (Exception e) {
                // Registrar solo el mensaje de error, no el stack trace completo
                logger.error("Error inesperado en POST form request: {}", e.getMessage());
                String mensaje = e.getMessage() != null ? e.getMessage() : "Error desconocido en la petición";
                throw new ApiException("Error en petición POST form: " + mensaje, e);
            }
        });
    }

    /**
     * Excepción personalizada para errores de la API.
     */
    public static class ApiException extends RuntimeException {
        
        /**
         * Constructor con mensaje de error.
         *
         * @param message el mensaje descriptivo del error
         */
        public ApiException(String message) {
            super(message);
        }

        /**
         * Constructor con mensaje de error y causa.
         *
         * @param message el mensaje descriptivo del error
         * @param cause la causa raíz de la excepción
         */
        public ApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

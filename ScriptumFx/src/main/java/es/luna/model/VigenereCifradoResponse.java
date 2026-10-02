package es.luna.model;

import com.google.gson.annotations.SerializedName;

import java.io.File;

/**
 * Response DTO para operación de cifrado Vigenère.
 * Corresponde al schema CifradoResponse de la API.
 *
 * @author Arantxa
 * @version 1.0
 * @since 2025-11-11
 */
public class VigenereCifradoResponse {

    /** Texto cifrado resultante */
    @SerializedName("texto_cifrado")
    private String textoCifrado;

    /** Clave utilizada (formateada) */
    @SerializedName("clave_usada")
    private String claveUsada;

    /** Longitud del texto original */
    @SerializedName("texto_original_length")
    private int textoOriginalLength;

    /**
     * Constructor vacío.
     */
    /** Archivo con el resultado cuando es demasiado grande para mantenerlo en memoria (no se serializa). */
    private transient File archivoSalida;

    public VigenereCifradoResponse() {
    }

    // Getters y Setters

    /**
     * Obtiene el texto cifrado resultante.
     *
     * @return el texto cifrado
     */
    public String getTextoCifrado() {
        return textoCifrado;
    }

    /**
     * Establece el texto cifrado.
     *
     * @param textoCifrado el texto cifrado
     */
    public void setTextoCifrado(String textoCifrado) {
        this.textoCifrado = textoCifrado;
    }

    /**
     * Obtiene la clave utilizada (formateada) en el cifrado.
     *
     * @return la clave usada
     */
    public String getClaveUsada() {
        return claveUsada;
    }

    /**
     * Establece la clave utilizada.
     *
     * @param claveUsada la clave usada
     */
    public void setClaveUsada(String claveUsada) {
        this.claveUsada = claveUsada;
    }

    /**
     * Obtiene la longitud del texto original antes del cifrado.
     *
     * @return la longitud del texto original
     */
    public int getTextoOriginalLength() {
        return textoOriginalLength;
    }

    /**
     * Establece la longitud del texto original.
     *
     * @param textoOriginalLength la longitud del texto original
     */
    public void setTextoOriginalLength(int textoOriginalLength) {
        this.textoOriginalLength = textoOriginalLength;
    }

    public File getArchivoSalida() {
        return archivoSalida;
    }

    public void setArchivoSalida(File archivoSalida) {
        this.archivoSalida = archivoSalida;
    }
}

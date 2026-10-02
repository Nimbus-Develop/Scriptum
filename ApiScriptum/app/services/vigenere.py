"""
SERVICIOS VIGENÈRE
SCRIPTUM - Servicio de cifrado Vigenère
Autoras: Arantxa - Wara

Este módulo proporciona funcionalidades para cifrar y descifrar texto
utilizando el algoritmo de cifrado Vigenère.

Arquitectura del módulo:
- Capa 1: Funciones puras de cifrado/descifrado (sin efectos secundarios)
- Capa 2: Validaciones y sanitización de entradas
- Capa 3: Utilidades para manejo de archivos (opcional)
"""
import logging
import os
import re
import unicodedata

# ============================================================================
# LOGGING
# ============================================================================
logger = logging.getLogger(__name__)

# ============================================================================
# ALFABETO EXTENDIDO (archivos grandes)
# ============================================================================
# Alfabeto sobre el que opera el Vigenère de archivos grandes. Cada carácter
# se desplaza dentro de esta lista (mod N), así que ningún carácter se pierde
# ni se sustituye por otro. Las letras A-Z van primero para que la clave
# (solo letras) conserve sus posiciones habituales 0-25.
ALFABETO = (
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "abcdefghijklmnopqrstuvwxyz"
    "0123456789"
    " \t\n\r\f"                              # Espacios y saltos
    "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"     # Puntuación ASCII
    "ÁÉÍÓÚÜÑáéíóúüñ"                         # Tildes y eñes
    "¿¡«»ªº°©®"                              # Símbolos del español
    "—–•…‘’“”€"                              # Puntuación tipográfica
)
INDICE_ALFABETO = {caracter: i for i, caracter in enumerate(ALFABETO)}

# ============================================================================
# CAPA 1: FUNCIONES PURAS DE CIFRADO/DESCIFRADO
# ============================================================================

def cifrar_bloque(texto: str, clave: str, posicion_clave: int) -> tuple[str, int]:
    """Cifra un fragmento de texto con Vigenère sobre el alfabeto extendido.

    Se usa por bloques: devuelve la posición de la clave en la que debe
    continuar el siguiente bloque.

    Args:
        texto (str): Fragmento a cifrar.
        clave (str): Clave ya formateada (letras en mayúsculas).
        posicion_clave (int): Posición de la clave por la que se continúa.

    Returns:
        tuple[str, int]: Texto cifrado y nueva posición de la clave.

    Raises:
        ValueError: Si el texto o la clave tienen caracteres fuera del alfabeto.

    Example:
        >>> cifrar_bloque("Hola", "KEY", 0)
        ('Rsje', 1)
    """
    resultado = []
    for caracter in texto:
        if caracter not in INDICE_ALFABETO:
            raise ValueError(f"Carácter no soportado: {caracter!r} (U+{ord(caracter):04X})")
        if clave[posicion_clave] not in INDICE_ALFABETO:
            raise ValueError(f"Carácter no soportado en la clave: {clave[posicion_clave]!r}")
        # Fórmula: C = (P + K) mod N
        posicion = (INDICE_ALFABETO[caracter] + INDICE_ALFABETO[clave[posicion_clave]]) % len(ALFABETO)
        resultado.append(ALFABETO[posicion])
        posicion_clave = (posicion_clave + 1) % len(clave)
    return ''.join(resultado), posicion_clave


def descifrar_bloque(texto: str, clave: str, posicion_clave: int) -> tuple[str, int]:
    """Descifra un fragmento de texto con Vigenère sobre el alfabeto extendido.

    Operación inversa de cifrar_bloque().

    Args:
        texto (str): Fragmento cifrado.
        clave (str): Clave ya formateada (letras en mayúsculas).
        posicion_clave (int): Posición de la clave por la que se continúa.

    Returns:
        tuple[str, int]: Texto descifrado y nueva posición de la clave.

    Raises:
        ValueError: Si el texto o la clave tienen caracteres fuera del alfabeto.
    """
    resultado = []
    for caracter in texto:
        if caracter not in INDICE_ALFABETO:
            raise ValueError(f"Carácter no soportado: {caracter!r} (U+{ord(caracter):04X})")
        if clave[posicion_clave] not in INDICE_ALFABETO:
            raise ValueError(f"Carácter no soportado en la clave: {clave[posicion_clave]!r}")
        # Fórmula: P = (C - K) mod N
        posicion = (INDICE_ALFABETO[caracter] - INDICE_ALFABETO[clave[posicion_clave]]) % len(ALFABETO)
        resultado.append(ALFABETO[posicion])
        posicion_clave = (posicion_clave + 1) % len(clave)
    return ''.join(resultado), posicion_clave


def cifrar_vigenere(texto: str, clave: str) -> str:
    """Cifra un texto utilizando el algoritmo de Vigenère.

    Args:
        texto (str): Texto a cifrar (será limpiado automáticamente).
        clave (str): Clave para el cifrado (solo letras).

    Returns:
        str: Texto cifrado en mayúsculas sin espacios.

    Raises:
        ValueError: Si el texto está vacío o la clave no es válida.

    Example:
        >>> cifrar_vigenere("HELLO WORLD", "KEY")
        'RIJVSUYVJN'
    """
    logger.info("Iniciando cifrado Vigenère")

    # Validar y formatear entradas
    logger.debug("Validando y formateando entradas")
    texto_limpio = formatear_texto(texto)
    clave_limpia = validar_y_formatear_clave(clave)

    if not texto_limpio:
        logger.error("El texto no contiene caracteres válidos para cifrar")
        raise ValueError("El texto no contiene caracteres válidos para cifrar.")

    logger.debug("Texto limpio: %d caracteres, Clave limpia: %d caracteres",
                 len(texto_limpio), len(clave_limpia))

    # Ajustar la clave al tamaño del texto
    clave_ajustada = ajustar_clave(clave_limpia, len(texto_limpio))

    # Cifrado Vigenère
    logger.debug("Aplicando cifrado Vigenère")
    texto_cifrado = ""
    for i, letra in enumerate(texto_limpio):
        # Fórmula: C = (P + K) mod 26
        posicion = (ord(letra) - ord('A') + ord(clave_ajustada[i]) - ord('A')) % 26
        letra_cifrada = chr(posicion + ord('A'))
        texto_cifrado += letra_cifrada

    logger.info("Cifrado Vigenère completado exitosamente")

    return texto_cifrado


def descifrar_vigenere(texto_cifrado: str, clave: str) -> str:
    """Descifra un texto utilizando el algoritmo de Vigenère.

    FUNCIÓN PURA: No realiza I/O, solo transformaciones.

    Args:
        texto_cifrado (str): Texto cifrado a descifrar (solo mayúsculas sin espacios).
        clave (str): Clave para el descifrado (solo letras).

    Returns:
        str: Texto original descifrado en mayúsculas.

    Raises:
        ValueError: Si el texto está vacío o la clave no es válida.

    Example:
        >>> descifrar_vigenere("RIJVSUYVJN", "KEY")
        'HELLOWORLD'
    """
    logger.info("Iniciando descifrado Vigenère")

    # Validar y formatear clave
    logger.debug("Validando y formateando clave")
    clave_limpia = validar_y_formatear_clave(clave)

    # Formatear texto cifrado (por si acaso viene con espacios)
    texto_limpio = formatear_texto(texto_cifrado)

    if not texto_limpio:
        logger.error("El texto cifrado no contiene caracteres válidos")
        raise ValueError("El texto cifrado no contiene caracteres válidos.")

    logger.debug("Texto cifrado limpio: %d caracteres, Clave limpia: %d caracteres",
                 len(texto_limpio), len(clave_limpia))

    # Ajustar la clave al tamaño del texto
    clave_ajustada = ajustar_clave(clave_limpia, len(texto_limpio))

    # Descifrado Vigenère
    logger.debug("Aplicando descifrado Vigenère")
    texto_original = ""
    for i, letra in enumerate(texto_limpio):
        # Fórmula: P = (C - K + 26) mod 26
        num_letra = ord(letra) - ord('A')
        num_clave = ord(clave_ajustada[i]) - ord('A')
        posicion = (num_letra - num_clave + 26) % 26
        letra_original = chr(posicion + ord('A'))
        texto_original += letra_original

    logger.info("Descifrado Vigenère completado exitosamente")

    return texto_original


# ============================================================================
# CAPA 2: VALIDACIONES Y FORMATEO
# ============================================================================

def validar_y_formatear_clave(clave: str) -> str:
    """Valida y formatea la clave para el cifrado Vigenère.

    La clave debe contener solo letras (A-Z, a-z). Se convierte a mayúsculas
    y se eliminan caracteres no alfabéticos.

    Args:
        clave (str): Clave a validar y formatear.

    Returns:
        str: Clave formateada (solo mayúsculas, sin espacios).

    Raises:
        ValueError: Si la clave está vacía o no contiene letras válidas.

    Example:
        >>> validar_y_formatear_clave("Mi Clave 123")
        'MICLAVE'
    """
    logger.debug("Validando clave")

    if not clave:
        logger.warning("Intento de validación con clave vacía")
        raise ValueError("La clave no puede estar vacía.")

    # Limpiar la clave: solo letras, convertir a mayúsculas
    clave_limpia = ''.join(c.upper() for c in clave if c.isalpha())

    if not clave_limpia:
        logger.warning("Clave no contiene caracteres alfabéticos válidos")
        raise ValueError("La clave debe contener al menos una letra (A-Z).")

    logger.debug("Clave validada correctamente: %d caracteres", len(clave_limpia))

    return clave_limpia


def formatear_texto(texto: str) -> str:
    """Formatea el texto eliminando caracteres no alfabéticos y convirtiendo a mayúsculas.

    Prepara el texto para el proceso de cifrado eliminando todos los
    caracteres que no sean letras (A-Z) y convirtiendo a mayúsculas.

    Normaliza caracteres con tildes (á→a, é→e, ñ→n, etc.) antes de procesar.

    Args:
        texto (str): Texto a formatear.

    Returns:
        str: Texto formateado (solo mayúsculas A-Z, sin espacios ni símbolos).

    Example:
        >>> formatear_texto("Hello, World! 123")
        'HELLOWORLD'
        >>> formatear_texto("Hola qué tal")
        'HOLAQUETAL'
    """
    if not texto:
        return ""

    # Normalizar texto: convertir caracteres con tildes a su forma base
    # NFD = Normalization Form Canonical Decomposition
    # Esto separa "é" en "e" + acento, luego eliminamos los acentos
    texto_normalizado = unicodedata.normalize('NFD', texto)

    # Eliminar marcas diacríticas (tildes, acentos, diéresis)
    texto_sin_tildes = ''.join(
        c for c in texto_normalizado
        if unicodedata.category(c) != 'Mn'  # Mn = Nonspacing_Mark (acentos)
    )

    # Método optimizado: usar lista y join es más eficiente que concatenación
    # Ahora solo dejamos pasar letras ASCII (A-Z)
    caracteres_validos = [c.upper() for c in texto_sin_tildes if c.isalpha() and c.isascii()]
    return "".join(caracteres_validos)


def ajustar_clave(clave: str, longitud: int) -> str:
    """Ajusta la clave repitiendo sus caracteres para alcanzar la longitud deseada.

    Repite la clave de forma cíclica hasta que coincida con la longitud
    del texto a procesar.

    Args:
        clave (str): La clave a repetir (ya debe estar formateada).
        longitud (int): Longitud objetivo para la clave.

    Returns:
        str: Clave repetida cíclicamente con la longitud especificada.

    Raises:
        ValueError: Si la clave está vacía.

    Example:
        >>> ajustar_clave("KEY", 10)
        'KEYKEYKEYK'
    """
    logger.debug("Ajustando clave a longitud: %d", longitud)

    if not clave:
        logger.error("Intento de ajustar clave vacía")
        raise ValueError("La clave no puede estar vacía.")

    if longitud <= 0:
        logger.debug("Longitud solicitada es <= 0, retornando cadena vacía")
        return ""

    # Método optimizado: repetir la clave completa y cortar
    repeticiones = (longitud // len(clave)) + 1
    clave_extendida = clave * repeticiones
    logger.debug("Clave ajustada exitosamente")
    return clave_extendida[:longitud]


# ============================================================================
# CAPA 3: UTILIDADES PARA ARCHIVOS
# ============================================================================

# NOTA: Las funciones cifrar_vigenere_file y descifrar_vigenere_file han sido
# eliminadas para reducir duplicación. Ahora se manejan directamente en el router
# usando las funciones puras cifrar_vigenere() y descifrar_vigenere().
#
# Esto simplifica el código y evita tener lógica de archivos en los servicios,
# que deben ser funciones puras sin efectos secundarios.


def grabar_fichero(nombre_fichero: str, contenido: str) -> str:
    """Guarda contenido en un archivo de texto en la carpeta data/.

    Crea o sobrescribe un archivo en el directorio 'data/' del proyecto.
    Incluye protección contra path traversal.

    Args:
        nombre_fichero (str): Nombre del archivo (solo nombre, sin path).
        contenido (str): Contenido a escribir en el archivo.

    Returns:
        str: Ruta completa del archivo guardado.

    Raises:
        ValueError: Si el nombre del archivo contiene caracteres peligrosos.

    Security:
        - Protege contra path traversal (../, ..\\, etc.)
        - Solo permite nombres de archivo seguros

    """
    logger.info("Iniciando grabación de archivo: %s", nombre_fichero)

    # SEGURIDAD: Validar nombre de archivo para prevenir path traversal
    logger.debug("Sanitizando nombre de archivo")
    nombre_seguro = sanitizar_nombre_archivo(nombre_fichero)

    # Obtener directorio del proyecto
    directorio_actual = os.path.dirname(os.path.abspath(__file__))
    directorio_proyecto = os.path.dirname(directorio_actual)

    # Construir ruta al directorio data
    directorio_data = os.path.join(directorio_proyecto, 'data')
    ruta_archivo = os.path.join(directorio_data, nombre_seguro)

    logger.debug("Ruta del archivo: %s", ruta_archivo)

    # Verificación adicional: asegurar que la ruta está dentro de data/
    ruta_absoluta = os.path.abspath(ruta_archivo)
    directorio_data_absoluto = os.path.abspath(directorio_data)

    if not ruta_absoluta.startswith(directorio_data_absoluto):
        logger.error("Intento de path traversal detectado: %s", ruta_absoluta)
        raise ValueError("Intento de path traversal detectado.")

    # Crear el directorio data si no existe
    logger.debug("Verificando/creando directorio data")
    try:
        os.makedirs(directorio_data, exist_ok=True)
    except OSError as e:
        logger.exception("Error al crear directorio data: %s", directorio_data)
        raise ValueError(f"No se pudo crear el directorio: {e}") from e


    # Escribir el contenido en el archivo
    logger.debug("Escribiendo contenido en el archivo")
    try:
        with open(ruta_archivo, 'w', encoding='utf-8') as f:
            f.write(contenido)
    except OSError as e:
        logger.exception("Error al escribir archivo: %s", ruta_archivo)
        raise ValueError(f"No se pudo escribir el archivo: {e}") from e

    logger.info("Archivo guardado exitosamente: %s", ruta_archivo)

    return ruta_archivo


def sanitizar_nombre_archivo(nombre: str) -> str:
    """Sanitiza el nombre de archivo para prevenir ataques de path traversal.

    Elimina caracteres peligrosos y secuencias de path traversal.

    Args:
        nombre (str): Nombre del archivo a sanitizar.

    Returns:
        str: Nombre de archivo seguro.

    Raises:
        ValueError: Si el nombre es inválido o contiene patrones peligrosos.

    Security:
        - Bloquea ../ y ..\\ (path traversal)
        - Solo permite caracteres alfanuméricos, guiones, puntos y guiones bajos
    """
    logger.debug("Sanitizando nombre de archivo")

    if not nombre:
        logger.warning("Intento de sanitizar nombre de archivo vacío")
        raise ValueError("El nombre del archivo no puede estar vacío.")

    # Detectar path traversal
    if '..' in nombre or '/' in nombre or '\\' in nombre:
        logger.warning("Intento de path traversal en nombre de archivo: %s", nombre)
        raise ValueError("El nombre del archivo contiene caracteres no permitidos (../, /, \\).")

    # Solo permitir caracteres seguros: letras, números, puntos, guiones, guiones bajos
    try:
        patron_seguro = re.compile(r'^[a-zA-Z0-9._-]+$')

        if not patron_seguro.match(nombre):
            logger.warning("Nombre de archivo contiene caracteres no permitidos: %s", nombre)
            raise ValueError(
                "El nombre del archivo solo puede contener letras, números, "
                "puntos, guiones y guiones bajos."
            )
    except re.error as e:
        logger.exception("Error al compilar patrón regex")
        raise ValueError(f"Error interno al validar nombre de archivo: {e}") from e

    logger.debug("Nombre de archivo sanitizado correctamente")

    return nombre

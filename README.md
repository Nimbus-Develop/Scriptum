# 🔐 Scriptum

<div align="center">

**Sistema de cifrado seguro con interfaz gráfica y API REST**

[![JavaFX](https://img.shields.io/badge/JavaFX-23-orange.svg)](https://openjfx.io/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.104+-009688.svg)](https://fastapi.tiangolo.com/)
[![Python](https://img.shields.io/badge/Python-3.8+-3776AB.svg)](https://www.python.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

[Características](#-características) •
[Arquitectura](#-arquitectura) •
[Instalación](#-instalación) •
[Uso](#-uso) •
[Documentación](#-documentación)

</div>

---

## 📖 Descripción

**Scriptum** es una aplicación completa de cifrado que combina la seguridad de algoritmos con una experiencia de usuario intuitiva. El proyecto está dividido en dos componentes principales:

- **ScriptumFX**: Aplicación de escritorio desarrollada en JavaFX
- **ApiScriptum**: API REST construida con FastAPI para procesamiento backend

### 🎯 ¿Para qué sirve?

Scriptum permite cifrar y descifrar:
- ✅ **Textos**: Mensajes, notas, contraseñas
- ✅ **Archivos**: Documentos, imágenes, PDFs, etc.

Utilizando algoritmos de cifrado robustos:
- 🔒 **AES** (Advanced Encryption Standard) - 128, 192 y 256 bits
- 📝 **Vigenère** - Cifrado clásico mejorado (texto y archivos pequeños: elimina espacios por seguridad; archivos grandes ≥ 9,5 MB: alfabeto extendido, reversible y sin pérdida de caracteres)

---

## ✨ Características

### 🖥️ Interfaz de Usuario (ScriptumFX)

- **Interfaz amigable y responsive** construida con JavaFX
- **Temas claro y oscuro** para mejor experiencia visual
- **Soporte multiidioma** (Español/Inglés)
- **Drag & Drop** para archivos
- **Indicadores de progreso** para operaciones grandes
- **Historial de operaciones** con exportación

### ⚙️ Backend Potente (ApiScriptum)

- **API REST completa** con documentación interactiva (Swagger/ReDoc)
- **Cifrado AES-GCM** con autenticación integrada
- **Streaming optimizado** para archivos grandes (> 10 MB)
- **Verificación de integridad** con SHA-256
- **Sistema de logging** robusto y configurable
- **Gestión automática** de claves y salt

### 🔒 Seguridad

- ✓ Generación de claves con **PBKDF2** (100,000 iteraciones)
- ✓ Modo **AES-GCM** con autenticación y cifrado
- ✓ **Nonces aleatorios** únicos para cada operación
- ✓ **Verificación de integridad** con hash SHA-256
- ✓ **Empaquetado binario** eficiente de metadatos

---

## 🏗️ Arquitectura

```
Scriptum/
│
├── 📱 ScriptumFX/              # Aplicación JavaFX (Frontend)
│   ├── src/main/java/
│   │   └── es/luna/
│   │       ├── controller/     # Controladores MVC
│   │       ├── model/          # Modelos de datos
│   │       ├── service/        # Lógica de negocio
│   │       ├── client/         # Cliente HTTP para API
│   │       └── util/           # Utilidades
│   └── src/main/resources/
│       ├── fxml/               # Vistas FXML
│       ├── css/                # Estilos
│       └── img/                # Recursos gráficos
│
└── 🔧 ApiScriptum/             # API REST (Backend)
    ├── app/
    │   ├── routers/            # Endpoints HTTP
    │   ├── services/           # Lógica de cifrado
    │   └── schemas/            # Validación de datos
    ├── tests/                  # Suite de pruebas
    └── docs/                   # Documentación técnica
```

### 🔄 Flujo de Comunicación

```
┌─────────────┐           ┌──────────────┐           ┌─────────────┐
│             │   HTTP    │              │  Crypto   │             │
│ ScriptumFX  │ ◄────────►│  ApiScriptum │ ◄────────►│  PyCrypto   │
│  (JavaFX)   │  Requests │   (FastAPI)  │ Services  │   Library   │
│             │           │              │           │             │
└─────────────┘           └──────────────┘           └─────────────┘
      │                          │
      │                          │
      ▼                          ▼
  Usuario                    Logs/Metrics
```

---

## 🚀 Instalación

### Requisitos Previos

#### Para ScriptumFX:
- ☕ **Java 21** o superior
- 📦 **Maven 3.8+**
- 🎨 **JavaFX 23**

#### Para ApiScriptum:
- 🐍 **Python 3.8+**
- 📦 **pip**

### Instalación Rápida

#### 1️⃣ Clonar el repositorio

```bash
git clone https://github.com/WaraYasy/Scriptum.git
cd Scriptum
```

#### 2️⃣ Configurar la API (Backend)

```bash
cd ApiScriptum

# Crear entorno virtual
python3 -m venv venv

# Activar entorno virtual
# En macOS/Linux:
source venv/bin/activate
# En Windows:
venv\Scripts\activate

# Instalar dependencias
pip install -r requirements.txt

# Ejecutar API
python main.py
```

La API estará disponible en: `http://localhost:8000`

#### 3️⃣ Configurar la Aplicación (Frontend)

```bash
cd ../ScriptumFX

# Compilar con Maven
mvn clean install

# Ejecutar aplicación
mvn javafx:run
```

> **⚙️ Configuración del dominio de la API**
>
> Antes de ejecutar ScriptumFX, configura la URL de tu API en el archivo:
> `src/main/resources/application.properties`
>
> ```properties
> # URL de la API de Scriptum
> scriptum.api.url=http://localhost:8000
> ```
>
> **Ejemplos de configuración:**
> - **Desarrollo local**: `http://localhost:8000`
> - **Servidor Railway**: `https://tu-api.up.railway.app`
> - **Servidor personalizado**: `https://tu-dominio.com`
>
> La aplicación usará esta URL para conectarse al backend de cifrado.

---

## 💡 Uso

### Desde la Interfaz Gráfica (ScriptumFX)

1. **Abrir la aplicación** ScriptumFX
2. **Seleccionar tipo de cifrado**: AES o Vigenère
3. **Elegir operación**: Cifrar o Descifrar
4. **Introducir datos**:
   - Para texto: escribir o pegar directamente
   - Para archivos: arrastrar o seleccionar con el botón
5. **Configurar parámetros**:
   - Password/clave
   - Tipo de AES (128/192/256 bits)
6. **Ejecutar** y obtener resultado

### Desde la API (ApiScriptum)

#### Cifrar texto

```bash
curl -X POST "http://localhost:8000/api/aes/cifrar" \
  -H "Content-Type: application/json" \
  -d '{
    "texto": "Mensaje secreto",
    "password": "miPassword123",
    "tipo_aes": "AES-256"
  }'
```

#### Cifrar archivo

```bash
curl -X POST "http://localhost:8000/api/aes/cifrar-archivo" \
  -F "file=@documento.pdf" \
  -F "password=miPassword123" \
  -F "tipo_aes=AES-256"
```

Ver más ejemplos en la [documentación de la API →](ApiScriptum/README.md)

---

## 📚 Documentación

### Documentación de Componentes

- **[API REST](ApiScriptum/README.md)**: Guía completa de instalación, endpoints y ejemplos
- **[Sistema de Logging](ApiScriptum/docs/LOGGING_SISTEMA.md)**: Configuración y uso de logs
- **[Empaquetado de Archivos](ApiScriptum/docs/EMPAQUETADO_ARCHIVOS.md)**: Formato binario de paquetes
- **[Streaming](ApiScriptum/docs/STREAMING_GUIA.md)**: Manejo de archivos grandes
- **[Tests](ApiScriptum/tests/README.md)**: Suite de pruebas y cobertura
- **[Integración API](ScriptumFX/INTEGRACION_API.md)**: Conexión JavaFX ↔ FastAPI

### Documentación Interactiva de la API

Una vez que la API esté ejecutándose:

- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

---

## 🧪 Testing

```bash
# Ejecutar tests de la API
cd ApiScriptum
pytest tests/ -v

# Con cobertura
pytest tests/ --cov=app --cov-report=html
```

---

## 🛠️ Tecnologías

### Backend (ApiScriptum)
- **FastAPI** - Framework web moderno y rápido
- **PyCryptodome** - Biblioteca de criptografía
- **Uvicorn** - Servidor ASGI de alto rendimiento
- **Pydantic** - Validación de datos
- **Pytest** - Framework de testing

### Frontend (ScriptumFX)
- **JavaFX 23** - Framework de interfaz gráfica
- **Maven** - Gestión de dependencias
- **Logback** - Sistema de logging
- **FXML + CSS** - Diseño de interfaces


## 👤 Autores

- 🧙🏻‍♀️ **Arantxa** - [@arantxaMain](https://github.com/arantxaMain)
- 🧙🏽‍♀️ **Wara** - [@WaraYasy](https://github.com/WaraYasy)

---

<div align="center">

Hecho con ❤️ y ☕

</div>

# VisorImagenes V2 — DAM & Workflow Automation

Sistema de gestión y catalogación de bibliotecas masivas de activos digitales (+40.000 imágenes). Renderizado virtual por ventana deslizante, edición jerárquica con tristate, exportación HTML interactiva para cliente y base de datos SQLite embebida.

**Lee el caso de estudio completo → [CASO_ESTUDIO.md](CASO_ESTUDIO.md)**

---

## Stack

| Capa | Tecnología |
|------|-----------|
| UI | Swing + FlatLaf 3.4.1 |
| Lenguaje | Java 21 (Maven) |
| Base de datos | SQLite (WAL, JDBC) |
| Caché | Caffeine 3.1.8 |
| Miniaturas | Thumbnailator 0.4.20, TwelveMonkeys 3.10.1 |
| Serialización | GSON 2.10.1 |
| Reportes | Apache PDFBox 3.0.5 |

## Modos de trabajo

- **VISUALIZADOR** — Grid virtual con previsualización, zoom, filtros aditivos/sustractivos
- **PROYECTO** — Consolidación, asignación de geometría 3D, preflight de exportación
- **CLIENTE** — Checkboxes tristate jerárquicos, comentarios, exportación HTML único
- **DATOS** — Etiquetas arbóreas, mapeo automático de estructura de disco
- **CARRUSEL** — Navegación secuencial

## Quick start

```bash
mvn compile          # compilar
mvn package          # empaquetar (destino configurable en pom.xml)
```

Entrypoint: `principal.VisorV2.main()`

---

## Documentación técnica

- [Caso de estudio detallado](CASO_ESTUDIO.md) — arquitectura, desafíos de ingeniería, esquema BD, flujos de trabajo
- [Guía para agentes IA](AGENTS.md) — convenciones de código, estructura del proyecto, gotchas

# Análisis Estático de Programas — TP1: Representación de Programas

**Materia:** Análisis Estático de Programas  
**Carrera:** Ingeniería en Sistemas de Información  
**Universidad:** Universidad Nacional de Villa Mercedes  

**Integrantes:**
- Elenio Garcia Bustamante
- Valentín Giménez

---

## Descripción

Este proyecto implementa un conjunto de algoritmos para el análisis estático de programas escritos en un mini-lenguaje definido por la cátedra. A partir del código fuente de un programa, el sistema construye automáticamente las siguientes representaciones:

1. **CFG** — Control Flow Graph (Grafo de Flujo de Control)
2. **Postdominadores** — Conjunto de postdominadores de cada nodo del CFG
3. **Árbol de Postdominadores** — Árbol construido a partir de los postdominadores inmediatos
4. **CDG** — Control Dependence Graph (Grafo de Dependencia de Control)

---

## El mini-lenguaje

Los programas de entrada deben seguir esta gramática:

```
PROGRAM    → TYPE ID () { STATEMENTS }
TYPE       → integer
STATEMENTS → STATEMENT | STATEMENT STATEMENTS
STATEMENT  → ID = EXP ;
           | return EXP ;
           | if ( EXP ) { STATEMENTS } else { STATEMENTS }
           | while ( EXP ) { STATEMENTS }
EXP        → VALUE | VALUE + VALUE
VALUE      → ID | NUMBER
```

### Ejemplo de programa válido (`ejemplo.txt`):

```
integer f() {
    x = 3 ;
    if (y) {
        z = x + 1 ;
    }
    else {
        y = x + z ;
    }
    return z ;
}
```

---

## Estructura del proyecto

```
CFG_Builder/
├── src/
│   ├── Main.java                          # Punto de entrada
│   ├── ast/                               # Nodos del árbol sintáctico (AST)
│   │   ├── Node.java
│   │   ├── AssignStmt.java
│   │   ├── IfStmt.java
│   │   ├── WhileStmt.java
│   │   ├── ReturnStmt.java
│   │   └── SeqStmt.java
│   ├── cfg/                               # Algoritmos de análisis
│   │   ├── CFGNode.java                   # Nodo del CFG (guarda objeto AST)
│   │   ├── CFGBuilder.java                # Construcción del CFG
│   │   ├── DominanceAnalysis.java         # Cálculo de postdominadores
│   │   ├── DotExporter.java               # Exportación a formato DOT
│   │   ├── PostDominatorTree.java         # Árbol de postdominadores
│   │   └── ControlDependenceGraph.java    # CDG
│   └── parser/                            # Parser del mini-lenguaje
│       ├── Lexer.java                     # Analizador léxico
│       ├── Parser.java                    # Analizador sintáctico (generado por CUP)
│       └── sym.java                       # Tabla de símbolos (generado por CUP)
├── lib/
│   ├── java-cup-11b-20160615.jar          # Generador de parsers CUP
│   └── java-cup-runtime-11b-20160615.jar  # Runtime de CUP
└── ejemplo.txt                            # Programa de ejemplo
```

---

## Requisitos

- **Java 17 o superior** (el proyecto usa Eclipse Temurin 21)
- **Graphviz** instalado para generar los PNG automáticamente
  - Windows: [https://graphviz.org/download/](https://graphviz.org/download/)
  - La ruta esperada por el programa es: `C:\Program Files\Graphviz\bin\dot.exe`

---

## Cómo ejecutar

### Opción 1 — Desde IntelliJ IDEA (recomendado)

1. Abrir el proyecto en IntelliJ IDEA
2. Verificar que el SDK esté configurado (Eclipse Temurin 21)
3. Ir a **Run → Edit Configurations**
4. En **Program arguments** escribir el nombre del archivo a analizar, por ejemplo: `ejemplo.txt`
5. Colocar el archivo `.txt` con el programa en la **raíz del proyecto**
6. Ejecutar con el botón ▶️

### Opción 2 — Desde la terminal (PowerShell)

Primero agregar Java al PATH si no está disponible:
```powershell
$env:PATH += ";C:\Users\<usuario>\.jdks\temurin-21.0.10\bin"
```

Compilar el proyecto:
```powershell
javac -cp lib\java-cup-runtime-11b-20160615.jar -sourcepath src -d out src\Main.java
```

Ejecutar con un archivo de programa:
```powershell
java -cp "out;lib\java-cup-runtime-11b-20160615.jar" Main ejemplo.txt
```

---

## Salida del programa

Al ejecutar, el programa imprime en consola los resultados de cada punto:

### Punto 2 — Postdominadores
```
--- Postdominadores ---
  pdom( x := 3 )    = { x := 3  if (y)  return z  EXIT }
  pdom( if (y) )    = { if (y)  return z  EXIT }
  ...
```

### Punto 3 — Árbol de Postdominadores
```
--- Arbol de Postdominadores (Punto 3) ---
  ipdom( x := 3 )      = if (y)
  ipdom( if (y) )      = return z
  ipdom( z := x + 1 )  = return z
  ...
```

### Punto 4 — CDG
```
--- Control Dependence Graph (Punto 4) ---
  START  --> { x := 3  if (y)  return z  EXIT }
  if (y) --> { z := x + 1  y := x + z }
```

---

## Archivos generados

Además de la salida en consola, el programa genera automáticamente los siguientes archivos en la raíz del proyecto:

| Archivo | Descripción |
|---|---|
| `cfg.dot` | CFG en formato DOT |
| `cfg.png` | Imagen del CFG (generada con Graphviz) |
| `postdom_tree.dot` | Árbol de postdominadores en formato DOT |
| `postdom_tree.png` | Imagen del árbol de postdominadores |
| `cdg.dot` | CDG en formato DOT |
| `cdg.png` | Imagen del CDG |

Los archivos `.png` se regeneran automáticamente cada vez que se ejecuta el programa.  
Si Graphviz no está instalado, los `.dot` pueden visualizarse online en [https://dreampuf.github.io/GraphvizOnline/](https://dreampuf.github.io/GraphvizOnline/).

---

## Descripción de los algoritmos implementados

### Punto 1 — CFG
Se recorre el AST recursivamente. Cada statement genera uno o más nodos CFG. Los nodos guardan una referencia al objeto AST original (no solo un string) para preservar la información estructural del programa.

### Punto 2 — Postdominadores
Se usa un algoritmo iterativo de punto fijo. Un nodo `B` postdomina a `A` si todos los caminos de `A` hacia EXIT pasan por `B`. Se inicializa el conjunto de postdominadores de cada nodo con todos los nodos del CFG y se itera hasta convergencia.

### Punto 3 — Árbol de Postdominadores
El postdominador inmediato (`ipdom`) de un nodo `n` es el postdominador más cercano a `n`. Se calcula buscando el candidato `c` que no postdomina a ningún otro postdominador de `n`. El árbol tiene como raíz a EXIT, y el padre de cada nodo es su `ipdom`.

### Punto 4 — CDG
Se augmenta el CFG con un nodo START con aristas hacia ENTRY y EXIT. Para cada arista `(A→B)` del grafo augmentado donde `B` no postdomina a `A`, se sube por el árbol de postdominadores desde `B` hasta `ipdom(A)`, marcando cada nodo visitado como control-dependiente de `A`. Los nodos que siempre se ejecutan quedan bajo START.

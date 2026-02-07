# Static Analysis Report — Deploy-PrimeFaces-JSF-Web-App-on-Tomcat-9.0-317345

Date: 2026-02-07

## Tooling note
Attempted to run Maven-based analysis (`mvn validate/compile`, `dependency:tree`, OWASP dependency-check), but **Maven is not available** in the execution environment (`mvn: command not found`). Findings below are from **POM + source/config scan**.

---

## Build & dependency analysis (POM review)

**pom.xml highlights**
- Packaging: `war`
- Java: `maven.compiler.source/target=11`
- Dependencies:
  - `javax.servlet:javax.servlet-api:4.0.1` (provided)
  - `org.glassfish:javax.faces:2.4.0`
  - `org.primefaces:primefaces:8.0`
  - `org.jboss.weld.servlet:weld-servlet:2.4.8.Final`
  - `org.projectlombok:lombok:1.18.12` (provided)
  - Test: `junit:4.13.1`, `mockito-core:2.23.0`

**Key concerns (priority order)**
1. **Old dependencies (High)**
   - PrimeFaces 8.0 is old; older PrimeFaces lines have had CVEs historically and miss many fixes.
   - Weld 2.x and Lombok 1.18.12 are old.
   - Mockito 2.23.0 is old.
2. **JSF implementation choice (Medium/High)**
   - `org.glassfish:javax.faces:2.4.0` is not the most common stable choice for JSF on Tomcat.
   - Consider Mojarra 2.3.x (`org.glassfish:javax.faces:2.3.9` etc.) or MyFaces 2.3.x.
3. **No BOM / dependencyManagement (Medium)**
   - Harder to guarantee dependency convergence and consistent transitive versions.

---

## Code quality / potential bugs (source scan)

### `TodoListController.java`
**Issues**
- **Bug / API mismatch (High)**:
  - `getSelectedItem()` returns `selectedItems` (List) and not `selectedItem`.
- **Unused field (Low/Medium)**:
  - `private List<TodoItem> todoItems;` never used.
- **Null-safety (Medium/High)**:
  - `buttonUpdateAction()` calls DAO with possibly-null `selectedItems`.

### `TodoItemManagementInMemory.java`
**Issues**
- **Potential NPE (High)**:
  - `updateTodoItem(List<TodoItem> items)` uses `items.stream()` without null guard.
- **Index/id coupling (High)**:
  - Uses `todoItems.set(item.getId().intValue(), item);` which assumes `id == index` always.
  - Risks `IndexOutOfBoundsException` and `NullPointerException` if id is null.
- **Concurrency smell (Medium)**:
  - `CopyOnWriteArrayList` plus manual `synchronized` blocks is inconsistent (fine for demo, not ideal for production).

### `TodoItem.java`
**Issues**
- **No equals/hashCode (Medium)**: may affect selection identity behavior depending on PrimeFaces usage.
- **Unnecessary `return;` statements in setters (Low)**.
- **Constructor argument order confusion (Medium)**: there is a `(String category, String name)` constructor, while elsewhere usage is `(name, category, complete)`.

### `beans.xml`
- `bean-discovery-mode="all"` (Medium): broader than needed; `annotated` is commonly preferred.

---

## JSF / PrimeFaces best-practice checks

### `index.xhtml`
- `p:dataTable`:
  - `rowKey="#{item.id}"` is good if ids are always non-null and stable.
  - Uses checkbox selection (`selectionMode="multiple"`) with `selection="#{todocontroller.selectedItems}"`.
- Ajax updates:
  - `p:commandButton update="itemTables"` is fine.
  - Consider clearing inputs after add (controller-side) for UX.

---

## WAR structure / Tomcat 9 compatibility

- `web.xml` uses Servlet 4.0 schema; ok for Tomcat 9.
- JSF servlet mapping to `*.xhtml` configured; welcome file set to `index.xhtml`.
- Weld is included; typical for CDI on Tomcat.

---

## Recommended next steps (minimal changes)

### A) Re-run with Maven available
Run:
- `mvn -DskipTests validate`
- `mvn -DskipTests compile`
- `mvn -DskipTests dependency:tree`
- `mvn -DskipTests org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=0`

### B) Add minimal analysis tooling (plugins)
- `maven-enforcer-plugin` (dependency convergence; Java/Maven version)
- `spotbugs-maven-plugin`
- `maven-checkstyle-plugin`
- `dependency-check-maven`

### C) Fix correctness first
- Fix controller `getSelectedItem()` bug.
- Guard against null `selectedItems` in controller/DAO.
- Remove id==index coupling in DAO update logic.

---

## Environment limitation
This report is based on static inspection only because `mvn` is not installed in the current execution environment.

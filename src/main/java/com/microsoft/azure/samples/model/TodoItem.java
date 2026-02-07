/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.samples.model;

import java.io.Serializable;
import java.util.Objects;

public class TodoItem implements Serializable {

\tprivate static final long serialVersionUID = 6437012982370705547L;
\tprivate Long id;
    private String category;
    private String name;
    private boolean complete;

    public TodoItem() {}

    public TodoItem(String name, String category, boolean complete) {
        this.name = name;
        this.category = category;
        this.complete = complete;
    }

    public TodoItem(String category, String name) {
        this.category = category;
        this.name = name;
        this.complete = false;
    }

    @Override
    public String toString() {
        return String.format(
                "TodoItem[id=%d, category='%s', name='%s', complete='%b']",
                id, category, name, complete);
    }

    /**
     * Equality is based on non-null id only.
     *
     * PrimeFaces DataTable selection commonly maps rows using rowKey. When the rowKey is the id
     * (see index.xhtml: rowKey="#{item.id}"), having equals/hashCode consistent with that id helps
     * avoid selection glitches when the table re-renders and different instances represent the
     * same logical row.
     *
     * If id is null (should not happen for in-list items; ids are assigned on add), we intentionally
     * treat the instance as not equal to any other instance except itself.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TodoItem)) {
            return false;
        }
        TodoItem other = (TodoItem) o;

        if (this.id == null || other.id == null) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        // Only non-null ids participate in hashing; otherwise fall back to identity hash semantics.
        return (id == null) ? System.identityHashCode(this) : id.hashCode();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

}

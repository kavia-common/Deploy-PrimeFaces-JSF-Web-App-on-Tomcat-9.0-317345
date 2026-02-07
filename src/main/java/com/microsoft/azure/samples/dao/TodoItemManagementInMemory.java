/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.samples.dao;

import com.microsoft.azure.samples.model.TodoItem;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class TodoItemManagementInMemory implements ItemManagement {

    private static final Logger LOGGER = Logger.getLogger(TodoItemManagementInMemory.class.getName());

    /**
     * In-memory storage. CopyOnWriteArrayList is sufficient for this demo app to keep iteration safe from
     * concurrent modifications by PrimeFaces/JSF rendering, while writes are guarded with synchronized blocks.
     */
    private CopyOnWriteArrayList<TodoItem> todoItems = new CopyOnWriteArrayList<TodoItem>();

    public CopyOnWriteArrayList<TodoItem> getTodoItems() {
        return todoItems;
    }

    public void setTodoItems(CopyOnWriteArrayList<TodoItem> todoItems) {
        // Null-safety: never allow the internal list reference to be null.
        this.todoItems = (todoItems == null) ? new CopyOnWriteArrayList<>() : todoItems;
    }

    public void addTodoItem(TodoItem item) {
        synchronized (this) {
            // Null-safety: ignore null items (demo-friendly behavior).
            if (item == null) {
                LOGGER.fine("addTodoItem ignored: item was null");
                return;
            }

            // Ensure list reference is never null (defensive; setTodoItems already enforces this).
            if (todoItems == null) {
                todoItems = new CopyOnWriteArrayList<>();
            }

            int size = todoItems.size();
            long id = 0;
            if (size != 0) {
                TodoItem last = todoItems.get(size - 1);
                // Null-safety: if existing last id is missing, fall back to using size as a best-effort id.
                Long lastId = (last == null) ? null : last.getId();
                id = (lastId == null) ? size : (lastId + 1);
            }
            item.setId(id);
            todoItems.add(item);
        }
    }

    public void updateTodoItem(List<TodoItem> items) {
        // Null-safety: caller may pass null; treat as no-op.
        if (items == null || items.isEmpty()) {
            return;
        }

        synchronized (this) {
            // Null-safety: internal list should never be null, but guard anyway.
            if (todoItems == null) {
                todoItems = new CopyOnWriteArrayList<>();
            }

            for (TodoItem incoming : items) {
                if (incoming == null) {
                    // Ignore null item entries rather than failing the whole update batch.
                    LOGGER.fine("updateTodoItem ignored: encountered null item in input list");
                    continue;
                }

                Long incomingId = incoming.getId();
                if (incomingId == null) {
                    // PrimeFaces rowKey relies on stable non-null IDs; ignore or log null ids.
                    LOGGER.fine("updateTodoItem ignored: item id was null (cannot match existing rowKey)");
                    continue;
                }

                // Business rule: "update" means marking complete.
                incoming.setComplete(true);

                // Find by id (do NOT assume id == index).
                int existingIndex = findIndexByIdLocked(incomingId);

                if (existingIndex >= 0) {
                    // Safe set by verified index.
                    todoItems.set(existingIndex, incoming);
                } else {
                    // Not found => append (demo-friendly upsert semantics).
                    todoItems.add(incoming);
                }
            }
        }
    }

    /**
     * Finds the index of an existing item by id using linear search.
     * Must be called with the instance lock held (synchronized(this)).
     */
    private int findIndexByIdLocked(Long id) {
        if (id == null || todoItems == null || todoItems.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < todoItems.size(); i++) {
            TodoItem existing = todoItems.get(i);
            if (existing == null) {
                continue;
            }
            Long existingId = existing.getId();
            if (id.equals(existingId)) {
                return i;
            }
        }
        return -1;
    }
}

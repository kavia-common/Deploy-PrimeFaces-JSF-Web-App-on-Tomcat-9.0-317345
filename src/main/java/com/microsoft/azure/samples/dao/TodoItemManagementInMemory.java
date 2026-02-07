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
import java.util.concurrent.atomic.AtomicLong;
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

    /**
     * Monotonic id generator to guarantee unique non-null IDs for new items.
     *
     * Note: because this is an in-memory demo DAO, a JVM restart resets ids.
     */
    private final AtomicLong nextId = new AtomicLong(0);

    public CopyOnWriteArrayList<TodoItem> getTodoItems() {
        return todoItems;
    }

    public void setTodoItems(CopyOnWriteArrayList<TodoItem> todoItems) {
        // Null-safety: never allow the internal list reference to be null.
        this.todoItems = (todoItems == null) ? new CopyOnWriteArrayList<>() : todoItems;

        // Keep id generator aligned with any pre-populated data.
        synchronized (this) {
            long maxId = findMaxIdLocked();
            // nextId holds "next value to hand out"
            nextId.set(maxId + 1);
        }
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

            // Ensure a unique, non-null id for PrimeFaces rowKey=#{item.id}.
            // If the incoming item already has an id, preserve it, but keep generator ahead to avoid duplicates.
            Long existingId = item.getId();
            long assignedId = (existingId != null) ? existingId : nextId.getAndIncrement();
            item.setId(assignedId);

            // If an explicit id was provided that's >= current nextId, advance generator.
            // This avoids future collisions if a caller sets an id manually.
            nextId.updateAndGet(cur -> Math.max(cur, assignedId + 1));

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

    /**
     * Computes the maximum id currently present in the list.
     * Must be called with the instance lock held (synchronized(this)).
     */
    private long findMaxIdLocked() {
        if (todoItems == null || todoItems.isEmpty()) {
            return -1;
        }

        long max = -1;
        for (TodoItem existing : todoItems) {
            if (existing == null) {
                continue;
            }
            Long id = existing.getId();
            if (id != null && id > max) {
                max = id;
            }
        }
        return max;
    }
}

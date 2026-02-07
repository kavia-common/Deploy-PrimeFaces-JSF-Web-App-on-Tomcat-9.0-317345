/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.samples.controller;

import com.microsoft.azure.samples.dao.TodoItemManagementInMemory;
import com.microsoft.azure.samples.model.TodoItem;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named("todocontroller")
@ViewScoped
public class TodoListController implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
    private TodoItemManagementInMemory todoManagement;

    private TodoItem selectedItem;
    private List<TodoItem> selectedItems;

    private String name;
    private String category;

    public List<TodoItem> getTodoItems() {
        return todoManagement.getTodoItems();
    }

    // PUBLIC_INTERFACE
    public void buttonUpdateAction() {
        /**
         * Marks currently selected todo items as complete.
         *
         * Null-safety: PrimeFaces may leave the selection list as null when nothing is selected,
         * so we must never call the DAO with a null list.
         */
        if (selectedItems == null || selectedItems.isEmpty()) {
            // Optional UX feedback: inform the user that there is nothing to update.
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null) {
                context.addMessage(
                        null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "No tasks selected", "Please select one or more tasks to update.")
                );
            }
            return;
        }

        todoManagement.updateTodoItem(selectedItems);
    }

    // PUBLIC_INTERFACE
    public void buttonAddAction() {
        /** Adds a new todo item using the current input values and resets the input fields on success. */
        TodoItem addItem = new TodoItem(name, category, false);
        todoManagement.addTodoItem(addItem);

        // Reset inputs so the UI clears after adding an item.
        // This is compatible with index.xhtml bindings to #{todocontroller.name} and #{todocontroller.category}.
        this.name = null;
        this.category = null;
    }

    public void setSelectedItem(TodoItem selectedItem) {
        this.selectedItem = selectedItem;
    }

    public TodoItem getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItems(List<TodoItem> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public List<TodoItem> getSelectedItems() {
        return selectedItems;
    }

    // PUBLIC_INTERFACE
    public void onRowSelect(SelectEvent<TodoItem> event) {
        /** PrimeFaces row select callback (kept for compatibility); null-safe. */
        if (event == null || event.getObject() == null || event.getObject().getId() == null) {
            return;
        }
        FacesMessage msg = new FacesMessage("TodoItem Selected", event.getObject().getId().toString());
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.addMessage(null, msg);
        }
    }

    // PUBLIC_INTERFACE
    public void onRowUnselect(UnselectEvent<TodoItem> event) {
        /** PrimeFaces row unselect callback (kept for compatibility); null-safe. */
        if (event == null || event.getObject() == null || event.getObject().getId() == null) {
            return;
        }
        FacesMessage msg = new FacesMessage("TodoItem Unselected", event.getObject().getId().toString());
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.addMessage(null, msg);
        }
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
}


import { test, expect } from '@playwright/test';

test.describe('TodoMVC Application', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    // Wait for the app to load
    await page.waitForSelector('.todoapp', { state: 'visible' });
  });

  test('should display the TodoMVC app', async ({ page }) => {
    // Check that the main elements are present
    await expect(page.locator('.todoapp')).toBeVisible();
    await expect(page.locator('h1')).toHaveText('todos');
    await expect(page.locator('.new-todo')).toBeVisible();
    await expect(page.locator('.new-todo')).toHaveAttribute('placeholder', 'What needs to be done?');
  });

  test('should add a new todo', async ({ page }) => {
    const todoText = 'Buy groceries';

    // Add a new todo
    await page.fill('.new-todo', todoText);
    await page.press('.new-todo', 'Enter');

    // Check that the todo was added
    await expect(page.locator('.todo-list li')).toHaveCount(1);
    await expect(page.locator('.todo-list li label')).toHaveText(todoText);

    // Check that the input is cleared
    await expect(page.locator('.new-todo')).toHaveValue('');

    // Check that the footer appears with correct count
    await expect(page.locator('.footer')).toBeVisible();
    await expect(page.locator('.todo-count')).toHaveText('1 item left');
  });

  test('should add multiple todos', async ({ page }) => {
    const todos = ['First todo', 'Second todo', 'Third todo'];

    // Add multiple todos
    for (const todo of todos) {
      await page.fill('.new-todo', todo);
      await page.press('.new-todo', 'Enter');
    }

    // Check that all todos were added
    await expect(page.locator('.todo-list li')).toHaveCount(3);

    // Check the content of each todo
    for (let i = 0; i < todos.length; i++) {
      await expect(page.locator('.todo-list li').nth(i).locator('label')).toHaveText(todos[i]);
    }

    // Check the footer count
    await expect(page.locator('.todo-count')).toHaveText('3 items left');
  });

  test('should toggle a todo as completed', async ({ page }) => {
    // Add a todo
    await page.fill('.new-todo', 'Test todo');
    await page.press('.new-todo', 'Enter');

    // Toggle the todo as completed
    await page.click('.todo-list li .toggle');

    // Check that the todo is marked as completed
    await expect(page.locator('.todo-list li')).toHaveClass(/completed/);
    await expect(page.locator('.toggle')).toBeChecked();

    // Check that the count is updated
    await expect(page.locator('.todo-count')).toHaveText('0 items left');

    // Check that clear completed button appears
    await expect(page.locator('.clear-completed')).toBeVisible();
  });

  test('should toggle a todo back to active', async ({ page }) => {
    // Add and complete a todo
    await page.fill('.new-todo', 'Test todo');
    await page.press('.new-todo', 'Enter');
    await page.click('.todo-list li .toggle');

    // Toggle it back to active
    await page.click('.todo-list li .toggle');

    // Check that the todo is no longer completed
    await expect(page.locator('.todo-list li')).not.toHaveClass(/completed/);
    await expect(page.locator('.toggle')).not.toBeChecked();

    // Check that the count is updated
    await expect(page.locator('.todo-count')).toHaveText('1 item left');
  });

  test('should delete a todo', async ({ page }) => {
    // Add a todo
    await page.fill('.new-todo', 'Todo to delete');
    await page.press('.new-todo', 'Enter');

    // Hover over the todo to reveal the delete button
    await page.hover('.todo-list li');

    // Click the delete button
    await page.click('.todo-list li .destroy');

    // Check that the todo was deleted
    await expect(page.locator('.todo-list li')).toHaveCount(0);

    // Check that the footer is hidden when no todos
    await expect(page.locator('.footer')).not.toBeVisible();
  });

  test('should edit a todo by double-clicking', async ({ page }) => {
    const originalText = 'Original todo';
    const editedText = 'Edited todo';

    // Add a todo
    await page.fill('.new-todo', originalText);
    await page.press('.new-todo', 'Enter');

    // Double-click to edit
    await page.dblclick('.todo-list li label');

    // Check that the todo is in editing mode
    await expect(page.locator('.todo-list li')).toHaveClass(/editing/);
    await expect(page.locator('.todo-list li .edit')).toBeVisible();

    // Edit the todo text
    await page.fill('.todo-list li .edit', editedText);
    await page.press('.todo-list li .edit', 'Enter');

    // Check that the todo was updated
    await expect(page.locator('.todo-list li')).not.toHaveClass(/editing/);
    await expect(page.locator('.todo-list li label')).toHaveText(editedText);
  });

  test('should cancel editing with Escape key', async ({ page }) => {
    const originalText = 'Original todo';

    // Add a todo
    await page.fill('.new-todo', originalText);
    await page.press('.new-todo', 'Enter');

    // Double-click to edit
    await page.dblclick('.todo-list li label');

    // Start editing but cancel with Escape
    await page.fill('.todo-list li .edit', 'Changed text');
    await page.press('.todo-list li .edit', 'Escape');

    // Check that the original text is preserved
    await expect(page.locator('.todo-list li')).not.toHaveClass(/editing/);
    await expect(page.locator('.todo-list li label')).toHaveText(originalText);
  });

  test('should delete todo when editing text is cleared', async ({ page }) => {
    // Add a todo
    await page.fill('.new-todo', 'Todo to clear');
    await page.press('.new-todo', 'Enter');

    // Double-click to edit
    await page.dblclick('.todo-list li label');

    // Clear the text and press Enter
    await page.fill('.todo-list li .edit', '');
    await page.press('.todo-list li .edit', 'Enter');

    // Check that the todo was deleted
    await expect(page.locator('.todo-list li')).toHaveCount(0);
  });

  test('should toggle all todos', async ({ page }) => {
    const todos = ['First todo', 'Second todo', 'Third todo'];

    // Add multiple todos
    for (const todo of todos) {
      await page.fill('.new-todo', todo);
      await page.press('.new-todo', 'Enter');
    }

    // Toggle all todos as completed
    await page.click('.toggle-all');

    // Check that all todos are completed
    const todoItems = page.locator('.todo-list li');
    await expect(todoItems).toHaveCount(3);

    for (let i = 0; i < 3; i++) {
      await expect(todoItems.nth(i)).toHaveClass(/completed/);
      await expect(todoItems.nth(i).locator('.toggle')).toBeChecked();
    }

    // Check that the toggle-all checkbox is checked
    await expect(page.locator('.toggle-all')).toBeChecked();

    // Check the count
    await expect(page.locator('.todo-count')).toHaveText('0 items left');
  });

  test('should clear completed todos', async ({ page }) => {
    const todos = ['Active todo', 'Completed todo 1', 'Completed todo 2'];

    // Add todos
    for (const todo of todos) {
      await page.fill('.new-todo', todo);
      await page.press('.new-todo', 'Enter');
    }

    // Complete the last two todos
    await page.click('.todo-list li:nth-child(2) .toggle');
    await page.click('.todo-list li:nth-child(3) .toggle');

    // Clear completed todos
    await page.click('.clear-completed');

    // Check that only the active todo remains
    await expect(page.locator('.todo-list li')).toHaveCount(1);
    await expect(page.locator('.todo-list li label')).toHaveText('Active todo');

    // Check that clear completed button is hidden
    await expect(page.locator('.clear-completed')).not.toBeVisible();
  });

  test('should filter todos by All', async ({ page }) => {
    // Add mixed todos
    await page.fill('.new-todo', 'Active todo');
    await page.press('.new-todo', 'Enter');
    await page.fill('.new-todo', 'Completed todo');
    await page.press('.new-todo', 'Enter');

    // Complete one todo
    await page.click('.todo-list li:nth-child(2) .toggle');

    // Click All filter
    await page.click('.filters a[href="#/all"]');

    // Check that all todos are visible
    await expect(page.locator('.todo-list li')).toHaveCount(2);
    await expect(page.locator('.filters a[href="#/all"]')).toHaveClass(/selected/);
  });

  test('should filter todos by Active', async ({ page }) => {
    // Add mixed todos
    await page.fill('.new-todo', 'Active todo');
    await page.press('.new-todo', 'Enter');
    await page.fill('.new-todo', 'Completed todo');
    await page.press('.new-todo', 'Enter');

    // Complete one todo
    await page.click('.todo-list li:nth-child(2) .toggle');

    // Click Active filter
    await page.click('.filters a[href="#/active"]');

    // Check that only active todos are visible
    await expect(page.locator('.todo-list li')).toHaveCount(1);
    await expect(page.locator('.todo-list li label')).toHaveText('Active todo');
    await expect(page.locator('.filters a[href="#/active"]')).toHaveClass(/selected/);
  });

  test('should filter todos by Completed', async ({ page }) => {
    // Add mixed todos
    await page.fill('.new-todo', 'Active todo');
    await page.press('.new-todo', 'Enter');
    await page.fill('.new-todo', 'Completed todo');
    await page.press('.new-todo', 'Enter');

    // Complete one todo
    await page.click('.todo-list li:nth-child(2) .toggle');

    // Click Completed filter
    await page.click('.filters a[href="#/completed"]');

    // Check that only completed todos are visible
    await expect(page.locator('.todo-list li')).toHaveCount(1);
    await expect(page.locator('.todo-list li label')).toHaveText('Completed todo');
    await expect(page.locator('.filters a[href="#/completed"]')).toHaveClass(/selected/);
  });

  test('should persist todos after page reload', async ({ page }) => {
    const todoText = 'Persistent todo';

    // Add a todo
    await page.fill('.new-todo', todoText);
    await page.press('.new-todo', 'Enter');

    // Reload the page
    await page.reload();
    await page.waitForSelector('.todoapp', { state: 'visible' });

    // Check that the todo persists
    await expect(page.locator('.todo-list li')).toHaveCount(1);
    await expect(page.locator('.todo-list li label')).toHaveText(todoText);
  });

  test('should handle empty input gracefully', async ({ page }) => {
    // Try to add empty todo
    await page.fill('.new-todo', '');
    await page.press('.new-todo', 'Enter');

    // Check that no todo was added
    await expect(page.locator('.todo-list li')).toHaveCount(0);

    // Try to add whitespace-only todo
    await page.fill('.new-todo', '   ');
    await page.press('.new-todo', 'Enter');

    // Check that no todo was added
    await expect(page.locator('.todo-list li')).toHaveCount(0);
  });

  test('should trim whitespace from todo text', async ({ page }) => {
    const todoText = '  Todo with spaces  ';
    const trimmedText = 'Todo with spaces';

    // Add a todo with leading/trailing spaces
    await page.fill('.new-todo', todoText);
    await page.press('.new-todo', 'Enter');

    // Check that the text is trimmed
    await expect(page.locator('.todo-list li label')).toHaveText(trimmedText);
  });

  test('should show correct item count with singular/plural', async ({ page }) => {
    // No todos - footer should be hidden
    await expect(page.locator('.footer')).not.toBeVisible();

    // Add one todo
    await page.fill('.new-todo', 'Single todo');
    await page.press('.new-todo', 'Enter');
    await expect(page.locator('.todo-count')).toHaveText('1 item left');

    // Add another todo
    await page.fill('.new-todo', 'Second todo');
    await page.press('.new-todo', 'Enter');
    await expect(page.locator('.todo-count')).toHaveText('2 items left');

    // Complete one todo
    await page.click('.todo-list li:first-child .toggle');
    await expect(page.locator('.todo-count')).toHaveText('1 item left');
  });

  test('should handle keyboard navigation', async ({ page }) => {
    // Add a todo
    await page.fill('.new-todo', 'Test todo');
    await page.press('.new-todo', 'Enter');

    // Test Tab navigation
    await page.keyboard.press('Tab'); // Should focus on toggle checkbox
    await page.keyboard.press('Space'); // Should toggle the todo

    // Check that the todo was toggled
    await expect(page.locator('.todo-list li')).toHaveClass(/completed/);
  });
});
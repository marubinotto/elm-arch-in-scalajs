# TodoMVC HTML Structure and CSS Integration

This document describes the HTML structure and CSS integration for the TodoMVC Elm Architecture implementation.

## HTML Structure

### Main Container
The application uses the standard TodoMVC HTML structure:

```html
<section class="todoapp" id="todoapp" role="main" aria-label="Todo application">
  <!-- Application content -->
</section>
```

### Key Elements

#### Loading State
- **Element**: `<div id="loading" class="loading">`
- **Purpose**: Shows loading indicator while the application initializes
- **Accessibility**: Uses `aria-live="polite"` for screen reader announcements

#### Error State  
- **Element**: `<div id="error" class="error" role="alert">`
- **Purpose**: Displays error messages if the application fails to load
- **Accessibility**: Uses `role="alert"` and `aria-live="assertive"`

#### Application Mount Point
- **Element**: `<div id="app">`
- **Purpose**: Container where the Scala.js application renders the TodoMVC interface

#### Footer
- **Element**: `<footer class="info" role="contentinfo">`
- **Purpose**: Contains TodoMVC credits and instructions
- **Content**: Standard TodoMVC footer requirements

## CSS Integration

### Official TodoMVC CSS
The application includes the official TodoMVC CSS from CDN:
```html
<link rel="stylesheet" href="https://unpkg.com/todomvc-app-css@2.4.1/index.css">
```

### Custom CSS Enhancements
Additional custom CSS provides:

#### Loading and Error States
- Animated loading spinner
- Error message styling with proper contrast
- Smooth transitions between states

#### Accessibility Features
- Enhanced focus indicators with blue outline and shadow
- Screen reader only content with `.sr-only` class
- High contrast mode support
- Reduced motion support for users with vestibular disorders

#### Responsive Design
- Mobile-friendly adjustments for screens under 430px
- Print styles that hide loading/error states
- Dark mode support using `prefers-color-scheme`

#### Visual Enhancements
- Smooth opacity transitions for app loading
- Hover effects for better user feedback
- Animation classes for todo item transitions
- Enhanced visual states for completed todos

## Required CSS Classes

The following CSS classes are required for TodoMVC compatibility:

### Core Structure
- `.todoapp` - Main application container
- `.header` - Header section with new todo input
- `.main` - Main section with todo list
- `.footer` - Footer with filters and stats

### Todo List
- `.todo-list` - Container for todo items
- `.view` - Todo item view mode
- `.edit` - Todo item edit mode
- `.toggle` - Individual todo checkbox
- `.toggle-all` - Toggle all todos checkbox
- `.destroy` - Delete todo button

### Input Fields
- `.new-todo` - New todo input field
- `.edit` - Edit todo input field

### Footer Elements
- `.todo-count` - Active todo count display
- `.filters` - Filter button container
- `.clear-completed` - Clear completed button

### Information
- `.info` - Footer information section

## Accessibility Features

### ARIA Labels and Roles
- `role="main"` on main application container
- `role="contentinfo"` on footer
- `role="alert"` on error messages
- `aria-live` regions for dynamic content announcements
- `aria-label` for application description

### Keyboard Navigation
- Enhanced focus indicators with 2px blue outline
- Focus offset for better visibility
- Box shadow for additional focus emphasis

### Screen Reader Support
- `.sr-only` class for screen reader only content
- Live regions for status announcements
- Proper semantic HTML structure

### Responsive and Accessible Design
- High contrast mode support
- Reduced motion support
- Mobile-friendly responsive design
- Print stylesheet optimization

## JavaScript Integration

### Loading Management
The HTML includes JavaScript for managing application states:

```javascript
// Show application when loaded
window.showApp = function() {
  // Hide loading, show app, add loaded class
};

// Show error state
window.showError = function(message) {
  // Hide loading, show error with message
};
```

### Error Handling
- Global error event listeners
- Unhandled promise rejection handling
- 10-second loading timeout
- Screen reader announcements for state changes

## Browser Compatibility

The HTML structure and CSS are designed to work with:
- Modern browsers supporting ES6 modules
- Screen readers and assistive technologies
- High contrast and reduced motion preferences
- Mobile devices and touch interfaces
- Print media

## Validation

The HTML structure follows:
- HTML5 semantic standards
- WCAG 2.1 accessibility guidelines
- TodoMVC specification requirements
- Progressive enhancement principles

## File Structure

```
├── index.html              # Main HTML file
├── styles/
│   └── custom.css          # Custom CSS enhancements
└── docs/
    └── html-structure.md   # This documentation
```

## Testing

The HTML structure is validated through:
- `HtmlIntegrationSpec.scala` - Tests for required elements and classes
- Manual accessibility testing with screen readers
- Cross-browser compatibility testing
- Mobile responsiveness testing
- Print stylesheet verification